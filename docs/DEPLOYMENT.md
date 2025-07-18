# 🚀 Deployment Guide

Complete guide for deploying the Home Assistant Log API to AWS using Terraform and Docker.

## 📋 Table of Contents

- [Prerequisites](#-prerequisites)
- [Quick Deploy](#-quick-deploy)
- [Infrastructure Overview](#-infrastructure-overview)
- [AWS Architecture](#-aws-architecture)
- [EC2 Management](#-ec2-management)
- [Infrastructure Components](#-infrastructure-components)
- [Configuration Management](#-configuration-management)
- [Docker & Containerization](#-docker--containerization)
- [Production Deployment](#-production-deployment)
- [Cost Management](#-cost-management)
- [Advanced Configurations](#-advanced-configurations)

---

## 🛠️ Prerequisites

### **Required Tools:**
```bash
# Install Terraform
brew install terraform  # macOS
# or download from: https://terraform.io/downloads

# Install AWS CLI
brew install awscli     # macOS
aws configure           # Set credentials

# Verify installations
terraform --version     # Should be v1.0+
aws --version          # Should be v2.0+
docker --version       # Should be v20.0+
```

### **AWS Requirements:**
- **AWS Account** with administrative access
- **AWS CLI configured** with your credentials
- **SNS Topic** created for notifications
- **SSH Key Pair** for EC2 access

### **Cost Estimation:**
- **t3.small instance**: ~$17/month
- **EBS storage**: ~$1/month
- **Data transfer**: ~$1/month
- **SNS**: ~$0.10/month
- **Total**: ~$19/month

---

## ⚡ Quick Deploy

### **1. Clone and Configure**
```bash
# Clone repository
git clone https://github.com/YOUR-USERNAME/home-assistant-log-api.git
cd home-assistant-log-api

# Setup Terraform configuration
cd terraform
cp terraform.tfvars.example terraform.tfvars
```

### **2. Edit Configuration**
```bash
# Edit terraform.tfvars with your details
vi terraform.tfvars
```

**Required variables:**
```hcl
# AWS Configuration
aws_region = "us-east-1"
aws_profile = "default"

# Project Configuration
project_name = "home-assistant-log-api"
environment = "dev"

# Instance Configuration (IMPORTANT: Use t3.small for sufficient memory)
instance_type = "t3.small"

# Network Configuration
allowed_ssh_cidr = "YOUR.IP.ADDRESS.HERE/32"  # Get from https://checkip.amazonaws.com
allowed_http_cidr = "0.0.0.0/0"

# SNS Configuration
sns_topic_arn = "arn:aws:sns:us-east-1:123456789012:your-topic-name"
```

### **3. Deploy Infrastructure**
```bash
# Initialize Terraform
terraform init

# Review deployment plan
terraform plan

# Deploy infrastructure (takes 3-5 minutes)
terraform apply
```

### **4. Get Connection Details**
```bash
# Get instance IP and connection info
terraform output

# Expected output:
# instance_ip = "54.123.45.67"
# ssh_connection = "ssh -i ~/.ssh/id_rsa ec2-user@54.123.45.67"
# health_check_url = "http://54.123.45.67:8080/api/v1/events/health"
```

### **5. Verify Deployment**
```bash
# Wait 3-5 minutes for application build, then test
curl -f http://$(terraform output -raw instance_ip):8080/api/v1/events/health

# Expected response: "Ok"
```

---

## 🏗️ Infrastructure Overview

The deployment creates a complete AWS infrastructure optimized for a Spring Boot application:

### **Core Components:**

| Component | Purpose | Configuration |
|-----------|---------|---------------|
| **EC2 Instance** | Application server | t3.small (2GB RAM) |
| **Security Group** | Firewall rules | SSH (22) + HTTP (8080) |
| **IAM Role** | AWS permissions | SNS publish access |
| **EBS Volume** | Persistent storage | 20GB gp3 |
| **Elastic IP** | Static IP address | Auto-assigned |

### **Automated Setup Process:**

The [`user_data.sh`](../terraform/user_data.sh) script automatically:

1. **System Setup**: Updates packages, installs Docker
2. **Application Deployment**: Clones repo, builds container
3. **Service Configuration**: Creates systemd service for auto-restart
4. **Management Tools**: Installs [`manage-api.sh`](../terraform/user_data.sh#L130) script

---

## 🏛️ AWS Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                  AWS Cloud                                      │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                              VPC (Default)                              │   │
│  │                                                                         │   │
│  │    ┌─────────────────────────────────────────────────────────────┐     │   │
│  │    │                    Public Subnet                            │     │   │
│  │    │                                                             │     │   │
│  │    │  ┌─────────────────┐         ┌─────────────────────────┐   │     │   │
│  │    │  │   EC2 Instance  │◄────────┤    Security Group       │   │     │   │
│  │    │  │    t3.small     │         │   SSH (22) + HTTP (8080)│   │     │   │
│  │    │  │    2GB RAM      │         └─────────────────────────┘   │     │   │
│  │    │  └─────────────────┘                                       │     │   │
│  │    │           │                                                 │     │   │
│  │    └───────────┼─────────────────────────────────────────────────┘     │   │
│  └────────────────┼───────────────────────────────────────────────────────┘   │
│                   │                                                           │
│  ┌────────────────┼─────────────┐  ┌─────────────────────────────────────┐   │
│  │      Storage   │             │  │             IAM                     │   │
│  │  ┌─────────────▼───────────┐ │  │  ┌─────────────────────────────────┐│   │
│  │  │     EBS Volume          │ │  │  │         IAM Role                ││   │
│  │  │      20GB gp3           │ │  │  │      SNS Publish                ││   │
│  │  │   (Persistent Data)     │ │  │  │       Permissions               ││   │
│  │  └─────────────────────────┘ │  │  └─────────────┬───────────────────┘│   │
│  └─────────────────────────────┘  └────────────────┼────────────────────┘   │
│                   │                                 │                        │
│  ┌────────────────┼─────────────┐  ┌────────────────▼───────────────────┐   │
│  │    Network     │             │  │          Notifications              │   │
│  │  ┌─────────────▼───────────┐ │  │  ┌─────────────────────────────────┐│   │
│  │  │     Elastic IP          │ │  │  │          SNS Topic              ││   │
│  │  │   Static Address        │ │  │  │      Home Assistant             ││   │
│  │  │   (Public Access)       │ │  │  │       Notifications             ││   │
│  │  └─────────────┬───────────┘ │  │  └─────────────▲───────────────────┘│   │
│  └────────────────┼─────────────┘  └────────────────┼────────────────────┘   │
│                   │                                 │                        │
└───────────────────┼─────────────────────────────────┼────────────────────────┘
                    │                                 │
              ┌─────▼─────┐                          │
              │ Internet  │                          │
              │  Gateway  │                          │
              └─────┬─────┘                          │
                    │                                │
┌───────────────────┼────────────────────────────────┼────────────────────┐
│               External Users                       │                    │
│                   │                                │                    │
│  ┌────────────────▼──────────┐        ┌───────────▼──────────────────┐ │
│  │       Developer           │        │      Home Assistant          │ │
│  │   SSH/HTTP Access         │        │      API Calls               │ │
│  │   Port 22, 8080           │        │   Event Logging              │ │
│  └───────────────────────────┘        └──────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘

Data Flow:
┌─────────────┐    HTTP/8080    ┌─────────────┐    SNS Publish    ┌─────────────┐
│Home Assistant│───────────────▶│EC2 Instance │──────────────────▶│ SNS Topic   │
└─────────────┘                 └─────────────┘                   └─────────────┘
                                       │                                  │
                               ┌───────▼────────┐                        │
                               │  Spring Boot   │                        │
                               │      API       │                        │
                               │ (Logs Events)  │                        │
                               └────────────────┘                        │
                                                                         │
                                                                ┌────────▼─────────┐
                                                                │  Notifications   │
                                                                │ (Email/SMS/etc.) │
                                                                └──────────────────┘
```

### **Security Model:**

- **Network**: Security groups restrict access to your IP
- **Authentication**: EC2 uses SSH keys, AWS uses IAM roles  
- **Authorization**: IAM role has minimal SNS-only permissions
- **Encryption**: All traffic uses HTTPS/TLS when possible

---

## 🖥️ EC2 Management

### **SSH Access:**
```bash
# Connect to instance
ssh -i ~/.ssh/id_rsa ec2-user@$(terraform output -raw instance_ip)

# Or use convenience output
$(terraform output -raw ssh_connection)
```

### **Application Management:**
```bash
# On EC2 instance - use the management script
./manage-api.sh status    # Check application status
./manage-api.sh logs      # View real-time logs
./manage-api.sh restart   # Restart application
./manage-api.sh update    # Pull latest code and rebuild
./manage-api.sh test      # Test API with sample data
```

### **System Monitoring:**
```bash
# Check system resources
free -h              # Memory usage
df -h               # Disk usage
docker stats        # Container resource usage
htop                # Process monitor

# Check application health
curl http://localhost:8080/api/v1/events/health
```

### **Log Locations:**
```bash
# Deployment logs (from user_data.sh)
sudo tail -f /var/log/user-data.log

# Application logs
docker-compose logs -f logs-api

# System logs
sudo journalctl -u home-assistant-log-api -f
```

---

## 🏗️ Infrastructure Components

### **EC2 Instance Configuration**

The Terraform configuration creates an optimized EC2 instance:

```hcl
# Key configuration from main.tf
resource "aws_instance" "api_server" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = var.instance_type
  
  # Security
  vpc_security_group_ids = [aws_security_group.api_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_sns_profile.name
  key_name              = var.key_name
  
  # Storage
  root_block_device {
    volume_type = "gp3"
    volume_size = 20
    encrypted   = true
  }
  
  # Automated setup
  user_data = base64encode(templatefile("user_data.sh", {
    aws_region    = var.aws_region
    sns_topic_arn = var.sns_topic_arn
  }))
}
```

### **Security Group Rules**

**Inbound Rules:**
- **SSH (22)**: Your IP only (for management)
- **HTTP (8080)**: Public access (for API)

**Outbound Rules:**
- **All traffic**: Unrestricted (for updates, Docker pulls, SNS)

### **IAM Permissions**

The instance has minimal permissions for security:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "arn:aws:sns:*:*:*"
    }
  ]
}
```

---

## ⚙️ Configuration Management

Configuration is managed through multiple layers for flexibility and security:

### **Environment-Specific Configurations:**

| Environment | Profile | Configuration | Use Case |
|-------------|---------|---------------|----------|
| **Development** | `dev` | [`application.yml`](../logs-api/src/main/resources/application.yml) + local env vars | Local development |
| **Testing** | `test` | [`application-test.yml`](../logs-api/src/main/resources/application-test.yml) + mocked services | Unit/Integration tests |
| **Production** | `prod` | [`application-prod.yml`](../logs-api/src/main/resources/application-prod.yml) + IAM roles | EC2 deployment |

### **Configuration Hierarchy:**

1. **Application Properties** (in JAR)
2. **Environment Variables** (`.env` file)
3. **AWS IAM Roles** (for credentials)
4. **Terraform Variables** (for infrastructure)

### **Environment Variables:**

The [`user_data.sh`](../terraform/user_data.sh) creates a `.env` file with optimized settings:

```bash
# Generated .env file on EC2
SPRING_PROFILES_ACTIVE=prod
AWS_REGION=us-east-1
AWS_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:your-topic
JAVA_OPTS=-Xmx1024m -Xms512m
SERVER_PORT=8080
```

---

## 🐳 Docker & Containerization

### **Multi-Stage Dockerfile**

Our optimized [`Dockerfile`](../Dockerfile) uses multi-stage builds for efficiency:

**Build Stage:**
- Uses full JDK for Gradle compilation
- Copies source code and builds Spring Boot JAR
- Optimized layer caching

**Runtime Stage:**
- Uses lightweight JRE (smaller image)
- Includes curl for health checks
- Optimized JVM settings for containers

### **Docker Compose Configuration**

The [`docker-compose.yml`](../docker-compose.yml) file defines:

**Service Configuration:**
- **Port mapping**: 8080 (host) → 8080 (container)
- **Environment**: Loads from `.env` file (see [`.envrc`](../.envrc) for local development)
- **Restart policy**: Unless manually stopped
- **Health checks**: Automatic API endpoint monitoring

### **Container Commands**

```bash
# Build and start
docker-compose up -d --build

# View logs
docker-compose logs -f logs-api

# Health check
docker-compose exec logs-api curl http://localhost:8080/api/v1/events/health

# Resource monitoring
docker stats
```

### **Memory Optimization**

Container memory is optimized for different instance types:

| Instance Type | RAM | JVM Settings | Result |
|---------------|-----|--------------|--------|
| **t3.micro** | 1GB | `-Xmx512m -Xms256m` | ❌ Insufficient for builds |
| **t3.small** | 2GB | `-Xmx1024m -Xms512m` | ✅ Optimal for production |
| **t3.medium** | 4GB | `-Xmx2048m -Xms1024m` | ✅ High performance |

---

## 🌐 Production Deployment

### **Pre-deployment Checklist:**

- [ ] AWS credentials configured
- [ ] SNS topic created and ARN updated
- [ ] SSH key pair available
- [ ] `terraform.tfvars` configured with your values
- [ ] Current IP address added to `allowed_ssh_cidr`

### **Deployment Steps:**

```bash
# 1. Initialize Terraform (first time only)
terraform init

# 2. Validate configuration
terraform validate
terraform plan

# 3. Deploy infrastructure
terraform apply

# 4. Wait for deployment (3-5 minutes)
# Monitor deployment progress:
ssh -i ~/.ssh/id_rsa ec2-user@$(terraform output -raw instance_ip)
tail -f /var/log/user-data.log

# 5. Verify deployment
curl -f http://$(terraform output -raw instance_ip):8080/api/v1/events/health
```

### **Post-deployment Verification:**

```bash
# Test API functionality
curl -X POST http://$(terraform output -raw instance_ip):8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "source": "home-assistant",
    "eventType": "USER_ACTION", 
    "payload": {"test": "deployment"}
  }'

# Check SNS notification was sent
# (Check your SNS topic subscriptions)
```

### **Monitoring Setup:**

```bash
# Enable CloudWatch monitoring (optional)
aws logs create-log-group --log-group-name /aws/ec2/home-assistant-log-api

# Set up billing alerts
aws ce put-anomaly-detector \
  --anomaly-detector Type=DIMENSIONAL,DimensionKey=SERVICE,MatchOptions=EQUALS,Values=AmazonEC2
```

---

## 💰 Cost Management

### **Monthly Cost Breakdown:**

| Service | Resource | Monthly Cost |
|---------|----------|--------------|
| **EC2** | t3.small instance | ~$17.00 |
| **EBS** | 20GB gp3 storage | ~$1.60 |
| **Data Transfer** | Outbound traffic | ~$1.00 |
| **SNS** | Notifications | ~$0.10 |
| **Elastic IP** | Static IP (when attached) | $0.00 |
| **Total** | | **~$19.70** |

### **Automated Cost Monitoring:**

✅ **Terraform automatically creates an AWS Budget with:**
- **Monthly Limit**: $22 USD (110% of expected costs)
- **Alert at 80%**: $17.60 USD (actual spending)  
- **Alert at 100%**: $22 USD (forecasted spending)
- **Notifications**: Via SNS topic (same as API events)
- **Filtered by**: Project tags (`Project=home-assistant-log-api`)

**Why This Works:**
- Monitors ALL AWS services used by your project
- Uses tag-based filtering for precise cost tracking
- Integrates with existing SNS notifications
- Automatically created and managed by Terraform

### **Cost Optimization Strategies:**

**1. Instance Right-sizing:**
```bash
# For development, use smaller instance
instance_type = "t3.micro"  # ~$8.50/month (may fail builds)

# For production, current t3.small is optimal
instance_type = "t3.small"  # ~$17.00/month (recommended)
```

**2. Scheduled Scaling:**
```bash
# Stop instance during off-hours (saves ~70% on compute)
# Keep data with EBS volume (~$1.60/month storage only)
terraform destroy  # Destroys everything
terraform apply    # Recreates from scratch
```

**3. Reserved Instances:**
```bash
# For long-term use (1+ years), consider Reserved Instances
# Can save 30-60% on EC2 costs
# Configure through AWS Console
```

**4. Cost Monitoring:**
```bash
# Set up billing alerts
aws budgets create-budget \
  --account-id YOUR-ACCOUNT-ID \
  --budget file://budget.json

# Monitor costs
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost
```

---

## 🔧 Advanced Configurations

### **Multi-Environment Setup**

Create separate environments with workspace isolation:

```bash
# Create staging environment
cp terraform.tfvars terraform-staging.tfvars
terraform workspace new staging
terraform apply -var-file="terraform-staging.tfvars"

# Create production environment
cp terraform.tfvars terraform-prod.tfvars
terraform workspace new production
terraform apply -var-file="terraform-prod.tfvars"

# Switch between environments
terraform workspace select staging
terraform workspace select production
```

### **Custom Domain Setup**

Add a custom domain with Route 53:

```hcl
# Add to main.tf
resource "aws_route53_record" "api" {
  zone_id = "Z123456789"  # Your hosted zone ID
  name    = "api.yourdomain.com"
  type    = "A"
  ttl     = "300"
  records = [aws_eip.api_eip.public_ip]
}

# SSL certificate with Let's Encrypt
resource "aws_acm_certificate" "api" {
  domain_name       = "api.yourdomain.com"
  validation_method = "DNS"
}
```

### **Load Balancer Setup**

For high availability, add an Application Load Balancer:

```hcl
# Add to main.tf for production scaling
resource "aws_lb" "api_lb" {
  name               = "${var.project_name}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets           = data.aws_subnets.default.ids

  tags = local.common_tags
}

resource "aws_lb_target_group" "api_tg" {
  name     = "${var.project_name}-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id

  health_check {
    path                = "/api/v1/events/health"
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}
```

### **Database Integration**

Add RDS for persistent data storage:

```hcl
# Add to main.tf for database needs
resource "aws_db_instance" "api_db" {
  allocated_storage    = 20
  storage_type         = "gp3"
  engine              = "postgres"
  engine_version      = "15.4"
  instance_class      = "db.t3.micro"
  db_name             = "homeassistant"
  username            = "apiuser"
  manage_master_user_password = true
  
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  skip_final_snapshot    = true
  
  tags = local.common_tags
}
```

### **Monitoring and Alerting**

Add CloudWatch monitoring:

```hcl
# Add to main.tf for monitoring
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "${var.project_name}-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "120"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ec2 cpu utilization"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.api_server.id
  }
}
```

---

## 🔄 CI/CD Pipeline

For automated deployments, create a GitHub Actions workflow file at `.github/workflows/deploy.yml`. 

**Key components:**
- **Trigger**: Push to main branch
- **Actions**: Checkout code, setup Terraform, apply infrastructure
- **Security**: Use GitHub Secrets for AWS credentials

See the [GitHub Actions documentation](https://docs.github.com/en/actions) for detailed workflow examples.

### **GitHub Secrets Required:**

```bash
# Add these secrets to your GitHub repository
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
TF_VAR_sns_topic_arn=arn:aws:sns:...
TF_VAR_allowed_ssh_cidr=YOUR.IP.ADDRESS/32
```

---

## 🆘 Emergency Procedures

### **Disaster Recovery:**

```bash
# 1. Backup current state
terraform show > backup-$(date +%Y%m%d).txt
cp terraform.tfstate terraform.tfstate.backup

# 2. Emergency rebuild
terraform destroy
terraform apply

# 3. Restore from git
ssh ec2-user@NEW-IP
cd home-assistant-log-api
git pull
./manage-api.sh update
```

### **Rollback Strategy:**

```bash
# Rollback infrastructure
terraform plan -destroy
terraform destroy

# Rollback application
ssh ec2-user@INSTANCE-IP
cd home-assistant-log-api
git checkout PREVIOUS-COMMIT
./manage-api.sh update
```

---

## 📚 Additional Resources

- **🏗️ Terraform AWS Provider**: [Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- **🐳 Docker Best Practices**: [Docker documentation](https://docs.docker.com/develop/dev-best-practices/)
- **🍃 Spring Boot Production**: [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- **☁️ AWS Best Practices**: [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- **🔒 Security**: [AWS Security Best Practices](https://aws.amazon.com/security/security-resources/)

---

*For troubleshooting issues, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)*
*For API documentation, see the main [README.md](../README.md)* 