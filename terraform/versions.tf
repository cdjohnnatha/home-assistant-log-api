terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.67"
    }
  }
}

# Configure the AWS Provider
provider "aws" {
  region = var.aws_region

  # Use your local AWS CLI credentials
  # (same as you use for testing SNS locally)
}
