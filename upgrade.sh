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

log "Stopping existing server"
pkill -f 'java.*app.jar' || echo "  (no server running)"

log "Stopping database"
docker stop mysql-wedding || echo "  (database was not running)"

log "Starting new server and database"
chmod +x "$NEW_DIR/run.sh"
nohup "$NEW_DIR/run.sh" > "$NEW_DIR/server.log" 2>&1 &
echo "  Server started (pid \$!), log: ~/$NEW_DIR/server.log"

log "Cleaning up old deployments"
for dir in ~/wedding-*/; do
  if [ "\$dir" != ~/"$NEW_DIR/" ]; then
    echo "  Removing \$dir"
    rm -rf "\$dir"
  fi
done
rm -f ~/"$ZIP_NAME"

log "Verifying deployment"
sleep 3
ERRORS=0

if pgrep -f 'java.*app.jar' > /dev/null; then
  echo "  [OK] Server is running"
else
  echo "  [FAIL] Server is NOT running — check ~/$NEW_DIR/server.log"
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

# ./build.sh
# ./upgrade.sh /Users/christiana/Desktop/invites/dist/wedding-20260620-174301.zip ec2-user@3.80.113.81 /Users/christiana/Downloads/wedding-key.pem