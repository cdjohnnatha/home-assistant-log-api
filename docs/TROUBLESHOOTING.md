# 🐛 Troubleshooting Guide

Common issues and solutions for the Home Assistant Log API deployment and operation.

## 📋 Quick Diagnostic Commands

Before diving into specific issues, run these commands to gather information:

### 🔍 On Your Local Machine
```bash
# Check Terraform status
cd terraform && terraform show | head -20

# Verify configuration
terraform plan

# Test connectivity
curl -f http://YOUR-INSTANCE-IP:8080/api/v1/events/health
```

### 🖥️ On EC2 Instance
```bash
# SSH to instance
ssh -i ~/.ssh/id_rsa ec2-user@YOUR-INSTANCE-IP

# Check deployment completion
cat /home/ec2-user/setup-complete.txt

# View deployment logs
tail -50 /var/log/user-data.log

# Check application status
./manage-api.sh status
```

---

## 🔥 Common Issues & Solutions

### 🧠 OutOfMemoryError during Gradle Build

**🔍 Problem:** 
```
FAILURE: Build failed with an exception.
* What went wrong: Gradle daemon disappeared unexpectedly
```

**💡 Root Cause:** t3.micro (1GB RAM) is insufficient for Gradle + Kotlin compilation

**✅ Solution:** Use t3.small in [`terraform.tfvars`](terraform/terraform.tfvars):
```bash
instance_type = "t3.small"  # 2GB RAM recommended
```

**📊 Memory Requirements:**
- **t3.micro**: 1GB RAM → ❌ Fails during build
- **t3.small**: 2GB RAM → ✅ Builds successfully 
- **t3.medium**: 4GB RAM → ✅ Faster builds

**🔧 If Already Deployed:**
```bash
# Stop current instance
terraform destroy

# Update terraform.tfvars
instance_type = "t3.small"

# Redeploy
terraform apply
```

---

### 🐳 "docker-compose: command not found"

**🔍 Problem:**
```bash
bash: docker-compose: command not found
```

**💡 Root Cause:** PATH doesn't include `/usr/local/bin/` where Docker Compose is installed

**✅ Solution:** Already fixed in latest [`user_data.sh`](terraform/user_data.sh) - uses full path

**🔧 Manual Fix (if needed):**
```bash
# Use full path instead of docker-compose
/usr/local/bin/docker-compose ps

# Or add to PATH temporarily
export PATH="/usr/local/bin:$PATH"
docker-compose ps
```

**🔍 Verify Installation:**
```bash
# Check if Docker Compose is installed
ls -la /usr/local/bin/docker-compose
which docker-compose

# Test with full path
/usr/local/bin/docker-compose --version
```

---

### 🏥 Health Check Failing After Deployment

**🔍 Problem:**
```bash
curl: (7) Failed to connect to IP:8080: Connection refused
```

**💡 Possible Causes:**
1. Application still starting up (Spring Boot takes 60-90s)
2. Container build failed
3. Port not exposed correctly
4. Security group blocking traffic

**✅ Debugging Steps:**

**1. Check Container Status:**
```bash
# On EC2 instance
docker-compose ps

# Expected output:
# home-assistant-logs-api   Up   0.0.0.0:8080->8080/tcp
```

**2. View Application Logs:**
```bash
# Real-time logs
docker-compose logs -f logs-api

# Last 50 lines
docker-compose logs --tail 50 logs-api
```

**3. Check Application Startup:**
```bash
# Look for this in logs:
# "Started LogsApiApplication in X.XXX seconds"
# "Tomcat started on port 8080"
```

**4. Manual Health Check:**
```bash
# From inside EC2
curl http://localhost:8080/api/v1/events/health

# Expected: "Ok"
```

**5. Check Security Groups:**
```bash
# From local machine - should work if SG is correct
curl -f http://YOUR-INSTANCE-IP:8080/api/v1/events/health
```

**⏱️ Expected Timeline:**
- **0-30s**: Container starting
- **30-90s**: Spring Boot initializing
- **90s+**: Application ready

---

### 📮 SNS Publish Access Denied

**🔍 Problem:**
```
AccessDeniedException: User: arn:aws:sts::123456789012:assumed-role/...
is not authorized to perform: SNS:Publish on resource: arn:aws:sns:...
```

**💡 Root Cause:** IAM role missing SNS permissions or incorrect topic ARN

**✅ Solution 1: Verify IAM Role (Terraform creates this automatically)**
```bash
# Check IAM role attached to instance
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/

# Should return role name like: home-assistant-log-api-ec2-sns-role
```

**✅ Solution 2: Verify SNS Topic ARN:**
```bash
# Check configured topic in .env
cat /home/ec2-user/home-assistant-log-api/.env | grep SNS

# Test SNS permissions
aws sns list-topics --region us-east-1
```

**✅ Solution 3: Test SNS Publishing:**
```bash
# Use management script test
./manage-api.sh test

# Or manual test
aws sns publish \
  --topic-arn "arn:aws:sns:us-east-1:123456789012:your-topic" \
  --message "Test from EC2" \
  --region us-east-1
```

---

### 💸 Unexpected AWS Charges

**🔍 Problem:** Higher than expected AWS bill

**💡 Common Causes:**
1. Forgot to destroy resources
2. Running larger instance than needed
3. Data transfer costs
4. EBS volume costs

**✅ Prevention & Solutions:**

**1. Monitor Costs:**
```bash
# Get current month costs
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost

# Set up billing alerts in AWS Console
```

**2. Optimize Instance Size:**
```bash
# For development, stop when not using
terraform destroy  # Destroys everything

# Or downgrade instance (keeps data)
# Edit terraform.tfvars:
instance_type = "t3.nano"  # Minimal cost
terraform apply
```

**3. Regular Cleanup:**
```bash
# Check what's running
terraform show

# Clean up old resources
terraform destroy

# Verify cleanup in AWS Console
```

**💰 Expected Monthly Costs:**
- **t3.small**: ~$17/month
- **t3.micro**: ~$8.50/month  
- **EBS Storage**: ~$1/month
- **Data Transfer**: ~$1/month
- **SNS**: ~$0.10/month

---

### ⚡ Application Slow or Timing Out

**🔍 Problem:** API responses slow or timing out

**💡 Possible Causes:**
1. Insufficient memory allocation
2. JVM garbage collection issues
3. Docker resource limits

**✅ Solutions:**

**1. Check JVM Memory:**
```bash
# View current Java processes
docker-compose exec logs-api ps aux | grep java

# Check JVM settings in .env
cat .env | grep JAVA_OPTS
```

**2. Optimize JVM Settings:**
```bash
# Edit .env file
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# Restart application
./manage-api.sh restart
```

**3. Monitor Resource Usage:**
```bash
# Check system resources
free -h
top
df -h

# Check container resources
docker stats
```

---

### 🔄 Git Related Issues

**🔍 Problem:** Git pull fails during updates

**💡 Common Issues:**
1. Divergent branches
2. Uncommitted changes
3. Network connectivity

**✅ Solutions:**

**1. Force Pull Latest:**
```bash
cd /home/ec2-user/home-assistant-log-api

# Reset to match remote
git fetch origin
git reset --hard origin/main

# Then rebuild
./manage-api.sh update
```

**2. Check Git Status:**
```bash
git status
git log --oneline -5
```

---

### 🌐 Network Connectivity Issues

**🔍 Problem:** Cannot connect to instance or services

**✅ Debugging Steps:**

**1. Check Security Groups:**
```bash
# From AWS Console or CLI
aws ec2 describe-security-groups \
  --group-ids sg-YOUR-SECURITY-GROUP-ID
```

**2. Verify Instance State:**
```bash
# Check if instance is running
terraform show | grep instance_state
# Should show: "running"
```

**3. Test Network:**
```bash
# From local machine
ping YOUR-INSTANCE-IP
telnet YOUR-INSTANCE-IP 8080
```

**4. Check Routes:**
```bash
# On EC2 instance
ip route
netstat -tlnp | grep 8080
```

---

## 📞 Getting Help

### 🔍 Information to Gather Before Asking for Help

1. **📋 Deployment logs:** `/var/log/user-data.log` (generated by [`user_data.sh`](terraform/user_data.sh))
2. **🐳 Container logs:** `docker-compose logs --tail 50`
3. **⚙️ Configuration:** Content of `terraform.tfvars` (redact sensitive data)
4. **🖥️ System info:** `free -h`, `df -h`, `docker --version`
5. **🔗 Network:** `curl -v http://localhost:8080/api/v1/events/health`

### 📝 Debug Information Template

```markdown
## Environment
- **Instance Type**: t3.small/micro/etc
- **OS**: Amazon Linux 2
- **Docker Version**: [output of `docker --version`]
- **Available Memory**: [output of `free -h`]

## Issue Description
[Describe what you were trying to do and what happened]

## Logs
```bash
# Last 20 lines of deployment log
tail -20 /var/log/user-data.log

# Container status
docker-compose ps

# Recent application logs
docker-compose logs --tail 10 logs-api
```

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3
```

### 🆘 Emergency Commands

**If everything seems broken:**
```bash
# 1. Start fresh (destroys everything)
terraform destroy
terraform apply

# 2. Force restart application only
./manage-api.sh stop
./manage-api.sh start

# 3. Rebuild application
./manage-api.sh update

# 4. Check if it's a temporary issue
./manage-api.sh status
```

### 📚 Additional Resources

- **🏗️ Terraform Issues**: Check [Terraform AWS Provider docs](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- **🐳 Docker Issues**: Check [Docker Compose troubleshooting](https://docs.docker.com/compose/troubleshooting/)
- **🍃 Spring Boot Issues**: Check [Spring Boot docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- **☁️ AWS Issues**: Check [AWS documentation](https://docs.aws.amazon.com/)

---

## 🔍 Debug Mode

Enable verbose logging for detailed troubleshooting:

```bash
# Set debug profile
export SPRING_PROFILES_ACTIVE=dev

# Restart with debug logging
./manage-api.sh restart

# View detailed logs
docker-compose logs -f logs-api
```

---

*Last updated: [Current Date]*
*For more help: Create an issue on GitHub with the debug information above.* 