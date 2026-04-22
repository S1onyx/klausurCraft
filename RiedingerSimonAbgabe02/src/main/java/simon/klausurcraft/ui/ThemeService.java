package simon.klausurcraft.ui;

import javafx.scene.Scene;
import javafx.css.PseudoClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

public final class ThemeService {

    public enum Theme {
        LIGHT("light.css"),
        DARK("dark.css");

        private final String cssName;
        Theme(String cssName) { this.cssName = cssName; }
        public String cssName() { return cssName; }
    }

    private static final Set<Scene> REGISTERED_SCENES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final PseudoClass DARK_PSEUDO = PseudoClass.getPseudoClass("dark");
    private static Theme currentTheme = Theme.LIGHT;

    private ThemeService() {}

    public static void register(Scene scene) {
        Objects.requireNonNull(scene, "scene must not be null");
        REGISTERED_SCENES.add(scene);
        applyThemeToScene(scene, currentTheme);
    }

    public static void apply(Scene scene, Theme theme) {
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(theme, "theme must not be null");

        REGISTERED_SCENES.add(scene);
        currentTheme = theme;
        applyThemeToAllRegisteredScenes();
    }

    public static Theme currentTheme() {
        return currentTheme;
    }

    public static void toggle() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        applyThemeToAllRegisteredScenes();
    }

    public static void toggle(Scene scene) {
        if (scene != null) {
            REGISTERED_SCENES.add(scene);
        }
        toggle();
    }

    private static void applyThemeToAllRegisteredScenes() {
        List<Scene> scenes = new ArrayList<>(REGISTERED_SCENES);
        for (Scene s : scenes) {
            if (s != null) {
                applyThemeToScene(s, currentTheme);
            }
        }
    }

    private static void applyThemeToScene(Scene scene, Theme theme) {
        scene.getStylesheets().removeIf(s -> s.endsWith("/light.css") || s.endsWith("/dark.css"));
        String css = Objects.requireNonNull(
                ThemeService.class.getResource("/simon/klausurcraft/" + theme.cssName()),
                () -> "CSS not found: " + theme.cssName()
        ).toExternalForm();
        scene.getStylesheets().add(css);
        if (scene.getRoot() != null) {
            scene.getRoot().pseudoClassStateChanged(DARK_PSEUDO, theme == Theme.DARK);
        }
    }
}
