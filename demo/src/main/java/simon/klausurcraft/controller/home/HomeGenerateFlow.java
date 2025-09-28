package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import simon.klausurcraft.App;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.pdf.PdfExporter;
import simon.klausurcraft.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        tfTitle.textProperty().addListener((o, ov, nv) -> root.examTitle.set(nv));

        DatePicker dp = new DatePicker(root.examDate.get());
        dp.valueProperty().addListener((o, ov, nv) -> root.examDate.set(nv));

        // NOTE: Sample solution checkbox intentionally removed from step 1

        content.getChildren().addAll(header,
                new Label("Scope"), scopeRow,
                new Label("Title"), tfTitle,
                new Label("Date"), dp);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sheet.setCenter(sp);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");
        Region filler = new Region(); HBox.setHgrow(filler, Priority.ALWAYS);

        Button cancel = new Button("Cancel");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        Button next = new Button("Next");
        next.getStyleClass().add("primary");
        next.setDefaultButton(true);
        next.setOnAction(e -> openStep2(root)); // opens modal window

        actions.getChildren().addAll(filler, cancel, next);
        sheet.setBottom(actions);

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);

        // Autofocus first field
        tfTitle.requestFocus();
    }

    /** Step 2 now as a separate MODAL window (no horizontal scrollbar). */
    static void openStep2(HomeController root) {
        // Close slide-over so only the modal window is visible
        if (root.getSlideOver().isShown()) {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        }

        // ----- Content (identical to before, but in its own Stage) -----
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(0));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label header = new Label("Select topics");
        header.getStyleClass().add("header");

        ListView<TaskSelection> list = new ListView<>();
        list.setCellFactory(v -> new TaskSelectionCell(root));
        list.setItems(TaskSelection.ensureFor(root.getTasks()));
        list.setFocusTraversable(false);

        // initial achievable based on current scope
        list.getItems().forEach(ts -> ts.recomputeAchievable(root.scope.get()));

        Label total = new Label();
        // Robust: Binding observes all items + their properties
        total.textProperty().bind(TaskSelection.totalPointsBinding(list.getItems()));

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(root.withSampleSolution);

        content.getChildren().addAll(header, list);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // no horizontal scrollbar
        pane.setCenter(sp);

        // Footer: left theme toggle, right sample+total+generate
        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");

        // Theme Toggle (left)
        Button themeToggle = new Button("◐");
        themeToggle.setPickOnBounds(true);
        themeToggle.setFocusTraversable(false);
        Tooltip.install(themeToggle, new Tooltip("Toggle theme (Ctrl+D)"));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button back = new Button("Back");

        HBox right = new HBox(12);
        right.getChildren().addAll(new Label(""), cbSample, total);

        Button btnGenerateExam = new Button("Generate Exam");
        btnGenerateExam.getStyleClass().add("primary");
        btnGenerateExam.setDefaultButton(true);

        actions.getChildren().addAll(themeToggle, back, spacer, right, btnGenerateExam);
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
            scene.getStylesheets().setAll(App.getScene().getStylesheets());
        } catch (Exception ignored) { /* best effort */ }
        stage.setScene(scene);

        // Theme Toggle & Shortcut (Ctrl+D) in dialog
        themeToggle.setOnAction(e -> ThemeManager.toggle(scene));
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN),
            () -> ThemeManager.toggle(scene)
        );

        // Actions
        back.setOnAction(e -> {
            stage.close();
            openStep1(root);
        });

        btnGenerateExam.setOnAction(e -> {
            generateExamNow(root, list.getItems());
            stage.close();
        });

        // Auto-recompute achievable sums when scope changes
        root.scope.addListener((o, ov, nv) -> list.getItems().forEach(ts -> ts.recomputeAchievable(nv)));

        stage.showAndWait();
    }

    static void generateExamNow(HomeController root, List<TaskSelection> selections) {
        try {
            PdfExporter exporter = new PdfExporter();

            List<PdfExporter.TaskAssembly> assemblies = new ArrayList<>();
            int taskIndex = 1;
            for (TaskSelection ts : selections) {
                if (!ts.isEnabled()) continue;
                int chosenPts = ts.getChosenPoints();
                TaskModel task = ts.getTask();

                List<SubtaskModel> eligible = task.getSubtasks().stream()
                        .filter(st -> st.isEligibleFor(root.scope.get()))
                        .collect(Collectors.toList());

                List<SubtaskModel> chosen = simon.klausurcraft.services.PointCombination.pickSubtasksWithDistribution(
                        eligible, chosenPts);

                if (chosen == null) {
                    HomeNotifications.showError("Task " + task.getId() + ": no feasible combination for " + chosenPts
                            + " points with near 1/3 difficulty. Add more subtasks of different difficulties.");
                    return;
                }

                assemblies.add(new PdfExporter.TaskAssembly(taskIndex++, task, chosen));
            }

            if (assemblies.isEmpty()) {
                HomeNotifications.showError("No tasks selected.");
                return;
            }

            exporter.export(root.getWindow(), root.examTitle.get(), root.examDate.get(), assemblies, root.withSampleSolution.get());

            // Banner & Cleanup (SlideOver is closed here anyway)
            HomeNotifications.showInfo("PDF(s) generated.");
        } catch (Exception ex) {
            HomeNotifications.showError("Generation failed: " + ex.getMessage());
        }
    }
}