package eu.heha.duplicatesfinder.model

object DuplicateFinder {

    private val charsToIgnore = arrayOf("(", ")", " ", "_", "-", ".", ",")
    private val numbersInParenthesis = Regex("\\([0-9]+\\)")

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
            if (baseName.isBlank()) return
            duplicatesMap[baseName] = duplicatesMap.getOrElse(baseName) { listOf() } + path
        }
    }

    private fun PathWithMetaData.getBaseName(): String = path.toString()
        .removeFileEnding()
        .removeNumbersInParentheses()
        .removeUnwantedChars()

    private fun String.removeFileEnding(): String {
        val lastDotIndex = this.lastIndexOf('.')
        val nameWithoutEnding = if (lastDotIndex == -1) this else this.substring(0, lastDotIndex)
        return nameWithoutEnding
    }

    private fun String.removeNumbersInParentheses(): String =
        numbersInParenthesis.replace(this, "")

    private fun String.removeUnwantedChars(): String =
        charsToIgnore.fold(this) { acc, char -> acc.replace(char, "") }
}

