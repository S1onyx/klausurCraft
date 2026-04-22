package simon.klausurcraft.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates postage for parcels.
 */
public final class PostageCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("100.00");

    private PostageCalculator() {}

    /**
     * Pricing model:
     * - base tariff by dimensions
     * - surcharge by weight bucket
     * - surcharge by shipping zone
     * - oversize surcharge for very large parcels
     * - optional priority/fragile surcharge
     * - final value clamped to [0, 100]
     */
    public static BigDecimal calculate(Parcel parcel) {
        BigDecimal base = baseTariff(parcel);
        BigDecimal weight = weightSurcharge(parcel.weightKg());
        BigDecimal zone = zoneSurcharge(parcel.zone());
        BigDecimal oversize = oversizeSurcharge(parcel);
        BigDecimal options = optionSurcharge(parcel.priority(), parcel.fragile());

        BigDecimal total = base.add(weight).add(zone).add(oversize).add(options);
        if (total.compareTo(ZERO) < 0) {
            total = ZERO;
        } else if (total.compareTo(MAX) > 0) {
            total = MAX;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
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

    private static BigDecimal oversizeSurcharge(Parcel parcel) {
        int over = parcel.girthCm() - 200;
        if (over <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(over).multiply(new BigDecimal("0.75"));
    }

    private static BigDecimal optionSurcharge(boolean priority, boolean fragile) {
        BigDecimal options = ZERO;
        if (priority) {
            options = options.add(new BigDecimal("3.00"));
        }
        if (fragile) {
            options = options.add(new BigDecimal("2.00"));
        }
        return options;
    }
}
