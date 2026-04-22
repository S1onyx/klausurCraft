package simon.klausurcraft.ui.home;

import javafx.animation.FadeTransition;
import javafx.animation.PathTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
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
        StackPane stageRoot = new StackPane(pane);

        VBox content = new VBox(16);
        content.setPadding(new Insets(16));

        Label header = new Label("Select tasks");
        header.getStyleClass().add("header");

        // Build TaskSelection lists
        var all = TaskSelection.ensureFor(root.getTasks());
        var selected = FXCollections.<TaskSelection>observableArrayList();
        var pool = FXCollections.<TaskSelection>observableArrayList();

        // recompute achievable upfront based on fixed scope
        all.forEach(ts -> {
            ts.recomputeAchievable(root.scope.get());
            suggestPointForTaskSelection(root, ts);
        });
        // initial: none selected
        pool.setAll(all);

        // --- Pool (bottom)
        Label lblPool = new Label("Task pool");
        ListView<TaskSelection> lvPool = new ListView<>(pool);
        lvPool.setFocusTraversable(true);
        lvPool.setTooltip(new Tooltip("Available tasks. Tick a row to include it."));

        // --- Selected (top)
        Label lblSelected = new Label("Selected tasks");
        Button btnSuggestForAll = new Button("Suggest for all");
        btnSuggestForAll.getStyleClass().add("chip");
        btnSuggestForAll.disableProperty().bind(Bindings.isEmpty(selected));
        btnSuggestForAll.setTooltip(new Tooltip("Apply suggested target points for all selected tasks."));
        HBox selectedHeader = new HBox(8, lblSelected, btnSuggestForAll);
        selectedHeader.setAlignment(Pos.CENTER_LEFT);
        ListView<TaskSelection> lvSelected = new ListView<>(selected);
        lvSelected.setCellFactory(v -> new SelectedTaskCell(root, selected, pool, ts -> {
            playSelectedToPoolAnimation(stageRoot, lvSelected, lvPool, ts);
            moveToPool(ts, selected, pool);
        }));
        lvSelected.setFocusTraversable(true);
        lvSelected.setTooltip(new Tooltip("Tasks included in the generated exam."));

        lvPool.setCellFactory(v -> new PoolTaskCell(root, ts -> {
            playPoolToSelectedAnimation(stageRoot, lvPool, selectedHeader, ts);
            moveToSelected(ts, pool, selected, root);
        }));

        installKeyboardTransfer(lvSelected, lvPool, root, selected, pool, stageRoot, selectedHeader);
        
        // UI/UX-Rule "Guidance"
        HBox progressStrip = new HBox(8);
        progressStrip.getStyleClass().add("generate-progress");
        Label step1 = buildProgressStep("1 Select tasks");
        Label step2 = buildProgressStep("2 Set points");
        Label step3 = buildProgressStep("3 Generate PDF");
        Label summary = new Label();
        summary.getStyleClass().add("generate-summary");
        summary.setMaxWidth(Double.MAX_VALUE);
        summary.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(summary, Priority.ALWAYS);
        StringBinding totalBinding = TaskSelection.totalPointsBinding(selected);
        summary.textProperty().bind(Bindings.createStringBinding(
                () -> "Selected: " + selected.size() + " / " + all.size() + "  |  " + totalBinding.get(),
                selected, totalBinding
        ));
        progressStrip.getChildren().addAll(step1, step2, step3, summary);
        refreshProgressStepState(selected, step1, step2, step3);
        selected.addListener((ListChangeListener<? super TaskSelection>) c -> refreshProgressStepState(selected, step1, step2, step3));
        btnSuggestForAll.setOnAction(e -> {
            int count = suggestPointsForAll(root, selected);
            if (count > 0) {
                HomeNotifications.showInfo("Applied suggestions for " + count + " task(s).");
                playSubtlePulse(progressStrip);
            }
        });

        // Table-like header for pool rows, including fixed second column for possible points.
        HBox poolHeader = new HBox(12);
        poolHeader.getStyleClass().add("generate-table-header");
        Label colTask = new Label("Task");
        colTask.getStyleClass().add("generate-col-task");
        HBox.setHgrow(colTask, Priority.ALWAYS);
        Label colPossible = new Label("Possible points");
        colPossible.getStyleClass().add("generate-col-possible");
        HBox colPossibleWrap = new HBox(colPossible);
        colPossibleWrap.getStyleClass().add("pool-possible-column");
        colPossibleWrap.setMinWidth(260);
        colPossibleWrap.setPrefWidth(360);
        poolHeader.getChildren().addAll(colTask, colPossibleWrap);

        content.getChildren().addAll(header, progressStrip, selectedHeader, lvSelected, new Separator(), lblPool, poolHeader, lvPool);

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

        // UI/UX-Rule "Novelty"
        Button btnSmartFill = new Button("Smart Fill");
        btnSmartFill.getStyleClass().add("chip");
        btnSmartFill.setTooltip(new Tooltip("Auto-pick a strong starter selection with sensible point defaults."));
        btnSmartFill.disableProperty().bind(Bindings.isEmpty(pool));

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(root.withSampleSolution);
        cbSample.setTooltip(new Tooltip("Include a sample-solution PDF in the export."));

        Label total = new Label();
        total.textProperty().bind(TaskSelection.totalPointsBinding(selected));
        total.setTooltip(new Tooltip("Current total points from selected tasks."));
        btnSmartFill.setOnAction(e -> {
            applySmartFill(root, selected, pool);
            playSubtlePulse(progressStrip);
            playSubtlePulse(total);
        });

        Button btnGenerateExam = new Button("Generate Exam");
        btnGenerateExam.getStyleClass().add("primary");
        btnGenerateExam.setDefaultButton(true);
        btnGenerateExam.disableProperty().bind(Bindings.isEmpty(selected));
        btnGenerateExam.setTooltip(new Tooltip("Generate PDF(s) with the selected tasks."));

        actions.getChildren().addAll(themeToggle, back, btnSmartFill, spacer, cbSample, total, btnGenerateExam);
        pane.setBottom(actions);

        // ----- Modal window -----
        Stage stage = new Stage();
        stage.initOwner(root.getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Generate — Select tasks");
        stage.setMinWidth(1100);   // wide enough -> no line breaks needed
        stage.setMinHeight(700);
        stage.setResizable(true);
        if (!KlausurCraftApp.getStageIcons().isEmpty()) {
            stage.getIcons().setAll(KlausurCraftApp.getStageIcons());
        }

        Scene scene = new Scene(stageRoot, 1100, 750);
        ThemeService.register(scene);
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
            playGeneratePrepAnimation(stageRoot, () -> {
                boolean ok = generateExamNow(root, selected);
                if (ok) {
                    stage.close();
                }
            });
        });

        Platform.runLater(() -> {
            // Animate only guidance to keep all tasks immediately visible.
            playEntranceAnimation(progressStrip);
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
                    Variant variant = HomeGeneratePlanner.pickVariant(variants);
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

    private static void installKeyboardTransfer(ListView<TaskSelection> lvSelected,
                                                ListView<TaskSelection> lvPool,
                                                HomeController root,
                                                ObservableList<TaskSelection> selected,
                                                ObservableList<TaskSelection> pool,
                                                StackPane stageRoot,
                                                Node selectedHeader) {
        lvPool.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.SPACE) return;
            TaskSelection item = lvPool.getSelectionModel().getSelectedItem();
            if (item == null) return;
            if (item.getAchievable().isEmpty()) {
                HomeNotifications.showError("This task has no possible points in the current scope.");
                e.consume();
                return;
            }

            playPoolToSelectedAnimation(stageRoot, lvPool, selectedHeader, item);
            moveToSelected(item, pool, selected, root);
            int next = Math.min(pool.size() - 1, Math.max(0, lvPool.getSelectionModel().getSelectedIndex()));
            if (!pool.isEmpty()) lvPool.getSelectionModel().select(next);
            e.consume();
        });

        lvSelected.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.SPACE) return;
            TaskSelection item = lvSelected.getSelectionModel().getSelectedItem();
            if (item == null) return;

            playSelectedToPoolAnimation(stageRoot, lvSelected, lvPool, item);
            moveToPool(item, selected, pool);
            int next = Math.min(selected.size() - 1, Math.max(0, lvSelected.getSelectionModel().getSelectedIndex()));
            if (!selected.isEmpty()) lvSelected.getSelectionModel().select(next);
            e.consume();
        });
    }

    private static void moveToSelected(TaskSelection item,
                                       ObservableList<TaskSelection> pool,
                                       ObservableList<TaskSelection> selected,
                                       HomeController root) {
        HomeGeneratePlanner.moveToSelected(item, pool, selected, root.scope.get());
    }

    private static void moveToPool(TaskSelection item,
                                   ObservableList<TaskSelection> selected,
                                   ObservableList<TaskSelection> pool) {
        HomeGeneratePlanner.moveToPool(item, selected, pool);
    }

    private static Label buildProgressStep(String text) {
        Label step = new Label(text);
        step.getStyleClass().add("generate-progress-step");
        return step;
    }

    private static void refreshProgressStepState(ObservableList<TaskSelection> selected,
                                                 Label step1,
                                                 Label step2,
                                                 Label step3) {
        boolean hasSelection = !selected.isEmpty();
        setProgressState(step1, hasSelection, !hasSelection);
        setProgressState(step2, hasSelection, hasSelection);
        setProgressState(step3, false, hasSelection);
    }

    private static void setProgressState(Label label, boolean done, boolean active) {
        label.getStyleClass().removeAll("generate-progress-step-done", "generate-progress-step-active");
        if (done) {
            label.getStyleClass().add("generate-progress-step-done");
        } else if (active) {
            label.getStyleClass().add("generate-progress-step-active");
        }
    }

    private static void applySmartFill(HomeController root,
                                       ObservableList<TaskSelection> selected,
                                       ObservableList<TaskSelection> pool) {
        int tuned = HomeGeneratePlanner.applySmartFill(root.scope.get(), selected, pool);
        if (tuned <= 0) {
            HomeNotifications.showError("Smart Fill could not find tasks with achievable points.");
            return;
        }

        HomeNotifications.showInfo("Smart Fill prepared " + tuned + " task(s).");
    }

    private static void suggestPointForTaskSelection(HomeController root, TaskSelection ts) {
        HomeGeneratePlanner.suggestPointForTaskSelection(root.scope.get(), ts);
    }

    private static int suggestPointsForAll(HomeController root, ObservableList<TaskSelection> selected) {
        return HomeGeneratePlanner.suggestPointsForAll(root.scope.get(), selected);
    }

    private static void playGeneratePrepAnimation(StackPane stageRoot, Runnable after) {
        StackPane scrim = new StackPane();
        scrim.getStyleClass().add("generate-export-overlay");
        scrim.setPickOnBounds(true);

        VBox card = new VBox(10);
        card.getStyleClass().add("generate-export-card");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(52, 52);
        Label title = new Label("Preparing export");
        title.getStyleClass().add("generate-export-title");
        Label subtitle = new Label("Checking task combinations and opening save dialog...");
        subtitle.getStyleClass().add("generate-export-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(320);
        card.getChildren().addAll(spinner, title, subtitle);
        card.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        card.setAlignment(Pos.CENTER);

        scrim.getChildren().add(card);
        stageRoot.getChildren().add(scrim);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(140), card);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(180), card);
        scaleIn.setFromX(0.96);
        scaleIn.setFromY(0.96);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        fadeIn.play();
        scaleIn.play();

        PauseTransition hold = new PauseTransition(Duration.millis(900));
        hold.setOnFinished(ev -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(130), scrim);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(done -> {
                stageRoot.getChildren().remove(scrim);
                after.run();
            });
            fadeOut.play();
        });
        hold.play();
    }

    private static void playSubtlePulse(Node node) {
        if (node == null) return;
        ScaleTransition pulse = new ScaleTransition(Duration.millis(170), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.015);
        pulse.setToY(1.015);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }

    private static void playEntranceAnimation(Node node) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(8);

        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), node);
        slide.setFromY(8);
        slide.setToY(0);
        fade.play();
        slide.play();
    }

    private static void playPoolToSelectedAnimation(StackPane stageRoot,
                                                    Node poolNode,
                                                    Node selectedHeader,
                                                    TaskSelection item) {
        playFunnelFlightAnimation(stageRoot, poolNode, selectedHeader, item, true);
    }

    private static void playSelectedToPoolAnimation(StackPane stageRoot,
                                                    Node selectedNode,
                                                    Node poolNode,
                                                    TaskSelection item) {
        playFunnelFlightAnimation(stageRoot, selectedNode, poolNode, item, false);
    }

    private static void playFunnelFlightAnimation(StackPane stageRoot,
                                                  Node sourceNode,
                                                  Node targetNode,
                                                  TaskSelection item,
                                                  boolean upward) {
        if (stageRoot == null || sourceNode == null || targetNode == null || item == null) return;

        Bounds sourceBounds = sourceNode.localToScene(sourceNode.getBoundsInLocal());
        Bounds targetBounds = targetNode.localToScene(targetNode.getBoundsInLocal());
        Bounds rootBounds = stageRoot.localToScene(stageRoot.getBoundsInLocal());
        if (sourceBounds == null || targetBounds == null || rootBounds == null) return;

        String title = item.getTask().getTitle();
        String chipText = (title == null || title.isBlank()) ? ("Task " + item.getTask().getId()) : title;
        if (chipText.length() > 28) {
            chipText = chipText.substring(0, 25) + "...";
        }

        Label ghost = new Label(chipText);
        ghost.getStyleClass().add("pool-fly-chip");
        ghost.setManaged(false);
        ghost.setMouseTransparent(true);
        stageRoot.getChildren().add(ghost);
        ghost.applyCss();
        ghost.autosize();

        double startX = sourceBounds.getMinX() + sourceBounds.getWidth() * 0.5;
        double startY = sourceBounds.getMinY() + sourceBounds.getHeight() * 0.5;
        double endX = targetBounds.getMinX() + targetBounds.getWidth() * (upward ? 0.55 : 0.45);
        double endY = targetBounds.getMinY() + targetBounds.getHeight() * (upward ? 0.55 : 0.40);

        ghost.relocate(startX - rootBounds.getMinX(), startY - rootBounds.getMinY());
        ghost.setOpacity(0.96);
        ghost.setScaleX(1.0);
        ghost.setScaleY(1.0);

        double dx = endX - startX;
        double dy = endY - startY;
        double bendX = dx * 0.42;
        double funnelLift = upward ? -34 : 34;
        double funnelPull = upward ? -16 : 16;

        Path path = new Path(
                new MoveTo(0, 0),
                new CubicCurveTo(
                        bendX, dy * 0.20 + funnelLift,
                        dx * 0.78, dy * 0.78 + funnelPull,
                        dx, dy
                )
        );

        PathTransition travel = new PathTransition(Duration.millis(340), path, ghost);

        ScaleTransition scale = new ScaleTransition(Duration.millis(340), ghost);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.42);
        scale.setToY(0.42);

        FadeTransition fade = new FadeTransition(Duration.millis(340), ghost);
        fade.setFromValue(0.96);
        fade.setToValue(0.10);

        ParallelTransition flight = new ParallelTransition(travel, scale, fade);
        flight.setOnFinished(e -> {
            stageRoot.getChildren().remove(ghost);
            playSubtlePulse(targetNode);
        });
        flight.play();
    }
}
