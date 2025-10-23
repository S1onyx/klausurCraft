module simon.klausurcraft {
    requires javafx.controls;
    requires javafx.fxml;

    // PDF generation (OpenPDF automatic module name)
    requires com.github.librepdf.openpdf;

    // XML parsing/validation & preferences
    requires java.xml;
    requires java.prefs;

    // FXML reflection open only for existing packages
    opens simon.klausurcraft to javafx.fxml;
    opens simon.klausurcraft.ui.home to javafx.fxml;
    opens simon.klausurcraft.ui.common to javafx.fxml;

    // export only for public APIs
    exports simon.klausurcraft;
    exports simon.klausurcraft.ui.home;
    exports simon.klausurcraft.ui.common;
    exports simon.klausurcraft.core.model;
    exports simon.klausurcraft.infrastructure.xml;
    exports simon.klausurcraft.infrastructure.pdf;
    exports simon.klausurcraft.ui.support;
}