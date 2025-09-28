package simon.klausurcraft.controller.home;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import simon.klausurcraft.model.Difficulty;

import java.util.EnumSet;
import java.util.Set;

public class HomeTopbarController {

    private HomeController root;

    @FXML private TextField searchField;
    @FXML private CheckBox fltEasy;
    @FXML private CheckBox fltMedium;
    @FXML private CheckBox fltHard;

    public void init(HomeController root) {
        this.root = root;

        // Attach semantic style classes so CSS can color them (IDs are not CSS ids).
        // This fixes the "grey checkboxes" issue.
        fltEasy.getStyleClass().addAll("difficulty-filter", "difficulty-easy");
        fltMedium.getStyleClass().addAll("difficulty-filter", "difficulty-medium");
        fltHard.getStyleClass().addAll("difficulty-filter", "difficulty-hard");

        // default all on
        fltEasy.setSelected(true);
        fltMedium.setSelected(true);
        fltHard.setSelected(true);

        // Re-render center on changes
        searchField.textProperty().addListener((obs, o, n) ->
            root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties())
        );
        fltEasy.selectedProperty().addListener((o, ov, nv) ->
            root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties())
        );
        fltMedium.selectedProperty().addListener((o, ov, nv) ->
            root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties())
        );
        fltHard.selectedProperty().addListener((o, ov, nv) ->
            root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties())
        );
    }

    String currentQuery() {
        String q = searchField.getText();
        return q == null ? "" : q.trim().toLowerCase();
    }

    Set<Difficulty> allowedDifficulties() {
        EnumSet<Difficulty> s = EnumSet.noneOf(Difficulty.class);
        if (fltEasy.isSelected()) s.add(Difficulty.EASY);
        if (fltMedium.isSelected()) s.add(Difficulty.MEDIUM);
        if (fltHard.isSelected()) s.add(Difficulty.HARD);
        if (s.isEmpty()) return EnumSet.allOf(Difficulty.class);
        return s;
    }
}