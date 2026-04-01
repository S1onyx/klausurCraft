package simon.klausurcraft.task;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointsTest {

    @Test
    void parseInput_acceptsCommaAndDotAndTrims() {
        assertEquals(new BigDecimal("1.5"), Points.parseInput(" 1,5 "));
        assertEquals(new BigDecimal("2.5"), Points.parseInput("2.5"));
        assertEquals(new BigDecimal("3"), Points.parseInput("3"));
    }

    @Test
    void parseInput_rejectsEmptyNegativeAndQuarterSteps() {
        assertThrows(IllegalArgumentException.class, () -> Points.parseInput(""));
        assertThrows(IllegalArgumentException.class, () -> Points.parseInput("-1"));
        assertThrows(IllegalArgumentException.class, () -> Points.parseInput("1,25"));
    }

    @Test
    void parseStorageValue_blankBecomesZero() {
        assertEquals(BigDecimal.ZERO, Points.parseStorageValue(""));
        assertEquals(BigDecimal.ZERO, Points.parseStorageValue("   "));
        assertEquals(BigDecimal.ZERO, Points.parseStorageValue(null));
    }

    @Test
    void storageAndDisplayFormat_areNormalized() {
        assertEquals("2", Points.toStorageString(new BigDecimal("2.0")));
        assertEquals("2.5", Points.toStorageString(new BigDecimal("2.50")));
        assertEquals("2,5", Points.toDisplayString(new BigDecimal("2.50")));
    }

    @Test
    void halfStepConversions_roundTrip() {
        assertEquals(3, Points.toHalfSteps(new BigDecimal("1.5")));
        assertEquals(new BigDecimal("1.5"), Points.fromHalfSteps(3));
        assertEquals(new BigDecimal("0"), Points.fromHalfSteps(0));
    }

    @Test
    void halfStepConversions_rejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> Points.toHalfSteps(new BigDecimal("1.25")));
        assertThrows(IllegalArgumentException.class, () -> Points.fromHalfSteps(-1));
        assertThrows(IllegalArgumentException.class, () -> Points.toStorageString(null));
    }
}
