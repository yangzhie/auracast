package com.innovatex.auracast.bluetooth

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.charset.StandardCharsets

interface FmaTransport {

    fun open(): Boolean

    fun writeLine(
        command: String
    )

    fun close()
}

class FmaUsbTransport(
    context: Context,
    private val onLineReceived: (String) -> Unit,
    private val onError: (String) -> Unit = {}
) : FmaTransport {

    private val appContext =
        context.applicationContext

    private val usbManager =
        appContext.getSystemService(
            UsbManager::class.java
        )

    private val mainHandler =
        Handler(
            Looper.getMainLooper()
        )

    private var device:
            UsbDevice? = null

    private var connection:
            UsbDeviceConnection? = null

    private var controlInterface:
            UsbInterface? = null

    private var inputEndpoint:
            UsbEndpoint? = null

    private var outputEndpoint:
            UsbEndpoint? = null

    private var readerThread:
            Thread? = null

    @Volatile
    private var reading =
        false

    private val writeLock =
        Any()

    val isOpen: Boolean
        get() {

            if (connection != null) {
                return true
            }

            return false
        }

    override fun open(): Boolean {

        if (connection != null) {
            return true
        }

        val manager =
            usbManager

        if (manager == null) {

            reportError(
                "USB service is unavailable."
            )

            return false
        }

        val foundDevice =
            findDevice(
                appContext
            )

        if (foundDevice == null) {

            reportError(
                "FMA120 control interface was not found."
            )

            return false
        }

        logDeviceInformation(
            foundDevice
        )

        if (!manager.hasPermission(foundDevice)) {

            device =
                foundDevice

            reportError(
                "USB permission has not been granted."
            )

            return false
        }

        val channel =
            findControlChannel(
                foundDevice
            )

        if (channel == null) {

            reportError(
                "FMA120 BAI control interface could not be found."
            )

            return false
        }

        val openedConnection =
            manager.openDevice(
                foundDevice
            )

        if (openedConnection == null) {

            reportError(
                "Unable to open FMA120 USB device."
            )

            return false
        }

        val claimed =
            openedConnection.claimInterface(
                channel.usbInterface,
                true
            )

        if (!claimed) {

            openedConnection.close()

            reportError(
                "Unable to claim FMA120 control interface."
            )

            return false
        }

        device =
            foundDevice

        connection =
            openedConnection

        controlInterface =
            channel.usbInterface

        inputEndpoint =
            channel.inputEndpoint

        outputEndpoint =
            channel.outputEndpoint

        startReader()

        Log.i(
            TAG,
            "FMA120 control interface opened."
        )

        return true
    }

    fun requestPermission(
        pendingIntent: PendingIntent
    ): Boolean {

        val manager =
            usbManager

        if (manager == null) {

            reportError(
                "USB service is unavailable."
            )

            return false
        }

        var foundDevice =
            device

        if (foundDevice == null) {

            foundDevice =
                findDevice(
                    appContext
                )
        }

        if (foundDevice == null) {

            reportError(
                "FMA120 USB device was not found."
            )

            return false
        }

        device =
            foundDevice

        if (manager.hasPermission(foundDevice)) {
            return true
        }

        manager.requestPermission(
            foundDevice,
            pendingIntent
        )

        return true
    }

    fun hasPermission(): Boolean {

        val manager =
            usbManager

        if (manager == null) {
            return false
        }

        var foundDevice =
            device

        if (foundDevice == null) {

            foundDevice =
                findDevice(
                    appContext
                )
        }

        if (foundDevice == null) {
            return false
        }

        if (manager.hasPermission(foundDevice)) {
            return true
        }

        return false
    }

    override fun writeLine(
        command: String
    ) {

        val currentConnection =
            connection

        if (currentConnection == null) {

            reportError(
                "FMA120 USB connection is not open."
            )

            return
        }

        val endpoint =
            outputEndpoint

        if (endpoint == null) {

            reportError(
                "FMA120 output endpoint is unavailable."
            )

            return
        }

        val cleanCommand =
            command.trimEnd(
                '\r',
                '\n'
            )

        val completeCommand =
            "$cleanCommand\r\n"

        val data =
            completeCommand.toByteArray(
                StandardCharsets.US_ASCII
            )

        val result: Int

        synchronized(writeLock) {

            result =
                currentConnection.bulkTransfer(
                    endpoint,
                    data,
                    data.size,
                    WRITE_TIMEOUT_MS
                )
        }

        if (result < 0) {

            if (!isCurrentDeviceConnected()) {

                reportError(
                    "FMA120 was disconnected."
                )

                close()

                return
            }

            reportError(
                "Failed to write command to FMA120."
            )

            return
        }

        if (result != data.size) {

            reportError(
                "Only $result of ${data.size} bytes were written."
            )

            return
        }

        Log.d(
            TAG,
            "TX: $cleanCommand"
        )
    }

    override fun close() {

        reading =
            false

        val currentThread =
            readerThread

        if (currentThread != null) {

            if (Thread.currentThread() != currentThread) {

                try {

                    currentThread.join(
                        500
                    )

                } catch (
                    exception: InterruptedException
                ) {

                    Thread.currentThread()
                        .interrupt()
                }
            }
        }

        readerThread =
            null

        val currentConnection =
            connection

        val currentInterface =
            controlInterface

        if (
            currentConnection != null &&
            currentInterface != null
        ) {

            try {

                currentConnection.releaseInterface(
                    currentInterface
                )

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to release FMA120 control interface.",
                    exception
                )
            }
        }

        if (currentConnection != null) {

            try {

                currentConnection.close()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to close FMA120 USB connection.",
                    exception
                )
            }
        }

        connection =
            null

        controlInterface =
            null

        inputEndpoint =
            null

        outputEndpoint =
            null

        device =
            null

        Log.i(
            TAG,
            "FMA120 USB connection closed."
        )
    }

    private fun startReader() {

        if (reading) {
            return
        }

        reading =
            true

        val thread =
            Thread {

                readLoop()
            }

        thread.name =
            "Fma120UsbReader"

        readerThread =
            thread

        thread.start()
    }

    private fun readLoop() {

        val buffer =
            ByteArray(
                READ_BUFFER_SIZE
            )

        val currentLine =
            StringBuilder()

        while (reading) {

            val currentConnection =
                connection

            val endpoint =
                inputEndpoint

            if (currentConnection == null) {
                break
            }

            if (endpoint == null) {
                break
            }

            val count =
                currentConnection.bulkTransfer(
                    endpoint,
                    buffer,
                    buffer.size,
                    READ_TIMEOUT_MS
                )

            if (count > 0) {

                val receivedText =
                    String(
                        buffer,
                        0,
                        count,
                        StandardCharsets.US_ASCII
                    )

                processCharacters(
                    receivedText,
                    currentLine
                )

            } else {

                if (!isCurrentDeviceConnected()) {

                    reading =
                        false

                    reportError(
                        "FMA120 USB device was disconnected."
                    )

                    break
                }
            }
        }
    }

    private fun processCharacters(
        text: String,
        currentLine: StringBuilder
    ) {

        for (character in text) {

            if (
                character == '\r' ||
                character == '\n'
            ) {

                if (currentLine.isNotEmpty()) {

                    val line =
                        currentLine
                            .toString()
                            .trim()

                    currentLine.clear()

                    if (line.isNotEmpty()) {

                        Log.d(
                            TAG,
                            "RX: $line"
                        )

                        deliverLine(
                            line
                        )
                    }
                }

            } else {

                currentLine.append(
                    character
                )
            }
        }
    }

    private fun deliverLine(
        line: String
    ) {

        mainHandler.post {

            onLineReceived(
                line
            )
        }
    }

    private fun reportError(
        message: String
    ) {

        Log.e(
            TAG,
            message
        )

        mainHandler.post {

            onError(
                message
            )
        }
    }

    private fun isCurrentDeviceConnected(): Boolean {

        val manager =
            usbManager

        if (manager == null) {
            return false
        }

        val currentDevice =
            device

        if (currentDevice == null) {
            return false
        }

        val connectedDevices =
            manager.deviceList.values

        for (connectedDevice in connectedDevices) {

            if (
                connectedDevice.deviceId ==
                currentDevice.deviceId
            ) {

                return true
            }
        }

        return false
    }

    companion object {

        private const val TAG =
            "FmaUsbTransport"

        private const val READ_BUFFER_SIZE =
            512

        private const val READ_TIMEOUT_MS =
            250

        private const val WRITE_TIMEOUT_MS =
            1000

        fun findDevice(
            context: Context
        ): UsbDevice? {

            val manager =
                context.applicationContext
                    .getSystemService(
                        UsbManager::class.java
                    )

            if (manager == null) {
                return null
            }

            val devices =
                manager.deviceList.values

            if (devices.isEmpty()) {

                Log.i(
                    TAG,
                    "No USB devices connected."
                )

                return null
            }

            val compatibleDevices =
                mutableListOf<UsbDevice>()

            for (usbDevice in devices) {

                logDeviceInformation(
                    usbDevice
                )

                val channel =
                    findControlChannel(
                        usbDevice
                    )

                if (channel != null) {

                    compatibleDevices.add(
                        usbDevice
                    )
                }
            }

            if (compatibleDevices.isEmpty()) {

                Log.i(
                    TAG,
                    "No USB device with a suitable BAI control interface was found."
                )

                return null
            }

            if (compatibleDevices.size == 1) {

                return compatibleDevices[0]
            }

            Log.w(
                TAG,
                "More than one possible USB control device was found. " +
                        "VID/PID must be confirmed before selecting automatically."
            )

            return null
        }

        fun logUsbDevices(
            context: Context
        ) {

            val manager =
                context.applicationContext
                    .getSystemService(
                        UsbManager::class.java
                    )

            if (manager == null) {

                Log.e(
                    TAG,
                    "USB service is unavailable."
                )

                return
            }

            val devices =
                manager.deviceList.values

            if (devices.isEmpty()) {

                Log.i(
                    TAG,
                    "No USB devices connected."
                )

                return
            }

            for (usbDevice in devices) {

                logDeviceInformation(
                    usbDevice
                )
            }
        }

        private fun findControlChannel(
            usbDevice: UsbDevice
        ): FmaControlChannel? {

            var preferredChannel:
                    FmaControlChannel? = null

            var fallbackChannel:
                    FmaControlChannel? = null

            for (
            interfaceIndex
            in 0 until usbDevice.interfaceCount
            ) {

                val usbInterface =
                    usbDevice.getInterface(
                        interfaceIndex
                    )

                if (
                    usbInterface.interfaceClass ==
                    UsbConstants.USB_CLASS_AUDIO
                ) {

                    continue
                }

                var bulkInput:
                        UsbEndpoint? = null

                var bulkOutput:
                        UsbEndpoint? = null

                for (
                endpointIndex
                in 0 until usbInterface.endpointCount
                ) {

                    val endpoint =
                        usbInterface.getEndpoint(
                            endpointIndex
                        )

                    if (
                        endpoint.type ==
                        UsbConstants.USB_ENDPOINT_XFER_BULK
                    ) {

                        if (
                            endpoint.direction ==
                            UsbConstants.USB_DIR_IN
                        ) {

                            bulkInput =
                                endpoint
                        }

                        if (
                            endpoint.direction ==
                            UsbConstants.USB_DIR_OUT
                        ) {

                            bulkOutput =
                                endpoint
                        }
                    }
                }

                if (
                    bulkInput != null &&
                    bulkOutput != null
                ) {

                    val channel =
                        FmaControlChannel(
                            usbInterface =
                                usbInterface,
                            inputEndpoint =
                                bulkInput,
                            outputEndpoint =
                                bulkOutput
                        )

                    if (
                        usbInterface.interfaceClass ==
                        UsbConstants.USB_CLASS_CDC_DATA
                    ) {

                        preferredChannel =
                            channel

                    } else {

                        if (fallbackChannel == null) {

                            fallbackChannel =
                                channel
                        }
                    }
                }
            }

            if (preferredChannel != null) {

                return preferredChannel
            }

            if (fallbackChannel != null) {

                return fallbackChannel
            }

            return null
        }

        private fun logDeviceInformation(
            usbDevice: UsbDevice
        ) {

            Log.i(
                TAG,
                "USB DEVICE"
            )

            Log.i(
                TAG,
                "vendorId = ${
                    usbDevice.vendorId
                } (0x${
                    usbDevice.vendorId
                        .toString(16)
                        .uppercase()
                })"
            )

            Log.i(
                TAG,
                "productId = ${
                    usbDevice.productId
                } (0x${
                    usbDevice.productId
                        .toString(16)
                        .uppercase()
                })"
            )

            Log.i(
                TAG,
                "interface count = ${
                    usbDevice.interfaceCount
                }"
            )

            for (
            interfaceIndex
            in 0 until usbDevice.interfaceCount
            ) {

                val usbInterface =
                    usbDevice.getInterface(
                        interfaceIndex
                    )

                Log.i(
                    TAG,
                    "Interface[$interfaceIndex]"
                )

                Log.i(
                    TAG,
                    "interface id = ${
                        usbInterface.id
                    }"
                )

                Log.i(
                    TAG,
                    "interface class = ${
                        usbInterface.interfaceClass
                    }"
                )

                Log.i(
                    TAG,
                    "interface subclass = ${
                        usbInterface.interfaceSubclass
                    }"
                )

                Log.i(
                    TAG,
                    "interface protocol = ${
                        usbInterface.interfaceProtocol
                    }"
                )

                Log.i(
                    TAG,
                    "endpoint count = ${
                        usbInterface.endpointCount
                    }"
                )

                for (
                endpointIndex
                in 0 until usbInterface.endpointCount
                ) {

                    val endpoint =
                        usbInterface.getEndpoint(
                            endpointIndex
                        )

                    var direction =
                        "OUT"

                    if (
                        endpoint.direction ==
                        UsbConstants.USB_DIR_IN
                    ) {

                        direction =
                            "IN"
                    }

                    val endpointType =
                        endpointTypeName(
                            endpoint.type
                        )

                    Log.i(
                        TAG,
                        "Endpoint[$endpointIndex] " +
                                "direction=$direction " +
                                "type=$endpointType " +
                                "address=0x${
                                    endpoint.address
                                        .toString(16)
                                        .uppercase()
                                } " +
                                "maxPacketSize=${
                                    endpoint.maxPacketSize
                                }"
                    )
                }
            }
        }

        private fun endpointTypeName(
            type: Int
        ): String {

            if (
                type ==
                UsbConstants.USB_ENDPOINT_XFER_CONTROL
            ) {

                return "CONTROL"
            }

            if (
                type ==
                UsbConstants.USB_ENDPOINT_XFER_ISOC
            ) {

                return "ISOCHRONOUS"
            }

            if (
                type ==
                UsbConstants.USB_ENDPOINT_XFER_BULK
            ) {

                return "BULK"
            }

            if (
                type ==
                UsbConstants.USB_ENDPOINT_XFER_INT
            ) {

                return "INTERRUPT"
            }

            return "UNKNOWN($type)"
        }
    }
}

private data class FmaControlChannel(
    val usbInterface: UsbInterface,
    val inputEndpoint: UsbEndpoint,
    val outputEndpoint: UsbEndpoint
)