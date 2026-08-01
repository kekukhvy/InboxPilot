package dev.inboxpilot.domain.analysis;

import java.net.URI;
import java.util.List;

/** Safe, inert unsubscribe mechanisms advertised by a message. */
public record UnsubscribeCapabilities(List<URI> mailto, List<URI> https) {

    public UnsubscribeCapabilities {
        mailto = List.copyOf(mailto);
        https = List.copyOf(https);
        if (mailto.stream().anyMatch(uri -> !"mailto".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Mail unsubscribe capabilities must use mailto");
        }
        if (https.stream().anyMatch(uri -> !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)) {
            throw new IllegalArgumentException("Web unsubscribe capabilities must use HTTPS with a host");
        }
    }
}
