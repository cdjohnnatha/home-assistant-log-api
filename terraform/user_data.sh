#!/bin/bash

# Log all output for debugging
exec > >(tee /var/log/user-data.log) 2>&1

echo "🚀 Starting Home Assistant Log API deployment..."
echo "📅 Started at: $(date)"

# Check available resources
echo "💾 Available resources:"
echo "Memory: $(free -h | grep Mem)"
echo "CPU cores: $(nproc)"
echo "Disk space: $(df -h /)"

# Update the system
echo "📦 Updating system packages..."
yum update -y

# Install Docker
echo "🐳 Installing Docker..."
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Install Docker Compose with retry
echo "🔧 Installing Docker Compose..."
for i in {1..3}; do
    if curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose; then
        chmod +x /usr/local/bin/docker-compose
        echo "✅ Docker Compose installed successfully"
        break
    else
        echo "❌ Docker Compose installation attempt $i failed, retrying..."
        sleep 5
    fi
done

# Install git for cloning repositories
echo "📥 Installing Git..."
yum install -y git

# Set AWS region for EC2 instance
echo "🌍 Setting AWS region..."
echo "export AWS_DEFAULT_REGION=${aws_region}" >> /home/ec2-user/.bashrc
echo "export AWS_REGION=${aws_region}" >> /home/ec2-user/.bashrc

# Create directory for the application
echo "📁 Creating application directory..."
mkdir -p /home/ec2-user/home-assistant-log-api
chown ec2-user:ec2-user /home/ec2-user/home-assistant-log-api
cd /home/ec2-user/home-assistant-log-api

# Create environment file for the application
echo "⚙️ Creating .env configuration..."
cat > .env << 'EOF'
# AWS Configuration - Using IAM Role (no need for access keys)
AWS_REGION=${aws_region}
AWS_SNS_TOPIC_ARN=${sns_topic_arn}

# Application Configuration
SPRING_PROFILE_ACTIVE=prod
SERVER_PORT=8080
LOGS_API_PORT=8080

# JVM Options optimized for t3.small (2GB RAM)
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
EOF

# Clone the application repository with retry
echo "📦 Cloning application repository..."
for i in {1..3}; do
    if git clone https://github.com/cdjohnnatha/home-assistant-log-api.git temp-repo; then
        echo "✅ Repository cloned successfully"
        mv temp-repo/* ./
        mv temp-repo/.* ./ 2>/dev/null || true
        rm -rf temp-repo
        break
    else
        echo "❌ Git clone attempt $i failed, retrying..."
        sleep 10
    fi
done

# Set ownership for all files
chown -R ec2-user:ec2-user /home/ec2-user/home-assistant-log-api

# Build and start the application as ec2-user
echo "🔨 Building and starting Docker application..."
sudo -u ec2-user bash << 'DEPLOY_SCRIPT'
cd /home/ec2-user/home-assistant-log-api

# Add /usr/local/bin to PATH for this session
export PATH="/usr/local/bin:$PATH"

echo "🏗️ Starting Docker Compose build (this may take 3-5 minutes for Gradle build)..."
/usr/local/bin/docker-compose up -d --build

echo "⏳ Waiting for application to start (extended wait for Spring Boot)..."
sleep 60

# Extended health check with better error reporting
echo "🏥 Running health check..."
for i in {1..15}; do
    if curl -f http://localhost:8080/api/v1/events/health; then
        echo "✅ Application is healthy!"
        break
    else
        echo "⏳ Health check attempt $i failed, retrying in 10 seconds..."
        if [ $i -eq 5 ] || [ $i -eq 10 ]; then
            echo "📋 Container logs (last 10 lines):"
            /usr/local/bin/docker-compose logs --tail 10
        fi
        sleep 10
    fi
done

# Show final status
echo "📊 Container status:"
/usr/local/bin/docker-compose ps

echo "📋 Recent application logs:"
/usr/local/bin/docker-compose logs --tail 20

DEPLOY_SCRIPT

# Create systemd service for auto-restart on reboot
echo "🔄 Creating systemd service..."
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
TimeoutStartSec=300
User=ec2-user
Group=ec2-user

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
systemctl enable home-assistant-log-api.service

# Create management script
echo "🛠️ Creating management script..."
cat > /home/ec2-user/manage-api.sh << 'EOF'
#!/bin/bash
cd /home/ec2-user/home-assistant-log-api

case "$1" in
    start)
        echo "🚀 Starting Home Assistant Log API..."
        /usr/local/bin/docker-compose up -d
        ;;
    stop)
        echo "🛑 Stopping Home Assistant Log API..."
        /usr/local/bin/docker-compose down
        ;;
    restart)
        echo "🔄 Restarting Home Assistant Log API..."
        /usr/local/bin/docker-compose down
        /usr/local/bin/docker-compose up -d
        ;;
    logs)
        echo "📋 Showing logs..."
        /usr/local/bin/docker-compose logs -f
        ;;
    status)
        echo "📊 Container status:"
        /usr/local/bin/docker-compose ps
        echo "🏥 Health check:"
        curl -f http://localhost:8080/api/v1/events/health && echo " ✅" || echo " ❌"
        ;;
    update)
        echo "📦 Updating from Git repository..."
        git pull
        /usr/local/bin/docker-compose down
        /usr/local/bin/docker-compose up -d --build
        ;;
    test)
        echo "🧪 Testing API with sample event..."
        curl -X POST http://localhost:8080/api/v1/events \
          -H "Content-Type: application/json" \
          -d '{
            "source": "terraform-deployment-test",
            "eventType": "SYSTEM_EVENT", 
            "payload": {
              "description": "Automated deployment test",
              "deployedAt": "'$(date)'",
              "instanceType": "t3.small"
            }
          }'
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|status|update|test}"
        exit 1
        ;;
esac
EOF

chmod +x /home/ec2-user/manage-api.sh
chown ec2-user:ec2-user /home/ec2-user/manage-api.sh

# Get instance IP for final report
INSTANCE_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)

# Log completion with detailed information
echo "✅ Deployment completed at $(date)" > /home/ec2-user/setup-complete.txt
echo "🌐 API available at: http://$INSTANCE_IP:8080" >> /home/ec2-user/setup-complete.txt
echo "🏥 Health check: http://$INSTANCE_IP:8080/api/v1/events/health" >> /home/ec2-user/setup-complete.txt
echo "📝 Management: ./manage-api.sh {start|stop|restart|logs|status|update|test}" >> /home/ec2-user/setup-complete.txt
echo "📋 Logs: /var/log/user-data.log" >> /home/ec2-user/setup-complete.txt

echo "🎉 Home Assistant Log API deployment complete!"
echo "📍 Access your API at: http://$INSTANCE_IP:8080" 