# Third-party components

Vezdehod includes independent open-source components. The project is not affiliated with their authors.

| Component | Version | License | Source |
| --- | --- | --- | --- |
| sing-box / libbox | 1.12.12 | GPL-3.0-or-later, with the upstream naming notice | https://github.com/SagerNet/sing-box/tree/v1.12.12 |
| Xray-core | 26.11.9 | MPL-2.0 | https://github.com/XTLS/Xray-core/tree/v26.11.9 |
| ZXing Core | 3.5.3 | Apache-2.0 | https://github.com/zxing/zxing/tree/zxing-3.5.3 |
| SnakeYAML | 2.7 | Apache-2.0 | https://bitbucket.org/snakeyaml/snakeyaml |
| HaGeZi Light domain list | bundled snapshot | See bundled upstream license | https://github.com/hagezi/dns-blocklists |

License texts are included in `licenses/`. Exact downloaded artifact hashes are recorded in `build-lock.json`.

The libbox extension maintained by this project is `mobile_app.go`. It adds application-facing selection, connectivity, and diagnostic methods. The source archive includes the corresponding sing-box source tree and this extension. Go dependencies are identified by the upstream `go.mod` and `go.sum` files.

Both native engines are rebuilt with Go 1.27.4. Their Go dependency versions include security updates beyond the upstream tags; the pinned go.mod and go.sum files in the source archive describe the exact inputs. Xray source is included in vendor/xray-source. Build instructions are in tools/rebuild_cores.sh. No VPN provider credentials are included in public artifacts.
