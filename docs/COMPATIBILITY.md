# Compatibility and test status

## Reference setup

- TCL Smart TV Pro / G08, Android 14, Linux 5.15.
- 32-bit ARM Android userspace.
- Android System WebView 157.0.8103.50.
- 3840×2160 panel, with Android reporting a 1920×1080 logical display and density 320.
- Microsoft receiver 045e:02e6 and Xbox One controller model 1697.
- Test date: 24 November 2026.

## Confirmed in the 0.2.x prototype

USB enumeration, radio startup, pairing, GIP security handshake, controller input in the local Gamepad API diagnostic page, all standard digital controls, analog triggers and both sticks.

The Luna UK page redirected from the US page. Both exact origins now receive the controller adapter and TV viewport correction. Completed page loads measured 1280×720 CSS pixels, document and scroll widths of 1280, DPR 2 and visual scale 0.75. This is a UI measurement, not the video-stream resolution.

## Playback problems reproduced

The tester reported:

- Browser not fully supported.
- Decode issues detected.
- Very low frame rate.
- Network issue detected.
- Reducing Luna to 720p improves playback, but it remains choppy with repeated decode warnings.

During the slow stream, MediaCodec log counters indicated approximately 8–10 released video buffers per second, and Chromium reported repeated WebRTC frame/timestamp warnings. These are diagnostic observations, not a measured presentation-FPS benchmark or a proven root cause.

A later sample showed repeated MediaCodec dropped-frame messages. A short Mac-to-TV ping test had no loss across 10 packets, averaging 8.7 ms with a 29.4 ms maximum; this does not measure the path to Luna.

Android reported no active thermal throttling. The Wi-Fi link reported 5 GHz, RSSI around -53 dBm and a 780 Mbps negotiated transmit rate; negotiated speed is not an end-to-end bandwidth, latency or packet-loss measurement. Codec selection, decoder queues, browser processing and network jitter need further investigation.

**Do not interpret working controller input or a successful game launch as evidence of smooth, reliable gameplay.**

## Not yet verified

- Public 0.3.0 firmware picker and file-backed native startup on the TV.
- Stable 10-minute game playback with acceptable video/audio latency.
- A proven codec/frame-pacing fix; 720p does not resolve the reported problem.
- Login persistence, active-game recovery and network interruption handling.
- The 045e:02fe receiver or other TV models.
- Rumble, headset audio, voice chat and keyboard/mouse gameplay.

## Automated checks

- Android release build and lint.
- Navigation, protected-media origin and user-agent policy tests.
- Browser gamepad mapping, disconnect/release and native-controller coexistence tests.
- Viewport insertion/hydration regression tests.
- Firmware size/hash validation, including rejection of malformed inputs.

Testing on additional devices is welcome. Include the app version, TV model, Android/WebView versions, controller model, receiver USB IDs and whether the issue is in native navigation, local diagnostics or an actual Luna game.
