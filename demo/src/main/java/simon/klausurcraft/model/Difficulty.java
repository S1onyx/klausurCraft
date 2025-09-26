package simon.klausurcraft.model;

public enum Difficulty {
    EASY, MEDIUM, HARD;

    public static Difficulty from(String s) {
        return switch (s.toLowerCase()) {
            case "easy" -> EASY;
            case "medium" -> MEDIUM;
            case "hard" -> HARD;
            default -> throw new IllegalArgumentException("Unknown difficulty: " + s);
        };
    }

    @Override public String toString() {
        return switch (this) {
            case EASY -> "easy";
            case MEDIUM -> "medium";
            case HARD -> "hard";
        };
    }
}