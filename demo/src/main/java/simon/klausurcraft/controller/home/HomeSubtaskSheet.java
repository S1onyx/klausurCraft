package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import simon.klausurcraft.model.*;
import simon.klausurcraft.services.XmlService;
import simon.klausurcraft.utils.UiUtil;

import java.math.BigDecimal;

final class HomeSubtaskSheet {

    private HomeSubtaskSheet(){}

    static void open(HomeController root, TaskModel task, SubtaskModel sub) {
        // Root layout with sticky footer
        BorderPane sheet = new BorderPane();
        sheet.setPadding(new Insets(0));

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        Label title = new Label("Subtask " + task.getId() + "." + sub.getId());
        title.getStyleClass().add("header");

        XmlService xmlService = root.getXmlService();

        // Subtask "title" from variants@group
        String currentGroup = xmlService.readSubtaskGroup(sub);
        TextField tfSubtaskTitle = new TextField(currentGroup);
        tfSubtaskTitle.setPromptText("Subtask title (variants@group)");
        tfSubtaskTitle.setMaxWidth(Double.MAX_VALUE);
        tfSubtaskTitle.textProperty().addListener((o, ov, nv) -> {
            xmlService.updateSubtaskGroup(sub, nv == null ? "" : nv);
            // live refresh center + tree
            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            root.rebuildToc();
        });

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
        tfPoints.setPromptText("Integer (≥ 0)");
        Label lblPtsError = new Label();
        lblPtsError.getStyleClass().add("field-error"); // styled via existing CSS theme
        lblPtsError.setManaged(false);
        lblPtsError.setVisible(false);

        tfPoints.textProperty().addListener((o, ov, nv) -> {
            String s = nv == null ? "" : nv.trim();
            boolean ok = s.matches("\\d+");
            if (ok) {
                try {
                    sub.setPoints(new BigDecimal(s));
                    xmlService.updateSubtaskMeta(sub);
                    root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                    lblPtsError.setManaged(false);
                    lblPtsError.setVisible(false);
                    tfPoints.getStyleClass().remove("field-error-border");
                } catch (Exception ex) {
                    lblPtsError.setText("Failed to save points: " + ex.getMessage());
                    lblPtsError.setManaged(true);
                    lblPtsError.setVisible(true);
                    if (!tfPoints.getStyleClass().contains("field-error-border")) {
                        tfPoints.getStyleClass().add("field-error-border");
                    }
                }
            } else {
                lblPtsError.setText("Please enter a non-negative integer.");
                lblPtsError.setManaged(true);
                lblPtsError.setVisible(true);
                if (!tfPoints.getStyleClass().contains("field-error-border")) {
                    tfPoints.getStyleClass().add("field-error-border");
                }
            }
        });
        ptsBox.getChildren().addAll(lblPts, tfPoints, lblPtsError);

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

        // Variants
        VBox variantsBox = new VBox(12);

        // Header row for variants with "+ Variant" and "Delete subtask"
        HBox variantsHeader = new HBox(8);
        Label vTitle = new Label("Variants");
        Region vSpacer = new Region(); HBox.setHgrow(vSpacer, Priority.ALWAYS);
        Button btnAddVariant = new Button("+ Variant");
        btnAddVariant.getStyleClass().add("chip");
        btnAddVariant.setOnAction(e -> {
            xmlService.addVariant(sub).ifPresent(v -> {
                open(root, task, sub); // refresh
                root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            });
        });

        Button btnDeleteSubtask = new Button("Delete subtask");
        btnDeleteSubtask.getStyleClass().add("chip");
        btnDeleteSubtask.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Delete subtask");
            a.setHeaderText("Delete this subtask?");
            a.setContentText("This will delete the subtask including all its variants. This action cannot be undone.");
            a.initOwner(root.getWindow());
            UiUtil.applyCurrentStyles(a);
            a.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    if (root.getXmlService().deleteSubtask(task, sub)) {
                        task.getSubtasks().remove(sub);
                        root.getSlideOver().hide();
                        root.rootStack.setMouseTransparent(true);
                        root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                        root.rebuildToc();
                    } else {
                        HomeNotifications.showError("Failed to delete subtask.");
                    }
                }
            });
        });

        variantsHeader.getChildren().addAll(vTitle, vSpacer, btnAddVariant, btnDeleteSubtask);
        variantsBox.getChildren().add(variantsHeader);

        for (VariantModel v : sub.getVariants()) {
            VBox vCard = new VBox(8);
            vCard.getStyleClass().add("card");
            vCard.setPadding(new Insets(12));

            HBox vHeader = new HBox(8);
            Label vHeaderLbl = new Label("Variant " + v.getId());
            vHeaderLbl.getStyleClass().add("header");
            Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            Button btnDelVar = new Button("Delete");
            btnDelVar.getStyleClass().add("chip");
            btnDelVar.setOnAction(e -> {
                if (sub.getVariants().size() <= 1) {
                    HomeNotifications.showError("Cannot delete the last variant of a subtask.");
                    return;
                }
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Delete variant");
                a.setHeaderText("Delete this variant?");
                a.setContentText("This action cannot be undone.");
                a.initOwner(root.getWindow());
                UiUtil.applyCurrentStyles(a);
                a.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        if (root.getXmlService().deleteVariant(sub, v)) {
                            sub.getVariants().remove(v);
                            open(root, task, sub); // refresh sheet content
                            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                        } else {
                            HomeNotifications.showError("Failed to delete variant.");
                        }
                    }
                });
            });

            vHeader.getChildren().addAll(vHeaderLbl, spacer, btnDelVar);

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

        // Build scrollable center and sticky footer
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sheet.setCenter(sp);

        HBox footer = new HBox(8);
        footer.getStyleClass().add("sheet-footer");
        Region footSpacer = new Region(); HBox.setHgrow(footSpacer, Priority.ALWAYS);
        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("chip");
        btnClose.setCancelButton(true);
        btnClose.setOnAction(e -> {
            root.getSlideOver().hide();
            rootStackMouseTransparent(root, true);
        });
        footer.getChildren().addAll(footSpacer, btnClose);
        sheet.setBottom(footer);

        // assemble content
        content.getChildren().addAll(
                title,
                new Label("Subtask title"), tfSubtaskTitle,
                new Separator(),
                meta,
                new Separator(),
                variantsBox
        );

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        rootStackMouseTransparent(root, false);

        // Autofocus subtask title
        tfSubtaskTitle.requestFocus();
    }

    private static void rootStackMouseTransparent(HomeController root, boolean v) {
        root.rootStack.setMouseTransparent(v);
    }
}