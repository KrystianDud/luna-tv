# Third-party notices

## Project license

Original Luna TV integration code is licensed under **GPL-3.0-or-later**. Copyright (C) 2026 Luna TV contributors. See LICENSE.txt. Third-party files retain their original notices and applicable licenses.

## xow / Android port

- [medusalix/xow](https://github.com/medusalix/xow), commit `d335d6024f8380f52767a7de67727d9b2f867871`: Copyright Medusalix, GPL-2.0-or-later.
- [trexx/moonlight-android](https://github.com/trexx/moonlight-android), commit `821459f53bbfc9aad2a6ad65c6afe1dc26d311f9`: GPL-3.0-or-later project containing the Android adaptation.

Imported controller, dongle and utility source lives under `app/src/main/cpp/xow`. `GipCrypto.java` is adapted from the Android port, and `receiver_jni.cpp` is a reduced adaptation of its JNI entry points. Original file copyright/license headers remain intact.

Local changes include the CMake build, app JNI bindings, checked USB construction and cleanup, removal of the embedded firmware, file-backed firmware loading and bounds checks. The native wired-controller interface, rumble and audio functions are not exposed by this app. Java USB lifecycle, firmware import, browser adapter and TV UI integration are project-specific.

`app/src/main/cpp/xow/UPSTREAM.md` documents the Android port's history; the local adaptation note distinguishes this project from that upstream snapshot.

## libusb

[libusb/libusb](https://github.com/libusb/libusb), commit `87a55632db62c9bdc58cd31d3ccfa673f1bb017f`, LGPL-2.1-or-later. Full vendored source and COPYING are under `app/src/main/cpp/libusb`. Built as a separate `libusb1.0.so` using the upstream Android source list/configuration and this project's CMake definition.

## AndroidX and Gradle

AndroidX WebKit 1.12.1 and its Maven-resolved AndroidX dependencies use Apache-2.0. [AndroidX source and licensing](https://android.googlesource.com/platform/frameworks/support/). Used for origin-scoped document-start JavaScript.

The Gradle wrapper is from Gradle 8.14.3, Apache-2.0; see [Gradle's license](https://github.com/gradle/gradle/blob/v8.14.3/LICENSE).

## Vendor firmware and trademarks

No receiver firmware is included in the public source or APK. Users obtain it separately; its vendor terms remain separate from this project's open-source license. The checksum identifies the revision expected by upstream xow.

Amazon Luna, Xbox, TCL and other product names belong to their respective owners and are used to describe compatibility. This project is unofficial and contains no Amazon application code.
