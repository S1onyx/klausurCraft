package simon.klausurcraft;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import simon.klausurcraft.utils.ThemeManager;

import java.io.IOException;
import java.util.Locale;

/**
 * JavaFX App entry
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("home"), 1200, 800);

        // Apply default theme (LIGHT by default)
        ThemeManager.apply(scene, ThemeManager.Theme.LIGHT);

        // Keyboard toggle: Ctrl + D switches theme
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
            () -> ThemeManager.toggle(scene)
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
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(name + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        // Force English locale globally
        Locale.setDefault(Locale.ENGLISH);

        launch();
    }
}