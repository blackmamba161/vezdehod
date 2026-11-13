# Changelog

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
