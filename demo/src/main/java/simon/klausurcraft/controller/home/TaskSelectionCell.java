package simon.klausurcraft.controller.home;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * ListCell for TaskSelection rows in Step 2 (Generate flow).
 * - Compact, responsive layout (no horizontal scrollbars needed)
 * - Reordering via ↑ / ↓ buttons and Drag & Drop
 * - Persistent graphic to avoid empty rows on reuse
 */
public class TaskSelectionCell extends ListCell<TaskSelection> {

    private final HomeController root;

    private final CheckBox cbEnable = new CheckBox();
    private final Label title = new Label();
    private final ComboBox<Integer> cbPoints = new ComboBox<>();
    private final Button btnUp = new Button("↑");
    private final Button btnDown = new Button("↓");

    private final HBox box;

    public TaskSelectionCell(HomeController root) {
        this.root = root;

        // Title should take remaining width and elide; keeps row compact on small widths.
        title.setMaxWidth(Double.MAX_VALUE);
        title.setEllipsisString("…");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Keep the points combo reasonably small to avoid horizontal overflow.
        cbPoints.setPrefWidth(70);
        cbPoints.setMaxWidth(100);
        cbPoints.setMinWidth(60);

        btnUp.getStyleClass().add("chip");
        btnDown.getStyleClass().add("chip");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Row layout: [☐][Title........][spacer][Points:][Combo][↑][↓]
        Label lblPoints = new Label("Points:");
        box = new HBox(8, cbEnable, title, spacer, lblPoints, cbPoints, btnUp, btnDown);
        box.setFillHeight(true);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(box);

        // ------- Behavior: enable/points -------
        cbEnable.selectedProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts != null) {
                ts.setEnabled(nv);
                if (nv && ts.getAchievable().isEmpty()) {
                    HomeNotifications.showError(
                        "No achievable sums for: " + ts.getTask().getTitle() +
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

        // ------- Reorder via buttons -------
        btnUp.setOnAction(e -> {
            int idx = getIndex();
            if (idx > 0) moveItem(idx, idx - 1);
        });
        btnDown.setOnAction(e -> {
            int idx = getIndex();
            if (idx >= 0 && idx < getListView().getItems().size() - 1) {
                moveItem(idx, idx + 1);
            }
        });

        // ------- Drag & Drop reordering -------
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

        setOnDragEntered(e -> {
            if (e.getGestureSource() != this && !isEmpty()) {
                setOpacity(0.85);
            }
        });
        setOnDragExited(e -> setOpacity(1.0));

        setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int from = Integer.parseInt(db.getString());
                int to = getIndex();
                moveItem(from, to);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });

        setOnDragDone(e -> setOpacity(1.0));
    }

    private void moveItem(int from, int to) {
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

        title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());
        cbEnable.setSelected(item.isEnabled());

        // Ensure achievable sums for the current scope
        item.recomputeAchievable(root.scope.get());
        cbPoints.setItems(item.getAchievable());

        if (!item.getAchievable().isEmpty()) {
            if (!item.getAchievable().contains(item.getChosenPoints())) {
                cbPoints.getSelectionModel().select(item.getAchievable().get(0));
            } else {
                cbPoints.getSelectionModel().select(Integer.valueOf(item.getChosenPoints()));
            }
        } else {
            cbPoints.getSelectionModel().clearSelection();
        }

        setGraphic(box);
    }
}