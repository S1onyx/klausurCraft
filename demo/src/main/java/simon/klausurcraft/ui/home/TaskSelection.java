package simon.klausurcraft.ui.home;

import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.planning.PointDistributionPlanner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TaskSelection {
    private final Task task;
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<BigDecimal> chosenPoints = new SimpleObjectProperty<>(Points.ZERO);
    private final ObservableList<BigDecimal> achievable = FXCollections.observableArrayList();

    public TaskSelection(Task task) {
        this.task = task;
    }

    public Task getTask() { return task; }
    public boolean isEnabled() { return enabled.get(); }
    public void setEnabled(boolean v) { enabled.set(v); }
    public BooleanProperty enabledProperty() { return enabled; }
    public BigDecimal getChosenPoints() { return chosenPoints.get(); }
    public ObjectProperty<BigDecimal> chosenPointsProperty() { return chosenPoints; }
    public ObservableList<BigDecimal> getAchievable() { return achievable; }

    public void recomputeAchievable(GenerateScope scope) {
        achievable.setAll(PointDistributionPlanner.achievablePointSums(task, scope));
        if (!achievable.contains(chosenPoints.get())) {
            chosenPoints.set(achievable.isEmpty() ? Points.ZERO : achievable.get(0));
        }
    }

    @Override public String toString() { return task.getTitle(); }

    // helpers for list
    public static ObservableList<TaskSelection> ensureFor(List<Task> tasks) {
        ObservableList<TaskSelection> list = FXCollections.observableArrayList();
        for (Task t : tasks) list.add(new TaskSelection(t));
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
                BigDecimal sum = items.stream()
                        .filter(TaskSelection::isEnabled)
                        .map(TaskSelection::getChosenPoints)
                        .reduce(Points.ZERO, BigDecimal::add);
                return "Total points: " + Points.toDisplayString(sum);
            }
        }
        return new TotalBinding();
    }
}
