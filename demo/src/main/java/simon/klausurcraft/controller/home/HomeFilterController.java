package simon.klausurcraft.controller.home;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import simon.klausurcraft.model.Difficulty;

import java.util.EnumSet;
import java.util.Set;

public class HomeFilterController {

    private HomeController root;

    @FXML private TextField searchField;
    @FXML private CheckBox fltEasy;
    @FXML private CheckBox fltMedium;
    @FXML private CheckBox fltHard;

    private final ObservableSet<Difficulty> allowed = FXCollections.observableSet(EnumSet.allOf(Difficulty.class));

    void init(HomeController root) {
        this.root = root;

        // Attach semantic classes so theme CSS can color them consistently.
        fltEasy.getStyleClass().addAll("difficulty-filter", "difficulty-easy");
        fltMedium.getStyleClass().addAll("difficulty-filter", "difficulty-medium");
        fltHard.getStyleClass().addAll("difficulty-filter", "difficulty-hard");

        // initial states (keep all active by default)
        fltEasy.setSelected(true);
        fltMedium.setSelected(true);
        fltHard.setSelected(true);

        // listeners: trigger actual re-render of the center view
        searchField.textProperty().addListener((obs, o, n) ->
            root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties())
        );

        fltEasy.selectedProperty().addListener((o, ov, nv) -> { updateAllowed(); root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties()); });
        fltMedium.selectedProperty().addListener((o, ov, nv) -> { updateAllowed(); root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties()); });
        fltHard.selectedProperty().addListener((o, ov, nv) -> { updateAllowed(); root.centerController.render(root.getTasks(), currentQuery(), allowedDifficulties()); });
    }

    private void updateAllowed() {
        allowed.clear();
        if (fltEasy.isSelected())   allowed.add(Difficulty.EASY);
        if (fltMedium.isSelected()) allowed.add(Difficulty.MEDIUM);
        if (fltHard.isSelected())   allowed.add(Difficulty.HARD);
    }

    String currentQuery() {
        String q = searchField.getText();
        return q == null ? "" : q.trim().toLowerCase();
    }

    Set<Difficulty> allowedDifficulties() {
        if (fltEasy.isSelected() || fltMedium.isSelected() || fltHard.isSelected()) {
            return Set.copyOf(allowed);
        }
        return EnumSet.allOf(Difficulty.class);
    }

    // expose to parent
    TextField getSearchField() { return searchField; }
}