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
    opens simon.klausurcraft.controller.home to javafx.fxml;
    opens simon.klausurcraft.controller.common to javafx.fxml;

    // expoert only vor public APIs
    exports simon.klausurcraft;
    exports simon.klausurcraft.controller.home;
    exports simon.klausurcraft.controller.common;
    exports simon.klausurcraft.model;
    exports simon.klausurcraft.services;
    exports simon.klausurcraft.pdf;
    exports simon.klausurcraft.utils;
}