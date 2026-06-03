package com.softwareimprovementgroup.plugins.sigrid.models

data class FileLocation(
    val component: String,
    val filePath: String,
    val startLine: Int? = null,
    val endLine: Int? = null,
)