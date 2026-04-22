package simon.klausurcraft.shipping;

/**
 * Parcel input data for postage calculation.
 */
public record Parcel(
        int lengthCm,
        int widthCm,
        int heightCm,
        double weightKg,
        ShippingZone zone,
        boolean priority,
        boolean fragile
) {
    public Parcel {
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive.");
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Shipping zone must not be null.");
        }
    }

    public int maxEdgeCm() {
        return Math.max(lengthCm, Math.max(widthCm, heightCm));
    }

    public int girthCm() {
        return lengthCm + widthCm + heightCm;
    }
}

