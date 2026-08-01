package dev.inboxpilot.domain.analysis;

import java.net.URI;
import java.util.List;

/** Safe, inert unsubscribe mechanisms advertised by a message. */
public record UnsubscribeCapabilities(List<URI> mailto, List<URI> https) {

    public UnsubscribeCapabilities {
        mailto = List.copyOf(mailto);
        https = List.copyOf(https);
    }
}
