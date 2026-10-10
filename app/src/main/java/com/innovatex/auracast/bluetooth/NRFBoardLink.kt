package com.innovatex.auracast.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

/**
 * Talks to the nRF52840 assistant board over a custom GATT service.
 * Sends the board a stop index and listens to what it reports back.
 */
@SuppressLint("MissingPermission")
class NRFBoardLink(
    private val context: Context,
    private val onStateChanged: (NRFBoardState) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit
) {

    private var gatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null
    private var scanning = false

    val isConnected: Boolean
        get() = commandCharacteristic != null

    // Scans for the board + connects
    fun connect() {
        // Check: GATT enabled
        if (gatt != null || scanning) {
            return
        }

        // Create LE Audio scanner
        val scanner = context.getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?.bluetoothLeScanner

        if (scanner == null) {
            Log.w(TAG, "No Bluetooth scanner available")
            return
        }

        // Build filters + settings
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            scanning = true
            Log.i(TAG, "Scanning for the assistant board")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_SCAN permission not granted", e)
        }
    }

    // Disconnect from NRF board
    fun disconnect() {
        // Stop scanning
        stopScan()

        // Close GATT client
        gatt?.let {
            try {
                it.disconnect()
                it.close()
            } catch (e: SecurityException) {}
        }

        // Reset vars
        gatt = null
        commandCharacteristic = null
        statusCharacteristic = null
        onConnectionChanged(false)
    }

    /**
     * Asks the board to join a stop's broadcast.
     *
     * @param stopIndex the transmitter index, or 0 to stop listening
     */
    fun requestStop(stopIndex: Int) {
        val g = gatt
        val characteristic = commandCharacteristic

        // Check: GATT and characteristics enabled
        if (g == null || characteristic == null) {
            Log.w(TAG, "Not connected to the board, dropping request for stop $stopIndex")
            return
        }

        val payload = byteArrayOf(stopIndex.toByte())

        try {
            // Give payload to write to board
            val status = g.writeCharacteristic(
                characteristic,
                payload,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
            Log.i(TAG, "Requested stop $stopIndex (status $status)")
        } catch (e: SecurityException) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission not granted", e)
        }
    }

    // Stop scanning
    private fun stopScan() {
        // Check: not scanning
        if (!scanning) {
            return
        }

        try {
            context.getSystemService(BluetoothManager::class.java)
                ?.adapter
                ?.bluetoothLeScanner
                ?.stopScan(scanCallback)
        } catch (e: SecurityException) {}

        scanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.i(TAG, "Found the board at ${result.device.address}")

            stopScan()

            gatt = result.device.connectGatt(
                context,
                false,
                gattCallback,
                android.bluetooth.BluetoothDevice.TRANSPORT_LE
            )
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code $errorCode")
            scanning = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to the board, discovering services")
                    g.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Board disconnected (status $status)")
                    commandCharacteristic = null
                    statusCharacteristic = null
                    onConnectionChanged(false)
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed, status $status")
                return
            }

            val service = g.getService(SERVICE_UUID)
            if (service == null) {
                Log.w(TAG, "Board has no assistant service")
                return
            }

            commandCharacteristic = service.getCharacteristic(COMMAND_UUID)
            statusCharacteristic = service.getCharacteristic(STATUS_UUID)

            if (commandCharacteristic == null || statusCharacteristic == null) {
                Log.w(TAG, "Assistant service is missing a characteristic")
                return
            }

            /* Readiness is reported from onDescriptorWrite rather than here.
             * Android serialises GATT operations, so a command written while
             * the descriptor write is still in flight is rejected with
             * GATT_WRITE_REQUEST_BUSY (201).
             */
            enableStatusNotifications(g)
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid != CCC_UUID) {
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Failed to enable status notifications, status $status")
                return
            }

            Log.i(TAG, "Status notifications confirmed, board ready")
            onConnectionChanged(true)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != STATUS_UUID || value.isEmpty()) {
                return
            }

            val state = NRFBoardState.fromByte(value[0].toInt() and 0xFF)
            if (state == null) {
                Log.w(TAG, "Board reported an unknown state: ${value[0]}")
                return
            }

            Log.i(TAG, "Board state: $state")
            onStateChanged(state)
        }
    }

    /**
     * Two steps are needed: telling the Android stack to pass notifications
     * through, and writing the CCC descriptor so the board starts sending them.
     * Skipping the second is the usual reason notifications never arrive.
     */
    private fun enableStatusNotifications(g: BluetoothGatt) {
        val characteristic = statusCharacteristic ?: return

        g.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCC_UUID)
        if (descriptor == null) {
            Log.w(TAG, "Status characteristic has no CCC descriptor")
            return
        }

        val status = g.writeDescriptor(
            descriptor,
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        )

        Log.i(TAG, "Enabled status notifications (status $status)")
    }

    private companion object {
        const val TAG = "BoardLink"

        val SERVICE_UUID: UUID = UUID.fromString("6c618b36-1ac6-4e8a-9797-6db150ca9c5f")
        val COMMAND_UUID: UUID = UUID.fromString("09162189-a913-415d-9d9a-681c6d450517")
        val STATUS_UUID: UUID = UUID.fromString("4d35a4c2-464b-4e22-b21d-c9f904c3d094")

        /** Client Characteristic Configuration, the standard notify-enable descriptor. */
        val CCC_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}