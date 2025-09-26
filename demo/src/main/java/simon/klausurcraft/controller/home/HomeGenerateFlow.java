package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import simon.klausurcraft.model.GenerateScope;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.pdf.PdfExporter;
import simon.klausurcraft.services.PointCombination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class HomeGenerateFlow {

    private HomeGenerateFlow(){}

    static void openStep1(HomeController root) {
        VBox step1 = new VBox(12);
        step1.setPadding(new Insets(16));

        Label header = new Label("Generate");
        header.getStyleClass().add("header");

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbExam = new RadioButton("exam");
        RadioButton rbPractice = new RadioButton("practice");
        RadioButton rbBoth = new RadioButton("both");
        rbExam.setToggleGroup(tg); rbPractice.setToggleGroup(tg); rbBoth.setToggleGroup(tg);
        rbExam.setSelected(root.scope.get() == GenerateScope.EXAM);
        rbPractice.setSelected(root.scope.get() == GenerateScope.PRACTICE);
        rbBoth.setSelected(root.scope.get() == GenerateScope.BOTH);

        tg.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (nv == rbExam) root.scope.set(GenerateScope.EXAM);
            else if (nv == rbPractice) root.scope.set(GenerateScope.PRACTICE);
            else root.scope.set(GenerateScope.BOTH);
        });

        TextField tfTitle = new TextField(root.examTitle.get());
        tfTitle.setPromptText("Title (e.g., Databases – Exam)");
        tfTitle.textProperty().addListener((o, ov, nv) -> root.examTitle.set(nv));

        DatePicker dp = new DatePicker(root.examDate.get());
        dp.valueProperty().addListener((o, ov, nv) -> root.examDate.set(nv));

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(root.withSampleSolution);

        Button next = new Button("Next");
        next.getStyleClass().add("primary");
        next.setOnAction(e -> openStep2(root));

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        HBox actions = new HBox(8, cancel, next);

        step1.getChildren().addAll(header, new Label("Scope"),
                new HBox(8, rbExam, rbPractice, rbBoth),
                new Label("Title"), tfTitle,
                new Label("Date"), dp,
                cbSample, actions);

        root.getSlideOver().setContent(step1);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);
    }

    static void openStep2(HomeController root) {
        VBox step2 = new VBox(12);
        step2.setPadding(new Insets(16));
        Label header = new Label("Select topics");
        header.getStyleClass().add("header");

        ListView<TaskSelection> list = new ListView<>();
        list.setCellFactory(v -> new TaskSelectionCell(root));
        list.setItems(TaskSelection.ensureFor(root.getTasks()));

        Label total = new Label();
        total.textProperty().bind(TaskSelection.totalPointsBinding(list.getItems()));

        CheckBox cbSample = new CheckBox("Sample Solution");
        cbSample.selectedProperty().bindBidirectional(root.withSampleSolution);

        Button back = new Button("Back");
        back.setOnAction(e -> openStep1(root));

        Button btnGenerateExam = new Button("Generate Exam");
        btnGenerateExam.getStyleClass().add("primary");
        btnGenerateExam.setOnAction(e -> generateExamNow(root, list.getItems()));

        HBox actions = new HBox(8, back, new Region(), cbSample, total, btnGenerateExam);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);

        step2.getChildren().addAll(header, list, actions);

        root.getSlideOver().setContent(step2);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);
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

                List<SubtaskModel> chosen = PointCombination.pickSubtasksWithDistribution(
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

            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
            HomeNotifications.showInfo("PDF(s) generated.");
        } catch (Exception ex) {
            HomeNotifications.showError("Generation failed: " + ex.getMessage());
        }
    }
}