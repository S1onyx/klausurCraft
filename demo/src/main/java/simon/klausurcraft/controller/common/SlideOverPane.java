package simon.klausurcraft.controller.common;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.util.Duration;

/**
 * Minimal, dependency-free slide-over ("sheet") from the right.
 */
public class SlideOverPane {

    private final StackPane container = new StackPane();
    private final StackPane backdrop = new StackPane();
    private final VBox sheet = new VBox();

    private boolean shown = false;

    public SlideOverPane() {
        container.setPickOnBounds(false); // don't block clicks outside children

        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.35);");
        backdrop.setVisible(false);
        backdrop.setManaged(false);

        sheet.getStyleClass().add("sheet");
        sheet.setMinWidth(420);
        sheet.setMaxWidth(520);
        sheet.setPadding(new Insets(12));
        sheet.setTranslateX(520);

        HBox holder = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        holder.getChildren().addAll(spacer, sheet);

        container.getChildren().addAll(backdrop, holder);

        backdrop.setOnMouseClicked(e -> hide());
    }

    public StackPane getContainer() { return container; }

    public void setContent(Node content) {
        sheet.getChildren().setAll(content);
    }

    public void show() {
        if (shown) return;
        shown = true;
        backdrop.setVisible(true);
        animateTo(0);
    }

    public void hide() {
        if (!shown) return;
        shown = false;
        animateTo(520);
        backdrop.setVisible(false);
    }

    public void hideInstant() {
        shown = false;
        sheet.setTranslateX(520);
        backdrop.setVisible(false);
    }

    private void animateTo(double x) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), sheet);
        tt.setToX(x);
        tt.play();
    }
}