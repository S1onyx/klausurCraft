package simon.klausurcraft.shipping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParcelValidationTest {

    @Test
    void constructor_rejectsInvalidDimensionsWeightAndZone() {
        // Covers defensive constructor branches.
        assertThrows(IllegalArgumentException.class, () -> new Parcel(0, 10, 10, 1.0, ShippingZone.LOCAL, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Parcel(10, -1, 10, 1.0, ShippingZone.LOCAL, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Parcel(10, 10, 10, 0.0, ShippingZone.LOCAL, false, false));
        assertThrows(IllegalArgumentException.class, () -> new Parcel(10, 10, 10, 1.0, null, false, false));
    }

    @Test
    void geometryHelpers_computeMaxEdgeAndGirth() {
        // Covers helper methods used by tariff logic.
        Parcel parcel = new Parcel(40, 25, 10, 2.5, ShippingZone.NATIONAL, true, false);

        assertEquals(40, parcel.maxEdgeCm());
        assertEquals(75, parcel.girthCm());
    }
}
