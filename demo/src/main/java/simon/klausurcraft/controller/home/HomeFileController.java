package simon.klausurcraft.controller.home;

import javafx.stage.FileChooser;
import org.xml.sax.SAXParseException;
import simon.klausurcraft.services.XmlService;

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
        XmlService.LoadResult result = root.getXmlService().load(f.toPath(), xsd);
        root.getTasks().setAll(result.tasks());
        root.loadedFileNameProperty().set(f.getName());
        HomeNotifications.showInfo("Loaded " + f.getName());
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