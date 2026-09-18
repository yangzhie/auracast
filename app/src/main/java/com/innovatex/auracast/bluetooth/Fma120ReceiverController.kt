package com.innovatex.auracast.bluetooth

import android.content.Context
import java.util.Locale

class Fma120ReceiverController {

    private val onBroadcastFound:
                (FmaReceiverBroadcast) -> Unit

    private val onReceiveStateChanged:
                (FmaReceiveState) -> Unit

    private val onError:
                (String) -> Unit

    private val transport:
            FmaTransport

    constructor(
        context: Context,
        onBroadcastFound:
            (FmaReceiverBroadcast) -> Unit,
        onReceiveStateChanged:
            (FmaReceiveState) -> Unit,
        onError:
            (String) -> Unit
    ) {

        this.onBroadcastFound =
            onBroadcastFound

        this.onReceiveStateChanged =
            onReceiveStateChanged

        this.onError =
            onError

        this.transport =
            FmaUsbTransport(
                context = context,
                onLineReceived = { line ->

                    handleLine(
                        line
                    )
                },
                onError = { message ->

                    handleTransportError(
                        message
                    )
                }
            )
    }

    constructor(
        transport: FmaTransport,
        onBroadcastFound:
            (FmaReceiverBroadcast) -> Unit,
        onReceiveStateChanged:
            (FmaReceiveState) -> Unit,
        onError:
            (String) -> Unit
    ) {

        this.transport =
            transport

        this.onBroadcastFound =
            onBroadcastFound

        this.onReceiveStateChanged =
            onReceiveStateChanged

        this.onError =
            onError
    }

    fun open(): Boolean {

        val opened =
            transport.open()

        if (!opened) {

            onError(
                "Unable to open the FMA120 receiver."
            )

            return false
        }

        return true
    }

    fun startScan() {

        transport.writeLine(
            "BC:BI"
        )
    }

    fun stopScan() {

        transport.writeLine(
            "BC:BI=00"
        )
    }

    fun receive(
        source: FmaReceiverBroadcast
    ) {

        if (source.broadcastIDs.isEmpty()) {

            onError(
                "The selected FMA120 broadcast has no Broadcast ID."
            )

            return
        }

        transport.writeLine(
            "BC:BA=${source.broadcastIDs}"
        )
    }

    fun stopReceiving() {

        transport.writeLine(
            "BC:BA=00"
        )
    }

    fun provideBroadcastCode(
        sourceId: Int,
        code: String
    ) {

        if (sourceId < 0) {

            onError(
                "Invalid FMA120 source ID."
            )

            return
        }

        if (sourceId > 255) {

            onError(
                "Invalid FMA120 source ID."
            )

            return
        }

        if (code.isEmpty()) {

            onError(
                "Broadcast Code cannot be empty."
            )

            return
        }

        val sourceIdHex =
            String.format(
                Locale.US,
                "%02X",
                sourceId
            )

        transport.writeLine(
            "BC:BK=$sourceIdHex,$code"
        )
    }

    fun close() {

        transport.close()
    }

    private fun handleLine(
        line: String
    ) {

        val cleanLine =
            line.trim()

        if (cleanLine.isEmpty()) {
            return
        }

        val broadcast =
            FmaBaiParser.parseBroadcast(
                cleanLine
            )

        if (broadcast != null) {

            onBroadcastFound(
                broadcast
            )

            return
        }

        val receiveState =
            FmaBaiParser.parseReceiveState(
                cleanLine
            )

        if (receiveState != null) {

            onReceiveStateChanged(
                receiveState
            )

            return
        }

        val isError =
            FmaBaiParser.isError(
                cleanLine
            )

        if (isError) {

            val errorMessage =
                FmaBaiParser.getErrorMessage(
                    cleanLine
                )

            if (errorMessage != null) {

                onError(
                    errorMessage
                )

            } else {

                onError(
                    "FMA120 returned an error."
                )
            }

            return
        }

        val isOk =
            FmaBaiParser.isOk(
                cleanLine
            )

        if (isOk) {
            return
        }
    }

    private fun handleTransportError(
        message: String
    ) {

        if (message.isEmpty()) {

            onError(
                "FMA120 transport error."
            )

            return
        }

        onError(
            message
        )
    }
}