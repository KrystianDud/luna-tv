# Changelog

## 0.3.0 — public development preview

- Publish the Android TV wrapper and Xbox Wireless receiver integration under GPL-3.0-or-later.
- Replace embedded vendor firmware with a separately supplied, checksum-validated import.
- Include native sources and libusb, build instructions and standalone regression tests.
- Document confirmed controller input, corrected US/UK desktop layout, and unresolved streaming performance problems.

The public firmware import path has not yet been tested end-to-end on the reference TV. Existing prototype hardware results are described in docs/COMPATIBILITY.md.

## 0.2.x — private prototype

- Pair and read a model 1697 controller through the 045e:02e6 USB receiver on TCL Android 14.
- Feed input to an origin-scoped standard Gamepad API adapter.
- Correct Luna UK redirects and viewport tags inserted during page startup.
- Observe unsupported-browser, decode/network warnings and low frame rate during gameplay.
