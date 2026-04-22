package simon.klausurcraft.task.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;
import simon.klausurcraft.testutil.TaskFixtures;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfExportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writeExam_withoutSolutions_createsReadablePdfForExamFlow() throws Exception {
        // Covers exam cover, teacher overview, subtask blocks and answer boxes.
        PdfExportService service = new PdfExportService();
        File out = tempDir.resolve("exam-flow.pdf").toFile();

        List<PdfExportService.TaskAssembly> tasks = List.of(
                assembly(1, "0001", "Databases", "", "", "2"),
                assembly(2, "0002", "Networking", longText(420), "TCP solution", "4")
        );

        invokeWriteExam(service, out, "", null, tasks, 0, false);

        assertTrue(out.exists());
        assertTrue(out.length() > 1500L, "Generated exam PDF should not be empty.");
    }

    @Test
    void writeExam_withSolutions_createsReadableSolutionPdf() throws Exception {
        // Covers solution cover and sample-solution rendering path.
        PdfExportService service = new PdfExportService();
        File out = tempDir.resolve("solution-flow.pdf").toFile();

        List<PdfExportService.TaskAssembly> tasks = List.of(
                assembly(1, "0003", "Algorithms", "What is BFS?", "", "1.5"),
                assembly(2, "0004", "OS", "Explain deadlock.", "Mutual exclusion...", "3")
        );

        invokeWriteExam(service, out, "Exam WS", LocalDate.of(2026, 4, 22), tasks, 90, true);

        assertTrue(out.exists());
        assertTrue(out.length() > 1500L, "Generated solution PDF should not be empty.");
    }

    @Test
    void writeExam_withEmptyTaskList_stillCreatesCoverAndOverview() throws Exception {
        // Covers branch where no task pages are added.
        PdfExportService service = new PdfExportService();
        File out = tempDir.resolve("empty-tasks.pdf").toFile();

        invokeWriteExam(service, out, "Only Cover", LocalDate.of(2026, 4, 22), List.of(), 60, false);

        assertTrue(out.exists());
        assertTrue(out.length() > 800L, "Even with no tasks, cover PDF should be generated.");
    }

    @Test
    void randomQuote_returnsNonBlankValue() throws Exception {
        // Covers hidden helper used for the PDF easter egg quote.
        Method randomQuote = PdfExportService.class.getDeclaredMethod("randomQuote");
        randomQuote.setAccessible(true);

        Object value = randomQuote.invoke(null);

        assertNotNull(value);
        assertFalse(value.toString().isBlank());
    }

    @Test
    void writeExam_largeBlocksTriggerPageBreakBranch() throws Exception {
        // Covers ensureSpaceForSubtask branch that forces a manual page break.
        PdfExportService service = new PdfExportService();
        File out = tempDir.resolve("page-break.pdf").toFile();

        Task task = TaskFixtures.newTask("0099", "Large Blocks");
        List<PdfExportService.ChosenVariant> chosen = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            String id = String.format("%04d", i);
            Subtask subtask = TaskFixtures.addSubtask(task, id, "10", Difficulty.HARD, Eligibility.BOTH);
            Variant variant = subtask.getVariants().getFirst();
            variant.setText(longText(1200));
            variant.setSolution("S" + i);
            chosen.add(new PdfExportService.ChosenVariant(subtask, variant));
        }

        List<PdfExportService.TaskAssembly> tasks = List.of(
                new PdfExportService.TaskAssembly(1, task, chosen)
        );

        invokeWriteExam(service, out, "Big Exam", LocalDate.of(2026, 4, 22), tasks, 120, false);

        assertTrue(out.exists());
        assertTrue(out.length() > 2000L);
    }

    @Test
    void export_returnsFalseWhenDialogIsCancelled() throws Exception {
        // Covers early-return branch when user cancels file selection.
        PdfExportService service = new StubPdfExportService(null);
        boolean ok = service.export(null, "Cancelled", LocalDate.of(2026, 4, 22), List.of(), 45, false);
        assertFalse(ok);
    }

    @Test
    void export_withSolution_createsExamAndSolutionFiles() throws Exception {
        // Covers export flow with extension normalization and solution generation.
        File selectedWithoutExt = tempDir.resolve("exam_export").toFile();
        PdfExportService service = new StubPdfExportService(selectedWithoutExt);

        List<PdfExportService.TaskAssembly> tasks = List.of(
                assembly(1, "0005", "Databases", "Question", "Answer", "2")
        );

        boolean ok = service.export(null, "Export", LocalDate.of(2026, 4, 22), tasks, 60, true);

        File exam = tempDir.resolve("exam_export.pdf").toFile();
        File solution = tempDir.resolve("exam_export_musterloesung.pdf").toFile();
        assertTrue(ok);
        assertTrue(exam.exists());
        assertTrue(solution.exists());
        assertTrue(exam.length() > 1000L);
        assertTrue(solution.length() > 1000L);
    }

    @Test
    void export_withoutSolution_createsOnlyExamFile() throws Exception {
        // Covers branch where no solution PDF is requested.
        File selected = tempDir.resolve("exam_only.pdf").toFile();
        PdfExportService service = new StubPdfExportService(selected);
        List<PdfExportService.TaskAssembly> tasks = List.of(
                assembly(1, "0006", "OS", "Explain paging", "Solution", "1")
        );

        boolean ok = service.export(null, "Export No Solution", LocalDate.of(2026, 4, 22), tasks, 60, false);

        File solution = tempDir.resolve("exam_only_musterloesung.pdf").toFile();
        assertTrue(ok);
        assertTrue(selected.exists());
        assertFalse(solution.exists());
    }

    private static void invokeWriteExam(PdfExportService service,
                                        File out,
                                        String title,
                                        LocalDate date,
                                        List<PdfExportService.TaskAssembly> tasks,
                                        int durationMinutes,
                                        boolean includeSolutions) throws Exception {
        Method writeExam = PdfExportService.class.getDeclaredMethod(
                "writeExam",
                File.class,
                String.class,
                LocalDate.class,
                List.class,
                int.class,
                boolean.class
        );
        writeExam.setAccessible(true);
        writeExam.invoke(service, out, title, date, tasks, durationMinutes, includeSolutions);

        assertTrue(Files.exists(out.toPath()));
    }

    private static PdfExportService.TaskAssembly assembly(int number,
                                                          String taskId,
                                                          String taskTitle,
                                                          String text,
                                                          String solution,
                                                          String points) {
        Task task = TaskFixtures.newTask(taskId, taskTitle);
        Subtask subtask = TaskFixtures.addSubtask(task, "0001", points, Difficulty.MEDIUM, Eligibility.BOTH);
        Variant variant = subtask.getVariants().getFirst();
        variant.setText(text);
        variant.setSolution(solution);

        List<PdfExportService.ChosenVariant> chosen = new ArrayList<>();
        chosen.add(new PdfExportService.ChosenVariant(subtask, variant));
        return new PdfExportService.TaskAssembly(number, task, chosen);
    }

    private static String longText(int minLength) {
        String chunk = "This is a detailed explanation block for page-break testing. ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < minLength) {
            sb.append(chunk);
        }
        return sb.toString();
    }

    private static final class StubPdfExportService extends PdfExportService {
        private final File selected;

        private StubPdfExportService(File selected) {
            this.selected = selected;
        }

        @Override
        protected File chooseSaveTarget(javafx.stage.Window owner, String suggestedName, String lastDir) {
            return selected;
        }
    }
}
