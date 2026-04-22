package simon.klausurcraft.ui.home;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

final class HomeNotifications {
    private static VBox host;
    private static VBox currentBanner;

    private HomeNotifications(){}

    static void attachHost(VBox notificationHost) {
        host = notificationHost;
    }

    static void showError(String msg) { showBanner(msg, true); }
    static void showInfo(String msg) { showBanner(msg, false); }

    private static void showBanner(String msg, boolean error) {
        if (host == null) return;

        Label l = new Label(msg);
        l.setWrapText(true);
        VBox banner = new VBox(l);
        banner.getStyleClass().addAll("banner", error ? "banner-error" : "banner-info");
        banner.setMaxWidth(680);

        if (currentBanner != null) {
            host.getChildren().remove(currentBanner);
        }
        currentBanner = banner;

        host.getChildren().setAll(banner);
        host.setManaged(true);
        host.setVisible(true);

        PauseTransition delay = new PauseTransition(Duration.millis(2500));
        delay.setOnFinished(e -> Platform.runLater(() -> {
            host.getChildren().remove(banner);
            if (host.getChildren().isEmpty()) {
                host.setManaged(false);
                host.setVisible(false);
                currentBanner = null;
            }
        }));
        delay.play();
    }
}
