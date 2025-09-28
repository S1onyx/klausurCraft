package simon.klausurcraft.controller.home;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import simon.klausurcraft.App;
import simon.klausurcraft.controller.common.SlideOverPane;
import simon.klausurcraft.model.Difficulty;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.utils.ThemeManager;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class HomeController {

    // Injected child controllers via fx:include
    @FXML private HomeTopbarController topbarController;
    @FXML private HomeSidebarController sidebarController;
    @FXML HomeCenterController centerController; // package-private: internal use

    // Status bar
    @FXML private Button themeToggle;      // icon-only theme toggle (changed from ToggleButton)
    @FXML private Label fileLabel;
    @FXML private Label countsLabel;
    @FXML private Button btnGenerateBottom; // generate button in status bar
    @FXML private Button btnAddTask;        // + Task button in status bar

    // Overlay for sheets
    @FXML StackPane rootStack;
    private SlideOverPane slideOver;

    // Models / Services
    private final simon.klausurcraft.services.XmlService xmlService = new simon.klausurcraft.services.XmlService();
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

    public simon.klausurcraft.services.XmlService getXmlService() { return xmlService; }
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

        // Icon-only theme toggle -> ThemeManager.toggle
        if (themeToggle != null) {
            themeToggle.setOnAction(e -> ThemeManager.toggle(App.getScene()));
            // also keep keyboard accelerator (Ctrl+D) set in App
        }

        // Wire sub-controllers
        topbarController.init(this);
        sidebarController.init(this);
        centerController.init(this);

        // Generate
        if (btnGenerateBottom != null) {
            btnGenerateBottom.setOnAction(e -> HomeGenerateFlow.openStep1(this));
        }

        // + Task button -> prompt & create + open edit
        if (btnAddTask != null) {
            btnAddTask.setOnAction(e -> HomeTaskSheet.promptNewTask(this).ifPresent(newTask -> {
                tasks.add(newTask);
                // open slide-over to edit right away
                HomeTaskSheet.openEdit(this, newTask);
            }));
        }

        // ESC closes sheet
        rootStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.ESCAPE && slideOver.isShown()) {
                        slideOver.hide();
                        rootStack.setMouseTransparent(true);
                        ev.consume();
                    }
                });
            }
        });

        // React to task list changes
        tasks.addListener((javafx.collections.ListChangeListener<? super TaskModel>) c -> {
            updateCounts();
            rebuildToc();
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

    @FXML
    private void onSwitchFile() {
        HomeFileController.chooseAndLoadXml(this);
    }

    public String currentQuery() {
        return (topbarController != null) ? topbarController.currentQuery() : "";
    }

    public Set<Difficulty> allowedDifficulties() {
        return (topbarController != null) ? topbarController.allowedDifficulties() : EnumSet.allOf(Difficulty.class);
    }

    /** Public helper so other controllers don't need access to sidebarController directly. */
    public void rebuildToc() {
        sidebarController.rebuildToc(tasks);
    }
}