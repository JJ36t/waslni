# WASLNI — Production Deployment Guide

> Step-by-step guide for deploying the Waselni backend to a production server.

## Prerequisites

- A VPS or cloud instance (recommended: 2 vCPU, 4GB RAM, 40GB SSD)
- Ubuntu 22.04+ or Debian 12+
- Domain name pointing to the server's IP (`api.waslni.com`)
- Docker + Docker Compose installed
- SSH key access (no password login)

---

## Step 1: Server Setup

### 1.1 Create a deploy user
```bash
# SSH in as root
ssh root@your-server-ip

# Create deploy user
adduser waselni
usermod -aG sudo waselni
mkdir -p /home/waselni/.ssh
cp ~/.ssh/authorized_keys /home/waselni/.ssh/
chown -R waselni:waselni /home/waselni/.ssh
chmod 700 /home/waselni/.ssh
chmod 600 /home/waselni/.ssh/authorized_keys
```

### 1.2 Disable SSH password login
```bash
nano /etc/ssh/sshd_config
```
Set:
```
PasswordAuthentication no
PermitRootLogin no
```
```bash
systemctl restart sshd
```

### 1.3 Firewall (UFW)
```bash
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp       # SSH
ufw allow 80/tcp       # HTTP (for Let's Encrypt + redirect)
ufw allow 443/tcp      # HTTPS
ufw enable
ufw status verbose
```

### 1.4 Install Docker
```bash
curl -fsSL https://get.docker.com | sh
usermod -aG docker waselni
# Log out and back in for group change to take effect
```

### 1.5 Create app directory
```bash
mkdir -p /opt/waselni
chown waselni:waselni /opt/waselni
```

---

## Step 2: SSL Certificates (Let's Encrypt)

### 2.1 Install certbot
```bash
apt update && apt install -y certbot
```

### 2.2 Obtain certificates
```bash
# Stop nginx if running (free port 80)
docker compose -f /opt/waselni/docker-compose.prod.yml down

# Obtain cert
certbot certonly --standalone -d api.waslni.com

# Verify
ls /etc/letsencrypt/live/api.waslni.com/
# Should show: fullchain.pem  privkey.pem
```

### 2.3 Auto-renewal
```bash
# Test renewal
certbot renew --dry-run

# Add cron job
echo "0 3 * * * certbot renew --quiet && docker compose -f /opt/waselni/docker-compose.prod.yml restart nginx" | crontab -
```

---

## Step 3: Deploy the Backend

### 3.1 Clone the repo
```bash
su - waselni
cd /opt/waselni
git clone https://github.com/your-org/waslni.git .
cd backend
```

### 3.2 Configure production environment
```bash
cp .env.production.example .env.production
nano .env.production
```
Set:
- `POSTGRES_PASSWORD` — strong random password
- `JWT_SECRET` — generate with `python -c "import secrets; print(secrets.token_hex(32))"`
- `CORS_ORIGINS` — your frontend URL

### 3.3 First deployment
```bash
# Build + start services
docker compose -f docker-compose.prod.yml up -d --build

# Wait for health check
sleep 30
curl http://localhost:8000/health
# Expected: {"status":"ok","db":"ok","env":"production","version":"1.0.0"}

# Run migrations
docker compose -f docker-compose.prod.yml exec backend alembic upgrade head

# Seed admin + driver
docker compose -f docker-compose.prod.yml exec backend python -m scripts.seed
```

### 3.4 Verify HTTPS
```bash
curl https://api.waslni.com/health
# Expected: {"status":"ok","db":"ok",...}
```

---

## Step 4: Automated Backups

### 4.1 Set up cron
```bash
crontab -e
```
Add:
```cron
# Daily backup at 2:00 AM (kept for 7 days)
0 2 * * * cd /opt/waselni/backend && ./scripts/backup_db.sh

# Weekly backup on Sunday at 3:00 AM (kept for 4 weeks)
0 3 * * 0 cd /opt/waselni/backend && ./scripts/backup_db.sh --weekly
```

### 4.2 Test backup
```bash
cd /opt/waselni/backend
./scripts/backup_db.sh
ls -lh backups/
```

### 4.3 Test restore (on staging!)
```bash
# Create a test database
docker compose -f docker-compose.prod.yml exec db \
    psql -U waselni -d postgres -c "CREATE DATABASE waslni_test_restore;"

# Restore
gunzip -c backups/daily_2026-09-08_020000.sql.gz | \
    docker compose -f docker-compose.prod.yml exec -T db \
    psql -U waselni -d waslni_test_restore

# Verify
docker compose -f docker-compose.prod.yml exec db \
    psql -U waselni -d waslni_test_restore -c "SELECT count(*) FROM users;"

# Clean up
docker compose -f docker-compose.prod.yml exec db \
    psql -U waselni -d postgres -c "DROP DATABASE waslni_test_restore;"
```

---

## Step 5: Ongoing Operations

### Update the backend
```bash
cd /opt/waselni/backend
./scripts/deploy.sh --migrate
```

### View logs
```bash
# All services
docker compose -f docker-compose.prod.yml logs -f

# Backend only
docker compose -f docker-compose.prod.yml logs -f backend

# Nginx only
docker compose -f docker-compose.prod.yml logs -f nginx

# Database only
docker compose -f docker-compose.prod.yml logs -f db
```

### Restart a service
```bash
docker compose -f docker-compose.prod.yml restart backend
```

### Scale backend (add more workers)
Edit `docker-compose.prod.yml` → change `--workers 4` to `--workers 8`.
```bash
docker compose -f docker-compose.prod.yml up -d backend
```

---

## Server Hardening Checklist

| Item | Status | Notes |
|------|--------|-------|
| SSH key-only login | ☐ | `PasswordAuthentication no` in sshd_config |
| Root login disabled | ☐ | `PermitRootLogin no` |
| UFW firewall enabled | ☐ | Only 22, 80, 443 open |
| Fail2ban installed | ☐ | `apt install fail2ban` — brute-force protection |
| Automatic security updates | ☐ | `apt install unattended-upgrades` |
| Docker user not root | ☐ | `waselni` user in `docker` group |
| .env.production not in git | ☐ | Added to .gitignore |
| SSL certificates valid | ☐ | `certbot certificates` |
| SSL auto-renewal cron | ☐ | Tested with `--dry-run` |
| Daily DB backups | ☐ | Cron + backup_db.sh |
| Weekly DB backups | ☐ | Cron + backup_db.sh --weekly |
| Backup restore tested | ☐ | On staging, not production |
| Health check endpoint | ☐ | `curl https://api.waslni.com/health` |
| Nginx rate limiting | ☐ | login: 5/min, api: 100/min |
| Security headers | ☐ | X-Content-Type-Options, X-Frame-Options, HSTS |
| Gzip compression | ☐ | nginx.conf |
| WebSocket support | ☐ | For future real-time features |
| Log rotation | ☐ | `logrotate` for nginx + app logs |
| Monitoring (future) | ☐ | Phase 28 — Sentry + uptime monitoring |
