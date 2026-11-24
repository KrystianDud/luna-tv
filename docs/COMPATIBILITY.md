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

## Instrumented 720p follow-up

A debug build of 0.3.0 retained the existing sign-in. The separately stored, validated receiver firmware started the radio and the controller reconnected. This verifies the file-backed native path, but not the Android document-picker import flow (the existing firmware was copied into app-private storage through ADB for this test).

Android's active media resource record identified `c2.mtk.avc.decoder` for the Luna process. A `MediaCodecList` probe reports this decoder as hardware accelerated, with advertised 720p60/1080p60 and low-latency capability. These are advertised capabilities, not a playback benchmark. Hardware decoding is therefore confirmed; 32-bit userspace does not prevent its use.

A 62.51-second WebRTC statistics interval at 1280×720 reported:

| Measurement | Result |
| --- | --- |
| Average decoded frame rate | 46.7 fps |
| Received video frames | 3,752 |
| Dropped video frames | 832 |
| Mean reported decode time per decoded frame | 59.7 ms |
| Video packet-loss counter increase | 0 |
| Freeze counter increase | 17 |
| Accumulated freeze duration increase | 6.27 s |

These are deltas from `RTCPeerConnection.getStats()` counters, not screen-recorded presentation measurements. Decode time can include asynchronous decoder queueing; it is not equivalent to serial CPU time or total input-to-display latency. Zero reported packet loss in this interval does not eliminate jitter or all network effects. Earlier samples showed greater decode delay. The evidence points toward the client decoder/browser pipeline, but does not isolate a single firmware or Chromium defect.

Chromium has a [low-delay MediaCodec selection path](https://chromium.googlesource.com/chromium/src/+/main/media/gpu/android/media_codec_video_decoder.cc), gated by `MediaCodecLowDelayMode`, [disabled by default in the inspected source](https://chromium.googlesource.com/chromium/src/+/main/media/base/media_switches.cc). Android's [low-latency decoder API](https://source.android.com/docs/core/media/low-latency-media) is distinct from hardware acceleration. The TV's Google WebView DevTools exposes `SkipMediaCodecReallocation` and `MediaCodecBlockModel`, both checked at Default after a reset, but not `MediaCodecLowDelayMode`.

A temporary command-line flag attempt was not applied: this production Android build ignores the flag file. It was removed. [Chromium documents](https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/commandline-flags.md) that production devices use the supported DevTools flag list. No successful low-delay comparison or performance fix is claimed.

## Not yet verified

- Public 0.3.0 firmware document-picker flow on the TV.
- Stable 10-minute game playback with acceptable video/audio latency.
- A proven codec/frame-pacing fix; 720p does not resolve the reported problem.
- Active-game recovery and network interruption handling; sign-in survived the tested in-place update.
- The 045e:02fe receiver or other TV models.
- Rumble, headset audio, voice chat and keyboard/mouse gameplay.

## Automated checks

- Android release build and lint.
- Navigation, protected-media origin and user-agent policy tests.
- Browser gamepad mapping, disconnect/release and native-controller coexistence tests.
- Viewport insertion/hydration regression tests.
- Firmware size/hash validation, including rejection of malformed inputs.

Testing on additional devices is welcome. Include the app version, TV model, Android/WebView versions, controller model, receiver USB IDs and whether the issue is in native navigation, local diagnostics or an actual Luna game.
