# Vezdehod

An Android VPN client with subscription management, per-network server selection, and flexible routing.

Vezdehod is a client. You bring your own VPN subscription or server configuration.

## Features

- Import subscriptions from a link, clipboard, QR code, or supported configuration formats.
- Choose separate servers for Wi-Fi and mobile data.
- Route applications and services through the VPN or directly.
- Organize routing rules into service groups and apply built-in presets.
- Check server latency and verify connectivity before showing a connected state.
- Manage multiple subscriptions and view provider-supplied usage and expiration details.
- Follow the device language or choose from 12 interface languages.
- Check for updates manually or automatically and install when ready.

## Requirements

Android 10 or newer on an ARM64 device. VPN service availability depends on your provider and network.

## Installation

Open **Releases** in this repository and download the ARM64 APK. Open it on your phone and confirm installation in Android. For an existing installation, install the new APK over the old version to retain your settings.

On first connection, Android asks for VPN permission. Import your subscription, select a server for your current network, and connect.

## Updates

Open **Settings → Updates**. Automatic checks are enabled by default and can be turned off. **Check now** remains available. When a new version is available, tap **Download update**, then **Install**.

Downloaded updates are checked against their published SHA-256 digest, application ID, version, and signing certificate. Installation requires your confirmation. Installing an update restarts the app and interrupts the active VPN connection.

Only published stable releases are offered.

## Development

See [Building](docs/BUILDING.md), [Releasing](docs/RELEASING.md), and [third-party components](THIRD_PARTY.md).

## Privacy and security

See [Privacy](PRIVACY.md) and [Security](SECURITY.md). Do not post subscription URLs, credentials, or unredacted logs in public issues.

## License

Vezdehod application code is distributed under GPL-3.0-or-later. Third-party components retain their respective licenses and notices. See [LICENSE](LICENSE) and [THIRD_PARTY.md](THIRD_PARTY.md).
