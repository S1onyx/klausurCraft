package simon.klausurcraft.app;

import java.io.IOException;
import java.util.Locale;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import simon.klausurcraft.ui.ThemeService;

/**
 * JavaFX entry point for KlausurCraft.
 */
public class KlausurCraftApp extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("home"), 1200, 800);

        // Apply default theme (LIGHT by default)
        ThemeService.apply(scene, ThemeService.Theme.LIGHT);

        // Keyboard toggle: Ctrl + D switches theme
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
            () -> ThemeService.toggle(scene)
        );

        stage.setTitle("klausurCraft");
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static Scene getScene() {
        return scene;
    }

    private static Parent loadFXML(String name) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(KlausurCraftApp.class.getResource("/simon/klausurcraft/" + name + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        // Force English locale globally
        Locale.setDefault(Locale.ENGLISH);

        launch();
    }
}
