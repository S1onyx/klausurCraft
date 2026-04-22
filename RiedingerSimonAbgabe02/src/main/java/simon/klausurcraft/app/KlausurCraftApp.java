package simon.klausurcraft.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import simon.klausurcraft.ui.ThemeService;

import java.awt.Taskbar;
import java.awt.image.BufferedImage;

/**
 * JavaFX entry point for KlausurCraft.
 */
public class KlausurCraftApp extends Application {

    private static Scene scene;
    private static Image appIcon;
    private static List<Image> stageIcons = List.of();

    @Override
    public void start(Stage stage) throws IOException {
        appIcon = createAppIcon(1024); // hi-res icon for macOS Dock/Taskbar
        stageIcons = createStageIcons();
        stage.setTitle("klausurCraft");
        stage.getIcons().setAll(stageIcons);
        installAppIconOnTaskbar();

        Stage splash = createSplashStage();
        splash.show();

        PauseTransition splashDelay = new PauseTransition(Duration.millis(1400));
        splashDelay.setOnFinished(event -> {
            try {
                showMainWindow(stage);
                splash.close();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        splashDelay.play();
    }

    private void showMainWindow(Stage stage) throws IOException {
        scene = new Scene(loadFXML("home"), 1200, 800);

        // Apply default theme (LIGHT by default)
        ThemeService.register(scene);
        ThemeService.apply(scene, ThemeService.Theme.LIGHT);

        // Keyboard toggle: Ctrl + D switches theme
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
            () -> ThemeService.toggle(scene)
        );

        stage.getIcons().setAll(stageIcons);
        stage.setTitle("klausurCraft");
        stage.setScene(scene);
        stage.show();
    }

    public static Image getAppIcon() {
        return appIcon;
    }

    public static List<Image> getStageIcons() {
        return stageIcons;
    }

    private void installAppIconOnTaskbar() {
        BufferedImage awtIcon = toBufferedImage(appIcon);
        if (awtIcon == null) return;

        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(awtIcon);
                    return;
                }
            }
        } catch (Exception ignored) {
            // Try macOS fallback below
        }

        // macOS legacy fallback for Dock icon
        try {
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Method getApplication = appClass.getMethod("getApplication");
            Object appInstance = getApplication.invoke(null);
            Method setDockIconImage = appClass.getMethod("setDockIconImage", java.awt.Image.class);
            setDockIconImage.invoke(appInstance, awtIcon);
        } catch (Exception ignored) {
            // best effort only
        }
    }

    private static BufferedImage toBufferedImage(Image fxImage) {
        if (fxImage == null) return null;
        int width = (int) Math.ceil(fxImage.getWidth());
        int height = (int) Math.ceil(fxImage.getHeight());
        PixelReader reader = fxImage.getPixelReader();
        if (reader == null || width <= 0 || height <= 0) return null;

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return out;
    }

    private Stage createSplashStage() {
        Label brandText = new Label("KC");
        brandText.setTextFill(Color.WHITE);
        brandText.setFont(Font.font("Inter", FontWeight.EXTRA_BOLD, 40));

        StackPane brandMark = new StackPane(brandText);
        brandMark.setMinSize(92, 92);
        brandMark.setPrefSize(92, 92);
        brandMark.setMaxSize(92, 92);
        brandMark.setStyle(
            "-fx-background-radius: 24;" +
            "-fx-background-color: linear-gradient(to bottom right, #475569, #64748b);" +
            "-fx-effect: dropshadow(gaussian, rgba(51,65,85,0.34), 16, 0.24, 0, 5);"
        );

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.2), brandMark);
        pulse.setFromX(0.95);
        pulse.setFromY(0.95);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setCycleCount(ScaleTransition.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();

        Label title = new Label("klausurCraft");
        title.setTextFill(Color.web("#f8fafc"));
        title.setFont(Font.font("Inter", FontWeight.BOLD, 30));

        Label subtitle = new Label("Building your exam workspace...");
        subtitle.setTextFill(Color.web("#cbd5e1"));
        subtitle.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(30, 30);
        loading.setStyle("-fx-progress-color: #94a3b8;");

        Label loadingLabel = new Label("Loading");
        loadingLabel.setTextFill(Color.web("#e2e8f0"));
        loadingLabel.setFont(Font.font("Inter", FontWeight.MEDIUM, 14));

        HBox loadingRow = new HBox(12, loading, loadingLabel);
        loadingRow.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, brandMark, title, subtitle, loadingRow);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));
        content.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #111827, #1f2937);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(148,163,184,0.24);" +
            "-fx-border-radius: 20;"
        );

        StackPane root = new StackPane(content);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: transparent;");

        Scene splashScene = new Scene(root, 500, 320);
        splashScene.setFill(Color.TRANSPARENT);

        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.getIcons().setAll(stageIcons);
        splashStage.setAlwaysOnTop(true);
        return splashStage;
    }

    private List<Image> createStageIcons() {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256, 512};
        List<Image> icons = new ArrayList<>(sizes.length);
        for (int size : sizes) {
            icons.add(createAppIcon(size));
        }
        return Collections.unmodifiableList(icons);
    }

    private Image createAppIcon(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double s = size;
        double corner = s * 0.25;

        gc.clearRect(0, 0, s, s);

        gc.setFill(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#4b5b70")),
            new Stop(1, Color.web("#708399"))
        ));
        gc.fillRoundRect(0, 0, s, s, corner, corner);

        gc.setFill(Color.rgb(255, 255, 255, 0.14));
        gc.fillOval(s * 0.08, s * 0.07, s * 0.42, s * 0.42);

        // Match requested brand mark: centered "KC" in white.
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.EXTRA_BOLD, s * 0.37));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("KC", s * 0.5, s * 0.53);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = new WritableImage(size, size);
        canvas.snapshot(params, image);
        return image;
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
