// GPL-3.0-or-later. Uses the xow Android port through a private app-scoped USB connection.
package dev.local.lunatv.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.AtomicFile;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class ReceiverManager {
    public interface Listener {
        void onReceiverStatus(String message);
        void onReceiverInput(State state);
    }
    public static final class State {
        public final boolean connected;
        public final int buttons;
        public final float lt, rt, lx, ly, rx, ry;
        public State(boolean c, int b, float lt, float rt, float lx, float ly, float rx, float ry) {
            connected=c; buttons=b; this.lt=lt; this.rt=rt;
            this.lx=lx; this.ly=ly; this.rx=rx; this.ry=ry;
        }
        public static State empty() { return new State(false,0,0,0,0,0,0,0); }
    }
    private final Context context;
    private final UsbManager usb;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<Integer, ReceiverPad> pads = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private final Listener listener;
    private final String permissionAction;
    private volatile boolean closed, active;
    private volatile int firstPad = -1;
    private boolean opening;
    private long handle; // Access only on worker.
    private UsbDeviceConnection connection; // Access only on worker.
    private volatile UsbDevice device;
    private String status = "Receiver not started.";
    private long reports;
    private long lastLog;
    private native long createDriver(int fd);
    private native boolean startDriver(long driver, String firmwarePath);
    private native boolean pairDriver(long driver, boolean enabled);
    private native void destroyDriver(long driver);
    public ReceiverManager(Context context, Listener listener) {
        this.context=context; this.listener=listener;
        usb=context.getSystemService(UsbManager.class);
        permissionAction=context.getPackageName()+".RECEIVER_PERMISSION";
        IntentFilter filter = new IntentFilter(permissionAction);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else context.registerReceiver(receiver, filter);
    }
    public String status() { return status; }
    public boolean active() { return active; }
    private File firmwareFile() { return new File(context.getFilesDir(), "xow_dongle.bin"); }
    public void importFirmware(Uri uri) {
        if (closed || opening || active) { status("Stop the receiver before importing firmware."); return; }
        status("Checking receiver firmware…");
        worker.execute(() -> {
            AtomicFile destination = new AtomicFile(firmwareFile());
            FileOutputStream output = null;
            try (var input = context.getContentResolver().openInputStream(uri)) {
                byte[] bytes = FirmwareImage.readValidated(input);
                output = destination.startWrite();
                output.write(bytes);
                destination.finishWrite(output);
                output = null;
                status("Firmware imported. Choose Start receiver.");
            } catch (IOException | RuntimeException e) {
                if (output != null) destination.failWrite(output);
                status("Firmware import failed: " + e.getMessage());
            }
        });
    }
    private static boolean supported(UsbDevice d) {
        return d != null && d.getVendorId()==0x045e
            && (d.getProductId()==0x02e6 || d.getProductId()==0x02fe);
    }
    private void status(String message) {
        Log.i("LunaReceiver", message);
        main.post(() -> { if (!closed) { status=message; listener.onReceiverStatus(message); } });
    }
    public void connect() {
        if (closed || opening || active) return;
        if (!firmwareFile().isFile()) { status("Import receiver firmware first. See the project's firmware setup guide."); return; }
        UsbDevice found=null;
        for (UsbDevice d:usb.getDeviceList().values()) if(supported(d)) {found=d;break;}
        if (found==null) { status("No supported Xbox USB receiver found. Plug it into the TV."); return; }
        device=found;
        if (!usb.hasPermission(found)) {
            status("Allow USB access on the TV to start the receiver.");
            int flags=PendingIntent.FLAG_UPDATE_CURRENT;
            if(Build.VERSION.SDK_INT >= 31) flags|=PendingIntent.FLAG_MUTABLE;
            PendingIntent pi=PendingIntent.getBroadcast(context,0,
                new Intent(permissionAction).setPackage(context.getPackageName()),flags);
            usb.requestPermission(found,pi);
        } else open(found);
    }
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            UsbDevice d=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (permissionAction.equals(intent.getAction())) {
                if (supported(d) && d.equals(device) && usb.hasPermission(d)) open(d);
                else status("USB access was not granted.");
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())
                    && d!=null && d.equals(device)) stop();
        }
    };
    private void open(UsbDevice d) {
        if (closed || opening || active) return;
        opening=true;
        final long epoch=generation.incrementAndGet();
        status("Starting Xbox receiver radio…");
        worker.execute(() -> {
            boolean ok=false;
            try {
                try (var firmware = new FileInputStream(firmwareFile())) { FirmwareImage.readValidated(firmware); }
                System.loadLibrary("luna-receiver");
                connection=usb.openDevice(d);
                if(connection==null) throw new IllegalStateException("USB open failed");
                handle=createDriver(connection.getFileDescriptor());
                if(handle==0) throw new IllegalStateException("USB interface could not be claimed");
                ok=startDriver(handle, firmwareFile().getAbsolutePath());
                if(!ok) throw new IllegalStateException("Radio initialization failed");
            } catch (IOException | RuntimeException | LinkageError e) {
                status("Receiver failed: "+e.getMessage());
            }
            if (!ok || closed || generation.get()!=epoch) release();
            else {
                active=true;
                status("Receiver radio ready. Choose Pair controller, then hold the controller’s pairing button.");
            }
            main.post(() -> opening=false);
        });
    }
    public void pair() {
        if(!active || closed) { status("Start the receiver first."); return; }
        final long epoch=generation.get();
        worker.execute(() -> {
            if(handle==0 || epoch!=generation.get()) return;
            boolean ok=pairDriver(handle,true);
            status(ok ? "Pairing enabled. Hold the small pairing button on top of your Xbox controller until its Xbox light flashes quickly."
                : "The receiver did not accept the pairing command.");
        });
    }
    public void stop() {
        if(closed) return;
        generation.incrementAndGet(); active=false;
        firstPad=-1; pads.clear(); reports=0;
        listener.onReceiverInput(State.empty());
        worker.execute(() -> { release(); status("Receiver stopped."); });
    }
    private void release() {
        active=false;
        if(handle!=0) { destroyDriver(handle); handle=0; }
        if(connection!=null) { connection.close(); connection=null; }
        pads.clear(); firstPad=-1;
    }
    public void close() {
        if(closed) return;
        closed=true; generation.incrementAndGet();
        context.unregisterReceiver(receiver);
        worker.execute(this::release);
        worker.shutdown();
    }
    // Invoked synchronously from the native reader, before any input callbacks.
    public void addNewController(int id, long nativeHandle, short vid, short pid, long address) {
        ReceiverPad pad=new ReceiverPad(this,id,nativeHandle);
        pads.put(id,pad);
        if(firstPad<0) firstPad=id;
        status("Controller associated. Waiting for button or stick input…");
    }
    public void removeController(int id) {
        pads.remove(id);
        if(firstPad==id) {
            firstPad=-1;
            main.post(() -> {if(!closed) listener.onReceiverInput(State.empty());});
        }
        status("Controller disconnected. Press its Xbox button to reconnect.");
    }
    void input(int id,int buttons,int lt,int rt,int lx,int ly,int rx,int ry) {
        if(closed || id!=firstPad) return;
        long epoch=generation.get();
        State state=new State(true,buttons,Math.min(1,lt/1023f),Math.min(1,rt/1023f),
            axis(lx),-axis(ly),axis(rx),-axis(ry));
        main.post(() -> {
            if(closed || !active || epoch!=generation.get()) return;
            listener.onReceiverInput(state);
            reports++;
            long now=android.os.SystemClock.uptimeMillis();
            if(reports==1 || now-lastLog>1500) {
                lastLog=now;
                status="Controller input received • "+reports+" reports";
                listener.onReceiverStatus(status);
                Log.i("LunaReceiver",status+" buttons="+Integer.toHexString(buttons)
                    +" LT="+state.lt+" RT="+state.rt+" sticks="+state.lx+","+state.ly+","+state.rx+","+state.ry);
            }
        });
    }
    private static float axis(int value) { return value<0 ? value/32768f : value/32767f; }
}
