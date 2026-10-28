package com.softwareimprovementgroup.plugins.sigrid.services

import com.softwareimprovementgroup.plugins.sigrid.SigridBundle
import java.io.IOException

/** Maps well-known failures to user-facing text; anything else goes through [generic] with the raw message. */
internal fun toErrorMessage(e: Exception, generic: (String) -> String): String {
    if (e is IOException || e.cause is IOException) {
        return SigridBundle["panel.error.network"]
    }
    val status = e.message?.substringAfter("HTTP ")?.substringBefore(" ")?.toIntOrNull()
    return when (status) {
        401  -> SigridBundle["panel.error.unauthorized"]
        403  -> SigridBundle["panel.error.forbidden"]
        404  -> SigridBundle["panel.error.not.found"]
        else -> generic(e.message ?: "")
    }
}
