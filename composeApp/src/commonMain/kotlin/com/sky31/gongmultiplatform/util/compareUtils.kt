package com.sky31.gongmultiplatform.util

enum class AppUpdateState {
    UP_TO_DATE,
    OPTIONAL_UPDATE,
    REQUIRED_UPDATE
}

fun getAppUpdateState(current: String, newest: String, least: String): AppUpdateState {
    val currentVersion = parseVersion(current) ?: return AppUpdateState.REQUIRED_UPDATE
    val newestVersion = parseVersion(newest) ?: return AppUpdateState.REQUIRED_UPDATE
    val leastVersion = parseVersion(least) ?: return AppUpdateState.REQUIRED_UPDATE

    if (compareVersionParts(currentVersion, leastVersion) < 0) {
        return AppUpdateState.REQUIRED_UPDATE
    }

    if (compareVersionParts(currentVersion, newestVersion) < 0) {
        return AppUpdateState.OPTIONAL_UPDATE
    }

    return AppUpdateState.UP_TO_DATE
}

private fun parseVersion(version: String): List<Int>? {
    val regex = Regex("""^(\d+(?:\.\d+)*)(?:-(.+))?$""")
    val match = regex.find(version) ?: return null
    return match.groupValues[1]
        .split(".")
        .map { it.toInt() }
}

private fun compareVersionParts(left: List<Int>, right: List<Int>): Int {
    val size = maxOf(left.size, right.size)

    for (index in 0 until size) {
        val leftPart = left.getOrElse(index) { 0 }
        val rightPart = right.getOrElse(index) { 0 }

        if (leftPart != rightPart) {
            return leftPart.compareTo(rightPart)
        }
    }

    return 0
}
