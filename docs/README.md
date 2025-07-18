# 📚 Documentation Index

Welcome to the Home Assistant Log API documentation! This directory contains detailed guides and references to help you deploy, manage, and troubleshoot the application.

## 📋 Available Documentation

### 🚀 **[DEPLOYMENT.md](DEPLOYMENT.md)**
**Complete infrastructure and deployment guide**
- AWS infrastructure setup with Terraform
- Step-by-step deployment instructions
- Cost management and optimization
- Advanced configurations (multi-environment, custom domains, load balancers)
- Production deployment checklist
- CI/CD pipeline setup

**Use this when:** You want to deploy to AWS, understand the infrastructure, or set up production environments.

---

### 🐛 **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)**
**Common issues and solutions**
- OutOfMemoryError fixes (t3.micro vs t3.small)
- Docker Compose PATH issues
- Health check failures
- SNS permission problems
- Network connectivity issues

**Use this when:** Something isn't working, you're getting errors, or need to diagnose issues.

---

### 💰 **[COST-MONITORING.md](COST-MONITORING.md)**
**AWS cost management and budget alerts**
- Automated budget setup ($22/month limit)
- Cost breakdown and monitoring commands
- Alert response playbook
- Cost optimization strategies
- Advanced cost management features

**Use this when:** You want to understand costs, set up monitoring, or respond to budget alerts.

---

## 🗂️ Documentation Organization

```
docs/
├── README.md           # 📋 This index file
├── DEPLOYMENT.md       # 🚀 Complete deployment guide  
├── TROUBLESHOOTING.md  # 🐛 Issue resolution guide
└── COST-MONITORING.md  # 💰 Cost management and budgets
```

## 🔗 Quick Links

| Task | Documentation | Key Sections |
|------|---------------|--------------|
| **Deploy to AWS** | [DEPLOYMENT.md](DEPLOYMENT.md#-quick-deploy) | Quick Deploy, Prerequisites |
| **Fix build errors** | [TROUBLESHOOTING.md](TROUBLESHOOTING.md#-outofmemoryerror-during-gradle-build) | OutOfMemoryError |
| **Monitor costs** | [COST-MONITORING.md](COST-MONITORING.md#-automated-budget-alert-system) | Budget Setup, Alerts |
| **Manage costs** | [COST-MONITORING.md](COST-MONITORING.md#️-cost-control-commands) | Cost Control, Optimization |
| **Monitor application** | [DEPLOYMENT.md](DEPLOYMENT.md#-ec2-management) | EC2 Management, Monitoring |
| **Debug issues** | [TROUBLESHOOTING.md](TROUBLESHOOTING.md#-quick-diagnostic-commands) | Diagnostic Commands |
| **Production setup** | [DEPLOYMENT.md](DEPLOYMENT.md#-production-deployment) | Production Deployment |

## 🆘 Need Help?

1. **Check the main [README.md](../README.md)** for quick start and API reference
2. **Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md)** for common issues
3. **Consult [DEPLOYMENT.md](DEPLOYMENT.md)** for infrastructure questions
4. **Create a GitHub issue** with logs and configuration details

## 🔄 Documentation Updates

This documentation is maintained alongside the codebase. When making changes:

- **Code changes** → Update relevant sections in DEPLOYMENT.md
- **New issues discovered** → Add to TROUBLESHOOTING.md  
- **Infrastructure changes** → Update architecture diagrams and costs
- **API changes** → Update main README.md

---

*For the latest version, see: [GitHub Repository](../README.md)* 