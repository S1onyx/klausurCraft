package simon.klausurcraft.controller.home;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import simon.klausurcraft.App;
import simon.klausurcraft.controller.common.SlideOverPane;
import simon.klausurcraft.model.Difficulty;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.services.XmlService;
import simon.klausurcraft.utils.ThemeManager;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class HomeController {

    // Injected child controllers via fx:include
    @FXML private HomeTopbarController topbarController;
    @FXML private HomeSidebarController sidebarController;
    @FXML HomeCenterController centerController; // package-private: intern genutzt

    // Status bar
    @FXML private ToggleButton themeToggle;
    @FXML private Label fileLabel;
    @FXML private Label countsLabel;
    @FXML private Button btnGenerateBottom; // NEU: Generate unten in der Statusleiste

    // Overlay for sheets
    @FXML StackPane rootStack;
    private SlideOverPane slideOver;

    // Models / Services
    private final XmlService xmlService = new XmlService();
    private final ObservableList<TaskModel> tasks = FXCollections.observableArrayList();

    // State / binding
    private final StringProperty loadedFileName = new SimpleStringProperty("No file loaded");
    private final IntegerProperty taskCount = new SimpleIntegerProperty(0);
    private final IntegerProperty subtaskCount = new SimpleIntegerProperty(0);

    // Generate flow state (shared)
    final ObjectProperty<GenerateScope> scope = new SimpleObjectProperty<>(GenerateScope.EXAM);
    final StringProperty examTitle = new SimpleStringProperty("Exam");
    final ObjectProperty<LocalDate> examDate = new SimpleObjectProperty<>(LocalDate.now());
    final BooleanProperty withSampleSolution = new SimpleBooleanProperty(false);

    public XmlService getXmlService() { return xmlService; }
    public ObservableList<TaskModel> getTasks() { return tasks; }
    public SlideOverPane getSlideOver() { return slideOver; }
    public Window getWindow() { return App.getScene().getWindow(); }

    @FXML
    public void initialize() {
        // Overlay
        slideOver = new SlideOverPane();
        rootStack.getChildren().add(slideOver.getContainer());
        rootStack.setMouseTransparent(true);
        slideOver.hideInstant();

        // Bind status bar
        fileLabel.textProperty().bind(loadedFileName);
        countsLabel.textProperty().bind(taskCount.asString().concat(" / ").concat(subtaskCount.asString()));
        themeToggle.setOnAction(e -> ThemeManager.toggle(App.getScene()));

        // Wire sub-controllers
        topbarController.init(this);
        sidebarController.init(this);
        centerController.init(this);

        // Generate unten (Statusleiste)
        if (btnGenerateBottom != null) {
            btnGenerateBottom.setOnAction(e -> HomeGenerateFlow.openStep1(this));
        }

        // React to task list changes
        tasks.addListener((javafx.collections.ListChangeListener<? super TaskModel>) c -> {
            updateCounts();
            sidebarController.rebuildToc(tasks);
            centerController.render(tasks, currentQuery(), allowedDifficulties());
        });

        // Initial auto-load
        Platform.runLater(() -> HomeFileController.autoLoadLastFile(this));
    }

    void updateCounts() {
        int t = tasks.size();
        int s = tasks.stream().mapToInt(task -> task.getSubtasks().size()).sum();
        taskCount.set(t);
        subtaskCount.set(s);
    }

    // Exposed for sub-controllers
    public StringProperty loadedFileNameProperty() { return loadedFileName; }
    public IntegerProperty taskCountProperty() { return taskCount; }
    public IntegerProperty subtaskCountProperty() { return subtaskCount; }

    // === Added: "Switch file…" handler für home.fxml ===
    @FXML
    private void onSwitchFile() {
        HomeFileController.chooseAndLoadXml(this);
    }

    // === NEU: Delegationsgetter, damit keine direkten Feldzugriffe nötig sind ===
    public String currentQuery() {
        return (topbarController != null) ? topbarController.currentQuery() : "";
    }

    public Set<Difficulty> allowedDifficulties() {
        return (topbarController != null) ? topbarController.allowedDifficulties() : EnumSet.allOf(Difficulty.class);
    }
}