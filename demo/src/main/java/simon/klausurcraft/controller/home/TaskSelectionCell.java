package simon.klausurcraft.controller.home;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import simon.klausurcraft.model.GenerateScope;

public class TaskSelectionCell extends ListCell<TaskSelection> {

    private final HomeController root;
    private final CheckBox cbEnable = new CheckBox();
    private final Label title = new Label();
    private final ComboBox<Integer> cbPoints = new ComboBox<>();
    private final Button recompute = new Button("Recompute");

    public TaskSelectionCell(HomeController root) {
        this.root = root;
        HBox box = new HBox(8, cbEnable, title, new Region(), new Label("Points:"), cbPoints, recompute);
        HBox.setHgrow(box.getChildren().get(2), Priority.ALWAYS);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(box);

        cbEnable.selectedProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts != null) {
                ts.setEnabled(nv);
                if (nv && ts.getAchievable().isEmpty()) {
                    HomeNotifications.showError("No achievable sums for: " + ts.getTask().getTitle() +
                            ". Add subtasks with diverse difficulties.");
                }
            }
        });

        cbPoints.valueProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts != null && nv != null) {
                ts.chosenPointsProperty().set(nv);
            }
        });

        recompute.setOnAction(e -> {
            TaskSelection ts = getItem();
            if (ts != null) {
                ts.recomputeAchievable(root.scope.get());
                if (ts.getAchievable().isEmpty()) {
                    HomeNotifications.showError("No achievable point sums (1/3 difficulty rule).");
                }
            }
        });
    }

    @Override
    protected void updateItem(TaskSelection item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());
            cbEnable.selectedProperty().unbind();
            cbEnable.selectedProperty().bindBidirectional(item.enabledProperty());

            cbPoints.itemsProperty().unbind();
            item.recomputeAchievable(root.scope.get());
            cbPoints.setItems(item.getAchievable());
            if (!item.getAchievable().isEmpty()) {
                cbPoints.getSelectionModel().select(item.getAchievable().get(0));
            } else {
                cbPoints.getSelectionModel().clearSelection();
            }
            setGraphic(((HBox) getGraphic()));
        }
    }
}