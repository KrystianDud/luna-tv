package dev.local.lunatv;

import android.annotation.SuppressLint;
import dev.local.lunatv.receiver.ReceiverManager;
import dev.local.lunatv.receiver.ReceiverWebBridge;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class MainActivity extends Activity implements InputManager.InputDeviceListener {
    private androidx.webkit.ScriptHandler viewportScript;
    private ReceiverManager receiver;
    private AlertDialog receiverDialog;
    private TextView receiverStatus;
    private ReceiverManager.State receiverState = ReceiverManager.State.empty();
    private boolean receiverFramePending;
    private boolean receiverBridgeAvailable;
    private int receiverButtons;
    private final Runnable receiverFrame = () -> {
        receiverFramePending = false;
        if (this.running) ReceiverWebBridge.send(this.web, receiverState, !this.browsing && !menuVisible()
            && (receiverDialog == null || !receiverDialog.isShowing()));
    };
    private FrameLayout root;
    private WebView web;
    private Cursor cursor;
    private ProgressBar progress;
    private LinearLayout welcome;
    private AlertDialog menu;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private boolean browsing = true;
    private boolean desktop;
    private boolean remotePointer = true;
    private boolean running;
    private boolean frameScheduled;
    private float stickX, stickY, scrollX, scrollY;
    private long lastFrame;
    private long viewDown;
    private final Runnable hideCursor = () -> { if (cursor != null) cursor.setVisibility(View.INVISIBLE); };
    private final Runnable controllerFrame = new Runnable() {
        @Override public void run() {
            frameScheduled = false;
            if (!running) return;
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(40, now - lastFrame) / 1000f;
            lastFrame = now;
            if (browsing && browserVisible() && fullscreen == null && !menuVisible()) {
                if (stickX != 0 || stickY != 0) movePointer(stickX * dp(700) * dt, stickY * dp(700) * dt);
                if (scrollX != 0 || scrollY != 0) wheel(scrollX * dt * 20, -scrollY * dt * 20);
            }
            scheduleControllerFrame();
        }
    };

    private void scheduleControllerFrame() {
        if (!frameScheduled && running && browsing && browserVisible() && fullscreen == null
                && !menuVisible() && (stickX != 0 || stickY != 0 || scrollX != 0 || scrollY != 0)) {
            frameScheduled = true;
            root.postOnAnimation(controllerFrame);
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        desktop = getPreferences(MODE_PRIVATE).getBoolean("desktop", true);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(16, 18, 28));
        setContentView(root);
        createBrowser();
        receiver = new ReceiverManager(this, new ReceiverManager.Listener() {
            @Override public void onReceiverStatus(String text) {
                if (receiverStatus != null) receiverStatus.setText(text);
            }
            @Override public void onReceiverInput(ReceiverManager.State state) { receiveController(state); }
        });
        showWelcome();
        immersive();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private boolean browserVisible() { return web != null && web.getVisibility() == View.VISIBLE; }
    private boolean menuVisible() { return menu != null && menu.isShowing(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createBrowser() {
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(16, 18, 28));
        web.setVisibility(View.GONE);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);
        // Keep target=_blank sign-in links in this view. No arbitrary popup windows.
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        setUserAgent();
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        receiverBridgeAvailable = ReceiverWebBridge.install(web);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return !"https".equals(request.getUrl().getScheme());
                String url = request.getUrl().toString();
                if (BrowserPolicy.internal(url)) return false;
                externalLink(request.getUrl());
                return true;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!"appassets.androidplatform.net".equals(request.getUrl().getHost())) return null;
                if (BrowserPolicy.DIAGNOSTICS.equals(request.getUrl().toString())) {
                    try { return new WebResourceResponse("text/html", "UTF-8", getAssets().open("diagnostics.html")); }
                    catch (IOException ignored) { }
                }
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                    java.util.Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageFinished(WebView view, String url) { CookieManager.getInstance().flush(); queueReceiverFrame();
                if(BrowserPolicy.lunaOrigin(url)) view.evaluateJavascript(
                    "JSON.stringify({width:innerWidth,height:innerHeight,documentWidth:document.documentElement.clientWidth,scrollWidth:document.documentElement.scrollWidth,dpr:devicePixelRatio,scale:visualViewport.scale,receiverAdapter:typeof window.__lunaReceiver})",
                    value -> android.util.Log.i("LunaLayout", value));
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    progress.setVisibility(View.GONE);
                    new AlertDialog.Builder(MainActivity.this).setTitle("Page could not load")
                        .setMessage("Check the TV’s internet connection. Update Android System WebView in the Play Store if the problem continues.")
                        .setPositiveButton("Retry", (d, w) -> web.reload())
                        .setNegativeButton("Home", (d, w) -> showWelcome()).show();
                }
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                hideFullscreen();
                root.removeView(web);
                web.destroy();
                web = null;
                new AlertDialog.Builder(MainActivity.this).setTitle("Browser stopped")
                    .setMessage("Restart the app. If this repeats, update Android System WebView and restart your TV.")
                    .setCancelable(false).setPositiveButton("Restart", (d, w) -> recreate()).show();
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int percent) {
                progress.setProgress(percent);
                progress.setVisibility(percent < 100 && browserVisible() ? View.VISIBLE : View.GONE);
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                // Grant only protected-media playback to the Luna origin, never microphone/camera.
                if (BrowserPolicy.lunaOrigin(request.getOrigin().toString())) {
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {
                            request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                            return;
                        }
                    }
                }
                request.deny();
            }
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return; }
                setBrowsing(false);
                fullscreen = view;
                fullscreenCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                view.requestFocus();
                immersive();
            }
            @Override public void onHideCustomView() { hideFullscreen(); }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setVisibility(View.GONE);
        root.addView(progress, new FrameLayout.LayoutParams(-1, dp(3)));
        cursor = new Cursor();
        cursor.setVisibility(View.INVISIBLE);
        cursor.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(cursor, new FrameLayout.LayoutParams(-1, -1));
    }

    private void setUserAgent() {
        if(viewportScript!=null) { viewportScript.remove(); viewportScript=null; }
        if(desktop && androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)) {
            try(var input=getAssets().open("tv-viewport.js")) {
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                byte[] buffer=new byte[4096]; int count;
                while((count=input.read(buffer))!=-1) bytes.write(buffer,0,count);
                viewportScript=androidx.webkit.WebViewCompat.addDocumentStartJavaScript(web,
                    new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8),
                    java.util.Set.of("https://luna.amazon.com","https://luna.amazon.co.uk"));
            } catch(IOException e) { toast("TV layout could not be loaded."); }
        }
        web.setInitialScale(desktop ? Math.round(getResources().getDisplayMetrics().widthPixels*100f/1280) : 0);
        web.getSettings().setUserAgentString(BrowserPolicy.userAgent(WebSettings.getDefaultUserAgent(this), desktop));
    }
    private void externalLink(Uri uri) {
        if (!"https".equals(uri.getScheme())) { toast("Only secure web links are supported."); return; }
        new AlertDialog.Builder(this).setTitle("Open external website?")
            .setMessage(uri.getHost()).setNegativeButton("Cancel", null)
            .setPositiveButton("Open browser", (d, w) -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (ActivityNotFoundException e) { toast("No external browser is installed."); }
            }).show();
    }

    private void showWelcome() {
        hideFullscreen();
        resetSticks();
        if (web != null) { web.loadUrl("about:blank"); web.setVisibility(View.GONE); }
        progress.setVisibility(View.GONE);
        cursor.setVisibility(View.INVISIBLE);
        if (welcome != null) root.removeView(welcome);
        welcome = new LinearLayout(this);
        welcome.setOrientation(LinearLayout.VERTICAL);
        welcome.setPadding(dp(48), dp(28), dp(48), dp(20));
        TextView title = new TextView(this);
        title.setText("Luna TV"); title.setTextSize(32); title.setTextColor(Color.WHITE);
        welcome.addView(title);
        TextView description = new TextView(this);
        description.setText("Unofficial • Experimental\n\nXbox: left stick moves the pointer, right stick scrolls, A selects.\nPress Menu ☰ to enter Play mode. Hold View ⧉ for one second, then release, for options.\nTV remote: arrows move, OK selects, Back opens options.");
        description.setTextSize(17); description.setTextColor(Color.LTGRAY);
        description.setPadding(0, dp(8), 0, dp(16));
        welcome.addView(description);
        Button open = button("Open Amazon Luna", () -> openPage(BrowserPolicy.HOME, true));
        welcome.addView(open);
        welcome.addView(button("Check controller and browser", () -> openPage(BrowserPolicy.DIAGNOSTICS, false)));
        welcome.addView(button("Xbox USB receiver", this::showReceiver));
        welcome.addView(button("Setup and compatibility", this::showHelp));
        root.addView(welcome, new FrameLayout.LayoutParams(-1, -1));
        open.requestFocus();
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextSize(17); b.setMinHeight(dp(48));
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void openPage(String url, boolean browse) {
        if (welcome != null) { root.removeView(welcome); welcome = null; }
        web.setVisibility(View.VISIBLE); web.requestFocus();
        setBrowsing(browse);
        web.loadUrl(url);
    }
    private void setBrowsing(boolean value) {
        browsing = value;
        queueReceiverFrame();
        resetSticks();
        if (cursor != null) cursor.setVisibility(View.INVISIBLE);
    }
    private void resetSticks() { stickX = stickY = scrollX = scrollY = 0; }
    private void hideFullscreen() {
        if (fullscreen == null) return;
        root.removeView(fullscreen); fullscreen = null;
        WebChromeClient.CustomViewCallback callback = fullscreenCallback;
        fullscreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
        if (web != null) web.requestFocus();
        immersive();
    }
    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("Setup and compatibility")
            .setMessage("For model 1697, plug the Xbox Wireless Adapter into the TV and open Xbox USB receiver. Import the receiver firmware once (see the project README), start the receiver, allow USB access, then choose Pair controller. Hold the controller’s pairing button until it flashes quickly. This driver works only inside this app. Newer Bluetooth controllers can instead pair in TV Settings → Remotes & accessories.\n\nUpdate Android System WebView through Google Play. Use Ethernet or 5 GHz Wi-Fi, and enable your TV’s Game picture mode if available.\n\nSign in on Amazon’s page with your own account. Use Browse mode for the website and Play mode for games. In Play mode, buttons and sticks go to the browser. USB receiver input uses an experimental Gamepad API adapter; compatibility with Luna must be tested. Hold View for one second and RELEASE for options; that View press also reaches the game.\n\nAmazon does not officially support Android TV. An unsupported-browser message, login failure or black stream may prevent play. Desktop browser mode is an optional compatibility experiment. Voice chat, controller vibration and headset audio are not implemented or verified.\n\nNo Amazon app code is included. This app has no analytics or credential-collecting bridge.")
            .setPositiveButton("OK", null).show();
    }

    private void showMenu() {
        if (menuVisible() || web == null) return;
        resetSticks(); cursor.setVisibility(View.INVISIBLE);
        String[] choices = {browsing ? "Switch to Play mode" : "Switch to Browse mode",
            "Return to page", "Browser back", "Reload page", "Luna home",
            "Controller and browser check", desktop ? "Use Android browser mode" : "Try desktop browser mode",
            remotePointer ? "TV remote: use focus navigation" : "TV remote: use pointer",
            "Setup and compatibility", "Clear local sign-in data", "Exit app", "Xbox USB receiver"};
        menu = new AlertDialog.Builder(this).setTitle("Luna TV • " + (browsing ? "Browse" : "Play"))
            .setItems(choices, (d, which) -> {
                switch (which) {
                    case 0:
                        hideFullscreen(); setBrowsing(!browsing);
                        toast(browsing ? "Browse: left stick moves, right stick scrolls, A clicks." : "Play: controller input goes to Luna. Hold View, then release, for options."); break;
                    case 1: break;
                    case 2:
                        confirmLeave(() -> { hideFullscreen(); if (web.canGoBack()) web.goBack(); else showWelcome(); }); break;
                    case 3: confirmLeave(() -> { hideFullscreen(); web.reload(); }); break;
                    case 4: confirmLeave(() -> { hideFullscreen(); openPage(BrowserPolicy.HOME, true); }); break;
                    case 5: confirmLeave(() -> { hideFullscreen(); openPage(BrowserPolicy.DIAGNOSTICS, false); }); break;
                    case 6: confirmLeave(() -> {
                        hideFullscreen(); desktop = !desktop;
                        getPreferences(MODE_PRIVATE).edit().putBoolean("desktop", desktop).apply();
                        setUserAgent(); web.reload();
                    }); break;
                    case 7: remotePointer = !remotePointer; break;
                    case 8: showHelp(); break;
                    case 9:
                        new AlertDialog.Builder(this).setTitle("Clear local sign-in data?")
                            .setMessage("This signs you out in this app and ends any active session.")
                            .setNegativeButton("Cancel", null).setPositiveButton("Clear", (a, b) -> {
                                hideFullscreen(); web.stopLoading(); web.loadUrl("about:blank");
                                CookieManager.getInstance().removeAllCookies(done -> {
                                    CookieManager.getInstance().flush(); WebStorage.getInstance().deleteAllData();
                                    web.clearCache(true); web.clearHistory(); web.clearFormData(); showWelcome();
                                });
                            }).show(); break;
                    case 10: confirmLeave(this::finish); break;
                    case 11: showReceiver(); break;
                    default: break;
                }
            }).create();
        menu.setOnDismissListener(d -> { if (web != null && fullscreen == null) web.requestFocus(); immersive(); queueReceiverFrame(); });
        menu.show();
        queueReceiverFrame();
    }
    private void queueReceiverFrame() {
        if (root != null && !receiverFramePending) {
            receiverFramePending=true;
            root.postOnAnimation(receiverFrame);
        }
    }
    private static final int IMPORT_FIRMWARE = 901;
    private void selectFirmware() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(pick, IMPORT_FIRMWARE); }
        catch (ActivityNotFoundException e) { toast("Install a file picker that supports Android documents, then try again."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == IMPORT_FIRMWARE && result == RESULT_OK && data != null && data.getData() != null)
            receiver.importFirmware(data.getData());
    }
    private void showReceiver() {
        if (receiverDialog != null && receiverDialog.isShowing()) return;
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(16), dp(24), dp(16));
        receiverStatus=new TextView(this);
        receiverStatus.setText(receiver.status()); receiverStatus.setTextSize(18);
        receiverStatus.setTextColor(Color.WHITE); panel.addView(receiverStatus);
        panel.addView(button("Import receiver firmware", this::selectFirmware));
        panel.addView(button("Start receiver", receiver::connect));
        panel.addView(button("Pair controller", receiver::pair));
        panel.addView(button("Test buttons and sticks", () -> {
            receiverDialog.dismiss(); openPage(BrowserPolicy.DIAGNOSTICS, false);
        }));
        panel.addView(button("Stop receiver", receiver::stop));
        if (!receiverBridgeAvailable) {
            TextView error=new TextView(this);
            error.setText("Update Android System WebView: the browser input adapter is unavailable.");
            panel.addView(error);
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(panel);
        receiverDialog=new AlertDialog.Builder(this).setTitle("Xbox USB receiver")
            .setView(scroll).setNegativeButton("Close", null).create();
        receiverDialog.setOnDismissListener(d -> { receiverStatus=null; queueReceiverFrame(); });
        receiverDialog.show(); queueReceiverFrame();
    }
    private void receiveController(ReceiverManager.State state) {
        receiverState=state; queueReceiverFrame();
        int changed=receiverButtons ^ state.buttons;
        receiverButtons=state.buttons;
        if (!running) return;
        boolean dialog = menuVisible() || (receiverDialog!=null && receiverDialog.isShowing());
        if(browsing && browserVisible() && !dialog && fullscreen==null && state.connected) {
            stickX=receiverAxis(state.lx); stickY=receiverAxis(state.ly);
            scrollX=receiverAxis(state.rx); scrollY=receiverAxis(state.ry);
            if(!frameScheduled) lastFrame=SystemClock.uptimeMillis();
            scheduleControllerFrame();
        } else resetSticks();
        if(!state.connected) { viewDown=0; return; }
        int[] flags={0x1000,0x2000,0x100,0x200,0x10,0x20,1,2,4,8};
        int[] keys={KeyEvent.KEYCODE_BUTTON_A,KeyEvent.KEYCODE_BUTTON_B,KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,KeyEvent.KEYCODE_BUTTON_START,KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT};
        for(int i=0;i<flags.length;i++) if((changed&flags[i])!=0) {
            boolean down=(state.buttons&flags[i])!=0;
            long now=SystemClock.uptimeMillis();
            if(!browsing && browserVisible() && !dialog) {
                if(keys[i]==KeyEvent.KEYCODE_BUTTON_SELECT) {
                    if(down) viewDown=now;
                    else if(viewDown!=0) {long held=now-viewDown; viewDown=0; if(held>=900) showMenu();}
                }
                continue;
            }
            int key=keys[i];
            if(dialog || !browserVisible()) {
                if(key==KeyEvent.KEYCODE_BUTTON_A) key=KeyEvent.KEYCODE_DPAD_CENTER;
                if(key==KeyEvent.KEYCODE_BUTTON_B) key=KeyEvent.KEYCODE_BACK;
            }
            KeyEvent event=new KeyEvent(now,now,down?KeyEvent.ACTION_DOWN:KeyEvent.ACTION_UP,
                key,0,0,-1,0,0,InputDevice.SOURCE_GAMEPAD);
            if(receiverDialog!=null && receiverDialog.isShowing()) receiverDialog.dispatchKeyEvent(event);
            else if(menuVisible()) menu.dispatchKeyEvent(event);
            else dispatchKeyEvent(event);
        }
    }
    private float receiverAxis(float value) {
        float magnitude=Math.max(0,(Math.abs(value)-0.18f)/0.82f);
        return Math.copySign(magnitude*magnitude,value);
    }

    private void confirmLeave(Runnable action) {
        new AlertDialog.Builder(this).setTitle("Leave or reload this page?")
            .setMessage("An active game or sign-in may be interrupted.")
            .setNegativeButton("Cancel", null).setPositiveButton("Continue", (d, w) -> action.run()).show();
    }

    private boolean controller(KeyEvent event) {
        InputDevice device = event.getDevice();
        return event.isFromSource(InputDevice.SOURCE_GAMEPAD) || KeyEvent.isGamepadButton(event.getKeyCode())
            || (device != null && device.supportsSource(InputDevice.SOURCE_JOYSTICK));
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        boolean pad = controller(event);
        InputDevice inputDevice = event.getDevice();
        boolean typingKeyboard = inputDevice != null
            && inputDevice.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;
        if (!browserVisible() || menuVisible()) return super.dispatchKeyEvent(event);
        if (pad && code == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (down && event.getRepeatCount() == 0) viewDown = event.getEventTime();
            // Deliver both edges to Chromium before opening the native menu: no stuck gamepad button.
            View target = fullscreen != null ? fullscreen : web;
            target.dispatchKeyEvent(event);
            if (!down && viewDown != 0) {
                long held = event.getEventTime() - viewDown; viewDown = 0;
                if (held >= 900 && !event.isCanceled()) { showMenu(); return true; }
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_MENU || (!pad && code == KeyEvent.KEYCODE_BACK)) {
            if (!down) showMenu(); return true;
        }
        if (pad && !browsing) {
            // Let Chromium observe gamepad input, then consume it to prevent Android's
            // Activity fallback from treating B as Back or moving focus out of the game.
            View target = fullscreen != null ? fullscreen : web;
            target.dispatchKeyEvent(event);
            return true;
        }
        if (pad && browsing && fullscreen == null) {
            if (code == KeyEvent.KEYCODE_BUTTON_START) {
                if (!down) { setBrowsing(false); toast("Play mode. Hold View for one second, then release, for options."); }
                return true;
            }
            if (code == KeyEvent.KEYCODE_BUTTON_A) { if (!down && !event.isCanceled()) clickPointer(); return true; }
            if (code == KeyEvent.KEYCODE_BUTTON_B) { if (!down) showMenu(); return true; }
            if (code == KeyEvent.KEYCODE_BUTTON_L1 || code == KeyEvent.KEYCODE_BUTTON_R1) {
                if (down) wheel(0, code == KeyEvent.KEYCODE_BUTTON_L1 ? 3 : -3); return true;
            }
        }
        boolean pointerInput = fullscreen == null
            && ((pad && browsing) || (!pad && !typingKeyboard && remotePointer));
        if (pointerInput) {
            float step = dp(event.getRepeatCount() > 5 ? 30 : 12);
            switch (code) {
                case KeyEvent.KEYCODE_DPAD_LEFT: if (down) movePointer(-step, 0); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: if (down) movePointer(step, 0); return true;
                case KeyEvent.KEYCODE_DPAD_UP: if (down) movePointer(0, -step); return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: if (down) movePointer(0, step); return true;
                case KeyEvent.KEYCODE_DPAD_CENTER: if (!down && !event.isCanceled()) clickPointer(); return true;
                default: break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) && browsing && browserVisible()
                && fullscreen == null && !menuVisible()) {
            stickX = axis(event, MotionEvent.AXIS_X); stickY = axis(event, MotionEvent.AXIS_Y);
            // Standard Android Xbox mappings: right stick Z/RZ, with RX/RY fallback.
            InputDevice device = event.getDevice();
            boolean zr = device != null && device.getMotionRange(MotionEvent.AXIS_Z, event.getSource()) != null;
            scrollX = axis(event, zr ? MotionEvent.AXIS_Z : MotionEvent.AXIS_RX);
            scrollY = axis(event, zr ? MotionEvent.AXIS_RZ : MotionEvent.AXIS_RY);
            if (!frameScheduled) lastFrame = SystemClock.uptimeMillis();
            scheduleControllerFrame();
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }
    private float axis(MotionEvent event, int axis) {
        float value = event.getAxisValue(axis);
        InputDevice device = event.getDevice();
        InputDevice.MotionRange range = device == null ? null : device.getMotionRange(axis, event.getSource());
        float deadzone = Math.max(0.18f, range == null ? 0 : range.getFlat());
        if (Math.abs(value) <= deadzone) return 0;
        float normalized = Math.min(1, (Math.abs(value) - deadzone) / (1 - deadzone));
        return Math.copySign(normalized * normalized, value);
    }
    private void ensurePointer() {
        if (cursor.x < 0) { cursor.x = web.getWidth() / 2f; cursor.y = web.getHeight() / 2f; }
        cursor.setVisibility(View.VISIBLE); cursor.invalidate();
        root.removeCallbacks(hideCursor); root.postDelayed(hideCursor, 3000);
    }
    private void movePointer(float dx, float dy) {
        ensurePointer();
        float nextY = cursor.y + dy;
        cursor.x = Math.max(dp(8), Math.min(web.getWidth() - dp(8), cursor.x + dx));
        cursor.y = Math.max(dp(8), Math.min(web.getHeight() - dp(8), nextY));
        if (nextY != cursor.y) wheel(0, dy > 0 ? -0.5f : 0.5f);
        cursor.invalidate();
    }
    private void clickPointer() {
        ensurePointer(); web.requestFocus();
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, cursor.x, cursor.y, 0);
        MotionEvent up = MotionEvent.obtain(time, time + 16, MotionEvent.ACTION_UP, cursor.x, cursor.y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN); up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        web.dispatchTouchEvent(down); web.dispatchTouchEvent(up); down.recycle(); up.recycle();
    }
    private void wheel(float horizontal, float vertical) {
        ensurePointer();
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0; properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
        coords.x = cursor.x; coords.y = cursor.y;
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, horizontal);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vertical);
        long time = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(time, time, MotionEvent.ACTION_SCROLL, 1,
            new MotionEvent.PointerProperties[]{properties}, new MotionEvent.PointerCoords[]{coords},
            0, 0, 1, 1, 0, 0, InputDevice.SOURCE_MOUSE, 0);
        web.dispatchGenericMotionEvent(event); event.recycle();
    }
    private final class Cursor extends View {
        float x = -1, y = -1;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Cursor() { super(MainActivity.this); setFocusable(false); setClickable(false); }
        @Override protected void onDraw(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(180, 16, 18, 28));
            canvas.drawCircle(x, y, dp(10), paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, dp(10), paint);
            paint.setStyle(Paint.Style.FILL); canvas.drawCircle(x, y, dp(2), paint);
        }
    }
    @Override public void onBackPressed() { if (browserVisible()) showMenu(); else super.onBackPressed(); }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) immersive(); else { resetSticks(); viewDown = 0; }
    }
    @Override protected void onResume() {
        super.onResume(); if (web != null) web.onResume();
        running = true;
        queueReceiverFrame();
        getSystemService(InputManager.class).registerInputDeviceListener(this, null);
    }
    @Override protected void onPause() {
        running = false; resetSticks(); root.removeCallbacks(controllerFrame);
        root.removeCallbacks(receiverFrame); receiverFramePending=false;
        ReceiverWebBridge.send(web, receiverState, false);
        frameScheduled = false;
        getSystemService(InputManager.class).unregisterInputDeviceListener(this);
        root.removeCallbacks(hideCursor);
        if (web != null) web.onPause();
        CookieManager.getInstance().flush(); super.onPause();
    }
    @Override protected void onStop() {
        super.onStop();
        if (receiver != null) receiver.stop();
    }
    @Override protected void onDestroy() {
        if (receiver != null) receiver.close();
        root.removeCallbacks(receiverFrame);
        running = false; root.removeCallbacks(controllerFrame); root.removeCallbacks(hideCursor);
        hideFullscreen();
        if (web != null) { root.removeView(web); web.destroy(); web = null; }
        super.onDestroy();
    }
    @Override public void onInputDeviceAdded(int deviceId) { resetSticks(); }
    @Override public void onInputDeviceChanged(int deviceId) { resetSticks(); }
    @Override public void onInputDeviceRemoved(int deviceId) { resetSticks(); viewDown = 0; }
}
