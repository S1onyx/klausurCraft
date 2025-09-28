package simon.klausurcraft.controller.home;

import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.services.PointCombination;

import java.util.ArrayList;
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

    /**
     * Binding that dynamically recalculates the sum,
     * whenever items are added/removed OR enabled/points change.
     */
    public static StringBinding totalPointsBinding(ObservableList<TaskSelection> items) {
        class TotalBinding extends StringBinding {
            private final List<Observable> observables = new ArrayList<>();

            private void rebind() {
                // unbind old bindings
                super.unbind(observables.toArray(Observable[]::new));
                observables.clear();

                // bind to the list itself
                observables.add(items);

                // and to all relevant properties of the items
                for (TaskSelection ts : items) {
                    observables.add(ts.enabledProperty());
                    observables.add(ts.chosenPointsProperty());
                }
                super.bind(observables.toArray(Observable[]::new));
                invalidate();
            }

            {
                // Rebind when the list structurally changes
                items.addListener((ListChangeListener<TaskSelection>) c -> rebind());
                // Initial
                rebind();
            }

            @Override
            protected String computeValue() {
                int sum = items.stream()
                        .filter(TaskSelection::isEnabled)
                        .map(TaskSelection::getChosenPoints)
                        .mapToInt(Integer::intValue)
                        .sum();
                return "Total points: " + sum;
            }
        }
        return new TotalBinding();
    }
}