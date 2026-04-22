package simon.klausurcraft.task;

import java.math.BigDecimal;

/**
 * Shared point parsing/formatting helpers.
 * Internal storage uses '.' as decimal separator; UI display uses ','.
 */
public final class Points {

    public static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TWO = new BigDecimal("2");

    private Points() {}

    public static BigDecimal parseInput(String raw) {
        String normalized = normalizeInput(raw);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Please enter points.");
        }
        try {
            BigDecimal value = new BigDecimal(normalized);
            return validateHalfStep(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Please enter a valid number (e.g., 1, 1,5).");
        }
    }

    public static BigDecimal parseStorageValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return ZERO;
        }
        try {
            return validateHalfStep(new BigDecimal(raw.trim()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid points value in XML: " + raw);
        }
    }

    public static String toStorageString(BigDecimal value) {
        return validateHalfStep(value).toPlainString();
    }

    public static String toDisplayString(BigDecimal value) {
        return toStorageString(value).replace('.', ',');
    }

    public static int toHalfSteps(BigDecimal value) {
        return validateHalfStep(value).multiply(TWO).intValueExact();
    }

    public static BigDecimal fromHalfSteps(int halfSteps) {
        if (halfSteps < 0) {
            throw new IllegalArgumentException("Points must be non-negative.");
        }
        return normalize(BigDecimal.valueOf(halfSteps).divide(TWO));
    }

    private static BigDecimal validateHalfStep(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Points must not be empty.");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Points must be non-negative.");
        }
        BigDecimal doubled = value.multiply(TWO).stripTrailingZeros();
        if (doubled.scale() > 0) {
            throw new IllegalArgumentException("Only whole and half points are allowed (e.g., 1, 1,5).");
        }
        return normalize(value);
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized;
    }

    private static String normalizeInput(String raw) {
        if (raw == null) return "";
        return raw.trim().replace(',', '.');
    }
}
