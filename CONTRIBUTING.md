# Contributing

This is an experimental client. Keep confirmed controller support separate from unverified or failing game-stream behavior.

Run these checks before submitting changes:

```sh
./gradlew assembleRelease lint
./scripts/test.sh
```

For firmware acceptance tests with your own separately obtained binary:

```sh
./scripts/test.sh /path/to/FW_ACC_00U.bin
```

Do not commit firmware, APKs containing firmware, signing keys, credentials, cookies, private IP addresses or unredacted device logs. The repository intentionally excludes local builds, personal test captures and firmware files.

Preserve upstream copyright/license notices and document native driver changes in THIRD_PARTY_NOTICES.md. Contributions to project-specific code are under GPL-3.0-or-later.

For bug reports include app version, Android and WebView versions, TV model, controller/receiver model, reproduction steps and expected/actual behavior. Include streaming resolution and wired/Wi-Fi connection type for playback problems. Redact account details, device serials and network names from logs and screenshots.

A useful playback fix should include before/after measurements and a physical-device test. Avoid presenting user-agent changes, supported-codec declarations or a successful build as proof of playable streaming.
