package simon.klausurcraft.ui;

import javafx.scene.Scene;
import simon.klausurcraft.app.KlausurCraftApp;

import java.util.Objects;

public final class ThemeService {

    public enum Theme {
        LIGHT("light.css"),
        DARK("dark.css");

        private final String cssName;
        Theme(String cssName) { this.cssName = cssName; }
        public String cssName() { return cssName; }
    }

    private ThemeService() {}

    public static void apply(Scene scene, Theme theme) {
        Objects.requireNonNull(scene, "scene must not be null");
        scene.getStylesheets().removeIf(s -> s.endsWith("/light.css") || s.endsWith("/dark.css"));
        String css = Objects.requireNonNull(
                KlausurCraftApp.class.getResource("/simon/klausurcraft/" + theme.cssName()),
                () -> "CSS not found: " + theme.cssName()
        ).toExternalForm();
        scene.getStylesheets().add(css);
        scene.getRoot().pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("dark"),
                theme == Theme.DARK);
    }

    public static void toggle(Scene scene) {
        boolean isDark = scene.getStylesheets().stream().anyMatch(s -> s.endsWith("/dark.css"));
        apply(scene, isDark ? Theme.LIGHT : Theme.DARK);
    }
}
