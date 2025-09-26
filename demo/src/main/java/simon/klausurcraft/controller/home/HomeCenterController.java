package simon.klausurcraft.controller.home;

import javafx.geometry.Insets;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import simon.klausurcraft.model.Difficulty;
import simon.klausurcraft.model.Eligibility;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;

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

            Label header = new Label("Task " + t.getId() + " — " + t.getTitle());
            header.getStyleClass().add("header");
            taskCard.getChildren().add(header);

            for (SubtaskModel st : t.getSubtasks()) {
                if (!allowed.contains(st.getDifficulty())) continue;
                boolean subMatches = taskMatches || matchesSubtask(st, q);
                if (!subMatches && !q.isEmpty()) continue;

                // Subtask-Name aus variants@group (Fallback auf ID)
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

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button btnOpen = new Button("Details");
                btnOpen.getStyleClass().add("chip");
                btnOpen.setOnAction(e -> HomeSubtaskSheet.open(root, t, st));

                row.getChildren().addAll(lblTitle, bPts, bDiff, bElig, spacer, btnOpen);
                taskCard.getChildren().add(row);
            }

            centerContainer.getChildren().add(taskCard);
        }
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