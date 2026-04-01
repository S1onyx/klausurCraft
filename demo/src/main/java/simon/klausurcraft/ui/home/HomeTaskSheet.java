package simon.klausurcraft.ui.home;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import simon.klausurcraft.task.Task;

import simon.klausurcraft.ui.UiStyles;

import java.util.Optional;

/**
 * Slide-over for editing/creating tasks (title only) and helper for prompting new task title.
 */
final class HomeTaskSheet {

    private HomeTaskSheet(){}

    /** Open slide-over to edit a task's title. */
    static void openEdit(HomeController root, Task task) {
        BorderPane sheet = new BorderPane();
        sheet.setPadding(new Insets(0));

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));

        Label header = new Label("Edit task");
        header.getStyleClass().add("header");

        TextField tfTitle = new TextField(task.getTitle());
        tfTitle.setPromptText("Task title");
        tfTitle.setMaxWidth(Double.MAX_VALUE);
        tfTitle.textProperty().addListener((o, ov, nv) -> {
            String newTitle = nv == null ? "" : nv.trim();
            if (newTitle.isEmpty()) newTitle = "New Task";
            if (newTitle.equals(task.getTitle())) return;
            task.setTitle(newTitle);
            root.getTaskRepository().updateTaskTitle(task);
            root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            root.rebuildToc();
        });

        content.getChildren().addAll(header, new Label("Title"), tfTitle);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sheet.setCenter(sp);

        HBox actions = new HBox(8);
        actions.getStyleClass().add("sheet-footer");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("chip");
        btnClose.setCancelButton(true);
        btnClose.setOnAction(e -> {
            root.getSlideOver().hide();
            root.rootStack.setMouseTransparent(true);
        });

        actions.getChildren().addAll(spacer, btnClose);
        sheet.setBottom(actions);

        root.getSlideOver().setContent(sheet);
        root.getSlideOver().show();
        root.rootStack.setMouseTransparent(false);

        // Autofocus title
        tfTitle.requestFocus();
    }

    /**
     * Prompt for a new task (title) and create it via TaskXmlStore.
     * Returns created Task on success, otherwise shows an English error banner.
     */
    static Optional<Task> promptNewTask(HomeController root) {
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
        UiStyles.applyCurrentStyles(dlg);

        Optional<String> res = dlg.showAndWait();
        if (res.isPresent()) {
            String title = res.get().trim();
            if (title.isEmpty()) title = "New Task";

            // Attempt to create task in the currently loaded XML
            Optional<Task> created = root.getTaskRepository().addTask(title);
            if (created.isEmpty()) {
                // Most likely no XML loaded yet (TaskXmlStore.doc == null)
                HomeNotifications.showError("No XML file is open. Please open an XML file first.");
                return Optional.empty();
            }

            return created;
        }
        return Optional.empty();
    }
}
