package com.xichugeek.lunatide.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.xichugeek.lunatide.R

/** Prefer strong sensors, allow weak biometrics as a UI shortcut, never device credentials. */
fun biometricAuthenticators(activity: FragmentActivity): Int? {
    val manager = BiometricManager.from(activity)
    return listOf(BiometricManager.Authenticators.BIOMETRIC_STRONG, BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .firstOrNull { manager.canAuthenticate(it) == BiometricManager.BIOMETRIC_SUCCESS }
}
fun showBiometric(activity: FragmentActivity, success: () -> Unit, failed: () -> Unit) {
    val authenticators = biometricAuthenticators(activity) ?: return failed()
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = failed()
        override fun onAuthenticationFailed() = failed()
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.pin_biometric))
        .setAllowedAuthenticators(authenticators).setNegativeButtonText(activity.getString(R.string.pin_use_pin)).build())
}
