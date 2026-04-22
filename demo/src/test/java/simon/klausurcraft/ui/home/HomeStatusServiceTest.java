package simon.klausurcraft.ui.home;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.testutil.TaskFixtures;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeStatusServiceTest {

    @Test
    void computeStatusStats_aggregatesCountsAcrossAllTasks() {
        // Representative sample with mixed difficulty/eligibility and null edge cases.
        Task t1 = TaskFixtures.newTask("0001", "T1");
        TaskFixtures.addSubtask(t1, "0001", "1", Difficulty.EASY, Eligibility.EXAM);
        TaskFixtures.addSubtask(t1, "0002", "2", Difficulty.MEDIUM, Eligibility.BOTH);

        Task t2 = TaskFixtures.newTask("0002", "T2");
        TaskFixtures.addSubtask(t2, "0001", "0.5", Difficulty.HARD, Eligibility.PRACTICE);
        Subtask unknown = TaskFixtures.addSubtask(t2, "0002", "1", Difficulty.EASY, Eligibility.BOTH);
        unknown.setPoints(null);
        unknown.setDifficulty(null);
        unknown.setEligibility(null);

        HomeStatusService.StatusStats stats = HomeStatusService.computeStatusStats(List.of(t1, t2));

        assertEquals(2, stats.tasks);
        assertEquals(4, stats.subtasks);
        assertEquals(4, stats.variants);
        assertEquals("3.5", stats.points.toPlainString());

        assertEquals(1, stats.easy);
        assertEquals(1, stats.medium);
        assertEquals(1, stats.hard);
        assertEquals(1, stats.withoutDifficulty);

        assertEquals(1, stats.exam);
        assertEquals(1, stats.practice);
        assertEquals(1, stats.both);
        assertEquals(1, stats.withoutEligibility);
    }

    @Test
    void computeStatusStats_handlesNullTaskListAsEmpty() {
        // Error path: null list should not crash the status bar logic.
        HomeStatusService.StatusStats stats = HomeStatusService.computeStatusStats(null);

        assertEquals(0, stats.tasks);
        assertEquals(0, stats.subtasks);
        assertEquals("0", stats.points.toPlainString());
    }

    @Test
    void buildStatusTooltip_includesOptionalUnknownSectionsOnlyWhenNeeded() {
        // Check text format rules used for the counts tooltip.
        Task t = TaskFixtures.newTask("0001", "Single");
        TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.BOTH);

        HomeStatusService.StatusStats clean = HomeStatusService.computeStatusStats(List.of(t));
        String cleanTooltip = HomeStatusService.buildStatusTooltip(clean);
        assertFalse(cleanTooltip.contains("ohne Schwierigkeit"));
        assertFalse(cleanTooltip.contains("ohne Eignung"));

        HomeStatusService.StatusStats withUnknown = HomeStatusService.computeStatusStats(List.of(t));
        withUnknown.withoutDifficulty = 2;
        withUnknown.withoutEligibility = 3;
        String unknownTooltip = HomeStatusService.buildStatusTooltip(withUnknown);
        assertTrue(unknownTooltip.contains("ohne Schwierigkeit: 2"));
        assertTrue(unknownTooltip.contains("ohne Eignung: 3"));
    }
}
