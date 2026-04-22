package simon.klausurcraft.ui;

import javafx.scene.control.Dialog;

/** UI helpers: apply current stylesheet to dialogs. */
public final class UiStyles {
    private UiStyles(){}

    /** Ensures the current app stylesheet is also applied to a dialog (alerts, input dialogs). */
    public static void applyCurrentStyles(Dialog<?> dialog) {
        try {
            dialog.setOnShown(ev -> {
                try {
                    if (dialog.getDialogPane().getScene() != null) {
                        ThemeService.register(dialog.getDialogPane().getScene());
                    }
                } catch (Exception ignored) {
                    // best-effort
                }
            });
            if (dialog.getDialogPane().getScene() != null) {
                ThemeService.register(dialog.getDialogPane().getScene());
            }
        } catch (Exception ignored) {
            // Best-effort; CSS should normally inherit via owner, but enforce here if possible
        }
    }
}
