package de.igsnanstein.bso;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CoinRulesTest {
    @Test void blocksStartAgainAtEleven() {
        assertEquals(CoinRules.Block.SEK_I, CoinRules.blockForGrade(10));
        assertEquals(CoinRules.Block.OBERSTUFE, CoinRules.blockForGrade(11));
    }

    @Test void thresholdsUseExactHalfCoinBoundaries() {
        assertEquals(CoinRules.Level.NONE, CoinRules.levelFor(new BigDecimal("127.5"), 9, "BR"));
        assertEquals(CoinRules.Level.SUCCESS, CoinRules.levelFor(new BigDecimal("128"), 9, "BR"));
        assertEquals(CoinRules.Level.SPECIAL_SUCCESS, CoinRules.levelFor(new BigDecimal("151"), 9, "BR"));
        assertEquals(CoinRules.Level.DISTINCTION, CoinRules.levelFor(new BigDecimal("171"), 9, "BR"));
    }

    @Test void unknownRulesAreReviewRequiredAndGradeThirteenHasNoCertificate() {
        assertEquals(CoinRules.Level.REVIEW_REQUIRED, CoinRules.levelFor(new BigDecimal("500"), 9, "UNKNOWN"));
        assertEquals(CoinRules.Level.NONE, CoinRules.levelFor(new BigDecimal("500"), 13, "MSS"));
    }

    @Test void valuesMustBeHalfSteps() {
        assertTrue(CoinRules.isHalfStep(new BigDecimal("20.5")));
        assertFalse(CoinRules.isHalfStep(new BigDecimal("20.25")));
        assertThrows(IllegalArgumentException.class,
            () -> CoinRules.requireCoinValue(new BigDecimal("0"), false));
    }

    @Test void allPublishedBoundaryCasesRemainLueckenlos() {
        assertEquals(CoinRules.Level.NONE, CoinRules.levelFor(new BigDecimal("39.5"),10,"S1"));
        assertEquals(CoinRules.Level.SUCCESS, CoinRules.levelFor(new BigDecimal("40"),10,"S1"));
        assertEquals(CoinRules.Level.NONE, CoinRules.levelFor(new BigDecimal("131.5"),12,"MSS"));
        assertEquals(CoinRules.Level.SUCCESS, CoinRules.levelFor(new BigDecimal("132"),12,"MSS"));
        assertEquals(new BigDecimal("0.5"), CoinRules.nextThresholdDistance(new BigDecimal("127.5"),9,"BR"));
    }

    @Test void trackAliasesUseTheSamePublishedRule() {
        assertEquals(CoinRules.Level.SUCCESS, CoinRules.levelFor(new BigDecimal("10"),8,"V10"));
        assertEquals(CoinRules.Level.SUCCESS, CoinRules.levelFor(new BigDecimal("10"),8,"S1"));
        assertEquals(CoinRules.Level.REVIEW_REQUIRED, CoinRules.levelFor(new BigDecimal("100"),10,"BR"));
    }
}
