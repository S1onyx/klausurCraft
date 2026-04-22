package simon.klausurcraft.task;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.testutil.TaskFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtaskEligibilityTest {

    @Test
    void isEligibleFor_examScope_matchesAllEligibilityClasses() {
        // Equivalence classes for EXAM scope: exam=true, both=true, practice=false.
        Task task = TaskFixtures.newTask("0001", "Eligibility");
        Subtask examOnly = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.EXAM);
        Subtask practiceOnly = TaskFixtures.addSubtask(task, "0002", "1", Difficulty.EASY, Eligibility.PRACTICE);
        Subtask both = TaskFixtures.addSubtask(task, "0003", "1", Difficulty.EASY, Eligibility.BOTH);

        assertEquals(task, examOnly.getParent());
        assertTrue(examOnly.isEligibleFor(GenerateScope.EXAM));
        assertFalse(practiceOnly.isEligibleFor(GenerateScope.EXAM));
        assertTrue(both.isEligibleFor(GenerateScope.EXAM));
    }

    @Test
    void isEligibleFor_practiceScope_matchesAllEligibilityClasses() {
        // Equivalence classes for PRACTICE scope: practice=true, both=true, exam=false.
        Task task = TaskFixtures.newTask("0001", "Eligibility");
        Subtask examOnly = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.EXAM);
        Subtask practiceOnly = TaskFixtures.addSubtask(task, "0002", "1", Difficulty.EASY, Eligibility.PRACTICE);
        Subtask both = TaskFixtures.addSubtask(task, "0003", "1", Difficulty.EASY, Eligibility.BOTH);

        assertFalse(examOnly.isEligibleFor(GenerateScope.PRACTICE));
        assertTrue(practiceOnly.isEligibleFor(GenerateScope.PRACTICE));
        assertTrue(both.isEligibleFor(GenerateScope.PRACTICE));
    }

    @Test
    void isEligibleFor_rejectsNullScopeAndNullEligibility() {
        // Error/special cases: null scope or null eligibility should be treated as not eligible.
        Task task = TaskFixtures.newTask("0001", "Eligibility");
        Subtask subtask = TaskFixtures.addSubtask(task, "0001", "1", Difficulty.EASY, Eligibility.BOTH);

        assertFalse(subtask.isEligibleFor(null));
        subtask.setEligibility(null);
        assertFalse(subtask.isEligibleFor(GenerateScope.EXAM));
        assertFalse(subtask.isEligibleFor(GenerateScope.PRACTICE));
        assertFalse(subtask.isEligibleFor(GenerateScope.BOTH));
    }
}
