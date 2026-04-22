package simon.klausurcraft.ui.home;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;
import simon.klausurcraft.testutil.TaskFixtures;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeGeneratePlannerTest {

    @Test
    void moveToSelected_movesTaskAndSetsSuggestedPoints() {
        // Use case: user picks one valid task from pool into selected list.
        Task task = TaskFixtures.newTask("0001", "Planner");
        TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        TaskSelection item = new TaskSelection(task);

        List<TaskSelection> pool = new ArrayList<>(List.of(item));
        List<TaskSelection> selected = new ArrayList<>();

        boolean moved = HomeGeneratePlanner.moveToSelected(item, pool, selected, GenerateScope.BOTH);

        assertTrue(moved);
        assertTrue(item.isEnabled());
        assertFalse(pool.contains(item));
        assertTrue(selected.contains(item));
        assertTrue(item.getAchievable().contains(item.getChosenPoints()));
    }

    @Test
    void moveToSelected_returnsFalseWhenScopeHasNoEligibleSubtasks() {
        // Error/special case: task has no achievable points in selected scope.
        Task task = TaskFixtures.newTask("0001", "Only practice");
        TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.PRACTICE);
        TaskSelection item = new TaskSelection(task);

        List<TaskSelection> pool = new ArrayList<>(List.of(item));
        List<TaskSelection> selected = new ArrayList<>();

        boolean moved = HomeGeneratePlanner.moveToSelected(item, pool, selected, GenerateScope.EXAM);

        assertFalse(moved);
        assertFalse(item.isEnabled());
        assertTrue(pool.contains(item));
        assertTrue(selected.isEmpty());
        assertEquals(Points.ZERO, item.getChosenPoints());
    }

    @Test
    void moveToPool_disablesAndMovesBackToPool() {
        // Reverse flow: selected task is deselected and moved back.
        TaskSelection item = selection("0001", Eligibility.BOTH);
        item.setEnabled(true);

        List<TaskSelection> selected = new ArrayList<>(List.of(item));
        List<TaskSelection> pool = new ArrayList<>();

        boolean moved = HomeGeneratePlanner.moveToPool(item, selected, pool);

        assertTrue(moved);
        assertFalse(item.isEnabled());
        assertFalse(selected.contains(item));
        assertTrue(pool.contains(item));
    }

    @Test
    void suggestPointsForAll_countsOnlyChangedSelections() {
        // Equivalence class: all selected tasks start at 0 and receive suggested values.
        TaskSelection a = selection("0001", Eligibility.BOTH);
        TaskSelection b = selection("0002", Eligibility.BOTH);
        a.chosenPointsProperty().set(BigDecimal.ZERO);
        b.chosenPointsProperty().set(BigDecimal.ZERO);

        int changed = HomeGeneratePlanner.suggestPointsForAll(GenerateScope.BOTH, List.of(a, b));

        assertEquals(2, changed);
        assertTrue(a.getChosenPoints().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(b.getChosenPoints().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void applySmartFill_selectsUpToFourTasksAndTunesPoints() {
        // Smart fill target: select up to four candidates and set reasonable points.
        List<TaskSelection> selected = new ArrayList<>();
        List<TaskSelection> pool = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            TaskSelection ts = selection(String.format("%04d", i), Eligibility.BOTH);
            ts.recomputeAchievable(GenerateScope.BOTH);
            pool.add(ts);
        }

        int tuned = HomeGeneratePlanner.applySmartFill(GenerateScope.BOTH, selected, pool);

        assertEquals(4, selected.size());
        assertEquals(1, pool.size());
        assertEquals(4, tuned);
        assertTrue(selected.stream().allMatch(TaskSelection::isEnabled));
        assertTrue(selected.stream().allMatch(ts -> ts.getAchievable().contains(ts.getChosenPoints())));
    }

    @Test
    void pickVariant_returnsNullForEmptyAndValueForSingleEntry() {
        // Error path + normal path for random variant selection helper.
        assertNull(HomeGeneratePlanner.pickVariant(List.of()));

        Task task = TaskFixtures.newTask("0001", "Variant Task");
        Subtask subtask = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        Variant single = subtask.getVariants().getFirst();

        Variant picked = HomeGeneratePlanner.pickVariant(List.of(single));

        assertNotNull(picked);
        assertEquals(single.getId(), picked.getId());
    }

    private static TaskSelection selection(String taskId, Eligibility eligibility) {
        Task task = TaskFixtures.newTask(taskId, "Task " + taskId);
        TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, eligibility);
        return new TaskSelection(task);
    }
}
