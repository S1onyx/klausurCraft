module simon.klausurcraft {
    requires javafx.controls;
    requires javafx.fxml;

    opens simon.klausurcraft to javafx.fxml;
    exports simon.klausurcraft;
}