#!/usr/bin/env bash
set -euo pipefail

# Stage 1. Install docker

sudo apt-get update
sudo apt-get install -y --no-install-recommends ca-certificates curl

export DEBIAN_FRONTEND=noninteractive
curl -fsSL https://get.docker.com | sudo -E sh

sudo usermod -aG docker "$USER"

# Stage 2. Generate self-signed TLS certificate for PROVIDED_ADDRESS

: "${PROVIDED_ADDRESS:?PROVIDED_ADDRESS environment variable is not set}"
export PROVIDED_ADDRESS

CERT_DIR="/etc/ssl/cloudimny"
CERT_DAYS=$((50 * 365))

sudo mkdir -p "$CERT_DIR"

if sudo test -f "$CERT_DIR/server.crt" \
	&& sudo openssl x509 -noout -subject -nameopt compat -in "$CERT_DIR/server.crt" | grep -qF "/CN=${PROVIDED_ADDRESS}"; then
	echo "Certificate for ${PROVIDED_ADDRESS} already exists, skipping generation"
else
	if [[ "$PROVIDED_ADDRESS" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]] || [[ "$PROVIDED_ADDRESS" == *:* ]]; then
		SAN="IP:${PROVIDED_ADDRESS}"
	else
		SAN="DNS:${PROVIDED_ADDRESS}"
	fi

	sudo openssl req -x509 -nodes -newkey rsa:4096 \
		-keyout "$CERT_DIR/server.key" \
		-out "$CERT_DIR/server.crt" \
		-days "$CERT_DAYS" \
		-subj "/CN=${PROVIDED_ADDRESS}" \
		-addext "subjectAltName=${SAN}"

	sudo chmod 600 "$CERT_DIR/server.key"
	sudo chmod 644 "$CERT_DIR/server.crt"
fi

# Stage 3. Download the docker compose stack from the public GitHub repo and start it

REPO_RAW_BASE="https://raw.githubusercontent.com/GlackyBagy/cloudimny/setup-script"
APP_DIR="/opt/cloudimny"

sudo mkdir -p "$APP_DIR"
sudo curl -fsSL "$REPO_RAW_BASE/compose.yaml" -o "$APP_DIR/compose.yaml"
sudo curl -fsSL "$REPO_RAW_BASE/Caddyfile" -o "$APP_DIR/Caddyfile"

if [[ "$PROVIDED_ADDRESS" == *:* ]]; then
	CADDY_ADDRESS="[${PROVIDED_ADDRESS}]"
else
	CADDY_ADDRESS="${PROVIDED_ADDRESS}"
fi

{
	echo "PROVIDED_ADDRESS=${PROVIDED_ADDRESS}"
	echo "CADDY_ADDRESS=${CADDY_ADDRESS}"
} | sudo tee "$APP_DIR/.env" > /dev/null

sudo docker compose --project-directory "$APP_DIR" up -d
