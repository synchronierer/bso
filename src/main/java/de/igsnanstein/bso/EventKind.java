package de.igsnanstein.bso;

import java.util.Locale;
import java.util.Map;

/** One canonical event-kind vocabulary shared by individual entry and CSV/XLSX import. */
public final class EventKind {
    private EventKind() {}

    private static final Map<String,String> CODES=Map.ofEntries(
        Map.entry("P","PFLICHT"),
        Map.entry("PFLICHT","PFLICHT"),
        Map.entry("WP","WAHLPFLICHT"),
        Map.entry("WAHLPFLICHT","WAHLPFLICHT"),
        Map.entry("W","WAHL"),
        Map.entry("WAHL","WAHL"),
        Map.entry("WS","WORKSHOP"),
        Map.entry("WORKSHOP","WORKSHOP")
    );

    public static String parse(String input) {
        if(input==null || input.isBlank())
            throw new IllegalArgumentException("Veranstaltungsart fehlt: P, WP, W oder WS angeben");
        String kind=CODES.get(input.trim().toUpperCase(Locale.ROOT));
        if(kind==null)
            throw new IllegalArgumentException("Ungültige Veranstaltungsart: nur P, WP, W oder WS");
        return kind;
    }
}
