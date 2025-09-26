package simon.klausurcraft.controller.home;

import javafx.stage.FileChooser;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.services.XmlService;

import java.io.File;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class HomeFileController {

    private static final String PREFS_NODE = "simon.klausurcraft";
    private static final String PREF_LAST_FILE = "lastXmlFile";

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
                    HomeNotifications.showError("Failed to load last file. " + ex.getMessage());
                }
            }
        }
    }

    public static void chooseAndLoadXml(HomeController root) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open tasks XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File f = chooser.showOpenDialog(root.getWindow());
        if (f != null) {
            try {
                loadXmlFile(root, f);
                Preferences.userRoot().node(PREFS_NODE).put(PREF_LAST_FILE, f.getAbsolutePath());
            } catch (Exception ex) {
                HomeNotifications.showError("Failed to load XML: " + ex.getMessage());
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
}