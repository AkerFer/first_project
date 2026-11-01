#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_BUILD_TOOLS:-/workspace/work/android-sdk/android-15}"
PLATFORM="${ANDROID_PLATFORM:-/workspace/work/android-sdk/android-35/android.jar}"
mkdir -p build/classes build/dex dist
"$SDK/aapt2" compile --dir res -o build/resources.zip
"$SDK/aapt2" link -o build/base.apk -I "$PLATFORM" --manifest AndroidManifest.xml -A assets build/resources.zip
if command -v javac >/dev/null; then
 javac --release 8 -classpath "$PLATFORM" -d build/classes src/ru/planwithme/app/MainActivity.java
else
 java -jar "${ECJ_JAR:-/workspace/work/android-sdk/ecj.jar}" -8 -nowarn -bootclasspath "$PLATFORM:$SDK/core-lambda-stubs.jar" -d build/classes src/ru/planwithme/app/MainActivity.java
fi
"$SDK/d8" --lib "$PLATFORM" --min-api 26 --output build/dex build/classes/ru/planwithme/app/*.class
cp build/base.apk build/unsigned.apk
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:z.write('build/dex/classes.dex','classes.dex')
PY
"$SDK/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f ../planwithme-signing.jks ]; then
 keytool -genkeypair -keystore ../planwithme-signing.jks -alias planwithme -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname 'CN=PlanWithMe Local Development' >/dev/null 2>&1
fi
"$SDK/apksigner" sign --ks ../planwithme-signing.jks --ks-pass pass:android --key-pass pass:android --out dist/PlanWithMe.apk build/aligned.apk
"$SDK/apksigner" verify --verbose dist/PlanWithMe.apk
"$SDK/aapt2" dump badging dist/PlanWithMe.apk
