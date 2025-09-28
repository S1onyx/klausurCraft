package simon.klausurcraft.controller.home;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import simon.klausurcraft.model.TaskModel;

import java.util.Optional;

/**
 * Slide-over for editing/creating tasks (title only) and helper for prompting new task title.
 */
final class HomeTaskSheet {

    private HomeTaskSheet(){}

    /** Open slide-over to edit a task's title. */
    static void openEdit(HomeController root, TaskModel task) {
        BorderPane sheet = new BorderPane();
        sheet.setPadding(new Insets(0));

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        Label header = new Label("Edit task");
        header.getStyleClass().add("header");

        TextField tfTitle = new TextField(task.getTitle());
        tfTitle.setPromptText("Task title");
        tfTitle.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(header, new Label("Title"), tfTitle);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sheet.setCenter(sp);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("chip");
        btnCancel.setCancelButton(true);
        btnCancel.setOnAction(e -> {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("primary");
        btnSave.setDefaultButton(true);
        btnSave.setOnAction(e -> {
            String newTitle = tfTitle.getText() == null ? "" : tfTitle.getText().trim();
            if (newTitle.isEmpty()) newTitle = "New Task";
            task.setTitle(newTitle);
            root.getXmlService().updateTaskTitle(task);
            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            root.rebuildToc();
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        actions.getChildren().addAll(spacer, btnCancel, btnSave);
        sheet.setBottom(actions);

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);

        // Autofocus title
        tfTitle.requestFocus();
    }

    /**
     * Prompt for a new task (title) and create it via XmlService.
     * Returns created TaskModel on success, otherwise shows an English error banner.
     */
    static Optional<TaskModel> promptNewTask(HomeController root) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("+ New task");
        dlg.setHeaderText("Create a new task");
        dlg.setContentText("Title:");
        // Make Enter/Escape work even on older JavaFX versions (null-safe)
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (okBtn != null) okBtn.setDefaultButton(true);
        Button cancelBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) cancelBtn.setCancelButton(true);

        dlg.initOwner(root.getWindow());
        // Apply app styles to dialog (dark mode fix)
        simon.klausurcraft.utils.UiUtil.applyCurrentStyles(dlg);

        Optional<String> res = dlg.showAndWait();
        if (res.isPresent()) {
            String title = res.get().trim();
            if (title.isEmpty()) title = "New Task";

            // Attempt to create task in the currently loaded XML
            Optional<TaskModel> created = root.getXmlService().addTask(title);
            if (created.isEmpty()) {
                // Most likely no XML loaded yet (XmlService.doc == null)
                HomeNotifications.showError("No XML file is open. Please open an XML file first.");
                return Optional.empty();
            }

            return created;
        }
        return Optional.empty();
    }
}