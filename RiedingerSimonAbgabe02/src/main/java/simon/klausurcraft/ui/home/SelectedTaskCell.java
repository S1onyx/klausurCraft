package simon.klausurcraft.ui.home;

import javafx.animation.ScaleTransition;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.planning.PointDistributionPlanner;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

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
    private final Consumer<TaskSelection> onMoveToPool;

    private final CheckBox cbSelected = new CheckBox();
    private final Label title = new Label();
    private final Label lblPoints = new Label("Points:");
    private final ComboBox<BigDecimal> cbPoints = new ComboBox<>();
    private final Button btnSuggest = new Button("Suggest");
    private final Button btnUp = new Button("↑");
    private final Button btnDown = new Button("↓");

    private final HBox box;
    private TaskSelection boundTaskSelection;
    private final ChangeListener<BigDecimal> chosenPointsListener = (obs, oldVal, newVal) -> refreshSuggestState(getItem());
    private final ListChangeListener<BigDecimal> achievableListener = c -> refreshSuggestState(getItem());

    SelectedTaskCell(HomeController root,
                     List<TaskSelection> selected,
                     List<TaskSelection> pool,
                     Consumer<TaskSelection> onMoveToPool) {
        this.root = root;
        this.selected = selected;
        this.pool = pool;
        this.onMoveToPool = onMoveToPool;

        cbSelected.setSelected(true); // in this list, items are selected
        cbSelected.setTooltip(new Tooltip("Uncheck to move this task back to the pool."));

        title.setMaxWidth(Double.MAX_VALUE);
        // Keep full title visible if possible (no ellipsis)
        HBox.setHgrow(title, Priority.ALWAYS);

        cbPoints.getStyleClass().add("points-combo");
        cbPoints.setMinWidth(120);
        cbPoints.setPrefWidth(140);
        cbPoints.setMaxWidth(180);
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
                        playSuggestFeedback();
                        refreshSuggestState(ts);
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
                if (onMoveToPool != null) {
                    onMoveToPool.accept(ts);
                }
            }
        });

        itemProperty().addListener((obs, oldItem, newItem) -> {
            if (oldItem != null) {
                oldItem.chosenPointsProperty().removeListener(chosenPointsListener);
                oldItem.getAchievable().removeListener(achievableListener);
            }
            if (newItem != null) {
                newItem.chosenPointsProperty().addListener(chosenPointsListener);
                newItem.getAchievable().addListener(achievableListener);
            }
            refreshSuggestState(newItem);
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
        lv.refresh();
    }

    @Override
    protected void updateItem(TaskSelection item, boolean empty) {
        super.updateItem(item, empty);

        if (boundTaskSelection != null) {
            cbPoints.valueProperty().unbindBidirectional(boundTaskSelection.chosenPointsProperty());
            boundTaskSelection = null;
        }

        if (empty || item == null) {
            cbPoints.setItems(javafx.collections.FXCollections.emptyObservableList());
            cbPoints.getSelectionModel().clearSelection();
            setGraphic(null);
            setText(null);
            return;
        }

        cbSelected.setSelected(true);
        item.setEnabled(true); // being in selected marks it enabled
        title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());

        // ensure achievable is up to date (scope fixed in step 2)
        cbPoints.setItems(item.getAchievable());
        if (!item.getAchievable().isEmpty()) {
            if (!item.getAchievable().contains(item.getChosenPoints())) {
                cbPoints.getSelectionModel().select(item.getAchievable().get(0));
            } else {
                cbPoints.getSelectionModel().select(item.getChosenPoints());
            }
        } else {
            cbPoints.getSelectionModel().clearSelection();
        }
        boundTaskSelection = item;
        cbPoints.valueProperty().bindBidirectional(item.chosenPointsProperty());
        refreshSuggestState(item);

        setGraphic(box);
    }

    private void refreshSuggestState(TaskSelection ts) {
        if (ts == null || ts.getAchievable().isEmpty()) {
            btnSuggest.setDisable(true);
            btnSuggest.setTooltip(new Tooltip("No suggestion available for this task in current scope."));
            return;
        }
        BigDecimal fallback = ts.getAchievable().get(0);
        BigDecimal suggested = PointDistributionPlanner
                .suggestBestPointSum(ts.getTask(), root.scope.get())
                .orElse(fallback);
        boolean alreadySuggested = suggested.equals(ts.getChosenPoints());
        btnSuggest.setDisable(alreadySuggested);
        if (alreadySuggested) {
            btnSuggest.setTooltip(new Tooltip("Suggested points are already selected."));
        } else {
            btnSuggest.setTooltip(new Tooltip("Auto-select the best reachable points for current scope."));
        }
    }

    private void playSuggestFeedback() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(180), cbPoints);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.03);
        pulse.setToY(1.03);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }
}
