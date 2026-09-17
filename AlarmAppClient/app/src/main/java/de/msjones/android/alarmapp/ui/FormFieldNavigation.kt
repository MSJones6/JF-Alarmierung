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

    /**
     * Prüft, ob vom letzten Eingabefeld zurück auf das erste gesprungen werden soll.
     *
     * @param move ermittelte Fokusbewegung
     * @param isLastField true, wenn das aktuelle Feld das letzte Eingabefeld ist
     * @return true, wenn der Fokus auf das erste Feld gelegt werden soll
     */
    fun shouldWrapToFirst(move: FormFocusMove, isLastField: Boolean): Boolean {
        return isLastField && move == FormFocusMove.NEXT
    }
}

/**
 * Verschiebt den Fokus bei Tab oder Enter auf das nächste bzw. vorherige Feld.
 *
 * Am letzten Feld springt die Vorwärtsbewegung auf das erste Eingabefeld zurück.
 *
 * @param focusManager Fokusverwaltung des aktuellen Compose-Baums
 * @param isLastField true, wenn es kein weiteres Eingabefeld gibt
 * @param onWrapToFirst setzt den Fokus auf das erste Eingabefeld
 */
fun Modifier.moveFocusOnTabOrEnter(
    focusManager: FocusManager,
    isLastField: Boolean = false,
    onWrapToFirst: () -> Unit = {}
): Modifier = onPreviewKeyEvent { event ->
    when (val move = FormFieldNavigation.focusMoveFor(event)) {
        FormFocusMove.PREVIOUS -> {
            focusManager.moveFocus(FocusDirection.Previous)
            true
        }
        FormFocusMove.NEXT -> {
            if (FormFieldNavigation.shouldWrapToFirst(move, isLastField)) {
                onWrapToFirst()
            } else {
                focusManager.moveFocus(FocusDirection.Next)
            }
            true
        }
        FormFocusMove.NONE -> false
    }
}
