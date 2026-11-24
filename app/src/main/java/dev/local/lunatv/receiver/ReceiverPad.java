// GPL-3.0-or-later. JNI callback contract adapted from Moonlight's GipController.
package dev.local.lunatv.receiver;

public final class ReceiverPad {
    private final ReceiverManager owner;
    private final int id;
    ReceiverPad(ReceiverManager owner, int id, long handle) {
        this.owner = owner; this.id = id;
        registerNative(handle);
    }
    private native void registerNative(long handle);
    public void updateInput(int buttons, short lt, short rt, short lx, short ly, short rx, short ry) {
        owner.input(id, buttons, lt & 0xffff, rt & 0xffff, lx, ly, rx, ry);
    }
    public void updateBattery(byte type, byte level, byte charge) {}
    public void audioDeviceRemoved() {}
}
