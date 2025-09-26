package simon.klausurcraft.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import simon.klausurcraft.App;
import simon.klausurcraft.ThemeManager;
import simon.klausurcraft.model.*;
import simon.klausurcraft.pdf.PdfExporter;
import simon.klausurcraft.services.XmlService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Main screen controller.
 */
public class HomeController {

    private static final String PREFS_NODE = "simon.klausurcraft";
    private static final String PREF_LAST_FILE = "lastXmlFile";

    private final XmlService xmlService = new XmlService();
    private final ObservableList<TaskModel> tasks = FXCollections.observableArrayList();

    private final StringProperty loadedFileName = new SimpleStringProperty("No file loaded");
    private final IntegerProperty taskCount = new SimpleIntegerProperty(0);
    private final IntegerProperty subtaskCount = new SimpleIntegerProperty(0);

    // Search/filter
    @FXML private TextField searchField;
    @FXML private CheckBox fltEasy;
    @FXML private CheckBox fltMedium;
    @FXML private CheckBox fltHard;

    // Main panes
    @FXML private TreeView<String> tocTree;
    @FXML private ScrollPane centerScroll;
    @FXML private VBox centerContainer;

    // Status bar
    @FXML private Label fileLabel;
    @FXML private Label countsLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private Button btnSwitchFile;
    @FXML private Button btnGenerate;

    // Overlay for slide-over
    @FXML private StackPane rootStack;
    private SlideOverPane slideOver;

    // Generate flow
    private final ObjectProperty<GenerateScope> scope = new SimpleObjectProperty<>(GenerateScope.EXAM);
    private final ObservableList<TaskSelection> selectedTasks = FXCollections.observableArrayList();
    private final StringProperty examTitle = new SimpleStringProperty("Exam");
    private final ObjectProperty<LocalDate> examDate = new SimpleObjectProperty<>(LocalDate.now());
    private final BooleanProperty withSampleSolution = new SimpleBooleanProperty(false);

    private List<TaskModel> currentFiltered = new ArrayList<>();

    @FXML
    public void initialize() {
        // Build slide-over into overlay stack
        slideOver = new SlideOverPane();
        rootStack.getChildren().add(slideOver.getContainer());
        StackPane.setAlignment(slideOver.getContainer(), javafx.geometry.Pos.CENTER_RIGHT);

        // overlay must not block clicks when no sheet is shown
        rootStack.setMouseTransparent(true);
        rootStack.setPickOnBounds(false);
        slideOver.hideInstant();

        // Bind status bar
        fileLabel.textProperty().bind(loadedFileName);
        countsLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Tasks: %d   Subtasks: %d", taskCount.get(), subtaskCount.get()),
                taskCount, subtaskCount));

        themeToggle.setOnAction(e -> ThemeManager.toggle(App.getScene()));

        tasks.addListener((ListChangeListener<TaskModel>) c -> {
            updateCounts();
            rebuildToc();
            renderCenter();
        });

        searchField.textProperty().addListener((obs, o, n) -> renderCenter());
        fltEasy.selectedProperty().addListener((obs, o, n) -> renderCenter());
        fltMedium.selectedProperty().addListener((obs, o, n) -> renderCenter());
        fltHard.selectedProperty().addListener((obs, o, n) -> renderCenter());

        Platform.runLater(this::autoLoadLastFile);
    }

    // ===== File handling =====

    @FXML
    private void onClickLoadXml(ActionEvent e) {
        chooseAndLoadXml(getWindow());
    }

    @FXML
    private void onClickGenerate(ActionEvent e) {
        openGenerateFlow();
    }

    private Window getWindow() {
        return App.getScene().getWindow();
    }

    private void autoLoadLastFile() {
        Preferences p = Preferences.userRoot().node(PREFS_NODE);
        String last = p.get(PREF_LAST_FILE, null);
        if (last != null) {
            File f = new File(last);
            if (f.exists() && f.isFile()) {
                try {
                    loadXmlFile(f);
                } catch (Exception ex) {
                    showError("Failed to load last file. " + ex.getMessage());
                }
            }
        }
    }

    private void chooseAndLoadXml(Window w) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open tasks XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File f = chooser.showOpenDialog(w);
        if (f != null) {
            try {
                loadXmlFile(f);
                Preferences.userRoot().node(PREFS_NODE).put(PREF_LAST_FILE, f.getAbsolutePath());
            } catch (Exception ex) {
                showError("Failed to load XML: " + ex.getMessage());
            }
        }
    }

    private void loadXmlFile(File f) throws Exception {
        Path xsd = Path.of(App.class.getResource("exam-tasks.xsd").toURI());
        XmlService.LoadResult result = xmlService.load(f.toPath(), xsd);

        tasks.setAll(result.tasks());

        loadedFileName.set(f.getName());
        updateCounts();
        rebuildToc();
        renderCenter();

        showInfo("Loaded " + f.getName());
    }

    private void updateCounts() {
        int t = tasks.size();
        int s = tasks.stream().mapToInt(task -> task.getSubtasks().size()).sum();
        taskCount.set(t);
        subtaskCount.set(s);
    }

    // ===== TOC =====

    private void rebuildToc() {
        TreeItem<String> root = new TreeItem<>("Contents");
        root.setExpanded(true);

        for (TaskModel t : tasks) {
            TreeItem<String> taskNode = new TreeItem<>(formatTaskTitle(t));
            for (SubtaskModel st : t.getSubtasks()) {
                taskNode.getChildren().add(new TreeItem<>("• " + formatSubtaskTitle(t, st)));
            }
            root.getChildren().add(taskNode);
        }

        tocTree.setRoot(root);
        tocTree.setShowRoot(false);

        tocTree.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                TreeItem<String> sel = tocTree.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    scrollToItem(sel.getValue());
                }
            }
        });
    }

    private String formatTaskTitle(TaskModel t) {
        return String.format("%s — %s", t.getId(), t.getTitle());
    }

    private String formatSubtaskTitle(TaskModel t, SubtaskModel st) {
        return String.format("%s.%s  (%s pts, %s, %s)",
                t.getId(), st.getId(), st.getPoints().stripTrailingZeros().toPlainString(),
                st.getDifficulty(), st.getEligibility());
    }

    private void scrollToItem(String label) {
        for (Node n : centerContainer.getChildren()) {
            if (n.getUserData() instanceof String s && label.contains(s)) {
                n.requestFocus();
                centerScroll.setVvalue(n.getLayoutY() / Math.max(1, centerContainer.getHeight()));
                break;
            }
        }
    }

    // ===== Center rendering =====

    private void renderCenter() {
        centerContainer.getChildren().clear();

        String q = Optional.ofNullable(searchField.getText()).orElse("").trim().toLowerCase();
        Set<Difficulty> allowed = new HashSet<>();
        if (fltEasy.isSelected() || fltMedium.isSelected() || fltHard.isSelected()) {
            if (fltEasy.isSelected()) allowed.add(Difficulty.EASY);
            if (fltMedium.isSelected()) allowed.add(Difficulty.MEDIUM);
            if (fltHard.isSelected()) allowed.add(Difficulty.HARD);
        } else {
            allowed.addAll(Arrays.asList(Difficulty.values()));
        }

        currentFiltered = tasks.stream().map(TaskModel::cloneShallow).collect(Collectors.toList());

        for (TaskModel t : tasks) {
            boolean taskMatches = matchesTask(t, q);

            VBox taskCard = makeCard();
            taskCard.setUserData(formatTaskTitle(t));

            Label header = new Label("Task " + t.getId() + " — " + t.getTitle());
            header.getStyleClass().add("header");
            taskCard.getChildren().add(header);

            for (SubtaskModel st : t.getSubtasks()) {
                if (!allowed.contains(st.getDifficulty())) continue;
                boolean subMatches = taskMatches || matchesSubtask(st, q);
                if (!subMatches && !q.isEmpty()) continue;

                HBox row = new HBox(10);
                row.setPadding(new Insets(6, 0, 6, 0));

                Label lblTitle = new Label(String.format("Subtask %s.%s", t.getId(), st.getId()));
                lblTitle.getStyleClass().add("muted");

                Label bPts  = badge(st.getPoints().stripTrailingZeros().toPlainString() + " pts");
                Label bDiff = badgeForDifficulty(st.getDifficulty());
                Label bElig = badgeForEligibility(st.getEligibility());

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnOpen = new Button("Details");
                btnOpen.getStyleClass().add("chip");
                btnOpen.setOnAction(e -> openSubtaskSheet(t, st));

                row.getChildren().addAll(lblTitle, bPts, bDiff, bElig, spacer, btnOpen);
                taskCard.getChildren().add(row);
            }

            centerContainer.getChildren().add(taskCard);
        }
    }

    private Label badge(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("badge");
        return l;
    }

    private Label badgeForDifficulty(Difficulty d) {
        Label l = badge(d.toString());
        l.getStyleClass().addAll("badge-diff", switch (d) {
            case EASY -> "badge-diff-easy";
            case MEDIUM -> "badge-diff-medium";
            case HARD -> "badge-diff-hard";
        });
        return l;
    }

    private Label badgeForEligibility(Eligibility e) {
        Label l = badge(e.toString());
        l.getStyleClass().addAll("badge-elig", switch (e) {
            case EXAM -> "badge-elig-exam";
            case PRACTICE -> "badge-elig-practice";
            case BOTH -> "badge-elig-both";
        });
        return l;
    }

    private VBox makeCard() {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(16));
        return box;
    }

    private boolean matchesTask(TaskModel t, String q) {
        if (q.isEmpty()) return true;
        if (t.getId().toLowerCase().contains(q)) return true;
        if (t.getTitle().toLowerCase().contains(q)) return true;
        for (SubtaskModel st : t.getSubtasks()) {
            if (matchesSubtask(st, q)) return true;
        }
        return false;
    }

    private boolean matchesSubtask(SubtaskModel st, String q) {
        if (q.isEmpty()) return true;
        if (st.getId().toLowerCase().contains(q)) return true;
        for (VariantModel v : st.getVariants()) {
            if ((v.getText() != null && v.getText().toLowerCase().contains(q)) ||
                (v.getSolution() != null && v.getSolution().toLowerCase().contains(q))) {
                return true;
            }
        }
        return false;
    }

    // ===== Subtask sheet =====

    private void openSubtaskSheet(TaskModel task, SubtaskModel sub) {
        VBox sheet = new VBox(14);
        sheet.setPadding(new Insets(16));

        Label title = new Label("Subtask " + task.getId() + "." + sub.getId());
        title.getStyleClass().add("header");

        // Instead of task title: show/edit the subtask title = variants@group
        String currentGroup = xmlService.readSubtaskGroup(sub);
        TextField tfSubtaskTitle = new TextField(currentGroup);
        tfSubtaskTitle.setPromptText("Subtask title (variants@group)");
        tfSubtaskTitle.setMaxWidth(Double.MAX_VALUE);
        tfSubtaskTitle.textProperty().addListener((o, ov, nv) -> {
            xmlService.updateSubtaskGroup(sub, nv == null ? "" : nv);
        });

        // Meta grid: Points | Difficulty | Eligibility (with same colors)
        GridPane meta = new GridPane();
        meta.setHgap(10);
        meta.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(33);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(33);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(34);
        meta.getColumnConstraints().addAll(c1, c2, c3);

        VBox pointsBox = new VBox(4);
        Label lblPts = new Label("Points");
        TextField tfPoints = new TextField(sub.getPoints().stripTrailingZeros().toPlainString());
        tfPoints.setPromptText("Integer");
        tfPoints.setMaxWidth(Double.MAX_VALUE);
        pointsBox.getChildren().addAll(lblPts, tfPoints);

        VBox diffBox = new VBox(4);
        Label lblDiff = new Label("Difficulty");
        ComboBox<Difficulty> cbDiff = new ComboBox<>();
        cbDiff.getItems().setAll(Difficulty.values());
        cbDiff.getSelectionModel().select(sub.getDifficulty());
        cbDiff.setMaxWidth(Double.MAX_VALUE);
        // colorize like badges
        Runnable paintDiff = () -> {
            cbDiff.getStyleClass().removeAll("combo-diff-easy","combo-diff-medium","combo-diff-hard");
            Difficulty d = cbDiff.getValue();
            if (d != null) switch (d) {
                case EASY -> cbDiff.getStyleClass().add("combo-diff-easy");
                case MEDIUM -> cbDiff.getStyleClass().add("combo-diff-medium");
                case HARD -> cbDiff.getStyleClass().add("combo-diff-hard");
            }
        };
        paintDiff.run();
        cbDiff.valueProperty().addListener((o, ov, nv) -> {
            sub.setDifficulty(nv);
            xmlService.updateSubtaskMeta(sub);
            paintDiff.run();
            renderCenter();
        });
        diffBox.getChildren().addAll(lblDiff, cbDiff);

        VBox eligBox = new VBox(4);
        Label lblElig = new Label("Eligibility");
        ComboBox<Eligibility> cbElig = new ComboBox<>();
        cbElig.getItems().setAll(Eligibility.values());
        cbElig.getSelectionModel().select(sub.getEligibility());
        cbElig.setMaxWidth(Double.MAX_VALUE);
        // colorize like badges
        Runnable paintElig = () -> {
            cbElig.getStyleClass().removeAll("combo-elig-exam","combo-elig-practice","combo-elig-both");
            Eligibility e = cbElig.getValue();
            if (e != null) switch (e) {
                case EXAM -> cbElig.getStyleClass().add("combo-elig-exam");
                case PRACTICE -> cbElig.getStyleClass().add("combo-elig-practice");
                case BOTH -> cbElig.getStyleClass().add("combo-elig-both");
            }
        };
        paintElig.run();
        cbElig.valueProperty().addListener((o, ov, nv) -> {
            sub.setEligibility(nv);
            xmlService.updateSubtaskMeta(sub);
            paintElig.run();
            renderCenter();
        });
        eligBox.getChildren().addAll(lblElig, cbElig);

        meta.add(pointsBox, 0, 0);
        meta.add(diffBox,   1, 0);
        meta.add(eligBox,   2, 0);

        tfPoints.textProperty().addListener((o, ov, nv) -> {
            if (nv.matches("\\d+")) {
                sub.setPoints(new java.math.BigDecimal(nv));
                xmlService.updateSubtaskMeta(sub);
                renderCenter();
            }
        });

        // Variants list (editable)
        VBox variantsBox = new VBox(12);
        for (VariantModel v : sub.getVariants()) {
            VBox vCard = makeCard();
            vCard.setPadding(new Insets(12));
            Label vHeader = new Label("Variant " + v.getId());
            vHeader.getStyleClass().add("header");

            TextArea taText = new TextArea(v.getText());
            taText.setPromptText("Variant text");
            taText.setPrefRowCount(3);
            taText.textProperty().addListener((o, ov, nv) -> {
                v.setText(nv);
                xmlService.updateVariant(v);
            });

            TextArea taSol = new TextArea(v.getSolution());
            taSol.setPromptText("Solution (leave empty if none)");
            taSol.setPrefRowCount(3);
            taSol.textProperty().addListener((o, ov, nv) -> {
                v.setSolution(nv);
                xmlService.updateVariant(v);
            });

            vCard.getChildren().addAll(vHeader, new Label("Text"), taText, new Label("Solution"), taSol);
            variantsBox.getChildren().add(vCard);
        }

        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("chip");
        btnClose.setOnAction(e -> {
            slideOver.hide();
            rootStack.setMouseTransparent(true);
        });

        sheet.getChildren().addAll(title,
                new Label("Subtask title"), tfSubtaskTitle,
                new Separator(),
                meta,
                new Separator(),
                new Label("Variants"), variantsBox,
                btnClose);

        slideOver.setContent(sheet);
        slideOver.show();
        rootStack.setMouseTransparent(false);
    }

    // ===== Generate flow =====

    private void openGenerateFlow() {
        VBox step1 = new VBox(12);
        step1.setPadding(new Insets(16));

        Label header = new Label("Generate");
        header.getStyleClass().add("header");

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbExam = new RadioButton("exam");
        RadioButton rbPractice = new RadioButton("practice");
        RadioButton rbBoth = new RadioButton("both");
        rbExam.setToggleGroup(tg);
        rbPractice.setToggleGroup(tg);
        rbBoth.setToggleGroup(tg);
        rbExam.setSelected(true);

        tg.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == rbExam) scope.set(GenerateScope.EXAM);
            else if (nv == rbPractice) scope.set(GenerateScope.PRACTICE);
            else scope.set(GenerateScope.BOTH);
        });

        TextField tfTitle = new TextField(examTitle.get());
        tfTitle.setPromptText("Title (e.g., Databases – Exam)");
        tfTitle.textProperty().addListener((o, ov, nv) -> examTitle.set(nv));

        DatePicker dp = new DatePicker(examDate.get());
        dp.valueProperty().addListener((o, ov, nv) -> examDate.set(nv));

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(withSampleSolution);

        Button next = new Button("Next");
        next.getStyleClass().add("primary");
        next.setOnAction(e -> openGenerateStep2());

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> {
            slideOver.hide();
            rootStack.setMouseTransparent(true);
        });

        HBox actions = new HBox(8, cancel, next);

        step1.getChildren().addAll(header, new Label("Scope"),
                new HBox(8, rbExam, rbPractice, rbBoth),
                new Label("Title"), tfTitle,
                new Label("Date"), dp,
                cbSample, actions);

        slideOver.setContent(step1);
        slideOver.show();
        rootStack.setMouseTransparent(false);
    }

    private void openGenerateStep2() {
        VBox step2 = new VBox(12);
        step2.setPadding(new Insets(16));
        Label header = new Label("Select topics");
        header.getStyleClass().add("header");

        ListView<TaskSelection> list = new ListView<>();
        list.setCellFactory(v -> new TaskSelectionCell(this));
        list.setItems(selectedTasks);

        if (selectedTasks.isEmpty()) {
            for (TaskModel t : tasks) selectedTasks.add(new TaskSelection(t));
        }

        Label total = new Label();
        total.textProperty().bind(Bindings.createStringBinding(
                () -> "Total points: " + selectedTasks.stream()
                        .filter(TaskSelection::isEnabled)
                        .map(TaskSelection::getChosenPoints)
                        .mapToInt(Integer::intValue).sum(),
                selectedTasks));

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(withSampleSolution);

        Button back = new Button("Back");
        back.setOnAction(e -> openGenerateFlow());

        Button btnGenerateExam = new Button("Generate Exam");
        btnGenerateExam.getStyleClass().add("primary");
        btnGenerateExam.setOnAction(e -> generateExamNow());

        HBox actions = new HBox(8, back, new Region(), cbSample, total, btnGenerateExam);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);

        step2.getChildren().addAll(header, list, actions);

        slideOver.setContent(step2);
        slideOver.show();
        rootStack.setMouseTransparent(false);
    }

    private void generateExamNow() {
        try {
            GenerateScope sc = scope.get();
            PdfExporter exporter = new PdfExporter();

            List<PdfExporter.TaskAssembly> assemblies = new ArrayList<>();
            int taskIndex = 1;
            for (TaskSelection ts : selectedTasks) {
                if (!ts.isEnabled()) continue;
                int chosenPts = ts.getChosenPoints();
                TaskModel task = ts.getTask();

                List<SubtaskModel> eligible = task.getSubtasks().stream()
                        .filter(st -> st.isEligibleFor(sc))
                        .collect(Collectors.toList());

                List<SubtaskModel> chosen = PointCombination.pickSubtasksWithDistribution(
                        eligible, chosenPts);

                if (chosen == null) {
                    showError("Task " + task.getId() + ": no feasible combination for " + chosenPts
                            + " points with near 1/3 difficulty. Add more subtasks of different difficulties.");
                    return;
                }

                assemblies.add(new PdfExporter.TaskAssembly(taskIndex++, task, chosen));
            }

            if (assemblies.isEmpty()) {
                showError("No tasks selected.");
                return;
            }

            exporter.export(getWindow(), examTitle.get(), examDate.get(), assemblies, withSampleSolution.get());

            slideOver.hide();
            rootStack.setMouseTransparent(true);
            showInfo("PDF(s) generated.");
        } catch (Exception ex) {
            showError("Generation failed: " + ex.getMessage());
        }
    }

    // ===== Notifications =====

    private void showError(String msg) { showBanner(msg, true); }
    private void showInfo(String msg) { showBanner(msg, false); }

    private void showBanner(String msg, boolean error) {
        Label l = new Label(msg);
        l.getStyleClass().add(error ? "banner-error" : "banner-info");
        VBox banner = new VBox(l);
        banner.getStyleClass().add("banner");
        ((StackPane) App.getScene().getRoot()).getChildren().add(banner);
        StackPane.setAlignment(banner, javafx.geometry.Pos.TOP_CENTER);
        new Thread(() -> {
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> ((StackPane) App.getScene().getRoot()).getChildren().remove(banner));
        }).start();
    }

    // ===== Types =====

    public enum GenerateScope { EXAM, PRACTICE, BOTH }

    public static class TaskSelection {
        private final TaskModel task;
        private final BooleanProperty enabled = new SimpleBooleanProperty(false);
        private final IntegerProperty chosenPoints = new SimpleIntegerProperty(0);
        private final ObservableList<Integer> achievable = FXCollections.observableArrayList();
        TaskSelection(TaskModel task) { this.task = task; }
        public TaskModel getTask() { return task; }
        public boolean isEnabled() { return enabled.get(); }
        public void setEnabled(boolean v) { enabled.set(v); }
        public BooleanProperty enabledProperty() { return enabled; }
        public int getChosenPoints() { return chosenPoints.get(); }
        public IntegerProperty chosenPointsProperty() { return chosenPoints; }
        public ObservableList<Integer> getAchievable() { return achievable; }
        public void recomputeAchievable(GenerateScope scope) {
            achievable.setAll(PointCombination.achievablePointSums(task, scope));
            if (!achievable.contains(chosenPoints.get())) {
                chosenPoints.set(achievable.isEmpty() ? 0 : achievable.get(0));
            }
        }
        @Override public String toString() { return task.getTitle(); }
    }

    private static class TaskSelectionCell extends ListCell<TaskSelection> {
        private final HomeController owner;
        private final CheckBox cbEnable = new CheckBox();
        private final Label title = new Label();
        private final ComboBox<Integer> cbPoints = new ComboBox<>();
        private final Button recompute = new Button("Recompute");

        TaskSelectionCell(HomeController owner) {
            this.owner = owner;
            HBox box = new HBox(8, cbEnable, title, new Region(), new Label("Points:"), cbPoints, recompute);
            HBox.setHgrow(box.getChildren().get(2), Priority.ALWAYS);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(box);

            cbEnable.selectedProperty().addListener((o, ov, nv) -> {
                TaskSelection ts = getItem();
                if (ts != null) {
                    ts.setEnabled(nv);
                    if (nv && ts.getAchievable().isEmpty()) {
                        owner.showError("No achievable sums for: " + ts.getTask().getTitle() +
                                ". Add subtasks with diverse difficulties.");
                    }
                }
            });

            cbPoints.valueProperty().addListener((o, ov, nv) -> {
                TaskSelection ts = getItem();
                if (ts != null && nv != null) {
                    ts.chosenPointsProperty().set(nv);
                }
            });

            recompute.setOnAction(e -> {
                TaskSelection ts = getItem();
                if (ts != null) {
                    ts.recomputeAchievable(owner.scope.get());
                    if (ts.getAchievable().isEmpty()) {
                        owner.showError("No achievable point sums (1/3 difficulty rule).");
                    }
                }
            });
        }

        @Override
        protected void updateItem(TaskSelection item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                title.setText("Task " + item.getTask().getId() + " — " + item.getTask().getTitle());
                cbEnable.selectedProperty().unbind();
                cbEnable.selectedProperty().bindBidirectional(item.enabledProperty());

                cbPoints.itemsProperty().unbind();
                item.recomputeAchievable(owner.scope.get());
                cbPoints.setItems(item.getAchievable());
                if (!item.getAchievable().isEmpty()) {
                    cbPoints.getSelectionModel().select(item.getAchievable().get(0));
                } else {
                    cbPoints.getSelectionModel().clearSelection();
                }
                setGraphic(((HBox) getGraphic()));
            }
        }
    }
}