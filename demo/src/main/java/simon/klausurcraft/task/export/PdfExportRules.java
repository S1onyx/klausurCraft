package simon.klausurcraft.task.export;

import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Pure helper rules for file naming, date formatting and point math used by PDF export.
 */
final class PdfExportRules {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private PdfExportRules() {}

    /**
     * Ensures that a chosen export file ends with the {@code .pdf} suffix.
     *
     * @param file selected file from the save dialog
     * @return original file or suffixed variant
     */
    static File ensurePdfExtension(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
        String name = file.getName();
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return file;
        }
        File parent = file.getParentFile();
        if (parent == null) {
            return new File(name + ".pdf");
        }
        return new File(parent, name + ".pdf");
    }

    /**
     * Builds the sibling file path for the sample solution export.
     *
     * @param examFile already normalized exam PDF path
     * @return solution PDF path with {@code _musterloesung.pdf} suffix
     */
    static File buildSolutionFile(File examFile) {
        if (examFile == null) {
            throw new IllegalArgumentException("Exam file must not be null.");
        }
        String name = examFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;

        File parent = examFile.getParentFile();
        if (parent == null) {
            return new File(base + "_musterloesung.pdf");
        }
        return new File(parent, base + "_musterloesung.pdf");
    }

    /**
     * Formats the date label for the PDF cover page.
     *
     * @param date nullable exam date
     * @return formatted date or em dash if no date is set
     */
    static String formatDate(LocalDate date) {
        return date == null ? "\u2014" : date.format(DATE_FORMAT);
    }

    /**
     * Sums points for one assembled task.
     *
     * @param taskAssembly assembled task with chosen subtasks
     * @return total point sum of the task
     */
    static BigDecimal taskPoints(PdfExportService.TaskAssembly taskAssembly) {
        if (taskAssembly == null) {
            throw new IllegalArgumentException("Task assembly must not be null.");
        }
        BigDecimal sum = Points.ZERO;
        for (PdfExportService.ChosenVariant chosenVariant : taskAssembly.chosenSubtasks()) {
            sum = sum.add(chosenVariant.subtask().getPoints());
        }
        return sum;
    }

    /**
     * Sums points over all assembled tasks.
     *
     * @param tasks selected task assemblies
     * @return overall exam point sum
     */
    static BigDecimal totalPoints(List<PdfExportService.TaskAssembly> tasks) {
        if (tasks == null) {
            throw new IllegalArgumentException("Task list must not be null.");
        }
        BigDecimal sum = Points.ZERO;
        for (PdfExportService.TaskAssembly taskAssembly : tasks) {
            sum = sum.add(taskPoints(taskAssembly));
        }
        return sum;
    }

    /**
     * Computes the writing box height for a subtask in exam mode.
     *
     * @param subtask subtask with configured points
     * @return minimum answer box height in PDF points
     */
    static float answerBoxHeight(Subtask subtask) {
        if (subtask == null || subtask.getPoints() == null) {
            throw new IllegalArgumentException("Subtask with points is required.");
        }

        // Min. ~3 lines; grows with points for better writing space.
        double points = subtask.getPoints().doubleValue();
        float base = 48f;
        float perPoint = 12f;
        float height = base + Math.max(0f, (float) (points - 3d)) * perPoint;
        return Math.max(height, base);
    }
}
