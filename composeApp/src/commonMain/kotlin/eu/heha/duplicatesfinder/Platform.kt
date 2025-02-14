package eu.heha.duplicatesfinder

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform