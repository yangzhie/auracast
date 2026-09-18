package com.innovatex.auracast.bluetooth

object FmaBaiParser {

    fun parseBroadcast(
        line: String
    ): FmaReceiverBroadcast? {

        val cleanLine =
            line.trim()

        if (!cleanLine.startsWith("BI=")) {
            return null
        }

        val body =
            cleanLine.removePrefix("BI=")

        val fields =
            body.split(",")

        if (fields.size < 5) {
            return null
        }

        val addressType =
            fields[0]
                .trim()
                .toIntOrNull(16)

        if (addressType == null) {
            return null
        }

        val address =
            fields[1]
                .trim()
                .uppercase()

        if (address.isEmpty()) {
            return null
        }

        val rawRssi =
            fields[2]
                .trim()
                .toIntOrNull(16)

        if (rawRssi == null) {
            return null
        }

        var rssi =
            rawRssi

        if (rawRssi >= 128) {
            rssi =
                rawRssi - 256
        }

        val broadcastIDs =
            fields[3]
                .trim()
                .uppercase()

        if (broadcastIDs.length != 8) {
            return null
        }

        val advertisingSid =
            broadcastIDs
                .substring(
                    0,
                    2
                )
                .toIntOrNull(16)

        if (advertisingSid == null) {
            return null
        }

        val broadcastId =
            broadcastIDs
                .substring(
                    2,
                    8
                )
                .toIntOrNull(16)

        if (broadcastId == null) {
            return null
        }

        val broadcastName =
            fields
                .drop(4)
                .joinToString(",")
                .trim()

        return FmaReceiverBroadcast(
            addressType =
                addressType,
            address =
                address,
            rssi =
                rssi,
            broadcastIDs =
                broadcastIDs,
            advertisingSid =
                advertisingSid,
            broadcastId =
                broadcastId,
            broadcastName =
                broadcastName
        )
    }

    fun parseReceiveState(
        line: String
    ): FmaReceiveState? {

        val cleanLine =
            line.trim()

        if (!cleanLine.startsWith("BA=")) {
            return null
        }

        val body =
            cleanLine.removePrefix("BA=")

        val fields =
            body.split(",")

        if (fields.size < 5) {
            return null
        }

        val sourceId =
            fields[0]
                .trim()
                .toIntOrNull(16)

        if (sourceId == null) {
            return null
        }

        val broadcastIDs =
            fields[1]
                .trim()
                .uppercase()

        if (broadcastIDs.isEmpty()) {
            return null
        }

        val syncState =
            fields[2]
                .trim()
                .toIntOrNull(16)

        if (syncState == null) {
            return null
        }

        val encryptionState =
            fields[3]
                .trim()
                .toIntOrNull(16)

        if (encryptionState == null) {
            return null
        }

        val bisState =
            fields[4]
                .trim()
                .toIntOrNull(16)

        if (bisState == null) {
            return null
        }

        return FmaReceiveState(
            sourceId =
                sourceId,
            broadcastIDs =
                broadcastIDs,
            syncState =
                syncState,
            encryptionState =
                encryptionState,
            bisState =
                bisState
        )
    }

    fun isOk(
        line: String
    ): Boolean {

        val cleanLine =
            line.trim()

        if (cleanLine == "OK") {
            return true
        }

        return false
    }

    fun isError(
        line: String
    ): Boolean {

        val cleanLine =
            line.trim()

        if (cleanLine == "ERROR") {
            return true
        }

        if (cleanLine.startsWith("ERROR")) {
            return true
        }

        return false
    }

    fun getErrorMessage(
        line: String
    ): String? {

        val cleanLine =
            line.trim()

        if (!isError(cleanLine)) {
            return null
        }

        if (cleanLine == "ERROR") {
            return "FMA120 returned an error."
        }

        val message =
            cleanLine
                .removePrefix("ERROR")
                .removePrefix("=")
                .removePrefix(":")
                .trim()

        if (message.isEmpty()) {
            return "FMA120 returned an error."
        }

        return message
    }
}