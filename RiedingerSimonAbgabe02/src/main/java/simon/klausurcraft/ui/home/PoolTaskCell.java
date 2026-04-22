package simon.klausurcraft.ui.home;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import simon.klausurcraft.task.Points;

import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Cell for the TASK POOL (bottom).
 * - Checkbox checked => move to selected (enable)
 * - Shows a textual list of achievable point sums (e.g., "Possible: 6, 8, 10")
 * - If no achievable sums, checkbox is disabled and a tooltip explains why.
 */
class PoolTaskCell extends ListCell<TaskSelection> {

    private final HomeController root;
    private final Consumer<TaskSelection> onSelectFromPool;

    private final CheckBox cbPick = new CheckBox();
    private final Label title = new Label();
    private final Label lblPossible = new Label();
    private final Button btnWhy = new Button("Why?");
    private final HBox possibleBox = new HBox(8);
    private final HBox box;

    PoolTaskCell(HomeController root,
                 Consumer<TaskSelection> onSelectFromPool) {
        this.root = root;
        this.onSelectFromPool = onSelectFromPool;
        cbPick.setTooltip(new Tooltip("Check to move this task into selected tasks."));

        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(false);
        HBox.setHgrow(title, Priority.ALWAYS);
        lblPossible.getStyleClass().add("pool-possible-cell");
        lblPossible.setWrapText(true);
        possibleBox.getStyleClass().add("pool-possible-column");
        possibleBox.setMinWidth(260);
        possibleBox.setPrefWidth(360);
        possibleBox.setMaxWidth(Double.MAX_VALUE);
        possibleBox.getChildren().add(lblPossible);
        lblPossible.maxWidthProperty().bind(possibleBox.widthProperty().subtract(8));
        HBox.setHgrow(possibleBox, Priority.SOMETIMES);

        btnWhy.getStyleClass().add("chip");
        btnWhy.setTooltip(new Tooltip("Show why this task currently has no possible points."));
        btnWhy.setOnAction(e -> showNoOptionsHelp());

        box = new HBox(12, cbPick, title, possibleBox);
        box.setFillHeight(true);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(box);

        cbPick.selectedProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts == null) return;
            if (nv) {
                if (ts.getAchievable().isEmpty()) {
                    cbPick.setSelected(false);
                    HomeNotifications.showError("This task has no possible points in the current scope.");
                    return;
                }
                if (onSelectFromPool != null) {
                    onSelectFromPool.accept(ts);
                }
            }
        });
    }

    @Override
    protected void updateItem(TaskSelection item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        cbPick.setSelected(false);
        boolean hasOptions = !item.getAchievable().isEmpty();
        cbPick.setDisable(!hasOptions);
        possibleBox.getChildren().setAll(lblPossible);

        if (!hasOptions) {
            // UI/UX-Rule "Empathy"
            cbPick.setTooltip(new Tooltip("No achievable points in current scope. Add subtasks with valid combinations."));
            lblPossible.setText("No possible points in this scope");
            possibleBox.getChildren().add(btnWhy);
        } else {
            String poss = item.getAchievable().stream()
                    .map(Points::toDisplayString)
                    .collect(Collectors.joining(", "));
            lblPossible.setText(poss);
            cbPick.setTooltip(new Tooltip("Check to move this task into selected tasks."));
        }

        title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());
        setGraphic(box);
    }

    private void showNoOptionsHelp() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("No possible points");
        info.setHeaderText("This task has no achievable point sum in the current scope.");
        info.setContentText("""
                Try one of these options:
                1) Go Back and change the scope (exam/practice/both).
                2) Add or edit subtasks so there are valid combinations.
                3) Return and include the task afterwards.
                """);
        info.initOwner(root.getWindow());
        simon.klausurcraft.ui.UiStyles.applyCurrentStyles(info);
        info.showAndWait();
    }
}
