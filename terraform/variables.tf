# terraform/variables.tf

variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Name of the project (used for tagging)"
  type        = string
  default     = "home-assistant-log-api"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro" # Free tier eligible
}

variable "my_ip" {
  description = "Your IP address for SSH access (get from https://checkip.amazonaws.com)"
  type        = string
  # We'll set this in terraform.tfvars
}

variable "sns_topic_arn" {
  description = "SNS Topic ARN for notifications"
  type        = string
  # We'll set this in terraform.tfvars
}

variable "ssh_public_key" {
  description = "SSH public key for EC2 access"
  type        = string
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC7... # Replace with your actual public key"
  # Set this in terraform.tfvars or export TF_VAR_ssh_public_key
}