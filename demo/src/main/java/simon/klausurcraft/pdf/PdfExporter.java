package simon.klausurcraft.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.model.VariantModel;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 * Generates two PDFs: exam and optional sample solution.
 * - Heading with title and date
 * - Tasks numbered 1..N; Subtasks a), b), ...
 * - Answer boxes proportional to points (min height)
 * - Solutions inline in solution PDF
 */
public class PdfExporter {

    private static final String PREFS_NODE = "simon.klausurcraft";
    private static final String PREF_LAST_EXPORT_DIR = "lastExportDir";

    public static class TaskAssembly {
        public final int number; // 1..N
        public final TaskModel task;
        public final List<SubtaskModel> chosenSubtasks;

        public TaskAssembly(int number, TaskModel task, List<SubtaskModel> chosenSubtasks) {
            this.number = number;
            this.task = task;
            this.chosenSubtasks = chosenSubtasks;
        }
    }

    public void export(Window owner, String title, LocalDate date,
                       List<TaskAssembly> tasks, boolean withSolution) throws Exception {

        Preferences p = Preferences.userRoot().node(PREFS_NODE);

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose export directory");

        String lastDir = p.get(PREF_LAST_EXPORT_DIR, null);
        if (lastDir != null) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File dir = chooser.showDialog(owner);
        if (dir == null) return;

        // remember chosen directory
        p.put(PREF_LAST_EXPORT_DIR, dir.getAbsolutePath());

        String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
        File examFile = new File(dir, "exam_" + ts + ".pdf");
        writeExam(examFile, title, date, tasks, false);

        if (withSolution) {
            File solFile = new File(dir, "solution_" + ts + ".pdf");
            writeExam(solFile, title + " — Solutions", date, tasks, true);
        }
    }

    private void writeExam(File out, String title, LocalDate date,
                           List<TaskAssembly> tasks, boolean includeSolutions) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(out));
        doc.open();

        // Fonts (larger, clearer task headers)
        Font h1 = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font bold = new Font(Font.HELVETICA, 13, Font.BOLD); // ↑ bigger for task headers

        Paragraph pTitle = new Paragraph(title, h1);
        pTitle.setSpacingAfter(8);
        doc.add(pTitle);

        Paragraph pDate = new Paragraph(date != null ? date.toString() : "", normal);
        pDate.setSpacingAfter(12);
        doc.add(pDate);

        // Content
        for (TaskAssembly ta : tasks) {
            Paragraph taskHeader = new Paragraph("Task " + ta.number + " — " + ta.task.getTitle(), bold);
            taskHeader.setSpacingBefore(10);
            taskHeader.setSpacingAfter(6);
            doc.add(taskHeader);

            AtomicInteger subIndex = new AtomicInteger(0);
            for (SubtaskModel st : ta.chosenSubtasks) {
                char letter = (char) ('a' + subIndex.getAndIncrement());

                // Select exactly one variant: pick first non-empty text, else first
                VariantModel variant = st.getVariants().stream().findFirst().orElse(null);
                String text = (variant != null ? variant.getText() : "").trim();
                if (text.isEmpty()) text = "(no text)";
                String sol = (variant != null ? variant.getSolution() : "").trim();

                Paragraph subHeader = new Paragraph(
                        String.format("%d.%c  (%s pts)",
                                ta.number, letter, st.getPoints().stripTrailingZeros().toPlainString()),
                        new Font(Font.HELVETICA, 11, Font.BOLD));
                subHeader.setSpacingBefore(4);
                subHeader.setSpacingAfter(3);
                doc.add(subHeader);

                Paragraph body = new Paragraph(text, normal);
                body.setSpacingAfter(6);
                doc.add(body);

                if (includeSolutions) {
                    Paragraph s = new Paragraph(sol.isEmpty() ? "(no solution provided)" : sol, normal);
                    s.setSpacingAfter(6);
                    doc.add(s);
                } else {
                    // Answer box: single-cell table with fixed height
                    PdfPTable table = new PdfPTable(1);
                    table.setWidthPercentage(100f);
                    PdfPCell cell = new PdfPCell();
                    cell.setMinimumHeight(answerBoxHeight(st));
                    cell.setBorderWidth(1f);
                    cell.setPhrase(new Phrase(""));
                    table.addCell(cell);
                    table.setSpacingAfter(10);
                    doc.add(table);
                }
            }
        }

        doc.close();
    }

    private float answerBoxHeight(SubtaskModel st) {
        // Minimum ~3 lines at 11pt -> ~48pt, add per point ~11pt
        int pts = st.getPoints().intValue();
        float base = 48f;
        float perPoint = 11f;
        float h = base + Math.max(0, pts - 3) * perPoint;
        return Math.max(h, base);
    }
}