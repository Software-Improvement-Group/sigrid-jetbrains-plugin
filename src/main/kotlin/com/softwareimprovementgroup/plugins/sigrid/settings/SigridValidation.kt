package com.softwareimprovementgroup.plugins.sigrid.settings

internal val CUSTOMER_NAME_REGEX = Regex("[a-z0-9]{2,65}")
internal val SYSTEM_NAME_REGEX = Regex("(?=.{2,65}$)[a-zA-Z0-9]+(-[a-zA-Z0-9]+)*")
internal val SUBSYSTEM_NAME_REGEX = Regex("[A-Za-z0-9][A-Za-z0-9._\\-/]*[A-Za-z0-9]")