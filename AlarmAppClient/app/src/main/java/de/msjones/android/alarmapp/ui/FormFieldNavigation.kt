package de.msjones.android.alarmapp.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Fokuswechsel innerhalb eines Formulars per Tastatur.
 */
enum class FormFocusMove {
    /** Kein Fokuswechsel. */
    NONE,

    /** Fokus auf das nächste Feld. */
    NEXT,

    /** Fokus auf das vorherige Feld. */
    PREVIOUS
}

/**
 * Hilfsfunktionen, damit Tab und Enter im Formular das nächste Feld ansteuern.
 */
object FormFieldNavigation {

    /**
     * Bestimmt den Fokuswechsel für eine Tastatureingabe.
     *
     * Tab und Enter springen vorwärts, Umschalt+Tab rückwärts.
     *
     * @param event Tastaturereignis aus Compose
     * @return gewünschte Fokusbewegung
     */
    fun focusMoveFor(event: KeyEvent): FormFocusMove {
        if (event.type != KeyEventType.KeyDown) {
            return FormFocusMove.NONE
        }
        if (event.key == Key.Tab) {
            return if (event.isShiftPressed) {
                FormFocusMove.PREVIOUS
            } else {
                FormFocusMove.NEXT
            }
        }
        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
            return FormFocusMove.NEXT
        }
        return FormFocusMove.NONE
    }
}

/**
 * Verschiebt den Fokus bei Tab oder Enter auf das nächste bzw. vorherige Feld.
 *
 * @param focusManager Fokusverwaltung des aktuellen Compose-Baums
 * @param isLastField true, wenn es kein weiteres Eingabefeld gibt
 */
fun Modifier.moveFocusOnTabOrEnter(
    focusManager: FocusManager,
    isLastField: Boolean = false
): Modifier = onPreviewKeyEvent { event ->
    when (FormFieldNavigation.focusMoveFor(event)) {
        FormFocusMove.PREVIOUS -> {
            focusManager.moveFocus(FocusDirection.Previous)
            true
        }
        FormFocusMove.NEXT -> {
            if (isLastField) {
                focusManager.clearFocus()
            } else {
                focusManager.moveFocus(FocusDirection.Next)
            }
            true
        }
        FormFocusMove.NONE -> false
    }
}
