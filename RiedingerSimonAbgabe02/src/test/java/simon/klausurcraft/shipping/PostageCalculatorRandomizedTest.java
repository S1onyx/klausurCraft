package simon.klausurcraft.shipping;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostageCalculatorRandomizedTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal MAX = new BigDecimal("100.00");

    @Test
    void randomizedPostageTest_1000Parcels_matchesExpectedResultAndRange() {
        // Assignment core: generate 1000 random parcels in sensible value ranges.
        Random random = new Random(20260422L);
        for (int i = 0; i < 1000; i++) {
            Parcel parcel = randomParcel(random);
            int idx = i;

            // Actual result from production code under test.
            BigDecimal actual = PostageCalculator.calculate(parcel);
            // Expected result from separate reference function.
            BigDecimal expected = expectedPostage(parcel);

            assertEquals(0, expected.compareTo(actual),
                    () -> "Mismatch at iteration " + idx + " for parcel " + parcel);

            // Additional required condition: postage must stay in [0, 100].
            assertTrue(actual.compareTo(ZERO) >= 0, "Postage must be >= 0.");
            assertTrue(actual.compareTo(MAX) <= 0, "Postage must be <= 100.");
        }
    }

    @Test
    void boundaryValues_hitTariffEdgesAndWeightEdges() {
        // Equivalence-class boundaries for dimensions and weight buckets.
        Parcel smallEdge = new Parcel(35, 20, 5, 1.00, ShippingZone.LOCAL, false, false); // girth=60
        Parcel mediumEdge = new Parcel(60, 40, 20, 5.00, ShippingZone.NATIONAL, true, false); // girth=120
        Parcel largeEdge = new Parcel(61, 40, 20, 10.00, ShippingZone.INTERNATIONAL, false, true);

        assertEquals(0, expectedPostage(smallEdge).compareTo(PostageCalculator.calculate(smallEdge)));
        assertEquals(0, expectedPostage(mediumEdge).compareTo(PostageCalculator.calculate(mediumEdge)));
        assertEquals(0, expectedPostage(largeEdge).compareTo(PostageCalculator.calculate(largeEdge)));
    }

    @Test
    void veryLargeParcel_isClampedTo100() {
        // Additional condition: verify upper bound enforcement explicitly.
        Parcel veryLarge = new Parcel(300, 200, 150, 35.0, ShippingZone.INTERNATIONAL, true, true);
        BigDecimal actual = PostageCalculator.calculate(veryLarge);
        BigDecimal expected = expectedPostage(veryLarge);

        assertEquals(new BigDecimal("100.00"), actual);
        assertEquals(0, expected.compareTo(actual));
    }

    /**
     * Reference implementation (Soll-Resultat) used by tests only.
     */
    private static BigDecimal expectedPostage(Parcel parcel) {
        BigDecimal total = baseTariff(parcel)
                .add(weightSurcharge(parcel.weightKg()))
                .add(zoneSurcharge(parcel.zone()))
                .add(oversizeSurcharge(parcel.girthCm()))
                .add(optionSurcharge(parcel.priority(), parcel.fragile()));

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        if (total.compareTo(MAX) > 0) {
            total = MAX;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static Parcel randomParcel(Random random) {
        int length = intInRange(random, 15, 160);
        int width = intInRange(random, 10, 120);
        int height = intInRange(random, 2, 100);
        double weight = decimalInRange(random, 0.10, 30.00);
        ShippingZone zone = ShippingZone.values()[random.nextInt(ShippingZone.values().length)];
        boolean priority = random.nextBoolean();
        boolean fragile = random.nextBoolean();
        return new Parcel(length, width, height, weight, zone, priority, fragile);
    }

    private static int intInRange(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static double decimalInRange(Random random, double min, double max) {
        double raw = min + random.nextDouble() * (max - min);
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal baseTariff(Parcel parcel) {
        if (parcel.girthCm() <= 60 && parcel.maxEdgeCm() <= 35) {
            return new BigDecimal("3.49");
        }
        if (parcel.girthCm() <= 120 && parcel.maxEdgeCm() <= 60) {
            return new BigDecimal("5.99");
        }
        return new BigDecimal("8.99");
    }

    private static BigDecimal weightSurcharge(double weightKg) {
        if (weightKg <= 1.0) return new BigDecimal("0.00");
        if (weightKg <= 5.0) return new BigDecimal("2.50");
        if (weightKg <= 10.0) return new BigDecimal("5.00");
        return new BigDecimal("8.50");
    }

    private static BigDecimal zoneSurcharge(ShippingZone zone) {
        return switch (zone) {
            case LOCAL -> new BigDecimal("0.00");
            case NATIONAL -> new BigDecimal("1.50");
            case INTERNATIONAL -> new BigDecimal("7.50");
        };
    }

    private static BigDecimal oversizeSurcharge(int girthCm) {
        int over = girthCm - 200;
        if (over <= 0) {
            return new BigDecimal("0.00");
        }
        return BigDecimal.valueOf(over).multiply(new BigDecimal("0.75"));
    }

    private static BigDecimal optionSurcharge(boolean priority, boolean fragile) {
        BigDecimal options = new BigDecimal("0.00");
        if (priority) {
            options = options.add(new BigDecimal("3.00"));
        }
        if (fragile) {
            options = options.add(new BigDecimal("2.00"));
        }
        return options;
    }
}
