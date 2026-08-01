package dev.inboxpilot.infrastructure.gmail;

import java.util.List;

record GmailMessagePage(List<String> messageIds, String nextPageToken) {
}
