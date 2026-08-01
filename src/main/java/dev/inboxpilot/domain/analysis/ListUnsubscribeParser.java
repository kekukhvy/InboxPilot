package dev.inboxpilot.domain.analysis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses List-Unsubscribe values without contacting any advertised endpoint. */
public final class ListUnsubscribeParser {

    private static final Pattern ANGLE_BRACKET_URI = Pattern.compile("<([^>]+)>");
    private static final String MAILTO_SCHEME = "mailto";
    private static final String HTTPS_SCHEME = "https";

    public UnsubscribeCapabilities parse(String headerValue) {
        List<URI> mailto = new ArrayList<>();
        List<URI> https = new ArrayList<>();
        if (headerValue == null || headerValue.isBlank()) {
            return new UnsubscribeCapabilities(mailto, https);
        }
        Matcher matcher = ANGLE_BRACKET_URI.matcher(headerValue);
        while (matcher.find()) {
            parseUri(matcher.group(1), mailto, https);
        }
        return new UnsubscribeCapabilities(mailto, https);
    }

    private static void parseUri(String value, List<URI> mailto, List<URI> https) {
        try {
            URI uri = new URI(value.strip());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (MAILTO_SCHEME.equals(scheme)) {
                mailto.add(uri);
            } else if (HTTPS_SCHEME.equals(scheme) && uri.getHost() != null) {
                https.add(uri);
            }
        } catch (URISyntaxException ignored) {
            // Invalid advertised mechanisms are omitted from the read-only report.
        }
    }
}
