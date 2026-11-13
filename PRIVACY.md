# Privacy

Vezdehod stores VPN subscriptions and configuration on your device. The configuration store is encrypted using a key held by Android Keystore. Android cloud backup is disabled for the application.

Network requests depend on the features you use:

- Subscription imports and refreshes contact the subscription URL you provide.
- VPN connections contact the servers and services selected by your configuration.
- Connectivity checks contact Google and Cloudflare test endpoints through the server being checked.
- Update checks contact the configured public GitHub repository. Downloads contact GitHub and its release-asset delivery infrastructure. These services receive normal connection metadata such as your IP address; update checks also include the app version in the User-Agent header.
- The update mechanism does not upload subscription URLs, VPN credentials, routing rules, or device logs.

You can disable automatic update checks in Settings → Updates. Manual checks contact GitHub only when requested. The QR scanner uses the camera while its screen is open.

Avoid sharing diagnostic logs without reviewing them. Provider URLs and server details may identify your account even when they do not look like passwords.
