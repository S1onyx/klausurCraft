package simon.klausurcraft.controller.home;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cell for the TASK POOL (bottom).
 * - Checkbox checked => move to selected (enable)
 * - Shows a textual list of achievable point sums (e.g., "Possible: 6, 8, 10")
 * - If no achievable sums, checkbox is disabled and a tooltip explains why.
 */
class PoolTaskCell extends ListCell<TaskSelection> {

    private final List<TaskSelection> selected;
    private final List<TaskSelection> pool;

    private final CheckBox cbPick = new CheckBox();
    private final Label title = new Label();
    private final Label lblPossible = new Label();
    private final HBox box;

    PoolTaskCell(HomeController root,
                 List<TaskSelection> selected,
                 List<TaskSelection> pool) {
        this.selected = selected;
        this.pool = pool;

        title.setMaxWidth(Double.MAX_VALUE);
        title.setWrapText(false);
        HBox.setHgrow(title, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        box = new HBox(8, cbPick, title, spacer, lblPossible);
        box.setFillHeight(true);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setGraphic(box);

        cbPick.selectedProperty().addListener((o, ov, nv) -> {
            TaskSelection ts = getItem();
            if (ts == null) return;
            if (nv) {
                // move to selected
                ts.setEnabled(true);
                pool.remove(ts);
                if (!selected.contains(ts)) {
                    if (ts.getChosenPoints() == 0 && !ts.getAchievable().isEmpty()) {
                        ts.chosenPointsProperty().set(ts.getAchievable().get(0));
                    }
                    selected.add(ts);
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

        if (!hasOptions) {
            Tooltip tip = new Tooltip("No achievable point sum for current scope. Add subtasks with diverse difficulties.");
            Tooltip.install(cbPick, tip);
            lblPossible.setText("(no possible points)");
        } else {
            String poss = item.getAchievable().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            lblPossible.setText("Possible: " + poss);
            Tooltip.uninstall(cbPick, null);
        }

        title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());
        setGraphic(box);
    }
}