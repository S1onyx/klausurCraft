package simon.klausurcraft.controller.home;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import simon.klausurcraft.model.SubtaskModel;
import simon.klausurcraft.model.TaskModel;

import java.util.List;

public class HomeSidebarController {

    private HomeController root;

    @FXML private TreeView<String> tocTree;
    @FXML private Button btnLoad;

    public void init(HomeController root) {
        this.root = root;
        btnLoad.setOnAction(e -> HomeFileController.chooseAndLoadXml(root));
    }

    public void rebuildToc(List<TaskModel> tasks) {
        TreeItem<String> rootItem = new TreeItem<>("Contents");
        rootItem.setExpanded(true);

        for (TaskModel t : tasks) {
            TreeItem<String> taskNode = new TreeItem<>(formatTaskTitle(t));
            for (SubtaskModel st : t.getSubtasks()) {
                String subTitle = formatSubtaskTitle(st);
                taskNode.getChildren().add(new TreeItem<>("• " + subTitle));
            }
            rootItem.getChildren().add(taskNode);
        }

        tocTree.setRoot(rootItem);
        tocTree.setShowRoot(false);

        tocTree.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                TreeItem<String> sel = tocTree.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    root.centerController.scrollToLabel(sel.getValue());
                }
            }
        });
    }

    private String formatTaskTitle(TaskModel t) {
        return String.format("%s — %s", t.getId(), t.getTitle());
    }

    // Subtask-Name aus variants@group; Punkte/Diff/Elig bleiben als Info erhalten
    private String formatSubtaskTitle(SubtaskModel st) {
        String name = root.getXmlService().readSubtaskGroup(st);
        if (name == null || name.isBlank()) {
            name = "Subtask " + st.getParent().getId() + "." + st.getId();
        }
        String pts = st.getPoints().stripTrailingZeros().toPlainString();
        return String.format("%s  (%s pts, %s, %s)", name, pts, st.getDifficulty(), st.getEligibility());
    }
}