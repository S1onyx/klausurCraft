package simon.klausurcraft.ui.home;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import simon.klausurcraft.app.KlausurCraftApp;
import simon.klausurcraft.ui.components.SlideOverPane;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.io.TaskXmlStore;
import simon.klausurcraft.ui.ThemeService;
import simon.klausurcraft.ui.UiStyles;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

public class HomeController {

    // Injected child controllers via fx:include
    @FXML private HomeTopbarController topbarController;
    @FXML private HomeSidebarController sidebarController;
    @FXML HomeCenterController centerController; // package-private: internal use

    // Status bar
    @FXML private Button themeToggle;      // icon-only theme toggle (changed from ToggleButton)
    @FXML private Button btnInfo;
    @FXML private Label fileLabel;
    @FXML private Label countsLabel;
    @FXML private Button btnGenerateBottom; // generate button in status bar
    @FXML private Button btnAddTask;        // + Task button in status bar

    // Overlay for sheets
    @FXML StackPane rootStack;
    private SlideOverPane slideOver;

    // Models / Services
    private final TaskXmlStore taskRepository = new TaskXmlStore();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    // State / binding
    private final StringProperty loadedFileName = new SimpleStringProperty("No file loaded");
    private final IntegerProperty taskCount = new SimpleIntegerProperty(0);
    private final IntegerProperty subtaskCount = new SimpleIntegerProperty(0);
    private final Tooltip countsTooltip = new Tooltip();

    // Generate flow state (shared)
    final ObjectProperty<GenerateScope> scope = new SimpleObjectProperty<>(GenerateScope.EXAM);
    final StringProperty examTitle = new SimpleStringProperty("Exam");
    final ObjectProperty<LocalDate> examDate = new SimpleObjectProperty<>(LocalDate.now());
    final IntegerProperty examDurationMinutes = new SimpleIntegerProperty(90);
    final BooleanProperty withSampleSolution = new SimpleBooleanProperty(true);

    public TaskXmlStore getTaskRepository() { return taskRepository; }
    public ObservableList<Task> getTasks() { return tasks; }
    public SlideOverPane getSlideOver() { return slideOver; }
    public Window getWindow() { return KlausurCraftApp.getScene().getWindow(); }

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
        countsTooltip.setWrapText(true);
        countsTooltip.setMaxWidth(360);
        countsLabel.setTooltip(countsTooltip);
        countsLabel.setAccessibleText("Hover for detailed statistics");
        countsLabel.setOnMouseEntered(e -> updateCounts());
        updateCounts();

        // Icon-only theme toggle -> ThemeService.toggle
        if (themeToggle != null) {
            themeToggle.setOnAction(e -> ThemeService.toggle(KlausurCraftApp.getScene()));
            // also keep keyboard accelerator (Ctrl+D) set in KlausurCraftApp
        }
        if (btnInfo != null) {
            btnInfo.setOnAction(e -> openInfoDialog());
        }

        // Wire sub-controllers
        topbarController.init(this);
        sidebarController.init(this);
        centerController.init(this);

        // Generate
        if (btnGenerateBottom != null) {
            btnGenerateBottom.setTooltip(new Tooltip("Open exam generation wizard."));
            btnGenerateBottom.setOnAction(e -> HomeGenerateFlow.openStep1(this));
        }

        // + Task button -> prompt & create + open edit
        if (btnAddTask != null) {
            btnAddTask.setTooltip(new Tooltip("Create a new task and open its editor."));
            btnAddTask.setOnAction(e -> HomeTaskSheet.promptNewTask(this).ifPresent(newTask -> {
                tasks.add(newTask);
                // open slide-over to edit right away
                HomeTaskSheet.openEdit(this, newTask);
            }));
        }

        // ESC closes sheet
        rootStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installGlobalKeybindings(newScene);
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.ESCAPE && slideOver.isShown()) {
                        slideOver.hide();
                        rootStack.setMouseTransparent(true);
                        ev.consume();
                    }
                });

                // Close open slide-over when clicking anywhere outside the sheet.
                newScene.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                    if (!slideOver.isShown()) return;
                    if (ev.getTarget() instanceof javafx.scene.Node n && !slideOver.isInSheet(n)) {
                        slideOver.hide();
                        rootStack.setMouseTransparent(true);
                    }
                });
            }
        });

        // React to task list changes
        tasks.addListener((javafx.collections.ListChangeListener<? super Task>) c -> {
            updateCounts();
            rebuildToc();
            centerController.render(tasks, currentQuery(), allowedDifficulties());
        });

        // Initial auto-load
        Platform.runLater(() -> HomeFileController.autoLoadLastFile(this));
    }

    void updateCounts() {
        StatusStats stats = computeStatusStats();
        taskCount.set(stats.tasks);
        subtaskCount.set(stats.subtasks);
        countsTooltip.setText(buildStatusTooltip(stats));
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

    private StatusStats computeStatusStats() {
        StatusStats stats = new StatusStats();
        stats.tasks = tasks.size();

        for (Task task : tasks) {
            for (Subtask subtask : task.getSubtasks()) {
                stats.subtasks++;
                stats.variants += subtask.getVariants().size();
                if (subtask.getPoints() != null) {
                    stats.points = stats.points.add(subtask.getPoints());
                }
                switch (subtask.getDifficulty()) {
                    case EASY -> stats.easy++;
                    case MEDIUM -> stats.medium++;
                    case HARD -> stats.hard++;
                    case null -> stats.withoutDifficulty++;
                }
                switch (subtask.getEligibility()) {
                    case EXAM -> stats.exam++;
                    case PRACTICE -> stats.practice++;
                    case BOTH -> stats.both++;
                    case null -> stats.withoutEligibility++;
                }
            }
        }

        return stats;
    }

    private String buildStatusTooltip(StatusStats s) {
        return "Unten angezeigt: Tasks / Subtasks\n\n"
                + "Tasks: " + s.tasks + "\n"
                + "Subtasks: " + s.subtasks + "\n"
                + "Varianten: " + s.variants + "\n"
                + "Punkte gesamt: " + Points.toDisplayString(s.points) + "\n\n"
                + "Schwierigkeit (Subtasks):\n"
                + "easy: " + s.easy + "\n"
                + "medium: " + s.medium + "\n"
                + "hard: " + s.hard
                + (s.withoutDifficulty > 0 ? "\nohne Schwierigkeit: " + s.withoutDifficulty : "")
                + "\n\n"
                + "Eignung (Subtasks):\n"
                + "exam: " + s.exam + "\n"
                + "practice: " + s.practice + "\n"
                + "both: " + s.both
                + (s.withoutEligibility > 0 ? "\nohne Eignung: " + s.withoutEligibility : "");
    }

    private static final class StatusStats {
        int tasks;
        int subtasks;
        int variants;
        BigDecimal points = Points.ZERO;

        int easy;
        int medium;
        int hard;
        int withoutDifficulty;

        int exam;
        int practice;
        int both;
        int withoutEligibility;
    }

    private void installGlobalKeybindings(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> HomeFileController.createNewXml(this)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                () -> HomeFileController.chooseAndLoadXml(this)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                () -> {
                    if (topbarController != null) {
                        topbarController.focusSearch();
                    }
                }
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                () -> HomeGenerateFlow.openStep1(this)
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN),
                this::openInfoDialog
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F1),
                this::openInfoDialog
        );
    }

    private void openInfoDialog() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("About klausurCraft");
        info.setHeaderText(null);
        info.setGraphic(null);

        VBox root = new VBox(12);
        root.getStyleClass().add("info-dialog-root");

        HBox head = new HBox(10);
        Label title = new Label("klausurCraft");
        title.getStyleClass().add("info-dialog-title");
        Label versionChip = new Label("v" + resolveAppVersion());
        versionChip.getStyleClass().addAll("badge", "kbd-chip");
        Region headSpacer = new Region();
        HBox.setHgrow(headSpacer, Priority.ALWAYS);
        head.getChildren().addAll(title, headSpacer, versionChip);

        GridPane meta = new GridPane();
        meta.getStyleClass().add("info-dialog-meta");
        meta.setHgap(10);
        meta.setVgap(6);
        addInfoMetaRow(meta, 0, "Developer", "Simon Riedinger");
        addInfoMetaRow(meta, 1, "Stack", "Java 25, JavaFX, OpenPDF");

        Label shortcutsTitle = new Label("Keybindings");
        shortcutsTitle.getStyleClass().add("info-dialog-section-title");

        GridPane shortcuts = new GridPane();
        shortcuts.getStyleClass().add("info-dialog-shortcuts");
        shortcuts.setHgap(10);
        shortcuts.setVgap(8);
        addShortcutRow(shortcuts, 0, "Cmd/Ctrl+N", "New XML");
        addShortcutRow(shortcuts, 1, "Cmd/Ctrl+O", "Load XML");
        addShortcutRow(shortcuts, 2, "Cmd/Ctrl+F", "Focus search");
        addShortcutRow(shortcuts, 3, "Cmd/Ctrl+G", "Generate exam");
        addShortcutRow(shortcuts, 4, "Cmd/Ctrl+D", "Toggle theme");
        addShortcutRow(shortcuts, 5, "Cmd/Ctrl+I", "Open info");
        addShortcutRow(shortcuts, 6, "F1", "Open info");
        addShortcutRow(shortcuts, 7, "Delete", "Delete selected item in left tree");
        addShortcutRow(shortcuts, 8, "Esc", "Close open side sheet");
        addShortcutRow(shortcuts, 9, "Space", "In Generate dialog: move selected task between lists");

        root.getChildren().addAll(head, meta, shortcutsTitle, shortcuts);
        info.getDialogPane().setContent(root);
        info.getDialogPane().setPrefWidth(560);
        info.initOwner(getWindow());
        UiStyles.applyCurrentStyles(info);
        info.showAndWait();
    }

    private void addInfoMetaRow(GridPane grid, int row, String key, String value) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("info-dialog-value");
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private void addShortcutRow(GridPane grid, int row, String key, String action) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().addAll("badge", "kbd-chip");
        Label actionLabel = new Label(action);
        actionLabel.getStyleClass().add("info-dialog-value");
        grid.add(keyLabel, 0, row);
        grid.add(actionLabel, 1, row);
    }

    private static String resolveAppVersion() {
        String v = HomeController.class.getPackage().getImplementationVersion();
        if (v != null && !v.isBlank()) return v;

        try (InputStream in = HomeController.class.getResourceAsStream("/META-INF/maven/simon.klausurcraft/demo/pom.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String fromPom = p.getProperty("version");
                if (fromPom != null && !fromPom.isBlank()) return fromPom.trim();
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return "dev";
    }
}
