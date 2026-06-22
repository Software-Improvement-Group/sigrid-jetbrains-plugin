package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class PasswordSafeCredential(private val key: String) {
    @Volatile private var cache: String = ""
    @Volatile private var loadFuture: Future<*>? = null

    fun loadAsync() {
        loadFuture = ApplicationManager.getApplication().executeOnPooledThread {
            cache = PasswordSafe.instance.get(CredentialAttributes(key))?.getPasswordAsString() ?: ""
        }
    }

    fun get(): String = if (ApplicationManager.getApplication().isDispatchThread) {
        // On EDT, PasswordSafe cannot be called directly. Use the cache populated by loadAsync().
        // Zero-timeout get() ensures cache visibility if the load already completed; if still
        // in-flight it throws TimeoutException and we fall through to the (possibly empty) cache.
        try { loadFuture?.get(0, TimeUnit.MILLISECONDS) } catch (_: Exception) {}
        cache
    } else {
        PasswordSafe.instance.get(CredentialAttributes(key))?.getPasswordAsString() ?: ""
    }

    fun set(value: String) {
        cache = value
        ApplicationManager.getApplication().executeOnPooledThread {
            PasswordSafe.instance.set(
                CredentialAttributes(key),
                if (value.isBlank()) null else Credentials(null, value)
            )
        }
    }
}