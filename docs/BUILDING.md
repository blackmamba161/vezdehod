# Building Vezdehod

The current build uses the Android command-line tools directly; Gradle is not required.

## Toolchain

- Linux with Bash, Python 3, zip, unzip, and a JDK 17-compatible Java toolchain.
- Android SDK platform 36 and Build Tools 36.0.0.
- For rebuilding libbox: the Go and Android NDK versions recorded in `build-lock.json`.

Set `JAVA_HOME` and `ANDROID_HOME`. A local `.build/env.sh` may provide these variables, but it must not be committed because it contains machine-specific paths.

## Dependencies

The full source archive prepared for a release includes the required dependency files and the corresponding sing-box source. A repository checkout requires these inputs before packaging:

- `vendor/sing-box-1.12.12/libbox.aar`
- `vendor/xray/xray` (Android ARM64 executable) and `vendor/xray/armeabi-v7a/xray` (Android ARMv7 executable)
- `vendor/zxing/core-3.5.3.jar`
- `vendor/snakeyaml/snakeyaml-2.7.jar`

Compare prebuilt dependency hashes with `build-lock.json`. Rebuilding libbox creates a different artifact hash; record the new hash after verifying that build.

To rebuild both engines with the pinned security updates, install the recorded Go version and the upstream SagerNet gomobile/gobind tools, set ANDROID_NDK_HOME, and run `GO=/path/to/go bash tools/rebuild_cores.sh`. The full source archive includes `vendor/xray-source`, and both engines retain their original protocol implementations.

The pinned HTTP/2 package uses its upstream `http2legacy` compatibility tag and `-gcflags=golang.org/x/net/http2=-l` to retain the symbol used by sing-box 1.12’s transport reset. Device JNI loading is checked before publication.

The bundled sing-box source also contains the Vezdehod DNS fallback/selector transports in `dns/transport/vezdehod` and their registration in `include/registry.go`. Keep these changes when rebuilding; copying `mobile_app.go` into unmodified upstream source alone is insufficient.

For reference, to rebuild the customized libbox, copy `mobile_app.go` into `vendor/sing-box-1.12.12/experimental/libbox/mobile_app.go`, then use the upstream Android library build process with Android ARM64 as the target. Keep the `with_gvisor`, `with_quic`, `with_wireguard`, `with_utls`, `with_clash_api`, and `with_conntrack` features required by the application. Consult the bundled upstream build script for the pinned version's additional flags.

## APK

```sh
bash build.sh release
```

The output is `Vezdehod.apk`. Test builds use `bash build.sh test` and include instrumentation; do not publish them.

The first local build generates a signing key in `private/` if one is absent. A newly generated key cannot update someone else's existing installation. Official builds must use the existing official signing key.

`release-config.json` holds the public GitHub `owner/repository` used by the updater. An empty value disables the update source in development builds. Release preparation refuses an empty repository.

## Verification

The build validates interface resources in all supported languages and verifies the APK signature. Additional host tests live in `tests/`. Device instrumentation is included only in test builds. Device tests must use disposable fixtures or restore the original configuration. Every candidate build must be verified on the Poco C51 test device before it is handed off; host checks and checks on a more powerful phone do not replace this step. The final ordinary APK must also be installed and smoke-tested after instrumentation.

The APK includes ARM64 and 32-bit ARMv7 libraries. The ARMv7 build uses the Android system TUN stack because gVisor is unavailable on that architecture. `tools/check_native.py` verifies both engines for both architectures before packaging. Application code must use Java APIs available on Android 10 (API 29); `Compat` provides UTF-8 text file and collection operations without newer Java runtime dependencies.

Xray Android workers use the app-owned same-UID Unix socket to protect and bind outbound file descriptors. The VPN app remains in its own TUN UID range so the system TCP stack can receive kernel replies. Do not reintroduce a package-wide VPN exclusion.

For interface changes, run `tests/device_safety.py --serial DEVICE_SERIAL theme-020` with a test APK, then inspect the captured screens in the app-private `files/test-screens` directory. This covers both themes, grouped Settings, native choices, inputs, app search, DNS/server/route pickers and 120% text. It checks text contrast and verifies that encrypted settings remain unchanged. Screens can contain private subscription data and must not be published.
