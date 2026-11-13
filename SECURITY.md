# Security

Updates must use the same application identity and signing certificate as the installed application. Keep the release signing key and its password outside the repository and maintain an offline backup.

Do not attach live subscriptions, private keys, or unredacted diagnostic files to public reports. Report vulnerabilities using GitHub's private vulnerability reporting feature when it is enabled for this repository.

The update client accepts APK assets only from the configured GitHub repository. It checks the asset size and SHA-256 digest, then checks the APK package name, version, SDK compatibility, and signing certificate. Android performs the final installation checks.

A higher installed version is never automatically replaced with a lower version.
