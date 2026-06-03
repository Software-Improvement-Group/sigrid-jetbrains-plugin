package com.softwareimprovementgroup.plugins.sigrid.mappers

fun normalizePath(path: String?, subsystem: String): String {
    if (path.isNullOrBlank()) return ""
    if (subsystem.isBlank()) return path
    val prefix = "$subsystem/"
    return if (path.startsWith(prefix)) path.removePrefix(prefix) else path
}

fun toDisplayFilePath(path: String?, prefix: String = ".../"): String {
    if (path.isNullOrBlank()) return ""
    val fileName = path.substringAfterLast("/")
    val dir = path.substringBeforeLast("/", "")
    return if (dir.isBlank()) fileName else "$prefix$fileName"
}