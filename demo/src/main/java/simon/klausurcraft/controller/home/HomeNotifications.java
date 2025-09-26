package simon.klausurcraft.controller.home;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import simon.klausurcraft.App;

final class HomeNotifications {
    private HomeNotifications(){}

    static void showError(String msg) { showBanner(msg, true); }
    static void showInfo(String msg) { showBanner(msg, false); }

    private static void showBanner(String msg, boolean error) {
        Label l = new Label(msg);
        l.getStyleClass().add(error ? "banner-error" : "banner-info");
        VBox banner = new VBox(l);
        banner.getStyleClass().add("banner");
        ((StackPane) App.getScene().getRoot()).getChildren().add(banner);
        StackPane.setAlignment(banner, javafx.geometry.Pos.TOP_CENTER);
        new Thread(() -> {
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> ((StackPane) App.getScene().getRoot()).getChildren().remove(banner));
        }).start();
    }
}