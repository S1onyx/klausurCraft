package simon.klausurcraft.task.export;

import org.junit.jupiter.api.Test;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.testutil.TaskFixtures;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfExportRulesTest {

    @Test
    void ensurePdfExtension_appendsPdfSuffixOnlyWhenMissing() {
        // Equivalence classes: filename without extension vs already valid .pdf name.
        File plain = new File("/tmp/exam");
        File alreadyPdf = new File("/tmp/exam.PDF");
        File noParent = new File("exam_no_parent");

        assertEquals("/tmp/exam.pdf", PdfExportRules.ensurePdfExtension(plain).getPath());
        assertEquals("/tmp/exam.PDF", PdfExportRules.ensurePdfExtension(alreadyPdf).getPath());
        assertEquals("exam_no_parent.pdf", PdfExportRules.ensurePdfExtension(noParent).getPath());
    }

    @Test
    void buildSolutionFile_createsMusterloesungSiblingFile() {
        // Use case: build solution export path next to selected exam file.
        File exam = new File("/tmp/my_exam.pdf");
        File noParentNoExt = new File("my_exam");

        File solution = PdfExportRules.buildSolutionFile(exam);
        File noParentSolution = PdfExportRules.buildSolutionFile(noParentNoExt);

        assertEquals("/tmp/my_exam_musterloesung.pdf", solution.getPath());
        assertEquals("my_exam_musterloesung.pdf", noParentSolution.getPath());
    }

    @Test
    void formatDate_usesGermanDotNotationAndFallbackForNull() {
        // Format rule check for PDF cover information.
        assertEquals("22.04.2026", PdfExportRules.formatDate(LocalDate.of(2026, 4, 22)));
        assertEquals("—", PdfExportRules.formatDate(null));
    }

    @Test
    void taskPointsAndTotalPoints_sumAllSubtasksAccurately() {
        // Sample with mixed whole/half values from two task assemblies.
        PdfExportService.TaskAssembly t1 = assembly("0001", "1", "2.5");
        PdfExportService.TaskAssembly t2 = assembly("0002", "0.5", "3");

        assertEquals("3.5", PdfExportRules.taskPoints(t1).toPlainString());
        assertEquals("3.5", PdfExportRules.taskPoints(t2).toPlainString());
        assertEquals("7.0", PdfExportRules.totalPoints(List.of(t1, t2)).toPlainString());
    }

    @Test
    void answerBoxHeight_respectsMinimumAndGrowsWithPoints() {
        // Special case + normal case: low points keep minimum size, higher points expand the box.
        Subtask low = TaskFixtures.addSubtask(TaskFixtures.newTask("0001", "Low"), "0001", "1", Difficulty.EASY, Eligibility.BOTH);
        Subtask high = TaskFixtures.addSubtask(TaskFixtures.newTask("0002", "High"), "0001", "5", Difficulty.EASY, Eligibility.BOTH);

        assertEquals(48f, PdfExportRules.answerBoxHeight(low), 0.01f);
        assertEquals(72f, PdfExportRules.answerBoxHeight(high), 0.01f);
    }

    @Test
    void helperMethods_throwClearExceptionsForNullInputs() {
        // Error paths for invalid invocation parameters.
        assertThrows(IllegalArgumentException.class, () -> PdfExportRules.ensurePdfExtension(null));
        assertThrows(IllegalArgumentException.class, () -> PdfExportRules.buildSolutionFile(null));
        assertThrows(IllegalArgumentException.class, () -> PdfExportRules.taskPoints(null));
        assertThrows(IllegalArgumentException.class, () -> PdfExportRules.totalPoints(null));
        assertThrows(IllegalArgumentException.class, () -> PdfExportRules.answerBoxHeight(null));
    }

    private static PdfExportService.TaskAssembly assembly(String taskId, String... points) {
        Task task = TaskFixtures.newTask(taskId, "Task " + taskId);
        List<PdfExportService.ChosenVariant> chosen = new ArrayList<>();

        for (int i = 0; i < points.length; i++) {
            String subtaskId = String.format("%04d", i + 1);
            Subtask subtask = TaskFixtures.addSubtask(task, subtaskId, points[i], Difficulty.EASY, Eligibility.BOTH);
            chosen.add(new PdfExportService.ChosenVariant(subtask, subtask.getVariants().getFirst()));
        }

        return new PdfExportService.TaskAssembly(1, task, chosen);
    }
}
