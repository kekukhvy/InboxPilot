package dev.inboxpilot.application.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class MailboxLabelCatalogTest {

    private static final String FIRST_ID = "Label_first";
    private static final String SECOND_ID = "Label_second";
    private static final String NAME = "Travel/Accommodation";

    @Test
    void mapsIdsToNamesAndNamesBackToIds() {
        MailboxLabelCatalog catalog = new MailboxLabelCatalog(
                List.of(new MailboxLabel(FIRST_ID, NAME)));

        assertThat(catalog.nameForId(FIRST_ID)).isEqualTo(NAME);
        assertThat(catalog.idForName(NAME)).isEqualTo(FIRST_ID);
    }

    @Test
    void rejectsAnAmbiguousVisibleNameBeforeMutation() {
        MailboxLabelCatalog catalog = new MailboxLabelCatalog(List.of(
                new MailboxLabel(FIRST_ID, NAME), new MailboxLabel(SECOND_ID, NAME)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> catalog.idForName(NAME))
                .withMessageContaining("Ambiguous");
    }
}
