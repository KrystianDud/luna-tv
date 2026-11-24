// GPL-3.0-or-later.
package dev.local.lunatv.receiver;
import android.webkit.WebView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class ReceiverWebBridge {
    private static final Set<String> ORIGINS = Set.of("https://luna.amazon.com","https://luna.amazon.co.uk","https://appassets.androidplatform.net");
    private ReceiverWebBridge() {}
    public static boolean install(WebView web) {
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false;
        try(var stream=web.getContext().getAssets().open("receiver-gamepad.js")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096];
            int count;
            while((count=stream.read(buffer))!=-1) bytes.write(buffer,0,count);
            WebViewCompat.addDocumentStartJavaScript(web,new String(bytes.toByteArray(),StandardCharsets.UTF_8),ORIGINS);
            return true;
        } catch(IOException e) {return false;}
    }
    public static void send(WebView web,ReceiverManager.State s,boolean playable) {
        if(web==null) return;
        String state=playable ? s.buttons+","+s.lt+","+s.rt+",["+s.lx+","+s.ly+","+s.rx+","+s.ry+"]"
            : "0,0,0,[0,0,0,0]";
        // Recheck inside the evaluated document: navigation may happen after this call is queued.
        web.evaluateJavascript("(()=>{if((location.origin==='https://luna.amazon.com'||location.origin==='https://luna.amazon.co.uk'||location.origin==='https://appassets.androidplatform.net')&&window.__lunaReceiver)window.__lunaReceiver("
            +s.connected+","+state+");})()",null);
    }
}
