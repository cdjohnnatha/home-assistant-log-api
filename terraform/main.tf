# terraform/main.tf

# Data source to get available AZs
data "aws_availability_zones" "available" {
  state = "available"
}

# Data source to get your current IP
data "http" "my_ip" {
  url = "https://checkip.amazonaws.com"
}

locals {
  # Clean up the IP response (remove newline)
  my_current_ip = chomp(data.http.my_ip.response_body)
  
  # Common tags for all resources
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}

# VPC - Your private network in AWS
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-vpc"
  })
}

# Internet Gateway - Allows internet access
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-igw"
  })
}

# Public Subnet - Where your EC2 will live
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-public-subnet"
    Type = "Public"
  })
}

# Route Table - Defines network routing
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-public-rt"
  })
}

# Associate Route Table with Subnet
resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Security Group for EC2 instance
resource "aws_security_group" "api_server" {
  name_prefix = "${var.project_name}-api-"
  vpc_id      = aws_vpc.main.id
  description = "Security group for Home Assistant Log API server"

  # Allow HTTP traffic on port 8080 from anywhere (for your API)
  ingress {
    description = "API access"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Allow from anywhere
  }

  # Allow SSH access only from your IP
  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["${var.my_ip}/32"]  # Only your IP
  }

  # Allow all outbound traffic (for Docker pulls, SNS calls, etc.)
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-api-sg"
  })
}

# IAM Role for EC2 instance (allows SNS access without hardcoded credentials)
resource "aws_iam_role" "ec2_sns_role" {
  name = "${var.project_name}-ec2-sns-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-ec2-sns-role"
  })
}

# IAM Policy for SNS access
resource "aws_iam_policy" "sns_publish_policy" {
  name        = "${var.project_name}-sns-publish-policy"
  description = "Policy to allow publishing to SNS topic"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = var.sns_topic_arn
      }
    ]
  })

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-sns-publish-policy"
  })
}

# Attach policy to role
resource "aws_iam_role_policy_attachment" "ec2_sns_policy_attachment" {
  role       = aws_iam_role.ec2_sns_role.name
  policy_arn = aws_iam_policy.sns_publish_policy.arn
}

# Instance profile (allows EC2 to use the IAM role)
resource "aws_iam_instance_profile" "ec2_sns_profile" {
  name = "${var.project_name}-ec2-sns-profile"
  role = aws_iam_role.ec2_sns_role.name

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-ec2-sns-profile"
  })
}

# Get the latest Amazon Linux 2 AMI
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# Key pair for SSH access
resource "aws_key_pair" "main" {
  key_name   = "${var.project_name}-key"
  public_key = file("~/.ssh/id_rsa.pub")  # Your SSH public key

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-key"
  })
}

# EC2 Instance
resource "aws_instance" "api_server" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  key_name              = aws_key_pair.main.key_name
  vpc_security_group_ids = [aws_security_group.api_server.id]
  subnet_id             = aws_subnet.public.id
  iam_instance_profile  = aws_iam_instance_profile.ec2_sns_profile.name

  # User data script to set up Docker and your application
  user_data = base64encode(templatefile("${path.module}/user_data.sh", {
    aws_region    = var.aws_region
    sns_topic_arn = var.sns_topic_arn
  }))

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-api-server"
  })
}

# Cost Management & Monitoring
# AWS Budget for cost control - monitors monthly spending
resource "aws_budgets_budget" "monthly_cost_budget" {
  name         = "${var.project_name}-monthly-budget"
  budget_type  = "COST"
  limit_amount = "22"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"
  time_period_start = "2025-01-01_00:00"

  cost_filter {
    name = "TagKeyValue"
    values = ["Project$${var.project_name}"]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 80
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_email_addresses = []
    subscriber_sns_topic_arns  = [var.sns_topic_arn]
  }

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 100
    threshold_type            = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = []
    subscriber_sns_topic_arns  = [var.sns_topic_arn]
  }

  depends_on = [aws_instance.api_server]
}
