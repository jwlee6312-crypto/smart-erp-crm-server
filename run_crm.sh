#!/bin/bash
# 🚀 Smart ERP CRM - Start Script for Server (172.30.1.11 & DuckDNS 18889)
PROJECT_ROOT="/home/smart/smart-erp-crm"

echo "=================================================="
echo "   Smart ERP CRM System Start (172.30.1.11 @ 3308)"
echo "=================================================="

if [ -d "$PROJECT_ROOT" ]; then
    cd "$PROJECT_ROOT"
else
    PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
    cd "$PROJECT_ROOT"
fi

echo "[1/4] Stopping existing services..."
pkill -f 'erp-backend' 2>/dev/null
sudo pkill -f 'vite' 2>/dev/null
sudo fuser -k 18889/tcp 2>/dev/null
sudo fuser -k 8080/tcp 2>/dev/null
sudo docker compose down 2>/dev/null

echo "[2/4] Fixing directory permissions..."
sudo mkdir -p /var/lib/asterisk/sounds/custom /data/smart-erp-crm/mssql /data/smart-erp-crm/mysql /data/smart-erp-crm/storage /data/smart-erp-crm/recordings /data/smart-erp-crm/chatwoot/postgres
sudo chmod -R 777 /var/lib/asterisk/sounds/custom
sudo chmod -R 777 /data/smart-erp-crm

echo "[3/4] Building backend BootJar..."
chmod +x gradlew
./gradlew :erp-backend:clean :erp-backend:bootJar --no-daemon

echo "[4/4] Launching Smart ERP CRM services via Docker Compose..."
sudo docker compose up --build -d

echo "=================================================="
echo " ✅ SMART-ERP-CRM STARTED SUCCESSFULLY (DB @ 3308)!"
echo " --------------------------------------------------"
echo " - External URL : http://smart-erp.duckdns.org:18889"
echo " - Internal Web : http://172.30.1.11:18889"
echo " - Backend API  : http://172.30.1.11:8090"
echo " - CRM DB Port  : 3308 (Persistent Volume Preserved)"
echo "=================================================="
