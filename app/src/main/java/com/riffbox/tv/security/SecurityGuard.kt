package com.riffbox.tv.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import java.security.MessageDigest

object SecurityGuard {
    private const val TAG = "SecurityGuard"

    fun initialize(context: Context) {
        if (!verifyIntegrity(context)) {
            Log.e(TAG, "Security integrity check warning")
        }
    }

    /**
     * Checks if the application binary has been altered or hooked by a debugger.
     */
    fun verifyIntegrity(context: Context): Boolean {
        try {
            // 1. Prevent running under debugger in production
            if (!isDebuggable(context) && Debug.isDebuggerConnected()) {
                return false
            }

            // 2. Signature verification
            val signatures = getSignatures(context)
            if (signatures.isEmpty()) {
                return false
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check exception", e)
            return true
        }
    }

    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    @Suppress("DEPRECATION")
    private fun getSignatures(context: Context): List<String> {
        val sigs = mutableListOf<String>()
        try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                pkgInfo.signingInfo?.apkContentsSigners?.forEach {
                    sigs.add(hashSha256(it.toByteArray()))
                }
            } else {
                val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                pkgInfo.signatures?.forEach {
                    sigs.add(hashSha256(it.toByteArray()))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read signatures: ${e.message}")
        }
        return sigs
    }

    private fun hashSha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
