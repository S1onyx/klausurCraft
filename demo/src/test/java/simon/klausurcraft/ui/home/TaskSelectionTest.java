package simon.klausurcraft.ui.home;

import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.testutil.TaskFixtures;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskSelectionTest {

    @Test
    void recomputeAchievable_setsDefaultChosenPointsIfCurrentChoiceIsInvalid() {
        Task t = TaskFixtures.newTask("0001", "Selection");
        TaskFixtures.addSubtask(t, "0001", "1.5", Difficulty.EASY, Eligibility.BOTH);
        TaskSelection selection = new TaskSelection(t);

        selection.recomputeAchievable(GenerateScope.BOTH);

        assertFalse(selection.getAchievable().isEmpty());
        assertEquals(selection.getAchievable().getFirst(), selection.getChosenPoints());
    }

    @Test
    void totalPointsBinding_tracksEnablePointsAndListChanges() {
        Task t1 = TaskFixtures.newTask("0001", "T1");
        TaskFixtures.addSubtask(t1, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        Task t2 = TaskFixtures.newTask("0002", "T2");
        TaskFixtures.addSubtask(t2, "0001", "2", Difficulty.MEDIUM, Eligibility.BOTH);
        Task t3 = TaskFixtures.newTask("0003", "T3");
        TaskFixtures.addSubtask(t3, "0001", "0.5", Difficulty.HARD, Eligibility.BOTH);

        ObservableList<TaskSelection> selections = FXCollections.observableArrayList(TaskSelection.ensureFor(List.of(t1, t2)));
        selections.forEach(s -> s.recomputeAchievable(GenerateScope.BOTH));

        TaskSelection s1 = selections.get(0);
        TaskSelection s2 = selections.get(1);
        StringBinding binding = TaskSelection.totalPointsBinding(selections);

        assertEquals("Total points: 0", binding.get());

        s1.setEnabled(true);
        s1.chosenPointsProperty().set(new BigDecimal("1.5"));
        assertEquals("Total points: 1,5", binding.get());

        s2.setEnabled(true);
        s2.chosenPointsProperty().set(new BigDecimal("2"));
        assertEquals("Total points: 3,5", binding.get());

        s1.setEnabled(false);
        assertEquals("Total points: 2", binding.get());

        TaskSelection s3 = new TaskSelection(t3);
        s3.recomputeAchievable(GenerateScope.BOTH);
        s3.setEnabled(true);
        s3.chosenPointsProperty().set(Points.parseInput("0,5"));
        selections.add(s3);
        assertEquals("Total points: 2,5", binding.get());
    }
}
