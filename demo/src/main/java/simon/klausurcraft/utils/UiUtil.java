package simon.klausurcraft.utils;

import javafx.scene.control.Dialog;
import simon.klausurcraft.App;

/** UI helpers: apply current stylesheet to dialogs. */
public final class UiUtil {
    private UiUtil(){}

    /** Ensures the current app stylesheet is also applied to a dialog (alerts, input dialogs). */
    public static void applyCurrentStyles(Dialog<?> dialog) {
        try {
            if (dialog.getDialogPane().getScene() != null) {
                var appSheets = App.getScene().getStylesheets();
                dialog.getDialogPane().getScene().getStylesheets().setAll(appSheets);
            }
        } catch (Exception ignored) {
            // Best-effort; CSS should normally inherit via owner, but enforce here if possible
        }
    }
}