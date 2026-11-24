# Xbox receiver firmware

The public repository and public APK do not include Microsoft's receiver firmware. Bluetooth/native Android controller input does not need this file.

The Xbox Wireless receiver requires the `FW_ACC_00U.bin` revision also used by upstream xow. Obtain it separately from the Microsoft Xbox Wireless Adapter driver package, following the vendor's terms. The [upstream extraction script](https://github.com/medusalix/xow/blob/d335d6024f8380f52767a7de67727d9b2f867871/install/firmware.sh.in) documents the driver package, filename and checksum.

If you already have the driver CAB and `cabextract` installed:

```sh
cabextract -F FW_ACC_00U.bin /path/to/driver.cab
shasum -a 256 FW_ACC_00U.bin
```

Expected file:

- Size: **70,620 bytes**
- SHA-256: `48084d9fa53b9bb04358f3bb127b7495dc8f7bb0b3ca1437bd24ef2b6eabdf66`

The filename may also be `xow_dongle.bin`. The app verifies the bytes, not the name. Do not import the whole CAB, a Windows driver executable or an arbitrary firmware image.

## Import on the TV

1. Copy the extracted binary to a USB drive or another location accessible through Android's document picker.
2. Open **Xbox USB receiver → Import receiver firmware**.
3. Select the binary. If the TV has no document picker, install a file manager that implements Android's document-provider interface.
4. Wait for **Firmware imported**.
5. Choose **Start receiver** and allow Android USB access.
6. Choose **Pair controller**, then hold the controller's top pairing button until its Xbox light flashes rapidly.
7. Open **Test buttons and sticks**.

Import rejects an incorrect size or hash and preserves a previously valid import on failure. The native loader also checks segment lengths before USB firmware upload. The firmware file is stored inside the app sandbox; uninstalling or clearing app data removes it.

Firmware is loaded into the receiver's radio during startup. This app does not root or flash the TV. The import/picker path in 0.3.0 has been build-tested and its validation logic tested locally; full TV import and radio startup with this public path still require verification.
