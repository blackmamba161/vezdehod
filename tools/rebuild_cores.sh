#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
VPN_BUILD_ROOT="$PWD"
VPN_GO="${GO:-go}"
if [ "$("$VPN_GO" env GOVERSION)" != go1.27.4 ]; then
  echo 'Use Go 1.27.4 as recorded in build-lock.json (set GO to its executable).' >&2
  exit 1
fi
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
: "${JAVA_HOME:?Set JAVA_HOME}"
export PATH="$(dirname "$(command -v "$VPN_GO")"):${GOPATH:-$("$VPN_GO" env GOPATH)}/bin:$JAVA_HOME/bin:$PATH"
export GOTOOLCHAIN=local
# cgo splits CC command strings; Android NDK paths containing spaces need a short alias.
VPN_NDK_ALIAS=$(mktemp -d /tmp/vezdehod-ndk.XXXXXX)
trap 'rm -rf -- "$VPN_NDK_ALIAS"' EXIT
ln -s "$ANDROID_NDK_HOME" "$VPN_NDK_ALIAS/ndk"
export ANDROID_NDK_HOME="$VPN_NDK_ALIAS/ndk"
export CGO_LDFLAGS='-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384'
cp mobile_app.go vendor/sing-box-1.12.12/experimental/libbox/mobile_app.go
(
  cd vendor/sing-box-1.12.12
  gomobile bind -v -target android/arm64 -androidapi 21 -javapkg io.nekohasekai -libname box \
    -trimpath -buildvcs=false -gcflags='golang.org/x/net/http2=-l' \
    -ldflags='-X github.com/sagernet/sing-box/constant.Version=1.12.12 -s -w -buildid= -checklinkname=0' \
    -tags http2legacy,with_gvisor,with_quic,with_wireguard,with_utls,with_clash_api,with_conntrack ./experimental/libbox
)
(
  cd vendor/xray-source
  GOOS=android GOARCH=arm64 CGO_ENABLED=0 "$VPN_GO" build -trimpath -buildvcs=false \
    -ldflags='-s -w -buildid= -checklinkname=0' -o "$VPN_BUILD_ROOT/vendor/xray/xray" ./main
)
# gVisor supports ARM64, but not ARMv7. Both builds expose the same Java API.
(
  cd vendor/sing-box-1.12.12
  gomobile bind -v -o libbox-arm.aar -target android/arm -androidapi 21 -javapkg io.nekohasekai -libname box \
    -trimpath -buildvcs=false -gcflags='golang.org/x/net/http2=-l' \
    -ldflags='-X github.com/sagernet/sing-box/constant.Version=1.12.12 -s -w -buildid= -checklinkname=0' \
    -tags http2legacy,with_quic,with_wireguard,with_utls,with_clash_api,with_conntrack ./experimental/libbox
)
python3 - <<'PYMERGE'
import zipfile,io
from pathlib import Path
root=Path('vendor/sing-box-1.12.12')
with zipfile.ZipFile(root/'libbox.aar') as arm64,zipfile.ZipFile(root/'libbox-arm.aar') as arm:
 a=zipfile.ZipFile(io.BytesIO(arm64.read('classes.jar')));b=zipfile.ZipFile(io.BytesIO(arm.read('classes.jar')))
 assert set(a.namelist())==set(b.namelist()),'ARM builds expose different Java classes'
 assert all(a.read(name)==b.read(name) for name in a.namelist()),'ARM builds expose different Java bytecode'
 members={i.filename:arm64.read(i) for i in arm64.infolist()}
 members['jni/armeabi-v7a/libbox.so']=arm.read('jni/armeabi-v7a/libbox.so')
with zipfile.ZipFile(root/'libbox.aar','w',zipfile.ZIP_DEFLATED) as output:
 for name,data in members.items():output.writestr(name,data)
(root/'libbox-arm.aar').unlink()
PYMERGE
mkdir -p vendor/xray/armeabi-v7a
(
 cd vendor/xray-source
 GOOS=android GOARCH=arm GOARM=7 CGO_ENABLED=1 \
   CC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi21-clang" \
   "$VPN_GO" build -trimpath -buildvcs=false \
   -ldflags='-s -w -buildid= -checklinkname=0' -o "$VPN_BUILD_ROOT/vendor/xray/armeabi-v7a/xray" ./main
)
echo 'Rebuilt ARM64 and ARMv7 sing-box and Xray. Verify binaries and refresh build-lock.json before release.'
