package eu.heha.duplicatesfinder.model

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

data class PathWithMetaData(
    val path: Path
) {
    private val metaData = SystemFileSystem.metadataOrNull(path)
        ?: throw IllegalArgumentException("Path $path does not contain meta data")

    val name get() = path.name
    val parent get() = path.parent?.let { PathWithMetaData(it) }
    val isDirectory get() = metaData.isDirectory
    val size get() = metaData.size

    constructor(pathString: String) : this(Path(pathString))

    fun delete() {
        SystemFileSystem.delete(path)
    }

    fun children(): List<PathWithMetaData> =
        SystemFileSystem.list(path).map { PathWithMetaData(it) }
}