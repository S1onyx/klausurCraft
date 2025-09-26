package simon.klausurcraft.controller.home;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.services.PointCombination;

import java.util.List;

public class TaskSelection {
    private final TaskModel task;
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final IntegerProperty chosenPoints = new SimpleIntegerProperty(0);
    private final ObservableList<Integer> achievable = FXCollections.observableArrayList();

    public TaskSelection(TaskModel task) {
        this.task = task;
    }

    public TaskModel getTask() { return task; }
    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean v) { enabled.set(v); }
    public BooleanProperty enabledProperty() { return enabled; }
    public int getChosenPoints() { return chosenPoints.get(); }
    public IntegerProperty chosenPointsProperty() { return chosenPoints; }
    public ObservableList<Integer> getAchievable() { return achievable; }

    public void recomputeAchievable(GenerateScope scope) {
        achievable.setAll(PointCombination.achievablePointSums(task, scope));
        if (!achievable.contains(chosenPoints.get())) {
            chosenPoints.set(achievable.isEmpty() ? 0 : achievable.get(0));
        }
    }

    @Override public String toString() { return task.getTitle(); }

    // helpers for list
    public static ObservableList<TaskSelection> ensureFor(List<TaskModel> tasks) {
        ObservableList<TaskSelection> list = FXCollections.observableArrayList();
        for (TaskModel t : tasks) list.add(new TaskSelection(t));
        return list;
    }

    public static javafx.beans.binding.StringBinding totalPointsBinding(ObservableList<TaskSelection> items) {
        return Bindings.createStringBinding(() ->
                        "Total points: " + items.stream()
                                .filter(TaskSelection::isEnabled)
                                .map(TaskSelection::getChosenPoints)
                                .mapToInt(Integer::intValue).sum(),
                items);
    }
}