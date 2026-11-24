// SPDX-License-Identifier: GPL-3.0-or-later
package dev.local.lunatv.receiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Accepts the firmware revision used by upstream xow; contains no firmware bytes. */
public final class FirmwareImage {
    public static final int SIZE = 70620;
    public static final String SHA256 = "48084d9fa53b9bb04358f3bb127b7495dc8f7bb0b3ca1437bd24ef2b6eabdf66";
    private FirmwareImage() {}
    public static byte[] readValidated(InputStream input) throws IOException {
        if (input == null) throw new IOException("Could not open the selected firmware.");
        ByteArrayOutputStream output = new ByteArrayOutputStream(SIZE);
        byte[] buffer = new byte[4096];
        for (int count; (count = input.read(buffer)) != -1;) {
            if (output.size() + count > SIZE) throw new IOException("Firmware is too large; select FW_ACC_00U.bin.");
            output.write(buffer, 0, count);
        }
        byte[] bytes = output.toByteArray();
        if (bytes.length != SIZE) throw new IOException("Firmware size is incorrect; select FW_ACC_00U.bin.");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            if (!SHA256.contentEquals(hex)) throw new IOException("Firmware checksum does not match the supported revision.");
        } catch (NoSuchAlgorithmException e) { throw new IOException("SHA-256 unavailable", e); }
        return bytes;
    }
}
