package simon.klausurcraft.ui.home;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import simon.klausurcraft.task.Difficulty;
import simon.klausurcraft.task.Eligibility;
import simon.klausurcraft.task.Points;
import simon.klausurcraft.task.Subtask;
import simon.klausurcraft.task.Task;
import simon.klausurcraft.ui.UiStyles;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HomeCenterController {

    private HomeController root;
    private final Map<String, VBox> taskCardByTaskId = new HashMap<>();
    private final Map<String, VBox> subtaskContainerByTaskId = new HashMap<>();
    private final Map<String, Button> toggleButtonByTaskId = new HashMap<>();
    private final Map<String, HBox> subtaskRowById = new HashMap<>();
    private final Map<String, Boolean> taskExpandedById = new HashMap<>();

    @FXML private ScrollPane centerScroll;
    @FXML private VBox centerContainer;

    public void init(HomeController root) {
        this.root = root;
    }

    /** Reset to default expansion behavior (all tasks expanded) e.g. when loading a new file. */
    public void resetExpansionState() {
        taskExpandedById.clear();
    }

    public void render(List<Task> tasks, String query, Set<Difficulty> allowed) {
        root.updateCounts();
        centerContainer.getChildren().clear();
        taskCardByTaskId.clear();
        subtaskContainerByTaskId.clear();
        toggleButtonByTaskId.clear();
        subtaskRowById.clear();
        String q = query == null ? "" : query;
        boolean hasQuery = !q.isEmpty();

        HBox taskControls = new HBox(8);
        taskControls.setPadding(new Insets(0, 0, 4, 0));
        Button btnExpandAll = new Button("Expand all");
        btnExpandAll.getStyleClass().add("chip");
        btnExpandAll.setTooltip(new Tooltip("Expand all task cards."));
        btnExpandAll.setOnAction(e -> expandAllTasks(tasks));
        Button btnCollapseAll = new Button("Collapse all");
        btnCollapseAll.getStyleClass().add("chip");
        btnCollapseAll.setTooltip(new Tooltip("Collapse all task cards."));
        btnCollapseAll.setOnAction(e -> collapseAllTasks(tasks));
        taskControls.getChildren().addAll(btnExpandAll, btnCollapseAll);
        centerContainer.getChildren().add(taskControls);

        for (Task t : tasks) {
            boolean taskHeaderMatches = HomeSearchService.matchesTaskHeader(t, q);
            List<Subtask> visibleSubtasks = new ArrayList<>();
            List<Subtask> highlightedSubtasks = new ArrayList<>();

            for (Subtask st : t.getSubtasks()) {
                if (!allowed.contains(st.getDifficulty())) continue;

                String subGroup = root.getTaskRepository().readSubtaskGroup(st);
                boolean subtaskMatches = HomeSearchService.matchesSubtask(st, subGroup, q);

                if (!hasQuery || taskHeaderMatches || subtaskMatches) {
                    visibleSubtasks.add(st);
                }
                if (hasQuery && subtaskMatches) {
                    highlightedSubtasks.add(st);
                }
            }

            if (hasQuery && !taskHeaderMatches && visibleSubtasks.isEmpty()) {
                continue;
            }

            VBox taskCard = makeCard();
            taskCard.setUserData(HomeSearchService.formatTaskTitle(t));
            taskCardByTaskId.put(taskKey(t), taskCard);

            // Header row with title + actions
            HBox headerRow = new HBox(8);
            boolean expanded = isTaskExpanded(t);
            Button btnToggle = new Button(expanded ? "▾" : "▸");
            btnToggle.getStyleClass().addAll("chip", "task-toggle");
            btnToggle.setTooltip(new Tooltip("Expand or collapse this task."));
            btnToggle.setOnAction(e -> setTaskExpanded(t, !isTaskExpanded(t)));

            Label header = new Label("Task " + t.getId() + " — " + t.getTitle());
            header.getStyleClass().add("header");
            if (hasQuery && taskHeaderMatches) {
                header.getStyleClass().add("search-hit-task-title");
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnEdit = new Button("Edit");
            btnEdit.getStyleClass().add("chip");
            btnEdit.setTooltip(new Tooltip("Edit task title."));
            btnEdit.setOnAction(e -> HomeTaskSheet.openEdit(root, t));

            Button btnAddSub = new Button("+Subtask");
            btnAddSub.getStyleClass().add("chip");
            btnAddSub.setTooltip(new Tooltip("Add a new subtask to this task."));
            btnAddSub.setOnAction(e -> {
                root.getTaskRepository().addSubtask(t).ifPresent(newSub -> {
                    root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                    HomeSubtaskSheet.open(root, t, newSub);
                });
            });

            Button btnDeleteTask = new Button("Delete");
            btnDeleteTask.getStyleClass().addAll("chip", "danger");
            btnDeleteTask.setTooltip(new Tooltip("Delete this task with all subtasks and variants."));
            btnDeleteTask.setOnAction(e -> tryDeleteTask(t));

            headerRow.getChildren().addAll(btnToggle, header, spacer, btnAddSub, btnEdit, btnDeleteTask);
            taskCard.getChildren().add(headerRow);
            toggleButtonByTaskId.put(taskKey(t), btnToggle);

            headerRow.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
                if (ev.getButton() != MouseButton.PRIMARY || ev.getClickCount() != 1) return;
                if (isDescendantOf(ev.getPickResult().getIntersectedNode(), btnToggle)) return;
                if (isDescendantOf(ev.getPickResult().getIntersectedNode(), btnAddSub)) return;
                if (isDescendantOf(ev.getPickResult().getIntersectedNode(), btnEdit)) return;
                if (isDescendantOf(ev.getPickResult().getIntersectedNode(), btnDeleteTask)) return;
                setTaskExpanded(t, !isTaskExpanded(t));
            });

            // Context menu on task card (right-click)
            ContextMenu taskMenu = new ContextMenu();
            MenuItem miEdit = new MenuItem("Edit task…");
            miEdit.setOnAction(e -> HomeTaskSheet.openEdit(root, t));
            MenuItem miAdd = new MenuItem("Add subtask");
            miAdd.setOnAction(e -> {
                root.getTaskRepository().addSubtask(t);
                root.centerController.render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            });
            MenuItem miDel = new MenuItem("Delete task");
            miDel.setOnAction(e -> tryDeleteTask(t));
            taskMenu.getItems().addAll(miEdit, miAdd, new SeparatorMenuItem(), miDel);
            taskCard.setOnContextMenuRequested((ContextMenuEvent ev) -> taskMenu.show(taskCard, ev.getScreenX(), ev.getScreenY()));

            VBox subtaskRowsBox = new VBox(0);
            subtaskContainerByTaskId.put(taskKey(t), subtaskRowsBox);
            subtaskRowsBox.setManaged(expanded);
            subtaskRowsBox.setVisible(expanded);

            for (Subtask st : visibleSubtasks) {
                String subName = root.getTaskRepository().readSubtaskGroup(st);
                if (subName == null || subName.isBlank()) {
                    subName = "Subtask " + t.getId() + "." + st.getId();
                }
                boolean isHighlightedSubtask = hasQuery && highlightedSubtasks.contains(st);

                HBox row = new HBox(10);
                subtaskRowById.put(subtaskKey(t, st), row);
                row.setUserData(subtaskKey(t, st));
                row.setPadding(new Insets(6, 0, 6, 0));
                row.getStyleClass().add("subtask-row-clickable");
                if (isHighlightedSubtask) {
                    row.getStyleClass().add("search-hit-subtask-row");
                }

                Label lblTitle = new Label(subName);
                lblTitle.getStyleClass().add("muted");
                if (isHighlightedSubtask) {
                    lblTitle.getStyleClass().add("search-hit-subtask-title");
                }

                Label bPts  = badge(Points.toDisplayString(st.getPoints()) + " pts");
                Label bDiff = badgeForDifficulty(st.getDifficulty());
                Label bElig = badgeForEligibility(st.getEligibility());
                Label bHit = null;
                if (isHighlightedSubtask) {
                    bHit = badge("match");
                    bHit.getStyleClass().add("badge-search-hit");
                }

                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);

                Button btnOpen = new Button("Details");
                btnOpen.getStyleClass().add("chip");
                btnOpen.setTooltip(new Tooltip("Open subtask details and variants."));
                btnOpen.setOnAction(e -> HomeSubtaskSheet.open(root, t, st));

                row.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
                    if (ev.getButton() != MouseButton.PRIMARY || ev.getClickCount() != 1) return;
                    if (isDescendantOf(ev.getPickResult().getIntersectedNode(), btnOpen)) return;
                    HomeSubtaskSheet.open(root, t, st);
                });

                // Context menu on subtask row
                ContextMenu subMenu = new ContextMenu();
                MenuItem miOpen = new MenuItem("Open details");
                miOpen.setOnAction(e -> HomeSubtaskSheet.open(root, t, st));
                MenuItem miDelete = new MenuItem("Delete subtask");
                miDelete.setOnAction(e -> tryDeleteSubtask(t, st));
                subMenu.getItems().addAll(miOpen, new SeparatorMenuItem(), miDelete);
                row.setOnContextMenuRequested(ev -> subMenu.show(row, ev.getScreenX(), ev.getScreenY()));

                if (bHit != null) {
                    row.getChildren().addAll(lblTitle, bHit, bPts, bDiff, bElig, spacer2, btnOpen);
                } else {
                    row.getChildren().addAll(lblTitle, bPts, bDiff, bElig, spacer2, btnOpen);
                }
                subtaskRowsBox.getChildren().add(row);
            }
            taskCard.getChildren().add(subtaskRowsBox);

            if (!hasQuery || taskHeaderMatches || !visibleSubtasks.isEmpty()) {
                centerContainer.getChildren().add(taskCard);
            }
        }
    }

    private void tryDeleteTask(Task t) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete task");
        a.setHeaderText("Delete this task?");
        a.setContentText("This will delete the task and all its subtasks and variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiStyles.applyCurrentStyles(a);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (root.getTaskRepository().deleteTask(t)) {
                    root.getTasks().remove(t);
                } else {
                    HomeNotifications.showError("Failed to delete task.");
                }
            }
        });
    }

    private void tryDeleteSubtask(Task task, Subtask st) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete subtask");
        a.setHeaderText("Delete this subtask?");
        a.setContentText("This will delete the subtask including all its variants. This action cannot be undone.");
        a.initOwner(root.getWindow());
        UiStyles.applyCurrentStyles(a);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (root.getTaskRepository().deleteSubtask(task, st)) {
                    task.getSubtasks().remove(st);
                    render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
                } else {
                    HomeNotifications.showError("Failed to delete subtask.");
                }
            }
        });
    }

    public void scrollToTask(Task task) {
        if (task == null) return;
        setTaskExpanded(task, true);
        Node card = taskCardByTaskId.get(taskKey(task));
        if (card != null) {
            scrollNodeIntoView(card);
            Platform.runLater(() -> scrollNodeIntoView(card));
        }
    }

    public void scrollToSubtask(Task task, Subtask subtask) {
        if (task == null || subtask == null) return;
        setTaskExpanded(task, true);
        String key = subtaskKey(task, subtask);
        Node row = subtaskRowById.get(key);
        if (row == null) {
            row = findNodeByUserData(centerContainer, key);
        }
        if (row == null) {
            // In case maps are stale after intermediate UI updates, rebuild once and retry.
            render(root.getTasks(), root.currentQuery(), root.allowedDifficulties());
            setTaskExpanded(task, true);
            row = subtaskRowById.get(key);
            if (row == null) {
                row = findNodeByUserData(centerContainer, key);
            }
        }
        if (row == null) {
            // Last fallback: render unfiltered so the clicked tree target is guaranteed visible.
            render(root.getTasks(), "", java.util.EnumSet.allOf(Difficulty.class));
            setTaskExpanded(task, true);
            row = subtaskRowById.get(key);
            if (row == null) {
                row = findNodeByUserData(centerContainer, key);
            }
        }
        if (row != null) {
            scrollNodeIntoView(row);
            Node rowFinal = row;
            Platform.runLater(() -> scrollNodeIntoView(rowFinal));
        } else {
            scrollToTask(task);
        }
    }

    public void scrollToLabel(String label) {
        for (Task task : root.getTasks()) {
            String taskTitle = HomeSearchService.formatTaskTitle(task);
            if (Objects.equals(label, taskTitle) || (label != null && label.contains(taskTitle))) {
                scrollToTask(task);
                return;
            }
            for (Subtask st : task.getSubtasks()) {
                String subName = root.getTaskRepository().readSubtaskGroup(st);
                if (subName == null || subName.isBlank()) {
                    subName = "Subtask " + task.getId() + "." + st.getId();
                }
                String treeLabel = "• " + subName;
                if (Objects.equals(label, treeLabel) || (label != null && label.contains(subName))) {
                    scrollToSubtask(task, st);
                    return;
                }
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

    private boolean isTaskExpanded(Task task) {
        return taskExpandedById.getOrDefault(taskKey(task), true);
    }

    private void setTaskExpanded(Task task, boolean expanded) {
        String taskKey = taskKey(task);
        taskExpandedById.put(taskKey, expanded);
        VBox box = subtaskContainerByTaskId.get(taskKey);
        if (box != null) {
            box.setManaged(expanded);
            box.setVisible(expanded);
        }
        Button toggle = toggleButtonByTaskId.get(taskKey);
        if (toggle != null) {
            toggle.setText(expanded ? "▾" : "▸");
        }
    }

    private void expandAllTasks(List<Task> tasks) {
        for (Task t : tasks) setTaskExpanded(t, true);
    }

    private void collapseAllTasks(List<Task> tasks) {
        for (Task t : tasks) setTaskExpanded(t, false);
    }

    private void scrollNodeIntoView(Node node) {
        if (node == null) return;
        centerContainer.applyCss();
        centerContainer.layout();

        Bounds viewport = centerScroll.getViewportBounds();
        Bounds content = centerContainer.getLayoutBounds();
        Bounds bounds = centerContainer.sceneToLocal(node.localToScene(node.getBoundsInLocal()));

        double viewportHeight = viewport.getHeight();
        double contentHeight = content.getHeight();
        if (contentHeight <= viewportHeight + 0.5) {
            centerScroll.setVvalue(0);
            return;
        }

        double targetTop = bounds.getMinY();
        double targetCenter = targetTop + bounds.getHeight() / 2.0;
        double raw = (targetCenter - viewportHeight / 2.0) / (contentHeight - viewportHeight);
        centerScroll.setVvalue(clamp(raw, 0, 1));
        node.requestFocus();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isDescendantOf(Node node, Node ancestor) {
        Node cur = node;
        while (cur != null) {
            if (cur == ancestor) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private static Node findNodeByUserData(Node node, Object userData) {
        if (node == null) return null;
        if (Objects.equals(node.getUserData(), userData)) return node;
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                Node found = findNodeByUserData(child, userData);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String taskKey(Task task) {
        return task.getId();
    }

    private static String subtaskKey(Task task, Subtask subtask) {
        return task.getId() + ":" + subtask.getId();
    }
}
