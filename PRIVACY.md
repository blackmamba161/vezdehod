# Privacy

Vezdehod stores VPN subscriptions and configuration on your device. The configuration store is encrypted using a key held by Android Keystore. Android cloud backup is disabled for the application. While Xray is running, generated engine configurations also exist as private temporary files with owner-only permissions; they are removed on normal shutdown and cleaned up when the app process next starts.

Network requests depend on the features you use:

- Subscription imports and refreshes contact the subscription URL you provide. The global **Send HWID** setting is on by default for new installations. An existing saved choice is preserved; you can turn it off under Settings → Protection. When enabled, these requests send an app-generated ID specific to the URL's HTTPS origin (scheme, host and port). Redirects to other origins do not receive the ID. The random seed stays in private app storage excluded from backup; it survives app updates but not clearing app data or uninstalling. A provider may count a new ID as a new device. No IMEI, serial number or MAC address is read.
- VPN connections contact the servers and services selected by your configuration.
- Connectivity checks contact Google and Cloudflare test endpoints through the server being checked.
- Update checks contact the configured public GitHub repository. Downloads contact GitHub and its release-asset delivery infrastructure. These services receive normal connection metadata such as your IP address; update checks also include the app version in the User-Agent header.
- The update mechanism does not upload subscription URLs, VPN credentials, routing rules, or device logs.

You can disable automatic update checks in Settings → Updates. Manual checks contact GitHub only when requested. The QR scanner uses the camera while its screen is open.

Avoid sharing diagnostic logs without reviewing them. Provider URLs and server details may identify your account even when they do not look like passwords.

Sharing or copying a subscription is performed only at your request. The Android share chooser receives the subscription URL, which can contain account credentials. Clipboard copies are marked sensitive. Unsafe imported transport or certificate settings require confirmation before being saved; background refresh does not silently approve new unsafe settings.
