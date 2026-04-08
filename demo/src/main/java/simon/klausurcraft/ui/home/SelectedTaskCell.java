package simon.klausurcraft.ui.home;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.planning.PointDistributionPlanner;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cell for the SELECTED list (top).
 * - Checkbox unchecked => move back to pool (disable)
 * - Always show "Points:" label and ComboBox with achievable values
 * - Reorder with ↑ / ↓ and Drag & Drop
 */
class SelectedTaskCell extends ListCell<TaskSelection> {

    private final HomeController root;
    private final List<TaskSelection> selected;
    private final List<TaskSelection> pool;

    private final CheckBox cbSelected = new CheckBox();
    private final Label title = new Label();
    private final Label lblPoints = new Label("Points:");
    private final ComboBox<BigDecimal> cbPoints = new ComboBox<>();
    private final Button btnSuggest = new Button("Suggest");
    private final Button btnUp = new Button("↑");
    private final Button btnDown = new Button("↓");

    private final HBox box;

    SelectedTaskCell(HomeController root,
                     List<TaskSelection> selected,
                     List<TaskSelection> pool) {
        this.root = root;
        this.selected = selected;
        this.pool = pool;

        cbSelected.setSelected(true); // in this list, items are selected
        cbSelected.setTooltip(new Tooltip("Uncheck to move this task back to the pool."));

        title.setMaxWidth(Double.MAX_VALUE);
        // Keep full title visible if possible (no ellipsis)
        HBox.setHgrow(title, Priority.ALWAYS);

        cbPoints.setPrefWidth(90);
        cbPoints.setVisibleRowCount(10);
        cbPoints.setTooltip(new Tooltip("Choose target points for this task."));
        cbPoints.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : Points.toDisplayString(item));
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        cbPoints.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : Points.toDisplayString(item));
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        btnSuggest.getStyleClass().add("chip");
        btnSuggest.setTooltip(new Tooltip("Auto-select the best reachable points for current scope."));
        btnSuggest.setOnAction(e -> {
            TaskSelection ts = getItem();
            if (ts == null) return;

            ts.recomputeAchievable(root.scope.get());
            PointDistributionPlanner.suggestBestPointSum(ts.getTask(), root.scope.get())
                    .ifPresentOrElse(best -> {
                        ts.chosenPointsProperty().set(best);
                        cbPoints.getSelectionModel().select(best);
                    }, () -> HomeNotifications.showError(
                            "No point suggestion available for this task in current scope."));
        });

        btnUp.getStyleClass().add("chip");
        btnDown.getStyleClass().add("chip");
        btnUp.setTooltip(new Tooltip("Move task one row up."));
        btnDown.setTooltip(new Tooltip("Move task one row down."));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box = new HBox(8, cbSelected, title, spacer, lblPoints, cbPoints, btnSuggest, btnUp, btnDown);
        box.setFillHeight(true);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(box);

        // Behavior
        cbSelected.selectedProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts == null) return;
            if (!nv) {
                // move to pool
                ts.setEnabled(false);
                selected.remove(ts);
                if (!pool.contains(ts)) pool.add(ts);
            }
        });

        cbPoints.valueProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts != null && nv != null) ts.chosenPointsProperty().set(nv);
        });

        btnUp.setOnAction(e -> moveItem(-1));
        btnDown.setOnAction(e -> moveItem(+1));

        // Drag & Drop reordering within selected
        setOnDragDetected(e -> {
            if (isEmpty()) return;
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(Integer.toString(getIndex()));
            db.setContent(cc);
            e.consume();
        });

        setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString() && !isEmpty() && getIndex() != Integer.parseInt(db.getString())) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int from = Integer.parseInt(db.getString());
                int to = getIndex();
                move(from, to);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void moveItem(int delta) {
        int idx = getIndex();
        int to = idx + delta;
        move(idx, to);
    }

    private void move(int from, int to) {
        var lv = getListView();
        if (lv == null) return;
        var items = lv.getItems();
        if (from < 0 || from >= items.size() || to < 0 || to >= items.size()) return;
        TaskSelection ts = items.remove(from);
        items.add(to, ts);
        lv.getSelectionModel().clearAndSelect(to);
        lv.scrollTo(Math.max(0, to - 1));
    }

    @Override
    protected void updateItem(TaskSelection item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        cbSelected.setSelected(true);
        item.setEnabled(true); // being in selected marks it enabled
        title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());

        // ensure achievable is up to date (scope fixed in step 2)
        cbPoints.setItems(item.getAchievable());
        btnSuggest.setDisable(item.getAchievable().isEmpty());
        if (!item.getAchievable().isEmpty()) {
            if (!item.getAchievable().contains(item.getChosenPoints())) {
                cbPoints.getSelectionModel().select(item.getAchievable().get(0));
            } else {
                cbPoints.getSelectionModel().select(item.getChosenPoints());
            }
        } else {
            cbPoints.getSelectionModel().clearSelection();
        }

        setGraphic(box);
    }
}
