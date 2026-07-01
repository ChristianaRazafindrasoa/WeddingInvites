#!/usr/bin/env bash
#
# Deploys a build zip to a remote machine.
# Usage: ./upgrade.sh <zip-file> <user@host> <key.pem>
#
set -euo pipefail

ZIP_FILE="${1:-}"
REMOTE="${2:-}"
PEM_FILE="${3:-}"

if [ -z "$ZIP_FILE" ] || [ -z "$REMOTE" ] || [ -z "$PEM_FILE" ]; then
  echo "Usage: $0 <zip-file> <user@host> <key.pem>"
  exit 1
fi

if [ ! -f "$ZIP_FILE" ]; then
  echo "ERROR: zip file not found: $ZIP_FILE"
  exit 1
fi

if [ ! -f "$PEM_FILE" ]; then
  echo "ERROR: pem file not found: $PEM_FILE"
  exit 1
fi

REMOTE_DIR="/home/$(echo "$REMOTE" | cut -d@ -f1)"
ZIP_NAME="$(basename "$ZIP_FILE")"
NEW_DIR="${ZIP_NAME%.zip}"

log() { printf '\n\033[1;34m==>\033[0m %s\n' "$1"; }

# --- 1. Copy zip to remote --------------------------------------------------
log "Copying $ZIP_NAME to $REMOTE"
scp -i "$PEM_FILE" "$ZIP_FILE" "$REMOTE:~/$ZIP_NAME"

# --- 2. Run deployment on remote --------------------------------------------
log "Deploying on remote"
ssh -i "$PEM_FILE" "$REMOTE" bash <<ENDSSH
set -euo pipefail

log() { printf '\n\033[1;34m==>\033[0m %s\n' "\$1"; }

log "Unzipping $ZIP_NAME"
cd ~
unzip -q "$ZIP_NAME" -d .
rm -f ~/wedding-*.zip

log "Syncing application config"
if [ -f ~/app/application.properties ]; then
  cp ~/app/application.properties ~/"$NEW_DIR/application.properties"
  echo "  Config copied from ~/app/application.properties"
else
  echo "  WARNING: no ~/app/application.properties found — place one there before starting"
fi
for profile in prod demo; do
  BUNDLED=~/"$NEW_DIR/application-\${profile}.properties"
  SECRETS=~/app/application-\${profile}.properties
  if [ -f "\$SECRETS" ]; then
    grep -v '^\s*#' "\$SECRETS" | grep '=' | while IFS='=' read -r key value; do
      key="\$(echo "\$key" | xargs)"
      if grep -q "^\${key}=" "\$BUNDLED" 2>/dev/null; then
        sed -i "s|^\${key}=.*|\${key}=\${value}|" "\$BUNDLED"
      else
        echo "\${key}=\${value}" >> "\$BUNDLED"
      fi
    done
    echo "  Merged secrets from \$SECRETS into \$BUNDLED"
  else
    echo "  WARNING: no application-\${profile}.properties found in ~/app/ — secrets not applied"
  fi
done

log "Stopping existing server"
pkill -f 'java.*app.jar' || echo "  (no server running)"

log "Stopping database"
docker stop mysql-wedding || echo "  (database was not running)"

log "Starting new server and database"
chmod +x "$NEW_DIR/run.sh"
"$NEW_DIR/run.sh" --spring.profiles.active=prod --server.port=8080
echo "  Prod server started, log: ~/$NEW_DIR/server-prod.log"
"$NEW_DIR/run.sh" --spring.profiles.active=demo --server.port=8081
echo "  Demo server started, log: ~/$NEW_DIR/server-demo.log"

log "Cleaning up old deployments"
for dir in ~/wedding-*/; do
  if [ "\$dir" != ~/"$NEW_DIR/" ]; then
    echo "  Removing \$dir"
    rm -rf "\$dir"
  fi
done

log "Verifying deployment"
sleep 3
ERRORS=0

if pgrep -f 'java.*app.jar' > /dev/null; then
  COUNT=\$(pgrep -c -f 'java.*app.jar')
  echo "  [OK] \$COUNT server instance(s) running"
  if [ "\$COUNT" -lt 2 ]; then
    echo "  WARNING: expected 2 instances (prod + demo), got \$COUNT — check ~/$NEW_DIR/server.log"
  fi
else
  echo "  [FAIL] Server is NOT running — check ~/$NEW_DIR/server-prod.log or ~/$NEW_DIR/server-demo.log"
  ERRORS=1
fi

if [ "\$(docker inspect -f '{{.State.Running}}' mysql-wedding 2>/dev/null)" = "true" ]; then
  echo "  [OK] Database is running"
else
  echo "  [FAIL] Database is NOT running"
  ERRORS=1
fi

if [ "\$ERRORS" -ne 0 ]; then
  echo ""
  echo "ERROR: deployment completed with failures" >&2
  exit 1
fi

log "Done — running: $NEW_DIR"
ENDSSH