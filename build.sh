#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .build/env.sh ]; then . .build/env.sh; fi
export VPN_ROOT="$PWD"
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
: "${JAVA_HOME:=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
export JAVA_HOME ANDROID_HOME
export PATH="$JAVA_HOME/bin:$PATH"
python3 tools/configure_updates.py
python3 tools/localize.py --generate
python3 tests/localization_test.py
python3 tools/check_native.py
B="$VPN_ROOT/.build/app"
VARIANT="${1:-release}"
ARCH="${2:-universal}"
case "$ARCH" in
 universal) VPN_ABIS='arm64-v8a armeabi-v7a'; VPN_SUFFIX='' ;;
 arm64-v8a) VPN_ABIS='arm64-v8a'; VPN_SUFFIX='-arm64' ;;
 *) echo 'Use architecture universal or arm64-v8a' >&2; exit 1 ;;
esac
if [ "$VARIANT" != test ] && [ "$VARIANT" != release ]; then echo "Use: build.sh [test|release]" >&2; exit 1; fi
SDK="$ANDROID_HOME"
BT="$SDK/build-tools/36.0.0"
JAR="$SDK/platforms/android-36/android.jar"
python3 -c 'import shutil;[shutil.rmtree(".build/app/"+p,ignore_errors=True) for p in ["classes","dex","package","aar"]]'
mkdir -p "$B" "$B/classes" "$B/generated" "$B/dex" "$B/aar" "$B/package/lib/arm64-v8a" "$VPN_ROOT/private"
unzip -oq vendor/sing-box-1.12.12/libbox.aar -d "$B/aar"
"$BT/aapt2" compile --dir app/res -o "$B/resources.zip"
python3 - "$VARIANT" <<'PYMANIFEST'
import sys,re,pathlib
s=pathlib.Path('app/AndroidManifest.xml').read_text()
if sys.argv[1]=='release':
 s=s.replace('android:debuggable="true"','android:debuggable="false"')
 s=re.sub(r'<instrumentation\b[^>]*/>','',s)
pathlib.Path('.build/app/AndroidManifest.xml').write_text(s)
PYMANIFEST
"$BT/aapt2" link -o "$B/base.apk" -I "$JAR" --manifest "$B/AndroidManifest.xml" --java "$B/generated" --auto-add-overlay "$B/resources.zip"
find app/src "$B/generated" -name '*.java' ! -name "$([ "$VARIANT" = release ] && echo SelfTest.java || echo __none__)" -print0 | xargs -0 "$JAVA_HOME/bin/javac" -source 17 -target 17 -encoding UTF-8 -classpath "$JAR:$B/aar/classes.jar:$VPN_ROOT/vendor/zxing/core-3.5.3.jar:$VPN_ROOT/vendor/snakeyaml/snakeyaml-2.7.jar" -d "$B/classes"
"$JAVA_HOME/bin/jar" cf "$B/app.jar" -C "$B/classes" .
"$BT/d8" --min-api 29 --lib "$JAR" --output "$B/dex" "$B/app.jar" "$B/aar/classes.jar" "$VPN_ROOT/vendor/zxing/core-3.5.3.jar" "$VPN_ROOT/vendor/snakeyaml/snakeyaml-2.7.jar"
for abi in $VPN_ABIS; do
 mkdir -p "$B/package/lib/$abi"
 cp "$B/aar/jni/$abi/libbox.so" "$B/package/lib/$abi/"
 if [ "$abi" = arm64-v8a ]; then cp vendor/xray/xray "$B/package/lib/$abi/libxray.so"; else cp "vendor/xray/$abi/xray" "$B/package/lib/$abi/libxray.so"; fi
done
cp "$B/dex/"*.dex "$B/package/"
cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/package" && zip -qr "$B/unsigned.apk" .)
"$BT/zipalign" -f -P 16 4 "$B/unsigned.apk" "$B/aligned.apk"
if [ ! -f private/signing.p12 ]; then
  python3 -c 'import secrets,pathlib; p=pathlib.Path("private/signing-password");p.write_text(secrets.token_hex(24));p.chmod(0o600)'
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore private/signing.p12 -storepass:file private/signing-password -alias personal-vpn -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Personal VPN' >/dev/null 2>&1
  chmod 600 private/signing.p12
fi
APK="$VPN_ROOT/Vezdehod$VPN_SUFFIX.apk"
if [ "$VARIANT" = test ]; then APK="$VPN_ROOT/Vezdehod-test$VPN_SUFFIX.apk"; fi
"$BT/apksigner" sign --ks private/signing.p12 --ks-pass file:private/signing-password --out "$APK" "$B/aligned.apk"
"$BT/apksigner" verify "$APK"
ls -lh "$APK"

python3 - "$VARIANT" "$APK" "$ARCH" "$VPN_SUFFIX" <<'PYRECEIPT'
from pathlib import Path
import sys,json,hashlib
apk=Path(sys.argv[2]);Path('.build/release-receipt'+sys.argv[4]+'.json').write_text(json.dumps({'architecture':sys.argv[3],'variant':sys.argv[1],'repository':json.loads(Path('release-config.json').read_text())['repository'],'sha256':hashlib.sha256(apk.read_bytes()).hexdigest()}))
PYRECEIPT
