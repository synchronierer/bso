package de.igsnanstein.bso;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventKindTest {
    @Test void acceptsAbbreviationsAndFullNames() {
        assertEquals("PFLICHT", EventKind.parse("P"));
        assertEquals("WAHLPFLICHT", EventKind.parse("wp"));
        assertEquals("WAHL", EventKind.parse("W"));
        assertEquals("WORKSHOP", EventKind.parse("ws"));
        assertEquals("WAHLPFLICHT", EventKind.parse("WAHLPFLICHT"));
        assertEquals("PFLICHT", EventKind.parse("pflicht"));
    }

    @Test void failsInsteadOfSilentlySelectingWrongType() {
        assertThrows(IllegalArgumentException.class, () -> EventKind.parse(""));
        assertThrows(IllegalArgumentException.class, () -> EventKind.parse(null));
        assertThrows(IllegalArgumentException.class, () -> EventKind.parse("UNKNOWN"));
    }
}
