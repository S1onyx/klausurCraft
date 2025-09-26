package simon.klausurcraft.model;

public enum Eligibility {
    EXAM, PRACTICE, BOTH;

    public static Eligibility from(String s) {
        return switch (s.toLowerCase()) {
            case "exam" -> EXAM;
            case "practice" -> PRACTICE;
            case "both" -> BOTH;
            default -> throw new IllegalArgumentException("Unknown eligibility: " + s);
        };
    }

    @Override public String toString() {
        return switch (this) {
            case EXAM -> "exam";
            case PRACTICE -> "practice";
            case BOTH -> "both";
        };
    }
}
