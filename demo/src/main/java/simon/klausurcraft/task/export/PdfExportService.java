package simon.klausurcraft.task.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.awt.Color;

/**
 * Generates two PDFs: exam and optional sample solution.
 */
public class PdfExportService {

    private static final String PREFS_NODE = "simon.klausurcraft";
    private static final String PREF_LAST_EXPORT_DIR = "lastExportDir";

    public static final class ChosenVariant {
        private final Subtask subtask;
        private final Variant variant;

        public ChosenVariant(Subtask subtask, Variant variant) {
            this.subtask = subtask;
            this.variant = variant;
        }

        public Subtask subtask() { return subtask; }
        public Variant variant() { return variant; }
    }

    public static final class TaskAssembly {
        private final int number;
        private final Task task;
        private final List<ChosenVariant> chosenSubtasks;

        public TaskAssembly(int number, Task task, List<ChosenVariant> chosenSubtasks) {
            this.number = number;
            this.task = task;
            this.chosenSubtasks = chosenSubtasks;
        }

        public int number() { return number; }
        public Task task() { return task; }
        public List<ChosenVariant> chosenSubtasks() { return chosenSubtasks; }
    }

    public boolean export(Window owner, String title, LocalDate date,
                          List<TaskAssembly> tasks, int durationMinutes, boolean withSolution) throws Exception {

        Preferences p = Preferences.userRoot().node(PREFS_NODE);

        String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save exam PDF");
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF Files", "*.pdf");
        chooser.getExtensionFilters().add(pdfFilter);
        chooser.setSelectedExtensionFilter(pdfFilter);
        chooser.setInitialFileName("exam_" + ts + ".pdf");

        String lastDir = p.get(PREF_LAST_EXPORT_DIR, null);
        if (lastDir != null) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File selected = chooser.showSaveDialog(owner);
        if (selected == null) return false;
        File examFile = ensurePdfExtension(selected);

        // remember chosen directory
        File parent = examFile.getParentFile();
        if (parent != null && parent.exists() && parent.isDirectory()) {
            p.put(PREF_LAST_EXPORT_DIR, parent.getAbsolutePath());
        }

        writeExam(examFile, title, date, tasks, durationMinutes, false);

        if (withSolution) {
            File solFile = buildSolutionFile(examFile);
            writeExam(solFile, title + " — Musterlösung", date, tasks, durationMinutes, true);
        }
        return true;
    }

    private static File ensurePdfExtension(File file) {
        String name = file.getName();
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return file;
        }
        return new File(file.getParentFile(), name + ".pdf");
    }

    private static File buildSolutionFile(File examFile) {
        String name = examFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return new File(examFile.getParentFile(), base + "_musterloesung.pdf");
    }

    private void writeExam(File out, String title, LocalDate date,
                           List<TaskAssembly> tasks, int durationMinutes, boolean includeSolutions) throws Exception {
        Document doc = new Document(PageSize.A4, 48, 48, 58, 58);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(out));
        writer.setPageEvent(new PageNumberFooter());
        doc.open();

        PdfFonts fonts = new PdfFonts();

        if (includeSolutions) {
            addSolutionCover(doc, title, date, tasks, fonts);
        } else {
            addExamCover(doc, title, date, tasks, durationMinutes, fonts);
            doc.newPage();
            addTeacherOverview(doc, tasks, fonts);
        }

        if (!tasks.isEmpty()) {
            doc.newPage();
            addTaskContent(doc, writer, tasks, includeSolutions, fonts);
        }

        doc.close();
    }

    private void addExamCover(Document doc, String title, LocalDate date, List<TaskAssembly> tasks,
                              int durationMinutes, PdfFonts f) throws Exception {
        BigDecimal totalPoints = totalPoints(tasks);
        int subtaskCount = tasks.stream().mapToInt(t -> t.chosenSubtasks().size()).sum();
        int minutes = Math.max(1, durationMinutes);

        Paragraph header = new Paragraph("Klausur", f.h1);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingBefore(24);
        header.setSpacingAfter(12);
        doc.add(header);

        Paragraph subtitle = new Paragraph(title == null || title.isBlank() ? "Unbenannte Klausur" : title, f.h2);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(18);
        doc.add(subtitle);

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(78f);
        info.setHorizontalAlignment(Element.ALIGN_CENTER);
        info.setWidths(new float[]{2.2f, 3.2f});
        info.setSpacingAfter(20);
        info.addCell(labelCell("Datum", f.smallBold));
        info.addCell(valueCell(date == null ? "—" : date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), f.normal));
        info.addCell(labelCell("Aufgaben", f.smallBold));
        info.addCell(valueCell(tasks.size() + " Teile / " + subtaskCount + " Teilaufgaben", f.normal));
        info.addCell(labelCell("Gesamtpunkte", f.smallBold));
        info.addCell(valueCell(Points.toDisplayString(totalPoints) + " Punkte", f.normal));
        info.addCell(labelCell("Bearbeitungszeit", f.smallBold));
        info.addCell(valueCell(minutes + " Minuten", f.normal));
        doc.add(info);

        Paragraph hintsTitle = new Paragraph("Hinweise", f.h3);
        hintsTitle.setSpacingAfter(6);
        doc.add(hintsTitle);

        com.lowagie.text.List hints = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
        hints.setIndentationLeft(18f);
        hints.add(new ListItem("Antworten bitte klar und strukturiert formulieren.", f.normal));
        hints.add(new ListItem("Zwischenschritte sind willkommen und können Teilpunkte bringen.", f.normal));
        hints.add(new ListItem("Zeit einteilen: erst sichere Punkte, dann Bonus-Knobelstellen.", f.normal));
        doc.add(hints);

        Paragraph egg = new Paragraph(randomQuote(), f.italicMuted);
        egg.setSpacingBefore(16);
        egg.setAlignment(Element.ALIGN_CENTER);
        doc.add(egg);
    }

    private void addSolutionCover(Document doc, String title, LocalDate date, List<TaskAssembly> tasks, PdfFonts f) throws Exception {
        BigDecimal total = totalPoints(tasks);
        Paragraph header = new Paragraph("Musterlösung", f.h1);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingBefore(28);
        header.setSpacingAfter(12);
        doc.add(header);

        Paragraph subtitle = new Paragraph(title == null || title.isBlank() ? "Unbenannte Klausur" : title, f.h2);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(18);
        doc.add(subtitle);

        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(70f);
        info.setHorizontalAlignment(Element.ALIGN_CENTER);
        info.setSpacingAfter(18);
        info.setWidths(new float[]{2f, 3f});
        info.addCell(labelCell("Datum", f.smallBold));
        info.addCell(valueCell(date == null ? "—" : date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), f.normal));
        info.addCell(labelCell("Aufgaben", f.smallBold));
        info.addCell(valueCell(String.valueOf(tasks.size()), f.normal));
        info.addCell(labelCell("Max. Punkte", f.smallBold));
        info.addCell(valueCell(Points.toDisplayString(total), f.normal));
        doc.add(info);

        Paragraph note = new Paragraph("Hinweis: Formulierungen sind Musterantworten; gleichwertige Lösungen sind natürlich korrekt.", f.italicMuted);
        note.setAlignment(Element.ALIGN_CENTER);
        doc.add(note);
    }

    private void addTeacherOverview(Document doc, List<TaskAssembly> tasks, PdfFonts f) throws Exception {
        Paragraph title = new Paragraph("Korrekturübersicht", f.h2);
        title.setSpacingBefore(4);
        title.setSpacingAfter(8);
        doc.add(title);

        Paragraph sub = new Paragraph("Eintragen der erreichten Punkte pro Aufgabe", f.smallMuted);
        sub.setSpacingAfter(12);
        doc.add(sub);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1.6f, 1.3f, 1.3f, 3.2f});
        table.setSpacingAfter(14);

        table.addCell(headerCell("Aufgabe", f.smallBold));
        table.addCell(headerCell("Max", f.smallBold));
        table.addCell(headerCell("Erreicht", f.smallBold));
        table.addCell(headerCell("Bemerkung", f.smallBold));

        BigDecimal total = BigDecimal.ZERO;
        for (TaskAssembly ta : tasks) {
            BigDecimal max = taskPoints(ta);
            total = total.add(max);

            table.addCell(bodyCell("Aufgabe " + ta.number(), f.normal));
            table.addCell(bodyCell(Points.toDisplayString(max), f.normal));
            table.addCell(emptyWriteCell(" "));
            table.addCell(emptyWriteCell(" "));
        }

        table.addCell(totalCell("Gesamt", f.smallBold));
        table.addCell(totalCell(Points.toDisplayString(total), f.smallBold));
        table.addCell(totalCell(" ", f.smallBold));
        table.addCell(totalCell(" ", f.smallBold));
        doc.add(table);

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(100f);
        summary.setWidths(new float[]{1f, 1f});
        summary.addCell(valueCell("Gesamtpunkte: ____ / " + Points.toDisplayString(total), f.normal));
        summary.addCell(valueCell("Gesamtbewertung / Note: ____________", f.normal));
        summary.addCell(valueCell("Korrigiert von: ____________________", f.normal));
        summary.addCell(valueCell("Datum / Unterschrift: _____________", f.normal));
        doc.add(summary);
    }

    private void addTaskContent(Document doc, PdfWriter writer, List<TaskAssembly> tasks, boolean includeSolutions, PdfFonts f) throws Exception {
        for (TaskAssembly ta : tasks) {
            if (ta.number() > 1) {
                doc.newPage();
            }

            BigDecimal max = taskPoints(ta);
            Paragraph taskHeader = new Paragraph("Aufgabe " + ta.number() + " — " + ta.task().getTitle(), f.h2);
            taskHeader.setSpacingAfter(2);
            doc.add(taskHeader);

            Paragraph meta = new Paragraph("Maximal: " + Points.toDisplayString(max) + " Punkte", f.smallMuted);
            meta.setSpacingAfter(10);
            doc.add(meta);

            AtomicInteger subIndex = new AtomicInteger(0);
            for (ChosenVariant entry : ta.chosenSubtasks()) {
                addSubtaskBlock(doc, writer, ta.number(), subIndex.getAndIncrement(), entry, includeSolutions, f);
            }
        }
    }

    private void addSubtaskBlock(Document doc, PdfWriter writer, int taskNumber, int idx, ChosenVariant entry,
                                 boolean includeSolutions, PdfFonts f) throws Exception {
        Subtask st = entry.subtask();
        Variant variant = entry.variant();
        char letter = (char) ('a' + idx);

        String text = (variant != null ? variant.getText() : "").trim();
        if (text.isEmpty()) text = "(Kein Aufgabentext hinterlegt)";
        String sol = (variant != null ? variant.getSolution() : "").trim();
        ensureSpaceForSubtask(doc, writer, st, text, includeSolutions);

        Paragraph subHeader = new Paragraph(
                String.format("%d.%c  (%s Punkte)", taskNumber, letter, Points.toDisplayString(st.getPoints())),
                f.subHeader);
        subHeader.setSpacingBefore(4);
        subHeader.setSpacingAfter(4);
        doc.add(subHeader);

        Paragraph body = new Paragraph(text, f.normal);
        body.setSpacingAfter(6);
        doc.add(body);

        if (includeSolutions) {
            PdfPTable solBox = new PdfPTable(1);
            solBox.setWidthPercentage(100f);
            PdfPCell c = new PdfPCell();
            c.setPadding(8f);
            c.setBorderWidth(0.8f);
            c.setBorderColor(new Color(150, 160, 175));
            c.setBackgroundColor(new Color(245, 247, 250));

            Phrase label = new Phrase("Musterlösung: ", f.smallBold);
            Phrase txt = new Phrase(sol.isEmpty() ? "Keine Lösung hinterlegt." : sol, f.normal);
            Paragraph p = new Paragraph();
            p.add(label);
            p.add(txt);
            c.setPhrase(p);
            solBox.addCell(c);
            solBox.setSpacingAfter(10f);
            doc.add(solBox);
        } else {
            PdfPTable answerBox = new PdfPTable(1);
            answerBox.setWidthPercentage(100f);
            PdfPCell cell = new PdfPCell(new Phrase(" "));
            cell.setPadding(6f);
            cell.setMinimumHeight(answerBoxHeight(st));
            cell.setBorderWidth(1f);
            cell.setBorderColor(new Color(165, 175, 190));
            answerBox.addCell(cell);
            answerBox.setSpacingAfter(11f);
            doc.add(answerBox);
        }
    }

    private float answerBoxHeight(Subtask st) {
        // Min. ~3 lines; grows with points for better writing space.
        double pts = st.getPoints().doubleValue();
        float base = 48f;
        float perPoint = 12f;
        float h = base + Math.max(0f, (float) (pts - 3d)) * perPoint;
        return Math.max(h, base);
    }

    private void ensureSpaceForSubtask(Document doc, PdfWriter writer, Subtask st, String text, boolean includeSolutions) {
        int approxLines = Math.max(1, (text.length() / 90) + 1);
        float textHeight = approxLines * 13f;
        float base = 42f + textHeight; // sub header + spacing + question text
        float needed = base + (includeSolutions ? 72f : answerBoxHeight(st) + 12f);

        float y = writer.getVerticalPosition(true);
        float minY = doc.bottom() + 16f;
        if (y - needed < minY) {
            doc.newPage();
        }
    }

    private static BigDecimal taskPoints(TaskAssembly ta) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ChosenVariant cv : ta.chosenSubtasks()) {
            sum = sum.add(cv.subtask().getPoints());
        }
        return sum;
    }

    private static BigDecimal totalPoints(List<TaskAssembly> tasks) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TaskAssembly ta : tasks) {
            sum = sum.add(taskPoints(ta));
        }
        return sum;
    }

    private static String randomQuote() {
        String[] quotes = {
            "Kaffee ist optional, klare Gedanken sind Pflicht.",
            "Wenn ein Ansatz nicht klappt, starte mit einer kleineren Teilfrage.",
            "Saubere Struktur gewinnt oft gegen lange Romane.",
            "Erst sichere Punkte, dann mutige Optimierungen.",
            "Eine gute Skizze spart fünf Minuten Grübeln.",
            "Fehler sind erlaubt, unkorrigierte Fehler eher nicht.",
            "Rechnen, prüfen, erst dann eintragen.",
            "Ein sauberer Zwischenschritt ist nie Zeitverschwendung.",
            "Wer Begriffe sauber definiert, sammelt leise Punkte.",
            "Halbe Idee aufschreiben kann halbe Punkte retten.",
            "Nicht raten, herleiten.",
            "Kurze Antwort, starke Begründung.",
            "Wenn es komplex wird: Problem in Blöcke schneiden.",
            "Einheiten und Größenordnungen sind gute Freunde.",
            "Präzision schlägt Geschwindigkeit, wenn beides nicht geht.",
            "Sauberer Ansatz > hektischer Endspurt.",
            "Zwischenergebnisse markieren hilft beim Kontrollblick.",
            "Ein Blick auf Randfälle kostet Sekunden, spart Punkte.",
            "Klarheit ist die schnellste Abkürzung.",
            "Denke wie ein Debugger: Schritt für Schritt.",
            "Unvollständig aber korrekt ist besser als voll und falsch.",
            "Begründe Annahmen kurz, dann weiter.",
            "Wenn du festhängst, wechsle kurz die Aufgabe.",
            "Ruhig bleiben ist ein legitimer Lösungsweg.",
            "Formel ohne Kontext ist nur Dekoration.",
            "Ein Beispiel kann eine Erklärung retten.",
            "Nicht alles auf einmal, aber alles nacheinander.",
            "Heute gewinnt die saubere Handschrift extra Sympathiepunkte.",
            "Punkte mögen Ordnung.",
            "Wer sauber trennt, versteht schneller.",
            "Kleine Checks verhindern große Korrekturen.",
            "Die erste Idee ist ein Start, nicht immer das Ziel.",
            "Konsequent benannte Variablen sind halbe Miete.",
            "Wenn etwas zu schön ist, nochmal gegenprüfen.",
            "Lieber eine Zeile weniger, aber korrekt.",
            "Zeitmanagement ist auch eine Fachkompetenz.",
            "Mut zur Lücke, aber nicht zur Logiklücke.",
            "Das Blatt weiß nichts ohne deine Zwischenschritte.",
            "Wenn es passt, erkläre es in einem Satz.",
            "Richtung prüfen, dann rechnen.",
            "Ein guter Anfang stabilisiert den Rest.",
            "Ein sauberer Schluss sichert den Punkt.",
            "Nicht nur Ergebnis, auch Weg zählt.",
            "Komplexität sinkt mit jeder klaren Notation.",
            "Fragen lesen, Schlüsselwort markieren, starten.",
            "Kleine Pause, großer Fokus.",
            "Richtig ist besser als spektakulär.",
            "Konzentration schlägt Panikmodus.",
            "Gut gegliedert ist halb korrigiert.",
            "Das hier ist kein Sprint, eher ein präziser Lauf."
        };
        return quotes[ThreadLocalRandom.current().nextInt(quotes.length)];
    }

    private static PdfPCell labelCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(7f);
        c.setBackgroundColor(new Color(236, 241, 248));
        c.setBorderColor(new Color(185, 195, 210));
        return c;
    }

    private static PdfPCell valueCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(7f);
        c.setBorderColor(new Color(185, 195, 210));
        c.setBackgroundColor(Color.WHITE);
        return c;
    }

    private static PdfPCell headerCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(7f);
        c.setBorderColor(new Color(170, 180, 195));
        c.setBackgroundColor(new Color(235, 240, 247));
        return c;
    }

    private static PdfPCell bodyCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(7f);
        c.setBorderColor(new Color(186, 194, 207));
        return c;
    }

    private static PdfPCell emptyWriteCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text));
        c.setPadding(9f);
        c.setMinimumHeight(24f);
        c.setBorderColor(new Color(186, 194, 207));
        return c;
    }

    private static PdfPCell totalCell(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setPadding(8f);
        c.setBorderColor(new Color(150, 162, 180));
        c.setBackgroundColor(new Color(245, 247, 251));
        return c;
    }

    private static final class PdfFonts {
        final Font h1 = new Font(Font.HELVETICA, 26, Font.BOLD);
        final Font h2 = new Font(Font.HELVETICA, 16, Font.BOLD);
        final Font h3 = new Font(Font.HELVETICA, 13, Font.BOLD);
        final Font subHeader = new Font(Font.HELVETICA, 11, Font.BOLD);
        final Font normal = new Font(Font.HELVETICA, 11, Font.NORMAL);
        final Font smallBold = new Font(Font.HELVETICA, 10, Font.BOLD);
        final Font smallMuted = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(90, 100, 115));
        final Font italicMuted = new Font(Font.HELVETICA, 10, Font.ITALIC, new Color(95, 104, 118));
    }

    private static final class PageNumberFooter extends PdfPageEventHelper {
        private PdfTemplate total;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            total = writer.getDirectContent().createTemplate(40, 12);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize page footer font", e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String text = "Seite " + writer.getPageNumber() + "/";
            float size = 9f;
            float textWidth = baseFont.getWidthPoint(text, size);
            float x = (document.left() + document.right()) / 2f;
            float y = 22f;

            PdfContentByte cb = writer.getDirectContent();
            cb.beginText();
            cb.setFontAndSize(baseFont, size);
            cb.setTextMatrix(x - textWidth / 2f, y);
            cb.showText(text);
            cb.endText();
            cb.addTemplate(total, x - textWidth / 2f + textWidth, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            total.beginText();
            total.setFontAndSize(baseFont, 9f);
            total.setTextMatrix(0, 0);
            total.showText(String.valueOf(writer.getPageNumber() - 1));
            total.endText();
        }
    }
}
