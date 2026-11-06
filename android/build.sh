#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
repo="$(cd .. && pwd)"
SDK="${ANDROID_BUILD_TOOLS:-/workspace/work/android-sdk/android-15}"
PLATFORM="${ANDROID_PLATFORM:-/workspace/work/android-sdk/android-35/android.jar}"
SIGNING_KEY="${SIGNING_KEY:-$repo/.local/planwithme-signing.jks}"
export SIGNING_STORE_PASSWORD="${SIGNING_STORE_PASSWORD:-android}"
export SIGNING_KEY_PASSWORD="${SIGNING_KEY_PASSWORD:-$SIGNING_STORE_PASSWORD}"
rm -rf build/classes build/dex build/generated build/assets
mkdir -p build/classes build/dex build/generated build/assets dist
cp -R "$repo/web/." build/assets/
"$SDK/aapt2" compile --dir res -o build/resources.zip
"$SDK/aapt2" link -o build/base.apk -I "$PLATFORM" --manifest AndroidManifest.xml --java build/generated -A build/assets build/resources.zip
mapfile -t sources < <(find src build/generated -name '*.java' | sort)
if command -v javac >/dev/null; then
 javac --release 8 -classpath "$PLATFORM" -d build/classes "${sources[@]}"
else
 java -jar "${ECJ_JAR:-/workspace/work/android-sdk/ecj.jar}" -8 -nowarn -bootclasspath "$PLATFORM:$SDK/core-lambda-stubs.jar" -d build/classes "${sources[@]}"
fi
mapfile -t classes < <(find build/classes -name '*.class' | sort)
"$SDK/d8" --lib "$PLATFORM" --min-api 26 --output build/dex "${classes[@]}"
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:z.write('build/dex/classes.dex','classes.dex')
PY
"$SDK/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f "$SIGNING_KEY" ]; then
 mkdir -p "$(dirname "$SIGNING_KEY")"
 keytool -genkeypair -keystore "$SIGNING_KEY" -alias planwithme -keyalg RSA -keysize 2048 -validity 10000 -storepass:env SIGNING_STORE_PASSWORD -keypass:env SIGNING_KEY_PASSWORD -dname 'CN=PlanWithMe Local Development' >/dev/null 2>&1
fi
"$SDK/apksigner" sign --ks "$SIGNING_KEY" --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_KEY_PASSWORD --out dist/PlanWithMe.apk build/aligned.apk
"$SDK/apksigner" verify --verbose --print-certs dist/PlanWithMe.apk
"$SDK/aapt2" dump badging dist/PlanWithMe.apk
mkdir -p "$repo/releases/android" "$repo/dist"
cp dist/PlanWithMe.apk "$repo/releases/android/PlanWithMe-1.1.apk"
cp dist/PlanWithMe.apk "$repo/dist/PlanWithMe.apk"
