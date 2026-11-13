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
echo 'Rebuilt sing-box and Xray. Verify the binaries and refresh build-lock.json before release.'
