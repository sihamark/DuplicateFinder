package eu.heha.duplicatesfinder

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DuplicatesFinder"
    ) {
        SideEffect {
            window.minimumSize = Dimension(400, 400)
        }
        App()
    }
}