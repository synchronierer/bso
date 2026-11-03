package de.igsnanstein.bso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/** Deterministic, exact BSO rules shared by HTTP services and tests. */
public final class CoinRules {
    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final Map<String, Thresholds> THRESHOLDS = Map.ofEntries(
        Map.entry("8|BR", new Thresholds("11", "18", "22")),
        Map.entry("8|V10/S1/Ü11", new Thresholds("10", "17", "21")),
        Map.entry("9|BR", new Thresholds("128", "151", "171")),
        Map.entry("9|V10/S1/Ü11", new Thresholds("24", "47", "67")),
        Map.entry("10|S1/Ü11", new Thresholds("40", "70", "97")),
        Map.entry("11|MSS", new Thresholds("116", "137", "153")),
        Map.entry("12|MSS", new Thresholds("132", "160", "183"))
    );

    private CoinRules() {}

    public static boolean isHalfStep(BigDecimal value) {
        return value != null && value.signum() >= 0
            && value.remainder(HALF).compareTo(BigDecimal.ZERO) == 0;
    }

    public static void requireCoinValue(BigDecimal value, boolean allowZero) {
        if (!isHalfStep(value) || (!allowZero && value.signum() == 0)) {
            throw new IllegalArgumentException("Münzen müssen positive 0,5er-Schritte sein");
        }
    }

    public static Block blockForGrade(int grade) {
        if (grade < 7 || grade > 13) throw new IllegalArgumentException("Jahrgang 7 bis 13 erforderlich");
        return grade <= 10 ? Block.SEK_I : Block.OBERSTUFE;
    }

    public static Level levelFor(BigDecimal total, int grade, String track) {
        if (grade == 13) return Level.NONE;
        Thresholds t = THRESHOLDS.get(grade + "|" + track);
        if (t == null) return Level.REVIEW_REQUIRED;
        Objects.requireNonNull(total, "total");
        if (total.compareTo(t.distinction()) >= 0) return Level.DISTINCTION;
        if (total.compareTo(t.specialSuccess()) >= 0) return Level.SPECIAL_SUCCESS;
        if (total.compareTo(t.success()) >= 0) return Level.SUCCESS;
        return Level.NONE;
    }

    public static BigDecimal nextThresholdDistance(BigDecimal total, int grade, String track) {
        Thresholds t = THRESHOLDS.get(grade + "|" + track);
        if (t == null || grade == 13) return null;
        for (BigDecimal threshold : new BigDecimal[]{t.success(), t.specialSuccess(), t.distinction()}) {
            if (total.compareTo(threshold) < 0) return threshold.subtract(total).setScale(1, RoundingMode.UNNECESSARY);
        }
        return BigDecimal.ZERO.setScale(1);
    }

    public enum Block { SEK_I, OBERSTUFE }
    public enum Level { NONE, SUCCESS, SPECIAL_SUCCESS, DISTINCTION, REVIEW_REQUIRED }
    public record Thresholds(BigDecimal success, BigDecimal specialSuccess, BigDecimal distinction) {
        public Thresholds(String success, String specialSuccess, String distinction) {
            this(new BigDecimal(success), new BigDecimal(specialSuccess), new BigDecimal(distinction));
        }
    }
}
