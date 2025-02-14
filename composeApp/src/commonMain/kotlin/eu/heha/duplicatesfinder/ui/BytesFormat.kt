package eu.heha.duplicatesfinder.ui

import kotlin.math.round

fun Long.humanReadableByteCount() = when {
    this == Long.MIN_VALUE || this < 0 -> "N/A"
    this < 1024L -> "$this B"
    this <= 0xfffccccccccccccL shr 40 -> (this.toDouble() / (0x1 shl 10)).roundTo10th()
        .toString() + " KiB"

    this <= 0xfffccccccccccccL shr 30 -> (this.toDouble() / (0x1 shl 20)).roundTo10th()
        .toString() + " MiB"

    this <= 0xfffccccccccccccL shr 20 -> (this.toDouble() / (0x1 shl 30)).roundTo10th()
        .toString() + " GiB"

    this <= 0xfffccccccccccccL shr 10 -> (this.toDouble() / (0x1 shl 40)).roundTo10th()
        .toString() + " TiB"

    this <= 0xfffccccccccccccL -> ((this shr 10).toDouble() / (0x1 shl 40)).roundTo10th()
        .toString() + " PiB"

    else -> ((this shr 20).toDouble() / (0x1 shl 40)).roundTo10th().toString() + " EiB"
}

fun Double.roundTo10th(): Double = round(this * 10.0) / 10.0