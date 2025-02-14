package eu.heha.duplicatesfinder.model

object DuplicateFinder {

    private val charsToIgnore = arrayOf("(", ")", " ", "_", "-", ".")

    /**
     * recursively looks up [path] for duplicates with same name and maybe different file endings
     */
    fun findDuplicates(path: PathWithMetaData): List<Duplicate> {
        val duplicatesMap = mutableMapOf<String, List<PathWithMetaData>>()
        findDuplicatesRecursively(path, duplicatesMap)
        return duplicatesMap.values.filter { it.size > 1 }
            .map { Duplicate(it.first().parent!!, it) }
    }

    private fun findDuplicatesRecursively(
        path: PathWithMetaData,
        duplicatesMap: MutableMap<String, List<PathWithMetaData>>
    ) {
        if (path.isDirectory) {
            path.children().forEach { subPath ->
                findDuplicatesRecursively(subPath, duplicatesMap)
            }
        } else {
            val baseName = path.getBaseName()
            duplicatesMap[baseName] =
                duplicatesMap.getOrElse(baseName) { listOf() } + path
        }
    }

    private fun PathWithMetaData.getBaseName(): String {
        val name = path.toString()
        val lastDotIndex = name.lastIndexOf('.')
        val nameWithoutEnding =  if (lastDotIndex == -1) name else name.substring(0, lastDotIndex)
        return charsToIgnore.fold(nameWithoutEnding) { acc, char ->
            acc.replace(char, "")
        }
    }
}