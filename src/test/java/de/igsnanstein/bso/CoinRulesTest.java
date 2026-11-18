package de.igsnanstein.bso;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CoinRulesTest {
    @Test void blocksStartAgainAtEleven() {
        assertEquals(CoinRules.Block.SEK_I,CoinRules.blockForGrade(10));
        assertEquals(CoinRules.Block.OBERSTUFE,CoinRules.blockForGrade(11));
    }

    @Test void allExactThresholdsFromWorkbook() {
        String[][] cases={
            {"8","ohne","10","17","22"},
            {"9","KA","119","126","133"},
            {"9","BR","119","126","133"},
            {"9","V10","16","23","30"},
            {"9","S1","16","23","30"},
            {"9","Ü11","16","23","30"},
            {"10","BR","18","25","32"},
            {"10","S1","18","25","32"},
            {"10","Ü11","118","125","132"},
            {"11","ohne","118","125","132"},
            {"12","ohne","17","24","31"}
        };
        for(String[] c:cases){
            int grade=Integer.parseInt(c[0]);
            String track=c[1];
            BigDecimal s=new BigDecimal(c[2]),p=new BigDecimal(c[3]),d=new BigDecimal(c[4]);
            String label=c[0]+"/"+track;
            assertEquals(CoinRules.Level.NONE,CoinRules.levelFor(s.subtract(new BigDecimal("0.5")),grade,track),label);
            assertEquals(CoinRules.Level.SUCCESS,CoinRules.levelFor(s,grade,track),label);
            assertEquals(CoinRules.Level.SPECIAL_SUCCESS,CoinRules.levelFor(p,grade,track),label);
            assertEquals(CoinRules.Level.DISTINCTION,CoinRules.levelFor(d,grade,track),label);
            assertEquals(new BigDecimal("0.5"),CoinRules.nextThresholdDistance(s.subtract(new BigDecimal("0.5")),grade,track),label);
        }
    }

    @Test void unlistedCombinationsNeedReviewAndGrade13None(){
        assertEquals(CoinRules.Level.REVIEW_REQUIRED,CoinRules.levelFor(new BigDecimal("500"),8,"BR"));
        assertEquals(CoinRules.Level.NONE,CoinRules.levelFor(new BigDecimal("500"),13,"ohne"));
    }

    @Test void halfCoinStepsAndUmlautAlias(){
        assertTrue(CoinRules.isHalfStep(new BigDecimal("20.5")));
        assertFalse(CoinRules.isHalfStep(new BigDecimal("20.25")));
        assertEquals(CoinRules.Level.SUCCESS,CoinRules.levelFor(new BigDecimal("118"),10,"U11"));
        assertThrows(IllegalArgumentException.class,()->CoinRules.requireCoinValue(BigDecimal.ZERO,false));
    }
}
