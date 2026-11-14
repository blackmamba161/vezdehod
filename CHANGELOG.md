# Changelog

## 0.2.17

- Add compact automatic/manual DNS settings under Connection, with optional backup, direct-route and bootstrap resolvers.
- Retain suitable subscription DNS, follow server selection, and use bounded same-route DNS fallback without restarting the VPN.
- Preserve manual settings and verify DNS on demand in an isolated engine.
- Fix stale cached proxy selection, isolate probe caches and avoid reconnecting for unchanged subscription refreshes.

## 0.2.16

- Subscription action menu with edit, sensitive URL copy, Android sharing, refresh, server list and deletion.
- Deletion lists affected assignments and offers a replacement server; changes are applied together. Removing the final servers clears assignments and disables linked rules after confirmation.
- Rebuilt sing-box and Xray with Go 1.27.4 and updated Go dependencies; native ELF segments support 16 KB pages.
- Explicit confirmation for unsafe imported TLS/transport settings; background refresh cannot silently approve them.
- Private runtime configuration permissions and cleanup after process restart.
- Compact Android notification permission controls and an in-app privacy summary.


## 0.2.15

- Add an Updates screen with manual and automatic update checks.
- Verify downloaded APK checksums, application identity, version, and signing certificate before installation.
- Use Android's download manager for progress, network constraints, and download retries.
- Keep installation under user control and preserve existing VPN settings.

## 0.2.14

- Verify connectivity through the selected server before reporting a connected state.
- Show a connection error and a server-selection action when connectivity checks fail.
- Prevent network callbacks from replacing a failed connection state with a false success state.

## 0.2.13

- Show protocol and transport details derived from actual server configurations.
- Preserve provider descriptions in the server list and network server picker.
