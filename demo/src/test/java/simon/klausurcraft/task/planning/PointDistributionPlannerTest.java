package simon.klausurcraft.task.planning;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.testutil.TaskFixtures;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PointDistributionPlannerTest {

    @Test
    void achievablePointSums_supportsHalfPointsAndReturnsSortedDistinctValues() {
        Task t = TaskFixtures.newTask("0001", "Planner");
        TaskFixtures.addSubtask(t, "0001", "0.5", Difficulty.EASY, Eligibility.BOTH);
        TaskFixtures.addSubtask(t, "0002", "1", Difficulty.MEDIUM, Eligibility.BOTH);
        TaskFixtures.addSubtask(t, "0003", "1.5", Difficulty.HARD, Eligibility.BOTH);

        List<String> sums = PointDistributionPlanner.achievablePointSums(t, GenerateScope.BOTH)
                .stream().map(Points::toStorageString).collect(Collectors.toList());

        assertEquals(List.of("0.5", "1", "1.5", "2", "2.5", "3"), sums);
    }

    @Test
    void achievablePointSums_respectsEligibilityScope() {
        Task t = TaskFixtures.newTask("0001", "Eligibility");
        TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.EXAM);
        TaskFixtures.addSubtask(t, "0002", "1.5", Difficulty.MEDIUM, Eligibility.PRACTICE);
        TaskFixtures.addSubtask(t, "0003", "0.5", Difficulty.HARD, Eligibility.BOTH);

        List<String> examSums = PointDistributionPlanner.achievablePointSums(t, GenerateScope.EXAM)
                .stream().map(Points::toStorageString).collect(Collectors.toList());
        List<String> practiceSums = PointDistributionPlanner.achievablePointSums(t, GenerateScope.PRACTICE)
                .stream().map(Points::toStorageString).collect(Collectors.toList());

        assertEquals(List.of("0.5", "1", "1.5"), examSums);
        assertEquals(List.of("0.5", "1.5", "2"), practiceSums);
    }

    @Test
    void pickSubtasksWithDistribution_returnsFeasibleCombinationForReachableTarget() {
        Task t = TaskFixtures.newTask("0001", "Pick");
        Subtask s1 = TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        Subtask s2 = TaskFixtures.addSubtask(t, "0002", "1", Difficulty.MEDIUM, Eligibility.BOTH);
        Subtask s3 = TaskFixtures.addSubtask(t, "0003", "1", Difficulty.HARD, Eligibility.BOTH);

        List<Subtask> picked = PointDistributionPlanner.pickSubtasksWithDistribution(
                List.of(s1, s2, s3), new BigDecimal("3"));

        assertNotNull(picked);
        assertEquals(3, picked.size());
        assertTrue(picked.containsAll(List.of(s1, s2, s3)));
    }

    @Test
    void pickSubtasksWithDistribution_returnsNullForImpossibleTarget() {
        Task t = TaskFixtures.newTask("0001", "Impossible");
        Subtask s1 = TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        Subtask s2 = TaskFixtures.addSubtask(t, "0002", "1.5", Difficulty.MEDIUM, Eligibility.BOTH);

        List<Subtask> picked = PointDistributionPlanner.pickSubtasksWithDistribution(
                List.of(s1, s2), new BigDecimal("10"));

        assertNull(picked);
    }

    @Test
    void pickSubtasksWithDistribution_rejectsNonHalfStepTargets() {
        Task t = TaskFixtures.newTask("0001", "Invalid Target");
        Subtask s1 = TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.BOTH);

        assertThrows(IllegalArgumentException.class, () ->
                PointDistributionPlanner.pickSubtasksWithDistribution(List.of(s1), new BigDecimal("0.25")));
    }

    @Test
    void suggestBestPointSum_prefersHigherPointsWhenDeviationTies() {
        Task t = TaskFixtures.newTask("0001", "Suggest");
        TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        TaskFixtures.addSubtask(t, "0002", "2", Difficulty.EASY, Eligibility.BOTH);

        BigDecimal best = PointDistributionPlanner.suggestBestPointSum(t, GenerateScope.BOTH).orElseThrow();
        assertEquals(new BigDecimal("2"), best);
    }

    @Test
    void suggestBestPointSum_returnsEmptyWhenNoEligibleSubtasks() {
        Task t = TaskFixtures.newTask("0001", "No eligible");
        TaskFixtures.addSubtask(t, "0001", "1", Difficulty.EASY, Eligibility.PRACTICE);

        assertTrue(PointDistributionPlanner.suggestBestPointSum(t, GenerateScope.EXAM).isEmpty());
    }
}
