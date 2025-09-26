module simon.klausurcraft {
    requires javafx.controls;
    requires javafx.fxml;

    // PDF generation (OpenPDF automatic module name)
    requires com.github.librepdf.openpdf;

    // XML parsing/validation & preferences
    requires java.xml;
    requires java.prefs;

    opens simon.klausurcraft to javafx.fxml;
    opens simon.klausurcraft.controller to javafx.fxml;

    exports simon.klausurcraft;
    exports simon.klausurcraft.controller;
    exports simon.klausurcraft.model;
    exports simon.klausurcraft.services;
    exports simon.klausurcraft.pdf;
}