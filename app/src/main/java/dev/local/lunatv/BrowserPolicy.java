package dev.local.lunatv;

import java.net.URI;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure policy: no credentials, network calls, or Android dependencies. */
public final class BrowserPolicy {
    public static final String HOME = "https://luna.amazon.com/";
    public static final String DIAGNOSTICS = "https://appassets.androidplatform.net/diagnostics.html";
    private static final Set<String> AMAZON_DOMAINS = Set.of(
        "amazon.com", "amazon.co.uk", "amazon.de", "amazon.fr", "amazon.it", "amazon.es",
        "amazon.ca", "amazon.nl", "amazon.pl", "amazon.se", "amazon.com.be",
        "amazon.ie", "amazon.at", "amazon.lu", "amazon.co.jp", "amazon.com.au",
        "amazon.in", "amazon.com.br", "amazon.com.mx");

    public static boolean internal(String url) {
        if (DIAGNOSTICS.equals(url)) return true;
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) return false;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase(java.util.Locale.ROOT);
            for (String domain : AMAZON_DOMAINS) {
                if (host.equals(domain) || host.endsWith("." + domain)) return true;
            }
        } catch (IllegalArgumentException ignored) { }
        return false;
    }

    public static boolean lunaOrigin(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                && ("luna.amazon.com".equalsIgnoreCase(uri.getHost())
                    || "luna.amazon.co.uk".equalsIgnoreCase(uri.getHost()))
                && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (IllegalArgumentException e) { return false; }
    }

    public static String userAgent(String actual, boolean desktop) {
        // Use the installed Chromium version; do not advertise invented codec/browser capabilities.
        Matcher version = Pattern.compile("Chrome/([0-9.]+)").matcher(actual);
        if (desktop && version.find()) {
            return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/"
                + version.group(1) + " Safari/537.36";
        }
        return actual.replace("; wv", "").replace("Version/4.0 ", "");
    }
}
