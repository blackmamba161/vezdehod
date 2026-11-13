# Releasing Vezdehod

## Repository configuration

Set `repository` in `release-config.json` to the public GitHub `owner/repository`. Public update checks do not require an account token. Never embed a GitHub token in the APK.

Keep all public repository descriptions, release titles, installation instructions, and release notes in English. The product name is **Vezdehod**; the repository name is **vezdehod**.

Suggested repository description:

> An Android VPN client with subscription management, per-network server selection, and flexible routing.

## Version and signing

Increase `versionCode` for every distributed build and update `versionName` in `app/AndroidManifest.xml`. Build with the existing release signing key. Keep the original key and an offline backup outside Git.

Run the relevant host and device checks. Test updates over an existing installation and confirm that subscriptions and routing rules remain intact.

## Prepare artifacts

```sh
bash build.sh release
python3 tools/prepare_release.py
```

The preparation script writes a versioned APK, checksum, source archive, and English notes under `release-output/`. It checks the repository setting, release manifest, and absence of debug instrumentation. It does not create commits, tags, push code, or publish releases.

The updater recognizes APK names in this format:

`Vezdehod-VERSION-VERSIONCODE-arm64-v8a.apk`

Keep that exact asset naming convention. GitHub's release asset API must expose the `sha256:` digest; without it, the updater rejects the asset.

## Publish

Create a GitHub release for the tested version and attach the prepared artifacts. Start with a draft, review its contents, then publish. Mark experimental builds as prereleases. The app ignores prereleases. The updater chooses the highest compatible published version code in the returned release list.

Upload APKs as release assets, not as files in the source repository. Include corresponding source and third-party notices with each release. Never upload `private/`, signing files, device fixtures, diagnostic captures, `.build/`, or personal subscriptions.

After publication, test the live release API, download the APK through the app, and complete the Android installation flow on a test device. Test cancellation and an interrupted download. Confirm that prereleases are ignored.
