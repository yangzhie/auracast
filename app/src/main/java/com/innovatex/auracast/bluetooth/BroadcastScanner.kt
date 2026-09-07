package com.innovatex.auracast.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.bluetooth.le.ScanCallback
import android.util.Log

class BroadcastScanner(
    private val context: Context,
    private val onBroadcastFound: (ScanResult, BroadcastMetadata) -> Unit
) {
    // Scan flag
    private var isScanning = false

    // Obtain the OS' BLE scanner - computed, not stored
    private val scanner: BluetoothLeScanner?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner

    // Filter - to compare the metadata for the company ID and the magic in each payload
    private fun buildFilter(): ScanFilter =
        ScanFilter.Builder()
            .setManufacturerData(
                MetadataParser.COMPANY_ID,
                byteArrayOf(0x41, 0x55)
            )
            .build()

    // How to scan the payload
    private fun buildSettings(): ScanSettings =
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Continuous scanning
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // How often the receiving is
            .setLegacy(false) // Bigger payloads
            .build()

    // Receives scan results from Bluetooth stack
    private val callback = object : ScanCallback() {
        // Scan all ads
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // Record of all results
            val record = result.scanRecord
            if (record == null) {
                return
            }

            // Length byte, 0xFF byte and company ID are all stripped
            // 9-byte payload parser expects
            val payload = record.getManufacturerSpecificData(MetadataParser.COMPANY_ID)
            // Check: if company ID is present
            if (payload == null) {
                return
            }

            // Null = advertisement isn't one of ours
            // Skip and keep scanning
            val metadata = MetadataParser.parse(payload)
            if (metadata == null) {
                return
            }

            onBroadcastFound(result, metadata)
        }

        // Scanning failed
        override fun onScanFailed(err: Int) {
            Log.e("BroadcastScanner", "Scan failed: $err")
        }
    }

    // Start BLE scanning
    fun start() {
        // Check: if already scanning
        if (isScanning) {
            return
        }

        // Check: BLE scanner has Bluetooth/is off
        val bleScanner = scanner
        if (bleScanner == null) {
            Log.w("BroadcastScanner", "No Bluetooth scanner available")
            return
        }

        // Create the filters + settings
        val filters = listOf(buildFilter())
        val settings = buildSettings()

        try {
            // Start the scanner
            bleScanner.startScan(filters, settings, callback)
            // Modify scan flag
            isScanning = true
        } catch (e: SecurityException) {
            Log.e("BroadcastScanner", "BLUETOOTH_SCAN permission not granted", e)
        }
    }

    // Stop BLE scanning
    fun stop() {
        // Check: if not scanning
        if (!isScanning) {
            return
        }

        try {
            // Stop the scanner
            scanner?.stopScan(callback)
        } catch (e: SecurityException) {}

        // Set scan flag
        isScanning = false
    }
}