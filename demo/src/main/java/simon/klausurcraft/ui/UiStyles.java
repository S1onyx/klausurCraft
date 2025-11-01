package simon.klausurcraft.ui;

import javafx.scene.control.Dialog;
import simon.klausurcraft.app.KlausurCraftApp;

/** UI helpers: apply current stylesheet to dialogs. */
public final class UiStyles {
    private UiStyles(){}

    /** Ensures the current app stylesheet is also applied to a dialog (alerts, input dialogs). */
    public static void applyCurrentStyles(Dialog<?> dialog) {
        try {
            if (dialog.getDialogPane().getScene() != null) {
                var appSheets = KlausurCraftApp.getScene().getStylesheets();
                dialog.getDialogPane().getScene().getStylesheets().setAll(appSheets);
            }
        } catch (Exception ignored) {
            // Best-effort; CSS should normally inherit via owner, but enforce here if possible
        }
    }
}
