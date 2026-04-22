package simon.klausurcraft.ui.home;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.ui.UiStyles;

import java.util.List;
import java.util.Optional;

/**
 * Sidebar "Contents" with strongly typed nodes to support edit/delete via context menu and Delete key.
 */
public class HomeSidebarController {

    private HomeController root;

    @FXML private TreeView<TocNode> tocTree;
    @FXML private Button btnLoad;
    @FXML private Button btnNewXml;

    public void init(HomeController root) {
        this.root = root;
        btnLoad.setOnAction(e -> HomeFileController.chooseAndLoadXml(root));
        btnLoad.setTooltip(new Tooltip("Open an existing XML task file."));
        if (btnNewXml != null) {
            btnNewXml.setOnAction(e -> HomeFileController.createNewXml(root));
            btnNewXml.setTooltip(new Tooltip("Create a new empty XML task file."));
        }
        tocTree.setTooltip(new Tooltip("Contents tree. Click to jump to task/subtask."));

        // Render cell text
        tocTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(TocNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(item.label());
                    setContextMenu(buildContextMenu(item));
                }
            }
        });

        // Delete key handling (only in tree)
        tocTree.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.DELETE) {
                TreeItem<TocNode> sel = tocTree.getSelectionModel().getSelectedItem();
                if (sel != null && sel.getValue() != null) {
                    handleDelete(sel.getValue());
                    ev.consume();
                }
            }
        });

        // Single click selection should jump to the corresponding position on the right.
        tocTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel == null || newSel.getValue() == null) return;
            navigateToNode(newSel.getValue());
        });

        // Also react on plain single-clicks so repeated clicks on the same selected item still jump.
        tocTree.setOnMouseClicked(ev -> {
            if (ev.getClickCount() != 1) return;
            TreeItem<TocNode> sel = tocTree.getSelectionModel().getSelectedItem();
            if (sel != null && sel.getValue() != null) {
                navigateToNode(sel.getValue());
            }
        });
    }

    public void rebuildToc(List<Task> tasks) {
        TreeItem<TocNode> rootItem = new TreeItem<>(new TocNode(TocNode.Type.ROOT, null, null, "Contents"));
        rootItem.setExpanded(true);

        for (Task t : tasks) {
            TreeItem<TocNode> taskNode = new TreeItem<>(TocNode.forTask(t));
            for (Subtask st : t.getSubtasks()) {
                String name = root.getTaskRepository().readSubtaskGroup(st);
                if (name == null || name.isBlank()) {
                    name = "Subtask " + t.getId() + "." + st.getId();
                }
                taskNode.getChildren().add(new TreeItem<>(TocNode.forSubtask(t, st, name)));
            }
            taskNode.setExpanded(false);
            rootItem.getChildren().add(taskNode);
        }

        tocTree.setRoot(rootItem);
        tocTree.setShowRoot(false);
    }

    private ContextMenu buildContextMenu(TocNode node) {
        ContextMenu menu = new ContextMenu();
        switch (node.type()) {
            case TASK -> {
                MenuItem open = new MenuItem("Scroll to task");
                open.setOnAction(e -> root.centerController.scrollToTask(node.task()));
                MenuItem addSub = new MenuItem("Add subtask");
                addSub.setOnAction(e -> {
                    // Create and immediately open the subtask editor (slide-over) with title focused
                    root.getTaskRepository().addSubtask(node.task()).ifPresent(newSub -> {
                        root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                        rebuildToc(root.getTasks());
                        HomeSubtaskSheet.open(root, node.task(), newSub); // focuses title field inside
                    });
                });
                MenuItem edit = new MenuItem("Edit task…");
                edit.setOnAction(e -> HomeTaskSheet.openEdit(root, node.task()));
                MenuItem del = new MenuItem("Delete task");
                del.setOnAction(e -> confirmDeleteTask(node.task()));
                menu.getItems().addAll(open, addSub, edit, new SeparatorMenuItem(), del);
            }
            case SUBTASK -> {
                MenuItem open = new MenuItem("Open details");
                open.setOnAction(e -> HomeSubtaskSheet.open(root, node.task(), node.subtask()));
                MenuItem del = new MenuItem("Delete subtask");
                del.setOnAction(e -> confirmDeleteSubtask(node.task(), node.subtask()));
                menu.getItems().addAll(open, new SeparatorMenuItem(), del);
            }
            default -> {}
        }
        return menu;
    }

    private void handleDelete(TocNode node) {
        switch (node.type()) {
            case TASK -> confirmDeleteTask(node.task()); 
            case SUBTASK -> confirmDeleteSubtask(node.task(), node.subtask());
            default -> {}
        }
    }

    private void navigateToNode(TocNode node) {
        if (node == null) return;
        Platform.runLater(() -> {
            switch (node.type()) {
                case TASK -> root.centerController.scrollToTask(node.task());
                case SUBTASK -> root.centerController.scrollToSubtask(node.task(), node.subtask());
                default -> { }
            }
        });
    }

    private void confirmDeleteTask(Task t) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete task");
        a.setHeaderText("Delete this task?");
        a.setContentText("This will delete the task and all its subtasks and variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiStyles.applyCurrentStyles(a);
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (root.getTaskRepository().deleteTask(t)) {
                root.getTasks().remove(t);
            } else {
                HomeNotifications.showError("Failed to delete task.");
            }
        }
    }

    private void confirmDeleteSubtask(Task task, Subtask st) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete subtask");
        a.setHeaderText("Delete this subtask?");
        a.setContentText("This will delete the subtask including all its variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiStyles.applyCurrentStyles(a);
        Optional<ButtonType> res = a.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            if (root.getTaskRepository().deleteSubtask(task, st)) {
                task.getSubtasks().remove(st);
                root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                rebuildToc(root.getTasks());
            } else {
                HomeNotifications.showError("Failed to delete subtask.");
            }
        }
    }

    /** Node descriptor for TreeView. */
    public static final class TocNode {
        enum Type { ROOT, TASK, SUBTASK }
        private final Type type;
        private final Task task;
        private final Subtask subtask;
        private final String label;

        private TocNode(Type type, Task task, Subtask subtask, String label) {
            this.type = type; this.task = task; this.subtask = subtask; this.label = label;
        }
        static TocNode forTask(Task t) { return new TocNode(Type.TASK, t, null, String.format("%s — %s", t.getId(), t.getTitle())); }
        static TocNode forSubtask(Task t, Subtask st, String label) { return new TocNode(Type.SUBTASK, t, st, "• " + label); }

        public Type type() { return type; }
        public Task task() { return task; }
        public Subtask subtask() { return subtask; }
        public String label() { return label; }
    }
}
