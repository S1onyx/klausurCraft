package simon.klausurcraft.ui.home;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.testutil.TaskFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeSearchServiceTest {

    @Test
    void matchesTaskHeader_checksIdAndTitleCaseInsensitive() {
        // Equivalence classes for task search: id hit, title hit, no hit.
        Task task = TaskFixtures.newTask("0007", "Relationale Algebra");

        assertTrue(HomeSearchService.matchesTaskHeader(task, "0007"));
        assertTrue(HomeSearchService.matchesTaskHeader(task, "algebra"));
        assertTrue(HomeSearchService.matchesTaskHeader(task, "ALGEBRA"));
        assertFalse(HomeSearchService.matchesTaskHeader(task, "graphql"));
    }

    @Test
    void matchesSubtask_checksTextSolutionAndGroup() {
        // Sample from render filtering: all searchable fields should be respected.
        Task task = TaskFixtures.newTask("0001", "Query");
        Subtask subtask = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        subtask.getVariants().getFirst().setText("Hash Join erklaeren");
        subtask.getVariants().getFirst().setSolution("Build-Probe Ablauf");

        assertTrue(HomeSearchService.matchesSubtask(subtask, "Join Grundlagen", "hash"));
        assertTrue(HomeSearchService.matchesSubtask(subtask, "Join Grundlagen", "probe"));
        assertTrue(HomeSearchService.matchesSubtask(subtask, "Join Grundlagen", "grundlagen"));
        assertFalse(HomeSearchService.matchesSubtask(subtask, "Join Grundlagen", "b-tree"));
    }

    @Test
    void emptyQueryAlwaysMatchesAndTitleFormatIsStable() {
        // UI behavior: empty query should not filter out entries.
        Task task = TaskFixtures.newTask("0012", "Normalformen");
        Subtask subtask = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.BOTH);

        assertTrue(HomeSearchService.matchesTaskHeader(task, ""));
        assertTrue(HomeSearchService.matchesSubtask(subtask, "NF", ""));
        assertEquals("0012 — Normalformen", HomeSearchService.formatTaskTitle(task));
    }
}
