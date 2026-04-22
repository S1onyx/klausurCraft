package simon.klausurcraft.task;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.testutil.TaskFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskModelBasicsTest {

    @Test
    void difficultyAndEligibility_parsingAndFormattingCoverAllCases() {
        // Covers enum parse/format and default branches.
        assertEquals(Difficulty.EASY, Difficulty.from("easy"));
        assertEquals(Difficulty.MEDIUM, Difficulty.from("medium"));
        assertEquals(Difficulty.HARD, Difficulty.from("hard"));
        assertEquals("easy", Difficulty.EASY.toString());
        assertEquals("medium", Difficulty.MEDIUM.toString());
        assertEquals("hard", Difficulty.HARD.toString());
        assertThrows(IllegalArgumentException.class, () -> Difficulty.from("unknown"));

        assertEquals(Eligibility.EXAM, Eligibility.from("exam"));
        assertEquals(Eligibility.PRACTICE, Eligibility.from("practice"));
        assertEquals(Eligibility.BOTH, Eligibility.from("both"));
        assertEquals("exam", Eligibility.EXAM.toString());
        assertEquals("practice", Eligibility.PRACTICE.toString());
        assertEquals("both", Eligibility.BOTH.toString());
        assertThrows(IllegalArgumentException.class, () -> Eligibility.from("none"));
    }

    @Test
    void taskAndVariant_normalizeNullValuesAndCloneShallow() {
        // Covers null normalization and shallow cloning helpers.
        Task task = TaskFixtures.newTask("0001", "Title");
        task.setTitle(null);
        assertEquals("", task.getTitle());

        Task cloned = task.cloneShallow();
        assertNotSame(task, cloned);
        assertEquals(task.getId(), cloned.getId());
        assertEquals(task.getTitle(), cloned.getTitle());
        assertTrue(cloned.getSubtasks().isEmpty());

        Variant variant = new Variant(task.getDom(), "0001", null, null);
        assertEquals("", variant.getText());
        assertEquals("", variant.getSolution());
        variant.setText(null);
        variant.setSolution(null);
        assertEquals("", variant.getText());
        assertEquals("", variant.getSolution());
    }
}
