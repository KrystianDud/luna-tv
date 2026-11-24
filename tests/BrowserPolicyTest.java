package dev.local.lunatv;

public final class BrowserPolicyTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++; if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        for (String url : new String[]{BrowserPolicy.HOME, BrowserPolicy.DIAGNOSTICS,
                "https://www.amazon.co.uk/ap/signin", "https://www.amazon.de/ap/signin", "https://LUNA.AMAZON.COM:443/"}) {
            check(BrowserPolicy.internal(url), "Allow Amazon login / local diagnostics: " + url);
        }
        for (String url : new String[]{"http://luna.amazon.com", "https://amazon.com.evil.test/",
                "https://evilamazon.com/", "https://amazon.com@evil.test/", "https://evil.test@amazon.com/",
                "javascript:alert(1)", "file:///data/data/test", "intent://luna.amazon.com/",
                "https://luna.amazon.com:8443", "https://appassets.androidplatform.net/other", "https://[broken"}) {
            check(!BrowserPolicy.internal(url), "Reject deceptive or unsafe navigation: " + url);
        }
        check(BrowserPolicy.lunaOrigin("https://luna.amazon.com/"), "Luna protected media origin");
        check(BrowserPolicy.lunaOrigin("https://luna.amazon.co.uk/"), "UK Luna protected media origin");
        for (String url : new String[]{"https://www.amazon.com/", "https://luna.amazon.com.evil.test",
                "http://luna.amazon.com", "https://luna.amazon.com:444", "https://user@luna.amazon.com",
                "https://luna.amazon.co.uk.evil.test", "http://luna.amazon.co.uk",
                "https://luna.amazon.co.uk:8443", "https://user@luna.amazon.co.uk", "https://www.amazon.co.uk"}) {
            check(!BrowserPolicy.lunaOrigin(url), "Deny media permission for other origins");
        }
        String actual = "Mozilla/5.0 (Linux; Android 14; TV Build/X; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.6723.1 Mobile Safari/537.36";
        String mobile = BrowserPolicy.userAgent(actual, false);
        check(!mobile.contains("; wv") && !mobile.contains("Version/4.0"), "Remove embedded-browser markers");
        check(mobile.contains("Android 14") && mobile.contains("Chrome/130.0.6723.1"), "Preserve real OS / engine version");
        String desktop = BrowserPolicy.userAgent(actual, true);
        check(desktop.contains("X11; Linux x86_64") && desktop.contains("Chrome/130.0.6723.1"), "Desktop mode preserves engine version");
        check(BrowserPolicy.userAgent("unknown", true).equals("unknown"), "No fabricated version");
        System.out.println("Passed " + checks + " navigation, permission-origin and user-agent checks.");
    }
}
