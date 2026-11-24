# Luna TV (Unofficial)

[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0--or--later-blue)](LICENSE.txt)
![Android 10+](https://img.shields.io/badge/Android-10%2B-green)
![Status: experimental](https://img.shields.io/badge/status-experimental-orange)

An experimental Android TV wrapper for Amazon Luna's web player, with Xbox controller navigation and an app-scoped driver for Xbox Wireless USB receivers.

**Controller support works on the tested TCL. Smooth game streaming does not yet.** Luna displays an unsupported-browser warning, and testing has produced decoder/network warnings and a very low frame rate. This project is a development preview, not an official or proven replacement for a supported Luna device.

Not affiliated with Amazon, Microsoft, TCL, Moonlight or libusb. No Amazon app code, account credentials or vendor receiver firmware is distributed here.

## Features

- Android TV / Google TV launcher entry and full-screen WebView.
- Desktop page layout sized for TV, including Luna's US-to-UK redirect.
- Xbox left-stick pointer, right-stick scrolling, A to click, and a separate Play mode.
- Experimental Xbox Wireless receiver support without root, using an Android adaptation of [xow](https://github.com/medusalix/xow).
- Standard browser gamepad mapping for digital buttons, both analog triggers and four stick axes.
- Local controller/browser diagnostics.
- Persistent cookies and website storage, with a clear-sign-in-data option.

The receiver driver operates **inside this app only**. It does not turn the receiver into an Android system-wide gamepad.

## What has been tested

| Area | Result |
| --- | --- |
| TCL Smart TV Pro / G08, Android 14, 32-bit ARM | App launches and renders |
| Microsoft Xbox Wireless Adapter 045e:02e6 | USB access, radio startup and pairing work |
| Xbox One controller model 1697 | Wireless handshake and browser inputs confirmed |
| A/B/X/Y, D-pad, bumpers, LT/RT and both sticks | Confirmed by the tester in browser diagnostics |
| Luna UK desktop layout | Corrected clipping; validated after complete page loads |
| Game streaming | Unsupported-browser, decode and network warnings; very low frame rate reported |
| Login persistence | Existing sign-in survived the tested in-place debug update |
| Public firmware path | File-backed native startup verified; TV document-picker flow remains untested |

The hardware results above came from the private 0.2.x prototype. Public 0.3.0 replaces its embedded receiver firmware with a user-supplied firmware import. See [compatibility notes](docs/COMPATIBILITY.md).

## Install and use

1. Obtain a preview APK from [Releases](https://github.com/KrystianDud/luna-tv/releases), or build from source.
2. Sideload it onto Android TV / Google TV running Android 10 or later.
3. Keep Android System WebView up to date.
4. Connect a compatible controller:
   - **Xbox model 1697 / Xbox Wireless receiver:** follow [firmware and receiver setup](docs/FIRMWARE.md). This controller has no Bluetooth.
   - **Bluetooth controller:** pair through the TV's Remotes & accessories settings. Other models have not been hardware-tested here.
5. Open **Check controller and browser** and verify the input indicators.
6. Open Luna, sign in directly on Amazon's page, and select a game available to your account. Switch to Play mode for gameplay.

The public build requires separately obtained firmware only for the Xbox USB receiver. Ordinary Android controllers and the web player do not require it.

With ADB already connected:

```sh
adb -s YOUR_TV_SERIAL install -r app/build/outputs/apk/release/app-release.apk
adb -s YOUR_TV_SERIAL shell am start -n dev.local.lunatv/.MainActivity
```

## Controls

| Input | Browse mode | Play mode |
| --- | --- | --- |
| Left stick | Move pointer | Game input |
| Right stick | Scroll | Game input |
| A | Click | Game input |
| B | Open app menu | Game input |
| D-pad | Move pointer | Game input |
| LB / RB | Scroll | Game input |
| Menu ☰ | Enter Play mode | Game input |
| Hold View ⧉ for one second, then release | App menu | App menu; View also reaches the game |
| TV remote Back | App menu | App menu |

A matching keyboard/mouse USB receiver can help with navigation and sign-in. Keyboard/mouse gameplay and pointer lock are not verified.

After leaving the app or returning from Home, start the Xbox receiver again from **Xbox USB receiver → Start receiver**. Pairing may need to be repeated after radio reinitialization.

## Limits and troubleshooting

- **Unsupported browser:** Android TV WebView is outside the official support matrix. Desktop mode changes the page presentation; it cannot guarantee stream compatibility.
- **Decode/network warnings or low FPS:** currently reproduced on the test TCL. A fast speed test does not rule out packet loss, jitter or client processing delays. 720p improved playback in testing, but choppiness and decode warnings persisted.
- **Resolution:** the TV panel, Android UI and game stream have separate resolutions. [Amazon describes Luna streaming at 1080p/60fps](https://aws.amazon.com/blogs/gametech/aaa-games-on-a-35-stick-how-luna-removes-the-hardware-barrier-with-aws/). This app does not unlock native 1440p or 4K streaming.
- **Rumble, headset audio and voice chat:** not exposed by this app.
- **Receiver support:** 045e:02e6 tested; 045e:02fe is recognized by the driver but untested here. Only one receiver/controller input stream is integrated with the browser.
- **Regions:** page input/viewport adapters and protected-media permissions currently cover the exact `luna.amazon.com` and `luna.amazon.co.uk` origins.
- **Sessions:** persistent cookies and local storage are retained. Temporary `sessionStorage`, the last page and active games are not restored after app termination.

## Privacy

Login happens on Amazon's HTTPS pages. The app exposes no JavaScript-to-Java object to websites and collects no analytics. Controller updates sent into the browser contain numeric input state only and are restricted to Luna's supported origins and the local diagnostics origin.

No microphone, camera, Bluetooth or broad storage permission is requested. The Android document picker grants access to the firmware file selected by the user. Validated firmware is copied into app-private storage. Android USB access is requested for the supported receiver.

App backups and release WebView debugging are disabled. Cookies and local storage can be removed using **Clear local sign-in data**. Android logcat may contain diagnostic information; inspect and redact logs before sharing them.

## Build

Requirements:

- JDK 17
- Android SDK Platform 35 and Build Tools 35.0.0
- Android NDK 27.1.12297006
- CMake 3.22.1
- Node.js for the standalone JavaScript tests

The Gradle 8.14.3 wrapper and Android Gradle Plugin 8.13.2 are pinned. Set `ANDROID_HOME`, or put `sdk.dir` in an untracked `local.properties`.

```sh
./gradlew assembleRelease lint
./scripts/test.sh
```

Build output: `app/build/outputs/apk/release/app-release.apk`. Both `armeabi-v7a` and `arm64-v8a` are built. Native sources, libusb and build configuration are included; the firmware blob is not needed to compile.

Preview releases use the builder's local Android debug signing key with application debugging disabled. No key is committed. Your own build may have a different signature from a downloaded preview and cannot update it in place; uninstalling clears app data. Production signing and Play Store distribution are outside the current scope.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Useful contributions include reproducible WebView playback diagnostics, codec/frame pacing fixes, receiver reconnect handling and tests on other TVs. Please separate controller functionality from game-stream quality in reports.

## License and credits

Project source: **GPL-3.0-or-later**. See [LICENSE.txt](LICENSE.txt) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

The receiver work depends on [medusalix/xow](https://github.com/medusalix/xow), its [trexx/moonlight-android port](https://github.com/trexx/moonlight-android), and [libusb](https://github.com/libusb/libusb). Their existing copyrights and licenses are preserved. Vendor firmware remains separately obtained software and is not licensed or redistributed by this project.
