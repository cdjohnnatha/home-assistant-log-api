output "instance_public_ip" {
  description = "Public IP address of the EC2 instance"
  value       = aws_instance.api_server.public_ip
}

output "instance_public_dns" {
  description = "Public DNS name of the EC2 instance"
  value       = aws_instance.api_server.public_dns
}

output "api_url" {
  description = "URL to access your API"
  value       = "http://${aws_instance.api_server.public_ip}:8080"
}

output "api_health_check" {
  description = "Health check URL for your API"
  value       = "http://${aws_instance.api_server.public_ip}:8080/api/v1/events/health"
}

output "ssh_connection" {
  description = "SSH connection command"
  value       = "ssh -i ~/.ssh/id_rsa ec2-user@${aws_instance.api_server.public_ip}"
}

output "iam_role_arn" {
  description = "ARN of the IAM role used by EC2"
  value       = aws_iam_role.ec2_sns_role.arn
}

output "budget_name" {
  description = "Name of the AWS Budget for cost monitoring"
  value       = aws_budgets_budget.monthly_cost_budget.name
}

output "budget_limit" {
  description = "Monthly budget limit in USD"
  value       = "$${aws_budgets_budget.monthly_cost_budget.limit_amount} USD"
}

output "cost_alert_info" {
  description = "Cost monitoring information"
  value = <<-EOT
    Budget created: ${aws_budgets_budget.monthly_cost_budget.name}
    Monthly limit: $22 USD
    Alert at 80%: $17.60 USD (actual costs)
    Alert at 100%: $22 USD (forecasted costs)
    Notifications: Via SNS topic
  EOT
}
