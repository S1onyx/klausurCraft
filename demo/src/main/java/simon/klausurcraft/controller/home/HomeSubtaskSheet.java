package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import simon.klausurcraft.model.*;
import simon.klausurcraft.services.XmlService;

import java.math.BigDecimal;

final class HomeSubtaskSheet {

    private HomeSubtaskSheet(){}

    static void open(HomeController root, TaskModel task, SubtaskModel sub) {
        VBox sheet = new VBox(14);
        sheet.setPadding(new Insets(16));

        Label title = new Label("Subtask " + task.getId() + "." + sub.getId());
        title.getStyleClass().add("header");

        XmlService xmlService = root.getXmlService();

        // Subtask "title" from variants@group (requested)
        String currentGroup = xmlService.readSubtaskGroup(sub);
        TextField tfSubtaskTitle = new TextField(currentGroup);
        tfSubtaskTitle.setPromptText("Subtask title (variants@group)");
        tfSubtaskTitle.setMaxWidth(Double.MAX_VALUE);
        tfSubtaskTitle.textProperty().addListener((o, ov, nv) -> xmlService.updateSubtaskGroup(sub, nv == null ? "" : nv));

        // Meta grid
        GridPane meta = new GridPane();
        meta.setHgap(10);
        meta.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(33);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(33);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(34);
        meta.getColumnConstraints().addAll(c1, c2, c3);

        // Points
        VBox ptsBox = new VBox(4);
        Label lblPts = new Label("Points");
        TextField tfPoints = new TextField(sub.getPoints().stripTrailingZeros().toPlainString());
        tfPoints.setPromptText("Integer");
        tfPoints.textProperty().addListener((o, ov, nv) -> {
            if (nv.matches("\\d+")) {
                sub.setPoints(new BigDecimal(nv));
                xmlService.updateSubtaskMeta(sub);
                root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            }
        });
        ptsBox.getChildren().addAll(lblPts, tfPoints);

        // Difficulty
        VBox diffBox = new VBox(4);
        Label lblDiff = new Label("Difficulty");
        ComboBox<Difficulty> cbDiff = new ComboBox<>();
        cbDiff.getItems().setAll(Difficulty.values());
        cbDiff.getSelectionModel().select(sub.getDifficulty());
        cbDiff.setMaxWidth(Double.MAX_VALUE);
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
            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
        });
        diffBox.getChildren().addAll(lblDiff, cbDiff);

        // Eligibility
        VBox eligBox = new VBox(4);
        Label lblElig = new Label("Eligibility");
        ComboBox<Eligibility> cbElig = new ComboBox<>();
        cbElig.getItems().setAll(Eligibility.values());
        cbElig.getSelectionModel().select(sub.getEligibility());
        cbElig.setMaxWidth(Double.MAX_VALUE);
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
            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
        });
        eligBox.getChildren().addAll(lblElig, cbElig);

        meta.add(ptsBox, 0, 0);
        meta.add(diffBox, 1, 0);
        meta.add(eligBox, 2, 0);

        // Variants (editable)
        VBox variantsBox = new VBox(12);
        for (VariantModel v : sub.getVariants()) {
            VBox vCard = new VBox(8);
            vCard.getStyleClass().add("card");
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
            root.getSlideOver().hide();
            rootStackMouseTransparent(root, true);
        });

        sheet.getChildren().addAll(
                title,
                new Label("Subtask title"), tfSubtaskTitle,
                new Separator(),
                meta,
                new Separator(),
                new Label("Variants"), variantsBox,
                btnClose
        );

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        rootStackMouseTransparent(root, false);
    }

    private static void rootStackMouseTransparent(HomeController root, boolean v) {
        root.rootStack.setMouseTransparent(v);
    }
}