package com.unistream.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val WEB_CLIENT_ID = "REPLACE_WITH_WEB_CLIENT_ID"

data class GoogleUserSession(val id: String, val displayName: String?, val avatarUrl: String?)

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)
    private var cachedSession: GoogleUserSession? = null

    suspend fun signIn(): Result<GoogleUserSession> = runCatching {
        check(WEB_CLIENT_ID != "REPLACE_WITH_WEB_CLIENT_ID") { "Google login isn't configured yet." }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        credentialManager.getCredential(context, request)
        val session = GoogleUserSession("google_user", "Google User", null)
        cachedSession = session
        session
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        cachedSession = null
    }

    fun restoreSession(): GoogleUserSession? = cachedSession
}
