package de.msjones.android.alarmapp.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit-Tests für den Fokuswechsel per Tab und Enter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class FormFieldNavigationTest {

    /** Enter und Tab springen ins nächste Feld. */
    @Test
    fun focusMoveFor_enterAndTabMoveNext() {
        assertEquals(FormFocusMove.NEXT, FormFieldNavigation.focusMoveFor(keyDown(AndroidKeyEvent.KEYCODE_ENTER)))
        assertEquals(FormFocusMove.NEXT, FormFieldNavigation.focusMoveFor(keyDown(AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)))
        assertEquals(FormFocusMove.NEXT, FormFieldNavigation.focusMoveFor(keyDown(AndroidKeyEvent.KEYCODE_TAB)))
    }

    /** Umschalt+Tab springt ins vorherige Feld. */
    @Test
    fun focusMoveFor_shiftTabMovesPrevious() {
        val event = keyDown(AndroidKeyEvent.KEYCODE_TAB, metaState = AndroidKeyEvent.META_SHIFT_ON)
        assertEquals(FormFocusMove.PREVIOUS, FormFieldNavigation.focusMoveFor(event))
    }

    /** Loslassen der Taste ändert den Fokus nicht erneut. */
    @Test
    fun focusMoveFor_keyUpIsIgnored() {
        val event = KeyEvent(
            AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, AndroidKeyEvent.KEYCODE_ENTER)
        )
        assertEquals(FormFocusMove.NONE, FormFieldNavigation.focusMoveFor(event))
    }

    /** Andere Tasten bleiben unverändert. */
    @Test
    fun focusMoveFor_otherKeysAreIgnored() {
        assertEquals(
            FormFocusMove.NONE,
            FormFieldNavigation.focusMoveFor(keyDown(AndroidKeyEvent.KEYCODE_A))
        )
    }

    /**
     * Erzeugt ein Compose-Tastaturereignis für das Drücken einer Taste.
     *
     * @param keyCode Android-Tastencode
     * @param metaState optionale Modifizierer wie Umschalt
     * @return Compose-KeyEvent
     */
    private fun keyDown(keyCode: Int, metaState: Int = 0): KeyEvent {
        return KeyEvent(
            AndroidKeyEvent(0L, 0L, AndroidKeyEvent.ACTION_DOWN, keyCode, 0, metaState)
        )
    }
}
