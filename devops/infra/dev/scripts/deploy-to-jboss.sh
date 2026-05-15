#!/bin/bash
# Deploy cosmolab-backend.war to local JBoss EAP 7.4 server
# Usage: ./deploy-to-jboss.sh [optional: path/to/war]

JBOSS_HOME="/opt/servers/jboss/jboss-eap-7.4"
WAR_PATH="${1:-$(dirname "$0")/../../../backend/target/cosmolab-backend.war}"
DEPLOY_DIR="$JBOSS_HOME/standalone/deployments"

if [ ! -f "$WAR_PATH" ]; then
  echo "WAR file not found: $WAR_PATH"
  exit 1
fi

if [ ! -d "$DEPLOY_DIR" ]; then
  echo "JBoss deployments directory not found: $DEPLOY_DIR"
  exit 2
fi

echo "Deploying $WAR_PATH to $DEPLOY_DIR ..."
cp "$WAR_PATH" "$DEPLOY_DIR/cosmolab-backend.war"

# Optionally, create a marker file for JBoss hot deployment
# touch "$DEPLOY_DIR/cosmolab-backend.war.dodeploy"

echo "Deployment complete."
