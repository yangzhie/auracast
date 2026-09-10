package com.innovatex.auracast.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log

object BroadcastAssistantProbe {

    private const val TAG = "AssistantProbe"

    // BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
    private const val PROFILE_BROADCAST_ASSISTANT = 29

    private const val ASSISTANT_CLASS = "android.bluetooth.BluetoothLeBroadcastAssistant"

    private val INTERESTING = setOf(
        "addSource", "removeSource", "modifySource",
        "startSearchingForSources", "stopSearchingForSources",
        "isSearchInProgress", "registerCallback", "getAllSources"
    )

    private fun probePrivilegedCall(proxy: BluetoothProfile) {
        val cls = proxy.javaClass

        Log.i(TAG, "5. Methods available:")
        cls.methods
            .filter { it.name in INTERESTING }
            .sortedBy { it.name }
            .forEach { m ->
                val params = m.parameterTypes.joinToString { it.simpleName }
                Log.i(TAG, "     ${m.name}($params)")
            }

        // A privileged call with no arguments — the cleanest possible test.
        try {
            val method = cls.getMethod("isSearchInProgress")
            val result = method.invoke(proxy)
            Log.i(TAG, "6. isSearchInProgress() returned $result")
            Log.i(TAG, "   PRIVILEGED CALL SUCCEEDED — the assistant route is open.")
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // Reflection wraps whatever the method threw.
            val cause = e.cause
            Log.w(TAG, "6. isSearchInProgress() threw ${cause?.javaClass?.simpleName}")
            Log.w(TAG, "   message: ${cause?.message}")
        } catch (e: Exception) {
            Log.w(TAG, "6. isSearchInProgress() unavailable: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun probe(context: Context) {
        Log.i(TAG, "──── Broadcast Assistant availability ────")

        // 1. Is the class present in this build at all?
        val assistantClass = try {
            Class.forName(ASSISTANT_CLASS)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "1. Class NOT present: $ASSISTANT_CLASS")
            null
        }

        if (assistantClass != null) {
            Log.i(TAG, "1. Class present: ${assistantClass.name}")

            val hasAddSource = assistantClass.methods.any { it.name == "addSource" }
            Log.i(TAG, "2. addSource method present: $hasAddSource")
        }

        // 3. Can we obtain the profile proxy? This is the gating question —
        //    it's where a privileged-permission check would bite.
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            Log.w(TAG, "3. No Bluetooth adapter")
            return
        }

        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                Log.i(TAG, "4. PROXY OBTAINED for profile $profile — ${proxy?.javaClass?.name}")
                if (proxy != null) {
                    probePrivilegedCall(proxy)
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.i(TAG, "4. Proxy disconnected for profile $profile")
            }
        }

        val requested = try {
            adapter.getProfileProxy(context, listener, PROFILE_BROADCAST_ASSISTANT)
        } catch (e: SecurityException) {
            Log.w(TAG, "3. SecurityException requesting proxy — privileged permission required", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "3. Failed to request proxy: ${e.javaClass.simpleName}: ${e.message}")
            false
        }

        Log.i(TAG, "3. getProfileProxy returned: $requested")
        if (requested) {
            Log.i(TAG, "   Waiting for callback — if nothing logs as (4), the request was rejected.")
        }
    }
}