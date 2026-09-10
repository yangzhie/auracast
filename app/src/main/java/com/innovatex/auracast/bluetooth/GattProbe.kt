package com.innovatex.auracast.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID

/**
 * Connects GATT to a bonded LE Audio device and logs every service and
 * characteristic it exposes.
 *
 * The question this answers: does the hearing device expose the Broadcast
 * Audio Scan Service (BASS, 0x184F) to a third-party GATT client? If it
 * does, writing to its control point may be an alternative to the
 * privileged BluetoothLeBroadcastAssistant API.
 *
 * Discovery succeeding does NOT prove a write will be accepted — that's a
 * separate gate, tested separately.
 */
@SuppressLint("MissingPermission")
object GattProbe {

    private const val TAG = "GattProbe"

    /** Broadcast Audio Scan Service. */
    private val BASS_SERVICE: UUID = uuid16("184F")

    /** Broadcast Audio Scan Control Point — where Add Source is written. */
    private val BASS_CONTROL_POINT: UUID = uuid16("2BC7")

    /** Broadcast Receive State — notifies the sink's current sources. */
    private val BASS_RECEIVE_STATE: UUID = uuid16("2BC8")

    private var gatt: BluetoothGatt? = null

    /**
     * Lists bonded devices so you can find the right name to pass to probe().
     */
    fun listBondedDevices(context: Context) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            Log.w(TAG, "No Bluetooth adapter")
            return
        }

        val bonded = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_CONNECT not granted", e)
            return
        }

        Log.i(TAG, "──── Bonded devices ────")
        bonded.forEach { device ->
            Log.i(TAG, "  ${device.name}  ${device.address}  type=${typeName(device.type)}")
        }
        if (bonded.isEmpty()) {
            Log.w(TAG, "  none — pair your hearing device first")
        }
    }

    /**
     * Connects to the first bonded device whose name contains [nameContains]
     * and dumps its GATT services.
     */
    fun probe(context: Context, nameContains: String) {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return

        val device: BluetoothDevice? = try {
            adapter.bondedDevices.firstOrNull {
                it.name?.contains(nameContains, ignoreCase = true) == true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_CONNECT not granted", e)
            null
        }

        if (device == null) {
            Log.w(TAG, "No bonded device matching \"$nameContains\"")
            listBondedDevices(context)
            return
        }

        Log.i(TAG, "──── Connecting to ${device.name} (${device.address}) ────")

        disconnect()

        // TRANSPORT_LE forces a BLE connection. Without it, a dual-mode
        // device may connect over classic Bluetooth and expose no GATT at all.
        gatt = device.connectGatt(
            context,
            false,
            callback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    fun disconnect() {
        gatt?.let {
            try {
                it.disconnect()
                it.close()
            } catch (e: SecurityException) {
                // Nothing to clean up without permission.
            }
        }
        gatt = null
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "Connection state: ${stateName(newState)} (status $status)")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Discovering services…")
                try {
                    g.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Cannot discover services", e)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed, status $status")
                return
            }

            val services = g.services
            Log.i(TAG, "──── ${services.size} services ────")

            services.forEach { service ->
                val label = if (service.uuid == BASS_SERVICE) "  ← BASS" else ""
                Log.i(TAG, "SERVICE ${service.uuid}$label")

                service.characteristics.forEach { ch ->
                    val marker = when (ch.uuid) {
                        BASS_CONTROL_POINT -> "  ← CONTROL POINT"
                        BASS_RECEIVE_STATE -> "  ← RECEIVE STATE"
                        else -> ""
                    }
                    Log.i(TAG, "    CHAR ${ch.uuid}  [${properties(ch)}]$marker")
                }
            }

            summarise(g)
        }
    }

    private fun summarise(g: BluetoothGatt) {
        Log.i(TAG, "──── Verdict ────")

        val bass = g.getService(BASS_SERVICE)
        if (bass == null) {
            Log.w(TAG, "BASS (0x184F) NOT exposed to this app.")
            Log.w(TAG, "The GATT route is closed — fall back to the USB dongle.")
            return
        }

        Log.i(TAG, "BASS (0x184F) IS exposed.")

        val controlPoint = bass.getCharacteristic(BASS_CONTROL_POINT)
        if (controlPoint == null) {
            Log.w(TAG, "  but the control point (0x2BC7) is missing — cannot write sources.")
            return
        }

        val writable =
            controlPoint.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    controlPoint.properties and
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0

        Log.i(TAG, "  control point present, writable = $writable")
        Log.i(TAG, "  Discovery does not prove a write will be ACCEPTED —")
        Log.i(TAG, "  that needs the two-byte Remove Source test next.")
    }

    // ---- helpers ----

    /** Expands a 16-bit Bluetooth UUID into its full 128-bit form. */
    private fun uuid16(short: String): UUID =
        UUID.fromString("0000$short-0000-1000-8000-00805F9B34FB")

    private fun properties(ch: BluetoothGattCharacteristic): String {
        val p = ch.properties
        val flags = mutableListOf<String>()
        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) flags += "read"
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) flags += "write"
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) flags += "writeNR"
        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) flags += "notify"
        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) flags += "indicate"
        return if (flags.isEmpty()) "none" else flags.joinToString(",")
    }

    private fun stateName(state: Int): String = when (state) {
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "UNKNOWN($state)"
    }

    private fun typeName(type: Int): String = when (type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "classic"
        BluetoothDevice.DEVICE_TYPE_LE -> "LE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "dual"
        else -> "unknown"
    }
}