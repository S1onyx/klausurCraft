module simon.klausurcraft {
    requires javafx.controls;
    requires javafx.fxml;

    // PDF generation (OpenPDF automatic module name)
    requires com.github.librepdf.openpdf;

    // XML parsing/validation & preferences
    requires java.xml;
    requires java.prefs;

    // FXML reflection open only for existing packages
    opens simon.klausurcraft.ui.home to javafx.fxml;
    opens simon.klausurcraft.ui.components to javafx.fxml;

    // export only for public APIs
    exports simon.klausurcraft.app;
    exports simon.klausurcraft.task;
    exports simon.klausurcraft.task.io;
    exports simon.klausurcraft.task.export;
    exports simon.klausurcraft.task.planning;
    exports simon.klausurcraft.ui;
    exports simon.klausurcraft.ui.home;
    exports simon.klausurcraft.ui.components;
}
