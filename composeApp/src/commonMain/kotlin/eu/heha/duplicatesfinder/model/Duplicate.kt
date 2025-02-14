package eu.heha.duplicatesfinder.model

data class Duplicate(
    val parent: PathWithMetaData,
    val duplicates: List<PathWithMetaData>
)
