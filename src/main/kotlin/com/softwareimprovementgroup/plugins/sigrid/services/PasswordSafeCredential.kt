package com.softwareimprovementgroup.plugins.sigrid.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager

class PasswordSafeCredential(private val key: String) {
    @Volatile private var cache: String = ""

    fun loadAsync() {
        ApplicationManager.getApplication().executeOnPooledThread {
            cache = PasswordSafe.instance.get(CredentialAttributes(key))?.getPasswordAsString() ?: ""
        }
    }

    fun get(): String = if (ApplicationManager.getApplication().isDispatchThread) {
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