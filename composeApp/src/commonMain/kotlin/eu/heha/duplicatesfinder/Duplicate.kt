package eu.heha.duplicatesfinder

import kotlinx.io.files.Path

data class Duplicate(
    val parent: Path,
    val duplicates: List<Path>
)
