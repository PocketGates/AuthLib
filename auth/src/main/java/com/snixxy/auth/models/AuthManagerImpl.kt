package com.snixxy.auth.models

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.snixxy.auth.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class AuthManagerImpl : AuthManager {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val userEmail = auth.currentUser?.email.toString()

    override fun checkAuth() = auth.currentUser != null

    override fun getUserEmail(): String {
        return userEmail
    }

    override fun resetPassword(userName: String): Flow<NetworkResult<Void>> {
        return callbackFlow {
            auth.sendPasswordResetEmail(userName).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(NetworkResult.Success(null))
                } else {
                    trySend(NetworkResult.Error(getError(task.exception)))
                }
            }
            awaitClose { }
        }
    }

    override fun signIn(userName: String, password: String): Flow<NetworkResult<Void>> {
        return callbackFlow {
            auth.signInWithEmailAndPassword(userName, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(NetworkResult.Success(null))
                } else {
                    val error = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> AuthError(
                            message = R.string.user_is_not_registered,
                            userNameError = true
                        )

                        else -> getError(task.exception)
                    }
                    trySend(NetworkResult.Error(error))
                }
            }
            awaitClose { }
        }
    }

    override fun signUp(userName: String, password: String): Flow<NetworkResult<Void>> {
        return callbackFlow {
            auth.createUserWithEmailAndPassword(userName, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        addUserDocumentToFirestore()
                        trySend(NetworkResult.Success(null))
                    } else {
                        trySend(NetworkResult.Error(getError(task.exception)))
                    }
                }
            awaitClose { }
        }
    }

    private fun addUserDocumentToFirestore() {
        val userDocRef = firestore.collection("userData")
            .document(auth.currentUser?.email.toString())

        userDocRef.set(hashMapOf("uid" to auth.currentUser?.uid))
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    throw task.exception as FirebaseFirestoreException
                }
            }
    }

    override fun signOut(): Flow<NetworkResult<Void>> {
        return flow {
            try {
                auth.signOut()
                emit(NetworkResult.Success(null))
            } catch (e: Throwable) {
                emit(NetworkResult.Error(getError(e)))
            }
        }
    }

    private fun getError(exception: Throwable?): AuthError {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> AuthError(
                message = R.string.no_reset_user,
                userNameError = true
            )

            is FirebaseAuthWeakPasswordException -> AuthError(
                message = R.string.password_is_weak,
                passwordError = true
            )

            is FirebaseAuthInvalidCredentialsException -> AuthError(
                message = R.string.invalid_credentials,
                userNameError = true,
                passwordError = true
            )

            is FirebaseAuthUserCollisionException -> AuthError(
                message = R.string.username_is_taken,
                userNameError = true
            )

            is FirebaseNetworkException -> AuthError(
                message = R.string.firebase_network_error
            )

            is FirebaseTooManyRequestsException -> AuthError(
                message = R.string.firebase_too_many_requests
            )

            else -> AuthError(message = R.string.something_is_wrong)
        }
    }
}
