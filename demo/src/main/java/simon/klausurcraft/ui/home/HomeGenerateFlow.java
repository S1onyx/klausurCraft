package simon.klausurcraft.ui.home;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import simon.klausurcraft.app.KlausurCraftApp;
import simon.klausurcraft.task.GenerateScope;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.task.Variant;
import simon.klausurcraft.task.export.PdfExportService;
import simon.klausurcraft.task.planning.PointDistributionPlanner;
import simon.klausurcraft.ui.ThemeService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

final class HomeGenerateFlow {

    private HomeGenerateFlow(){}

    static void openStep1(HomeController root) {
        BorderPane sheet = new BorderPane();
        sheet.setPadding(new Insets(0));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label header = new Label("Generate");
        header.getStyleClass().add("header");

        // --- Scope as colored "badges"
        ToggleGroup tg = new ToggleGroup();
        RadioButton rbExam = new RadioButton("exam");
        RadioButton rbPractice = new RadioButton("practice");
        RadioButton rbBoth = new RadioButton("both");
        rbExam.setToggleGroup(tg); rbPractice.setToggleGroup(tg); rbBoth.setToggleGroup(tg);
        rbExam.setTooltip(new Tooltip("Generate only exam-eligible subtasks."));
        rbPractice.setTooltip(new Tooltip("Generate only practice-eligible subtasks."));
        rbBoth.setTooltip(new Tooltip("Generate from all eligible subtasks."));

        // add pill styles (CSS provides colors per scope)
        rbExam.getStyleClass().addAll("scope-chip", "scope-exam");
        rbPractice.getStyleClass().addAll("scope-chip", "scope-practice");
        rbBoth.getStyleClass().addAll("scope-chip", "scope-both");

        rbExam.setSelected(root.scope.get() == GenerateScope.EXAM);
        rbPractice.setSelected(root.scope.get() == GenerateScope.PRACTICE);
        rbBoth.setSelected(root.scope.get() == GenerateScope.BOTH);

        tg.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == rbExam) root.scope.set(GenerateScope.EXAM);
            else if (nv == rbPractice) root.scope.set(GenerateScope.PRACTICE);
            else root.scope.set(GenerateScope.BOTH);
        });

        HBox scopeRow = new HBox(10, rbExam, rbPractice, rbBoth);

        TextField tfTitle = new TextField(root.examTitle.get());
        tfTitle.setPromptText("Title (e.g., Databases – Exam)");
        tfTitle.setTooltip(new Tooltip("Title shown in the exported exam PDF."));
        tfTitle.textProperty().addListener((o, ov, nv) -> root.examTitle.set(nv));

        DatePicker dp = new DatePicker(root.examDate.get());
        dp.setTooltip(new Tooltip("Exam date printed in the export."));
        dp.valueProperty().addListener((o, ov, nv) -> root.examDate.set(nv));

        TextField tfDuration = new TextField(Integer.toString(Math.max(1, root.examDurationMinutes.get())));
        tfDuration.setPromptText("e.g., 90");
        tfDuration.setTooltip(new Tooltip("Duration in minutes, used in the exam header."));
        tfDuration.textProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.isBlank()) return;
            if (nv.matches("\\d+")) {
                try {
                    int minutes = Integer.parseInt(nv);
                    if (minutes > 0) {
                        root.examDurationMinutes.set(minutes);
                    }
                } catch (NumberFormatException ignored) {
                    // keep previous valid value
                }
            }
        });
        tfDuration.focusedProperty().addListener((o, ov, focused) -> {
            if (!focused) {
                tfDuration.setText(Integer.toString(Math.max(1, root.examDurationMinutes.get())));
            }
        });

        // NOTE: Sample solution checkbox intentionally removed from step 1

        content.getChildren().addAll(header,
                new Label("Scope"), scopeRow,
                new Label("Title"), tfTitle,
                new Label("Date"), dp,
                new Label("Duration (minutes)"), tfDuration);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sheet.setCenter(sp);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");
        Region filler = new Region(); HBox.setHgrow(filler, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setTooltip(new Tooltip("Close the wizard without generating."));
        cancel.setOnAction(e -> {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        Button next = new Button("Next");
        next.getStyleClass().add("primary");
        next.setDefaultButton(true);
        next.setTooltip(new Tooltip("Continue to task selection and point setup."));
        next.setOnAction(e -> openStep2(root)); // opens modal window

        actions.getChildren().addAll(filler, cancel, next);
        sheet.setBottom(actions);

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);

        // Autofocus first field
        tfTitle.requestFocus();
    }

    /** Step 2 redesigned: single modal window with two stacked lists: Selected (top) and Task pool (bottom). */
    static void openStep2(HomeController root) {
        // Close slide-over so only the modal window is visible
        if (root.getSlideOver().isShown()) {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        }

        // ----- Content (new layout) -----
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(0));

        VBox content = new VBox(16);
        content.setPadding(new Insets(16));

        Label header = new Label("Select tasks");
        header.getStyleClass().add("header");

        // Build TaskSelection lists
        var all = TaskSelection.ensureFor(root.getTasks());
        var selected = FXCollections.<TaskSelection>observableArrayList();
        var pool = FXCollections.<TaskSelection>observableArrayList();

        // recompute achievable upfront based on fixed scope
        all.forEach(ts -> ts.recomputeAchievable(root.scope.get()));
        // initial: none selected
        pool.setAll(all);

        // --- Selected (top)
        Label lblSelected = new Label("Selected tasks");
        ListView<TaskSelection> lvSelected = new ListView<>(selected);
        lvSelected.setCellFactory(v -> new SelectedTaskCell(root, selected, pool));
        lvSelected.setFocusTraversable(true);
        lvSelected.setTooltip(new Tooltip("Tasks included in the generated exam."));

        // --- Pool (bottom)
        Label lblPool = new Label("Task pool");
        ListView<TaskSelection> lvPool = new ListView<>(pool);
        lvPool.setCellFactory(v -> new PoolTaskCell(root, selected, pool));
        lvPool.setFocusTraversable(true);
        lvPool.setTooltip(new Tooltip("Available tasks. Tick a row to include it."));

        installKeyboardTransfer(lvSelected, lvPool, selected, pool);

        content.getChildren().addAll(header, lblSelected, lvSelected, new Separator(), lblPool, lvPool);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // no horizontal scrollbar
        pane.setCenter(sp);

        // Footer: left theme toggle, right sample+total+generate
        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");
        actions.setAlignment(Pos.CENTER_LEFT);

        // Theme Toggle (left)
        Button themeToggle = new Button("◐");
        themeToggle.getStyleClass().add("icon-button");
        themeToggle.setPickOnBounds(true);
        themeToggle.setFocusTraversable(false);
        Tooltip.install(themeToggle, new Tooltip("Toggle theme (Ctrl+D)"));

        Button back = new Button("Back");
        back.setTooltip(new Tooltip("Return to step 1 and keep your settings."));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(root.withSampleSolution);
        cbSample.setTooltip(new Tooltip("Include a sample-solution PDF in the export."));

        Label total = new Label();
        total.textProperty().bind(TaskSelection.totalPointsBinding(selected));
        total.setTooltip(new Tooltip("Current total points from selected tasks."));

        Button btnGenerateExam = new Button("Generate Exam");
        btnGenerateExam.getStyleClass().add("primary");
        btnGenerateExam.setDefaultButton(true);
        btnGenerateExam.disableProperty().bind(Bindings.isEmpty(selected));
        btnGenerateExam.setTooltip(new Tooltip("Generate PDF(s) with the selected tasks."));

        actions.getChildren().addAll(themeToggle, back, spacer, cbSample, total, btnGenerateExam);
        pane.setBottom(actions);

        // ----- Modal window -----
        Stage stage = new Stage();
        stage.initOwner(root.getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Generate — Select tasks");
        stage.setMinWidth(1100);   // wide enough -> no line breaks needed
        stage.setMinHeight(700);
        stage.setResizable(true);

        Scene scene = new Scene(pane, 1100, 750);
        // Adopt app styles (dark/light consistent)
        try {
            scene.getStylesheets().setAll(KlausurCraftApp.getScene().getStylesheets());
        } catch (Exception ignored) { /* best effort */ }
        stage.setScene(scene);

        // Theme Toggle & Shortcut (Ctrl+D) in dialog
        themeToggle.setOnAction(e -> ThemeService.toggle(scene));
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
            () -> ThemeService.toggle(scene)
        );

        // Actions
        back.setOnAction(e -> {
            stage.close();
            openStep1(root);
        });

        btnGenerateExam.setOnAction(e -> {
            boolean ok = generateExamNow(root, selected);
            if (ok) {
                stage.close();
            }
        });

        Platform.runLater(() -> {
            if (!pool.isEmpty()) lvPool.getSelectionModel().select(0);
            lvPool.requestFocus();
        });

        stage.showAndWait();
    }

    static boolean generateExamNow(HomeController root, List<TaskSelection> selections) {
        try {
            PdfExportService exporter = new PdfExportService();

            List<PdfExportService.TaskAssembly> assemblies = new ArrayList<>();
            int taskIndex = 1;
            for (TaskSelection ts : selections) {
                if (!ts.isEnabled()) continue;
                BigDecimal chosenPts = ts.getChosenPoints();
                Task task = ts.getTask();

                List<Subtask> eligible = task.getSubtasks().stream()
                        .filter(st -> st.isEligibleFor(root.scope.get()))
                        .collect(Collectors.toList());

                List<Subtask> chosen = PointDistributionPlanner.pickSubtasksWithDistribution(
                        eligible, chosenPts);

                if (chosen == null) {
                    HomeNotifications.showError("Task " + task.getId() + ": no feasible combination for "
                            + Points.toDisplayString(chosenPts)
                            + " points with near 1/3 difficulty. Add more subtasks of different difficulties.");
                    return false;
                }

                List<PdfExportService.ChosenVariant> chosenVariants = new ArrayList<>();
                for (Subtask sub : chosen) {
                    List<Variant> variants = sub.getVariants();
                    Variant variant = pickVariant(variants);
                    chosenVariants.add(new PdfExportService.ChosenVariant(sub, variant));
                }

                assemblies.add(new PdfExportService.TaskAssembly(taskIndex++, task, chosenVariants));
            }

            if (assemblies.isEmpty()) {
                HomeNotifications.showError("No tasks selected.");
                return false;
            }

            boolean exported = exporter.export(
                    root.getWindow(),
                    root.examTitle.get(),
                    root.examDate.get(),
                    assemblies,
                    root.examDurationMinutes.get(),
                    root.withSampleSolution.get()
            );
            if (!exported) {
                // User canceled the save dialog -> keep current generate screen open.
                return false;
            }

            // Banner & Cleanup (SlideOver is closed here anyway)
            HomeNotifications.showInfo("PDF(s) generated.");
            return true;
        } catch (Exception ex) {
            HomeNotifications.showError("Generation failed: " + ex.getMessage());
            return false;
        }
    }

    private static Variant pickVariant(List<Variant> variants) {
        if (variants == null || variants.isEmpty()) return null;
        return variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
    }

    private static void installKeyboardTransfer(ListView<TaskSelection> lvSelected,
                                                ListView<TaskSelection> lvPool,
                                                ObservableList<TaskSelection> selected,
                                                ObservableList<TaskSelection> pool) {
        lvPool.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.SPACE) return;
            TaskSelection item = lvPool.getSelectionModel().getSelectedItem();
            if (item == null) return;

            moveToSelected(item, pool, selected);
            int next = Math.min(pool.size() - 1, Math.max(0, lvPool.getSelectionModel().getSelectedIndex()));
            if (!pool.isEmpty()) lvPool.getSelectionModel().select(next);
            e.consume();
        });

        lvSelected.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.SPACE) return;
            TaskSelection item = lvSelected.getSelectionModel().getSelectedItem();
            if (item == null) return;

            moveToPool(item, selected, pool);
            int next = Math.min(selected.size() - 1, Math.max(0, lvSelected.getSelectionModel().getSelectedIndex()));
            if (!selected.isEmpty()) lvSelected.getSelectionModel().select(next);
            e.consume();
        });
    }

    private static void moveToSelected(TaskSelection item,
                                       ObservableList<TaskSelection> pool,
                                       ObservableList<TaskSelection> selected) {
        item.setEnabled(true);
        pool.remove(item);
        if (!selected.contains(item)) {
            if (item.getChosenPoints().compareTo(Points.ZERO) == 0 && !item.getAchievable().isEmpty()) {
                item.chosenPointsProperty().set(item.getAchievable().get(0));
            }
            selected.add(item);
        }
    }

    private static void moveToPool(TaskSelection item,
                                   ObservableList<TaskSelection> selected,
                                   ObservableList<TaskSelection> pool) {
        item.setEnabled(false);
        selected.remove(item);
        if (!pool.contains(item)) {
            pool.add(item);
        }
    }
}
