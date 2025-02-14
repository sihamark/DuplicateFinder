package eu.heha.duplicatesfinder

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

object DuplicateFinder {

    /**
     * recursively looks up [path] for duplicates with same name and maybe different file endings
     */
    fun findDuplicates(path: Path): List<Duplicate> {
        val duplicatesMap = mutableMapOf<String, List<Path>>()
        findDuplicatesRecursively(path, duplicatesMap)
        return duplicatesMap.values.filter { it.size > 1 }
            .map { Duplicate(it.first().parent!!, it) }
    }

    private fun findDuplicatesRecursively(
        path: Path,
        duplicatesMap: MutableMap<String, List<Path>>
    ) {
        val metaData = SystemFileSystem.metadataOrNull(path) ?: return
        if (metaData.isDirectory) {
            SystemFileSystem.list(path).forEach { subPath ->
                findDuplicatesRecursively(subPath, duplicatesMap)
            }
        } else {
            val baseName = path.getBaseName()
            duplicatesMap[baseName] =
                duplicatesMap.getOrElse(baseName) { mutableListOf() } + path
        }
    }

    private fun Path.getBaseName(): String {
        val name = this.toString()
        val lastOpenParenthesis = name.lastIndexOf('(')
        if (lastOpenParenthesis != -1) {
            return name.substring(0, lastOpenParenthesis)
        }
        val lastDotIndex = name.lastIndexOf('.')
        return if (lastDotIndex == -1) name else name.substring(0, lastDotIndex)
    }
}