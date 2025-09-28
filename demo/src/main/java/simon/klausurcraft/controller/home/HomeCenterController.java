package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.*;
import simon.klausurcraft.model.Difficulty;
import simon.klausurcraft.model.Eligibility;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;
import simon.klausurcraft.utils.UiUtil;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class HomeCenterController {

    private HomeController root;

    @FXML private ScrollPane centerScroll;
    @FXML private VBox centerContainer;

    public void init(HomeController root) {
        this.root = root;
    }

    public void render(List<TaskModel> tasks, String query, Set<Difficulty> allowed) {
        centerContainer.getChildren().clear();
        String q = query == null ? "" : query;

        for (TaskModel t : tasks) {
            boolean taskMatches = matchesTask(t, q);

            VBox taskCard = makeCard();
            taskCard.setUserData(formatTaskTitle(t));

            // Header row with title + actions
            HBox headerRow = new HBox(8);
            Label header = new Label("Task " + t.getId() + " — " + t.getTitle());
            header.getStyleClass().add("header");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnEdit = new Button("Edit");
            btnEdit.getStyleClass().add("chip");
            btnEdit.setOnAction(e -> HomeTaskSheet.openEdit(root, t));

            Button btnAddSub = new Button("+Subtask");
            btnAddSub.getStyleClass().add("chip");
            btnAddSub.setOnAction(e -> {
                root.getXmlService().addSubtask(t).ifPresent(newSub -> {
                    root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                    HomeSubtaskSheet.open(root, t, newSub);
                });
            });

            Button btnDeleteTask = new Button("Delete");
            btnDeleteTask.getStyleClass().add("chip");
            btnDeleteTask.setOnAction(e -> tryDeleteTask(t));

            headerRow.getChildren().addAll(header, spacer, btnAddSub, btnEdit, btnDeleteTask);
            taskCard.getChildren().add(headerRow);

            // Context menu on task card (right-click)
            ContextMenu taskMenu = new ContextMenu();
            MenuItem miEdit = new MenuItem("Edit task…");
            miEdit.setOnAction(e -> HomeTaskSheet.openEdit(root, t));
            MenuItem miAdd = new MenuItem("Add subtask");
            miAdd.setOnAction(e -> {
                root.getXmlService().addSubtask(t);
                root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            });
            MenuItem miDel = new MenuItem("Delete task");
            miDel.setOnAction(e -> tryDeleteTask(t));
            taskMenu.getItems().addAll(miEdit, miAdd, new SeparatorMenuItem(), miDel);
            taskCard.setOnContextMenuRequested((ContextMenuEvent ev) -> taskMenu.show(taskCard, ev.getScreenX(), ev.getScreenY()));

            for (SubtaskModel st : t.getSubtasks()) {
                if (!allowed.contains(st.getDifficulty())) continue;
                boolean subMatches = taskMatches || matchesSubtask(st, q);
                if (!subMatches && !q.isEmpty()) continue;

                String subName = root.getXmlService().readSubtaskGroup(st);
                if (subName == null || subName.isBlank()) {
                    subName = "Subtask " + t.getId() + "." + st.getId();
                }

                HBox row = new HBox(10);
                row.setPadding(new Insets(6, 0, 6, 0));

                Label lblTitle = new Label(subName);
                lblTitle.getStyleClass().add("muted");

                Label bPts  = badge(st.getPoints().stripTrailingZeros().toPlainString() + " pts");
                Label bDiff = badgeForDifficulty(st.getDifficulty());
                Label bElig = badgeForEligibility(st.getEligibility());

                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);

                Button btnOpen = new Button("Details");
                btnOpen.getStyleClass().add("chip");
                btnOpen.setOnAction(e -> HomeSubtaskSheet.open(root, t, st));

                // Context menu on subtask row
                ContextMenu subMenu = new ContextMenu();
                MenuItem miOpen = new MenuItem("Open details");
                miOpen.setOnAction(e -> HomeSubtaskSheet.open(root, t, st));
                MenuItem miDelete = new MenuItem("Delete subtask");
                miDelete.setOnAction(e -> tryDeleteSubtask(t, st));
                subMenu.getItems().addAll(miOpen, new SeparatorMenuItem(), miDelete);
                row.setOnContextMenuRequested(ev -> subMenu.show(row, ev.getScreenX(), ev.getScreenY()));

                row.getChildren().addAll(lblTitle, bPts, bDiff, bElig, spacer2, btnOpen);
                taskCard.getChildren().add(row);
            }

            centerContainer.getChildren().add(taskCard);
        }
    }

    private void tryDeleteTask(TaskModel t) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete task");
        a.setHeaderText("Delete this task?");
        a.setContentText("This will delete the task and all its subtasks and variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiUtil.applyCurrentStyles(a);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (root.getXmlService().deleteTask(t)) {
                    root.getTasks().remove(t);
                } else {
                    HomeNotifications.showError("Failed to delete task.");
                }
            }
        });
    }

    private void tryDeleteSubtask(TaskModel task, SubtaskModel st) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete subtask");
        a.setHeaderText("Delete this subtask?");
        a.setContentText("This will delete the subtask including all its variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiUtil.applyCurrentStyles(a);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (root.getXmlService().deleteSubtask(task, st)) {
                    task.getSubtasks().remove(st);
                    render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                } else {
                    HomeNotifications.showError("Failed to delete subtask.");
                }
            }
        });
    }

    public void scrollToLabel(String label) {
        for (Node n : centerContainer.getChildren()) {
            if (Objects.equals(n.getUserData(), label) || (n.getUserData() instanceof String s && label.contains(s))) {
                n.requestFocus();
                centerScroll.setVvalue(n.getLayoutY() / Math.max(1, centerContainer.getHeight()));
                break;
            }
        }
    }

    // helpers

    private VBox makeCard() {
        VBox box = new VBox(8);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(16));
        return box;
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

    private boolean matchesTask(TaskModel t, String q) {
        if (q.isEmpty()) return true;
        if (t.getId().toLowerCase().contains(q)) return true;
        if (t.getTitle().toLowerCase().contains(q)) return true;
        for (SubtaskModel st : t.getSubtasks()) if (matchesSubtask(st, q)) return true;
        return false;
    }

    private boolean matchesSubtask(SubtaskModel st, String q) {
        if (q.isEmpty()) return true;
        if (st.getId().toLowerCase().contains(q)) return true;
        return st.getVariants().stream().anyMatch(v ->
            (v.getText() != null && v.getText().toLowerCase().contains(q)) ||
            (v.getSolution() != null && v.getSolution().toLowerCase().contains(q)) ||
            (root.getXmlService().readSubtaskGroup(st) != null &&
             root.getXmlService().readSubtaskGroup(st).toLowerCase().contains(q))
        );
    }

    private String formatTaskTitle(TaskModel t) {
        return String.format("%s — %s", t.getId(), t.getTitle());
    }
}