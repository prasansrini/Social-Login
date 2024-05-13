package com.social.login.presentation.sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.social.login.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleAuthUiClient(
		private val context: Context, private val oneTapClient: SignInClient
) {
	private val auth = Firebase.auth

	suspend fun signIn(): IntentSender? {
		val result = try {
			oneTapClient
				.beginSignIn(
					buildSignInRequest()
				)
				.await()
		} catch (e: Exception) {
			e.printStackTrace()
			if (e is CancellationException) throw e
			null
		}

		return result?.pendingIntent?.intentSender
	}

	suspend fun getSignInWithIntent(intent: Intent): SignInResult {
		val credentials = oneTapClient.getSignInCredentialFromIntent(intent)
		val googleIdToken = credentials.googleIdToken
		val googleCredentials = GoogleAuthProvider.getCredential(
			googleIdToken,
			null
		)

		return try {
			val user = auth
				.signInWithCredential(googleCredentials)
				.await().user
			SignInResult(
				data = user?.run {
					UserData(
						userId = uid,
						userName = displayName,
						profilePicture = photoUrl?.toString()
					)
				},
				errorMessage = null
			)
		} catch (e: Exception) {
			e.printStackTrace()
			if (e is CancellationException) throw e
			SignInResult(
				data = null,
				errorMessage = e.message
			)
		}
	}

	suspend fun signOut() {
		try {
			oneTapClient
				.signOut()
				.await()
			auth.signOut()
		} catch (e: Exception) {
			e.printStackTrace()
			if (e is CancellationException) throw e
		}
	}

	fun getSignedInUser(): UserData? = auth.currentUser?.run {
		UserData(
			userId = uid,
			userName = displayName,
			profilePicture = photoUrl?.toString()
		)
	}

	private fun buildSignInRequest(): BeginSignInRequest {
		return BeginSignInRequest
			.Builder()
			.setGoogleIdTokenRequestOptions(
				GoogleIdTokenRequestOptions
					.builder()
					.setSupported(true)
					.setFilterByAuthorizedAccounts(false)
					.setServerClientId(context.getString(R.string.web_client_id))
					.build()
			)
			.setAutoSelectEnabled(true)
			.build()
	}
}