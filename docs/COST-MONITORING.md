# 💰 Cost Monitoring Guide

Complete guide for understanding and managing AWS costs for the Home Assistant Log API project.

## 📊 Automated Budget Alert System

### **What Gets Created Automatically:**

When you run `terraform apply`, an AWS Budget is automatically created with:

```
Budget Name: home-assistant-log-api-monthly-budget
Monthly Limit: $22 USD
Alert Thresholds:
  - 80% ($17.60) → Actual costs
  - 100% ($22.00) → Forecasted costs
Notifications: Via your SNS topic
```

### **How It Works:**

1. **Tag-Based Filtering**: Only monitors resources tagged with `Project=home-assistant-log-api`
2. **All Services**: Tracks costs across ALL AWS services, not just EC2
3. **Real-time Alerts**: Notifications sent via SNS (same topic as API events)
4. **Forecasting**: Alerts before you hit the limit based on usage trends

---

## 🔍 Verifying Your Budget

### **Check Budget in AWS Console:**

1. **Login to AWS Console** → Billing & Cost Management → Budgets
2. **Find**: `home-assistant-log-api-monthly-budget`
3. **Verify**: 
   - Limit: $22 USD
   - Filters: `TagKeyValue = Project$home-assistant-log-api`
   - Notifications: Your SNS topic

### **Test Budget via Terraform Output:**

```bash
# Get budget information
terraform output cost_alert_info

# Expected output:
# Budget created: home-assistant-log-api-monthly-budget
# Monthly limit: $22 USD
# Alert at 80%: $17.60 USD (actual costs)
# Alert at 100%: $22 USD (forecasted costs)
# Notifications: Via SNS topic
```

---

## 📈 Understanding Your Costs

### **Expected Monthly Breakdown:**

| Service | Resource | Estimated Cost |
|---------|----------|----------------|
| **EC2** | t3.small instance (24/7) | ~$17.00 |
| **EBS** | 20GB root volume | ~$1.60 |
| **VPC** | Data transfer | ~$1.00 |
| **SNS** | Notifications | ~$0.10 |
| **CloudWatch** | Logs (basic) | ~$0.50 |
| **AWS Budgets** | 1 budget (free tier) | $0.00 |
| **🔶 Total** | | **~$20.20** |

### **Why $22 Budget Limit:**

- **Expected costs**: ~$20.20
- **Safety margin**: +10% = $22.22
- **Rounded down**: $22.00
- **Alert triggers**: Before hitting the limit

---

## 🚨 When You Get Alerts

### **80% Alert ($17.60) - Actual Costs:**

**What it means:** You've actually spent $17.60 this month

**Action needed:**
```bash
# 1. Check current usage
aws ce get-cost-and-usage \
  --time-period Start=2025-01-01,End=2025-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost

# 2. Review EC2 instance status
terraform show | grep instance_state

# 3. Check for unexpected resources
aws resourcegroupstaggingapi get-resources \
  --tag-filters Key=Project,Values=home-assistant-log-api
```

### **100% Alert ($22.00) - Forecasted Costs:**

**What it means:** Based on current usage, AWS predicts you'll spend $22+ this month

**Immediate actions:**
1. **Review usage patterns**: Are you running unnecessary services?
2. **Check instance type**: Is t3.small really needed?
3. **Temporary shutdown**: `terraform destroy` to stop costs immediately

---

## 🎛️ Cost Control Commands

### **Stop All Resources (Zero Cost):**
```bash
# Destroys everything - use carefully!
terraform destroy

# Confirm when prompted
# Result: ~$0/month (only storage costs remain)
```

### **Downgrade Instance (Reduce Costs):**
```bash
# Edit terraform.tfvars
instance_type = "t3.micro"  # $8.50/month vs $17/month

# Apply changes
terraform apply

# Result: ~$11/month (but may fail during builds)
```

### **Temporary Shutdown (Keep Data):**
```bash
# Stop instance via AWS Console or CLI
aws ec2 stop-instances --instance-ids $(terraform output -raw instance_id)

# Result: ~$2/month (only storage costs)
# Note: Manual restart required
```

---

## 📊 Monitoring Commands

### **Check Current Month Costs:**
```bash
# Get cost breakdown
aws ce get-cost-and-usage \
  --time-period Start=$(date +%Y-%m-01),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics BlendedCost \
  --group-by Type=DIMENSION,Key=SERVICE

# Human-readable daily costs
aws ce get-cost-and-usage \
  --time-period Start=$(date +%Y-%m-01),End=$(date +%Y-%m-%d) \
  --granularity DAILY \
  --metrics BlendedCost \
  --query 'ResultsByTime[*].[TimePeriod.Start,Total.BlendedCost.Amount]' \
  --output table
```

### **Check Budget Status:**
```bash
# List all budgets
aws budgets describe-budgets --account-id $(aws sts get-caller-identity --query Account --output text)

# Get specific budget details
aws budgets describe-budget \
  --account-id $(aws sts get-caller-identity --query Account --output text) \
  --budget-name "home-assistant-log-api-monthly-budget"
```

### **Resource Usage Tracking:**
```bash
# Find all project resources
aws resourcegroupstaggingapi get-resources \
  --tag-filters Key=Project,Values=home-assistant-log-api \
  --query 'ResourceTagMappingList[*].[ResourceARN,Tags[?Key==`Name`].Value]' \
  --output table

# Check EC2 instance details
aws ec2 describe-instances \
  --filters "Name=tag:Project,Values=home-assistant-log-api" \
  --query 'Reservations[*].Instances[*].[InstanceId,InstanceType,State.Name,PublicIpAddress]' \
  --output table
```

---

## ⚙️ Advanced Cost Management

### **Set Up Cost Anomaly Detection:**
```bash
# Create anomaly detector (optional)
aws ce create-anomaly-detector \
  --anomaly-detector Type=DIMENSIONAL,DimensionKey=SERVICE \
  --monitor Type=DIMENSIONAL,DimensionKey=SERVICE,MatchOptions=EQUALS,Values=AmazonEC2
```

### **Custom Budget Modifications:**

To modify the budget after deployment, edit `terraform/main.tf`:

```hcl
resource "aws_budgets_budget" "monthly_cost_budget" {
  # Change limit
  limit_amount = "30"  # Increase to $30

  # Add email notifications
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                 = 50  # Alert at 50%
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_email_addresses = ["your-email@domain.com"]
    subscriber_sns_topic_arns  = [var.sns_topic_arn]
  }
}
```

### **Multi-Environment Budgets:**

For separate dev/staging/prod budgets:

```hcl
# Create environment-specific budgets
resource "aws_budgets_budget" "env_budget" {
  name         = "${var.project_name}-${var.environment}-budget"
  limit_amount = var.environment == "prod" ? "50" : "25"
  
  cost_filter {
    name = "TagKeyValue"
    values = ["Environment$${var.environment}"]
  }
}
```

---

## 🚦 Budget Alert Response Playbook

### **Level 1: 50-79% of Budget**
- ✅ **Action**: Monitor only
- 📊 **Check**: Weekly cost review
- 🎯 **Goal**: Stay under 80%

### **Level 2: 80-99% of Budget**
- ⚠️ **Action**: Investigation required
- 🔍 **Check**: Daily cost monitoring
- 📋 **Review**: Resource utilization
- 🎯 **Goal**: Optimize before hitting 100%

### **Level 3: 100%+ of Budget**
- 🚨 **Action**: Immediate intervention
- 🛑 **Consider**: Temporary shutdown
- 📞 **Escalate**: Team notification
- 🎯 **Goal**: Prevent cost overrun

---

## 📚 Additional Resources

- **AWS Cost Explorer**: Visual cost analysis
- **AWS Trusted Advisor**: Cost optimization recommendations  
- **AWS Cost Anomaly Detection**: Automated anomaly alerts
- **Reserved Instances**: Long-term cost savings
- **Savings Plans**: Flexible cost savings

---

*For infrastructure questions, see [DEPLOYMENT.md](DEPLOYMENT.md)*
*For troubleshooting, see [TROUBLESHOOTING.md](TROUBLESHOOTING.md)* 