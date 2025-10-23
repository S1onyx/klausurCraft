package simon.klausurcraft.ui.home;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.xml.sax.SAXParseException;
import simon.klausurcraft.infrastructure.xml.TaskXmlRepository;
import simon.klausurcraft.ui.support.UiUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class HomeFileController {

    private static final String PREFS_NODE = "simon.klausurcraft";
    private static final String PREF_LAST_FILE = "lastXmlFile";
    private static final String PREF_LAST_DIR  = "lastXmlDir";

    private HomeFileController() {}

    public static void autoLoadLastFile(HomeController root) {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);
        String last = p.get(PREF_LAST_FILE, null);
        if (last != null) {
            File f = new File(last);
            if (f.exists() && f.isFile()) {
                try {
                    loadXmlFile(root, f);
                } catch (Exception ex) {
                    HomeNotifications.showError("Failed to load last file. " + englishXmlError(ex));
                }
            }
        }
    }

    public static void chooseAndLoadXml(HomeController root) {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open tasks XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));

        // Remember last directory
        String lastDir = p.get(PREF_LAST_DIR, null);
        if (lastDir != null) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File f = chooser.showOpenDialog(root.getWindow());
        if (f != null) {
            try {
                loadXmlFile(root, f);
                p.put(PREF_LAST_FILE, f.getAbsolutePath());
                p.put(PREF_LAST_DIR, f.getParentFile().getAbsolutePath());
            } catch (Exception ex) {
                HomeNotifications.showError("Failed to load XML: " + englishXmlError(ex));
            }
        }
    }

    public static void loadXmlFile(HomeController root, File f) throws Exception {
        Path xsd = Path.of(HomeController.class.getResource("/simon/klausurcraft/exam-tasks.xsd").toURI());
        TaskXmlRepository.LoadResult result = root.getTaskRepository().load(f.toPath(), xsd);
        root.getTasks().setAll(result.tasks());
        root.loadedFileNameProperty().set(f.getName());
        HomeNotifications.showInfo("Loaded " + f.getName());
    }

    public static void createNewXml(HomeController root) {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Create tasks XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        chooser.setInitialFileName("tasks.xml");

        String lastDir = p.get(PREF_LAST_DIR, null);
        if (lastDir != null) {
            File dir = new File(lastDir);
            if (dir.exists() && dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File f = chooser.showSaveDialog(root.getWindow());
        if (f == null) {
            return;
        }

        if (f.exists()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Overwrite existing file?");
            confirm.setHeaderText("The file already exists.");
            confirm.setContentText("Do you want to overwrite \"" + f.getName() + "\"?");
            confirm.initOwner(root.getWindow());
            UiUtil.applyCurrentStyles(confirm);

            var decision = confirm.showAndWait();
            if (decision.isEmpty() || decision.get() != ButtonType.OK) {
                return;
            }
        }

        try {
            Path xsd = Path.of(HomeController.class.getResource("/simon/klausurcraft/exam-tasks.xsd").toURI());
            TaskXmlRepository.LoadResult result = root.getTaskRepository().createNew(f.toPath(), xsd);
            root.getTasks().setAll(result.tasks());
            root.loadedFileNameProperty().set(f.getName());
            HomeNotifications.showInfo("Created " + f.getName());

            p.put(PREF_LAST_FILE, f.getAbsolutePath());
            if (f.getParentFile() != null) {
                p.put(PREF_LAST_DIR, f.getParentFile().getAbsolutePath());
            }
        } catch (Exception ex) {
            HomeNotifications.showError("Failed to create XML: " + englishXmlError(ex));
        }
    }

    /** Build a clear, English-only message for XML parse/validation errors. */
    private static String englishXmlError(Exception ex) {
        if (ex instanceof SAXParseException spe) {
            int line = spe.getLineNumber();
            int col  = spe.getColumnNumber();
            return "Invalid XML (line " + line + ", column " + col + "). " +
                   "Please check for unescaped characters (e.g., use &amp; for '&').";
        }
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank())
                ? (ex.getClass().getSimpleName() + " occurred.")
                : msg;
    }
}
