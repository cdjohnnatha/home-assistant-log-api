#!/bin/bash

# Log all output for debugging
exec > >(tee /var/log/user-data.log) 2>&1

echo "🚀 Starting Home Assistant Log API deployment..."

# Update the system
yum update -y

# Install Docker
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install git for cloning repositories
yum install -y git

# Set AWS region for EC2 instance
echo "export AWS_DEFAULT_REGION=${aws_region}" >> /home/ec2-user/.bashrc
echo "export AWS_REGION=${aws_region}" >> /home/ec2-user/.bashrc

# Create directory for the application
mkdir -p /home/ec2-user/home-assistant-log-api
chown ec2-user:ec2-user /home/ec2-user/home-assistant-log-api
cd /home/ec2-user/home-assistant-log-api

# Create environment file for the application
cat > .env << 'EOF'
# AWS Configuration - Using IAM Role (no need for access keys)
AWS_REGION=${aws_region}
AWS_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:276535936051:HomeAssistantAutomationAlert

# Application Configuration
SPRING_PROFILE_ACTIVE=prod
SERVER_PORT=8080
LOGS_API_PORT=8080
JAVA_OPTS=-Xmx512m -Xms256m
EOF

# Clone the application repository
# Replace with your actual Git repository URL
echo "📦 Cloning application repository..."
git clone https://github.com/cdjohnnatha/home-assistant-log-api.git temp-repo
mv temp-repo/* ./
mv temp-repo/.* ./ 2>/dev/null || true
rm -rf temp-repo

# Set ownership for all files
chown -R ec2-user:ec2-user /home/ec2-user/home-assistant-log-api

# Build and start the application as ec2-user
echo "🔨 Building and starting Docker application..."
sudo -u ec2-user bash << 'DEPLOY_SCRIPT'
cd /home/ec2-user/home-assistant-log-api

# Build and start the application
docker-compose up -d --build

# Wait for application to start
echo "⏳ Waiting for application to start..."
sleep 30

# Health check
echo "🏥 Running health check..."
for i in {1..10}; do
    if curl -f http://localhost:8080/api/v1/events/health; then
        echo "✅ Application is healthy!"
        break
    else
        echo "⏳ Health check attempt $i failed, retrying in 10 seconds..."
        sleep 10
    fi
done

# Show container status
echo "📊 Container status:"
docker-compose ps

DEPLOY_SCRIPT

# Create systemd service for auto-restart on reboot
cat > /etc/systemd/system/home-assistant-log-api.service << 'EOF'
[Unit]
Description=Home Assistant Log API
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/ec2-user/home-assistant-log-api
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0
User=ec2-user
Group=ec2-user

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
systemctl enable home-assistant-log-api.service

# Create management script
cat > /home/ec2-user/manage-api.sh << 'EOF'
#!/bin/bash
cd /home/ec2-user/home-assistant-log-api

case "$1" in
    start)
        echo "🚀 Starting Home Assistant Log API..."
        docker-compose up -d
        ;;
    stop)
        echo "🛑 Stopping Home Assistant Log API..."
        docker-compose down
        ;;
    restart)
        echo "🔄 Restarting Home Assistant Log API..."
        docker-compose down
        docker-compose up -d
        ;;
    logs)
        echo "📋 Showing logs..."
        docker-compose logs -f
        ;;
    status)
        echo "📊 Container status:"
        docker-compose ps
        echo "🏥 Health check:"
        curl -f http://localhost:8080/api/v1/events/health && echo " ✅" || echo " ❌"
        ;;
    update)
        echo "📦 Updating from Git repository..."
        git pull
        docker-compose down
        docker-compose up -d --build
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|status|update}"
        exit 1
        ;;
esac
EOF

chmod +x /home/ec2-user/manage-api.sh
chown ec2-user:ec2-user /home/ec2-user/manage-api.sh

# Log completion
echo "✅ Deployment completed at $(date)" > /home/ec2-user/setup-complete.txt
echo "🌐 API available at: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080" >> /home/ec2-user/setup-complete.txt
echo "🏥 Health check: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/api/v1/events/health" >> /home/ec2-user/setup-complete.txt

echo "🎉 Home Assistant Log API deployment complete!" 