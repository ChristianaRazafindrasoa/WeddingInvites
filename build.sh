#!/usr/bin/env bash
#
# Builds the wedding web application (Spring Boot server + React client)
# and packages everything into a single deployable zip under dist/.
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$ROOT_DIR/server"
CLIENT_DIR="$ROOT_DIR/client"
DIST_DIR="$ROOT_DIR/dist"

STAMP="$(date +%Y%m%d-%H%M%S)"
STAGE_DIR="$DIST_DIR/wedding-$STAMP"
ZIP_PATH="$DIST_DIR/wedding-$STAMP.zip"

log() { printf '\n\033[1;34m==>\033[0m %s\n' "$1"; }

# --- 1. Build the React client ----------------------------------------------
log "Building React client"
cd "$CLIENT_DIR"
npm ci
npm run build

# --- 2. Build the Spring Boot server jar ------------------------------------
log "Building Spring Boot server jar"
cd "$SERVER_DIR"
mvn -B clean package

JAR_PATH="$(find "$SERVER_DIR/target" -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
if [ -z "$JAR_PATH" ]; then
  echo "ERROR: no jar produced in server/target" >&2
  exit 1
fi
log "Server jar: $JAR_PATH"

# --- 3. Assemble the deployment staging directory ---------------------------
log "Assembling deployment package"
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR/public" "$STAGE_DIR/sql"

cp "$JAR_PATH" "$STAGE_DIR/app.jar"
cp -R "$CLIENT_DIR/build/." "$STAGE_DIR/public/"
cp "$SERVER_DIR/src/main/resources/application.properties.example" \
   "$STAGE_DIR/application.properties.example"
cp -R "$SERVER_DIR/src/main/resources/sql/." "$STAGE_DIR/sql/"

# Launcher script: reads application.properties from the same dir if present
cat > "$STAGE_DIR/run.sh" <<'LAUNCH'
#!/usr/bin/env bash
# Starts the wedding server. Place a filled-in application.properties next to
# this script (copy from application.properties.example) before running.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"
sudo systemctl start docker
docker start mysql-wedding
nohup java ${JAVA_OPTS:-} \
  -Dspring.config.additional-location=optional:file:./application.properties \
  -jar app.jar "$@" > server.log 2>&1 &
LAUNCH
chmod +x "$STAGE_DIR/run.sh"

# --- 4. Zip it up ------------------------------------------------------------
log "Creating zip"
cd "$DIST_DIR"
zip -r -q "$ZIP_PATH" "$(basename "$STAGE_DIR")"

log "Done"
echo "  Package: $ZIP_PATH"
