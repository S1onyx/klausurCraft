package simon.klausurcraft.ui.home;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import simon.klausurcraft.app.KlausurCraftApp;

final class HomeNotifications {
    private HomeNotifications(){}

    static void showError(String msg) { showBanner(msg, true); }
    static void showInfo(String msg) { showBanner(msg, false); }

    private static void showBanner(String msg, boolean error) {
        Label l = new Label(msg);
        l.getStyleClass().add(error ? "banner-error" : "banner-info");
        VBox banner = new VBox(l);
        banner.getStyleClass().add("banner");
        StackPane root = (StackPane) KlausurCraftApp.getScene().getRoot();
        root.getChildren().add(banner);
        StackPane.setAlignment(banner, Pos.TOP_CENTER);

        PauseTransition delay = new PauseTransition(Duration.millis(2500));
        delay.setOnFinished(e -> root.getChildren().remove(banner));
        delay.play();
    }
}
