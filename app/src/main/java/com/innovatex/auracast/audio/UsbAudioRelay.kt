package com.innovatex.auracast.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat

class UsbAudioRelay(
    context: Context,
    private val onError: (String) -> Unit = {}
) {

    private val appContext =
        context.applicationContext

    private var recorder:
            AudioRecord? = null

    private var player:
            AudioTrack? = null

    private var relayThread:
            Thread? = null

    @Volatile
    private var running =
        false

    val isRunning: Boolean
        get() {

            if (running) {
                return true
            }

            return false
        }

    fun start(
        usbInput: AudioDeviceInfo,
        hearingOutput: AudioDeviceInfo
    ): Boolean {

        if (running) {
            return true
        }

        val permissionGranted =
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {

            reportError(
                "RECORD_AUDIO permission has not been granted."
            )

            return false
        }

        logInputCapabilities(
            usbInput
        )

        logOutputCapabilities(
            hearingOutput
        )

        val channelCount =
            chooseChannelCount(
                usbInput
            )

        if (channelCount == null) {

            reportError(
                "The FMA120 USB audio channel configuration could not be determined."
            )

            return false
        }

        val inputChannelMask: Int
        val outputChannelMask: Int

        if (channelCount == 1) {

            inputChannelMask =
                AudioFormat.CHANNEL_IN_MONO

            outputChannelMask =
                AudioFormat.CHANNEL_OUT_MONO

        } else if (channelCount == 2) {

            inputChannelMask =
                AudioFormat.CHANNEL_IN_STEREO

            outputChannelMask =
                AudioFormat.CHANNEL_OUT_STEREO

        } else {

            reportError(
                "Unsupported FMA120 channel count: $channelCount"
            )

            return false
        }

        val audioConfiguration =
            findAudioConfiguration(
                usbInput = usbInput,
                inputChannelMask = inputChannelMask,
                outputChannelMask = outputChannelMask
            )

        if (audioConfiguration == null) {

            reportError(
                "No supported FMA120 USB audio format was found."
            )

            return false
        }

        val sampleRate =
            audioConfiguration.sampleRate

        val recorderBufferSize =
            audioConfiguration.recorderBufferSize

        val playerBufferSize =
            audioConfiguration.playerBufferSize

        var bufferSize =
            recorderBufferSize

        if (playerBufferSize > bufferSize) {
            bufferSize =
                playerBufferSize
        }

        Log.i(
            TAG,
            "Selected sample rate = $sampleRate"
        )

        Log.i(
            TAG,
            "Selected channel count = $channelCount"
        )

        Log.i(
            TAG,
            "Recorder buffer size = $recorderBufferSize"
        )

        Log.i(
            TAG,
            "Player buffer size = $playerBufferSize"
        )

        val inputFormat =
            AudioFormat.Builder()
                .setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                )
                .setSampleRate(
                    sampleRate
                )
                .setChannelMask(
                    inputChannelMask
                )
                .build()

        val outputFormat =
            AudioFormat.Builder()
                .setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                )
                .setSampleRate(
                    sampleRate
                )
                .setChannelMask(
                    outputChannelMask
                )
                .build()

        val newRecorder: AudioRecord

        try {

            newRecorder =
                AudioRecord.Builder()
                    .setAudioSource(
                        MediaRecorder.AudioSource.DEFAULT
                    )
                    .setAudioFormat(
                        inputFormat
                    )
                    .setBufferSizeInBytes(
                        bufferSize
                    )
                    .build()

        } catch (
            exception: Exception
        ) {

            reportError(
                "Unable to create AudioRecord: ${exception.message}"
            )

            return false
        }

        if (
            newRecorder.state !=
            AudioRecord.STATE_INITIALIZED
        ) {

            newRecorder.release()

            reportError(
                "AudioRecord could not be initialized."
            )

            return false
        }

        val inputDeviceSelected =
            newRecorder.setPreferredDevice(
                usbInput
            )

        if (!inputDeviceSelected) {

            newRecorder.release()

            reportError(
                "Android did not accept the FMA120 as the preferred audio input."
            )

            return false
        }

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SPEECH
                )
                .build()

        val newPlayer: AudioTrack

        try {

            newPlayer =
                AudioTrack.Builder()
                    .setAudioAttributes(
                        audioAttributes
                    )
                    .setAudioFormat(
                        outputFormat
                    )
                    .setBufferSizeInBytes(
                        bufferSize
                    )
                    .setTransferMode(
                        AudioTrack.MODE_STREAM
                    )
                    .build()

        } catch (
            exception: Exception
        ) {

            newRecorder.release()

            reportError(
                "Unable to create AudioTrack: ${exception.message}"
            )

            return false
        }

        if (
            newPlayer.state !=
            AudioTrack.STATE_INITIALIZED
        ) {

            newRecorder.release()
            newPlayer.release()

            reportError(
                "AudioTrack could not be initialized."
            )

            return false
        }

        val outputDeviceSelected =
            newPlayer.setPreferredDevice(
                hearingOutput
            )

        if (!outputDeviceSelected) {

            newRecorder.release()
            newPlayer.release()

            reportError(
                "Android did not accept the hearing device as the preferred audio output."
            )

            return false
        }

        recorder =
            newRecorder

        player =
            newPlayer

        try {

            newRecorder.startRecording()

        } catch (
            exception: Exception
        ) {

            cleanupAudioObjects()

            reportError(
                "Unable to start FMA120 audio recording: ${exception.message}"
            )

            return false
        }

        try {

            newPlayer.play()

        } catch (
            exception: Exception
        ) {

            cleanupAudioObjects()

            reportError(
                "Unable to start hearing-device playback: ${exception.message}"
            )

            return false
        }

        running =
            true

        startRelayThread(
            bufferSize
        )

        val routedInput =
            newRecorder.routedDevice

        if (routedInput != null) {

            Log.i(
                TAG,
                "Actual input route type = ${routedInput.type}"
            )
        }

        val routedOutput =
            newPlayer.routedDevice

        if (routedOutput != null) {

            Log.i(
                TAG,
                "Actual output route type = ${routedOutput.type}"
            )
        }

        return true
    }

    fun stop() {

        running =
            false

        val currentThread =
            relayThread

        if (currentThread != null) {

            if (
                Thread.currentThread() !=
                currentThread
            ) {

                try {

                    currentThread.join(
                        1000
                    )

                } catch (
                    exception: InterruptedException
                ) {

                    Thread.currentThread()
                        .interrupt()
                }
            }
        }

        relayThread =
            null

        cleanupAudioObjects()

        Log.i(
            TAG,
            "USB audio relay stopped."
        )
    }

    private fun startRelayThread(
        bufferSize: Int
    ) {

        val thread =
            Thread {

                relayLoop(
                    bufferSize
                )
            }

        thread.name =
            "FmaUsbAudioRelay"

        relayThread =
            thread

        thread.start()
    }

    private fun relayLoop(
        bufferSize: Int
    ) {

        val audioBuffer =
            ByteArray(
                bufferSize
            )

        while (running) {

            val currentRecorder =
                recorder

            val currentPlayer =
                player

            if (currentRecorder == null) {

                reportError(
                    "AudioRecord became unavailable."
                )

                running =
                    false

                break
            }

            if (currentPlayer == null) {

                reportError(
                    "AudioTrack became unavailable."
                )

                running =
                    false

                break
            }

            val bytesRead =
                currentRecorder.read(
                    audioBuffer,
                    0,
                    audioBuffer.size,
                    AudioRecord.READ_BLOCKING
                )

            if (bytesRead > 0) {

                var totalWritten =
                    0

                while (
                    totalWritten <
                    bytesRead &&
                    running
                ) {

                    val bytesWritten =
                        currentPlayer.write(
                            audioBuffer,
                            totalWritten,
                            bytesRead -
                                    totalWritten,
                            AudioTrack.WRITE_BLOCKING
                        )

                    if (bytesWritten > 0) {

                        totalWritten +=
                            bytesWritten

                    } else {

                        reportError(
                            "AudioTrack write failed with code $bytesWritten."
                        )

                        running =
                            false

                        break
                    }
                }

            } else {

                if (
                    bytesRead ==
                    AudioRecord.ERROR_DEAD_OBJECT
                ) {

                    reportError(
                        "FMA120 USB audio device was disconnected."
                    )

                    running =
                        false

                    break
                }

                if (
                    bytesRead ==
                    AudioRecord.ERROR_INVALID_OPERATION
                ) {

                    reportError(
                        "AudioRecord is in an invalid state."
                    )

                    running =
                        false

                    break
                }

                if (
                    bytesRead ==
                    AudioRecord.ERROR_BAD_VALUE
                ) {

                    reportError(
                        "AudioRecord received an invalid audio configuration."
                    )

                    running =
                        false

                    break
                }

                if (
                    bytesRead ==
                    AudioRecord.ERROR
                ) {

                    reportError(
                        "Unknown AudioRecord error."
                    )

                    running =
                        false

                    break
                }
            }
        }
    }

    private fun findAudioConfiguration(
        usbInput: AudioDeviceInfo,
        inputChannelMask: Int,
        outputChannelMask: Int
    ): AudioConfiguration? {

        val sampleRates =
            usbInput.sampleRates

        if (sampleRates.isEmpty()) {

            Log.w(
                TAG,
                "FMA120 did not report any sample rates."
            )

            return null
        }

        for (sampleRate in sampleRates) {

            if (sampleRate <= 0) {
                continue
            }

            val recorderBufferSize =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    inputChannelMask,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            if (
                recorderBufferSize <= 0
            ) {

                continue
            }

            val playerBufferSize =
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    outputChannelMask,
                    AudioFormat.ENCODING_PCM_16BIT
                )

            if (
                playerBufferSize <= 0
            ) {

                continue
            }

            return AudioConfiguration(
                sampleRate =
                    sampleRate,
                recorderBufferSize =
                    recorderBufferSize,
                playerBufferSize =
                    playerBufferSize
            )
        }

        return null
    }

    private fun chooseChannelCount(
        usbInput: AudioDeviceInfo
    ): Int? {

        val channelCounts =
            usbInput.channelCounts

        for (channelCount in channelCounts) {

            if (channelCount == 2) {
                return 2
            }
        }

        for (channelCount in channelCounts) {

            if (channelCount == 1) {
                return 1
            }
        }

        val channelMasks =
            usbInput.channelMasks

        for (channelMask in channelMasks) {

            val channelCount =
                Integer.bitCount(
                    channelMask
                )

            if (channelCount == 2) {
                return 2
            }
        }

        for (channelMask in channelMasks) {

            val channelCount =
                Integer.bitCount(
                    channelMask
                )

            if (channelCount == 1) {
                return 1
            }
        }

        return null
    }

    private fun logInputCapabilities(
        usbInput: AudioDeviceInfo
    ) {

        Log.i(
            TAG,
            "FMA120 USB INPUT"
        )

        Log.i(
            TAG,
            "id = ${usbInput.id}"
        )

        Log.i(
            TAG,
            "type = ${usbInput.type}"
        )

        Log.i(
            TAG,
            "productName = ${usbInput.productName}"
        )

        Log.i(
            TAG,
            "sampleRates = ${
                usbInput.sampleRates.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "channelCounts = ${
                usbInput.channelCounts.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "channelMasks = ${
                usbInput.channelMasks.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "encodings = ${
                usbInput.encodings.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )
    }

    private fun logOutputCapabilities(
        hearingOutput: AudioDeviceInfo
    ) {

        Log.i(
            TAG,
            "HEARING OUTPUT"
        )

        Log.i(
            TAG,
            "id = ${hearingOutput.id}"
        )

        Log.i(
            TAG,
            "type = ${hearingOutput.type}"
        )

        Log.i(
            TAG,
            "productName = ${hearingOutput.productName}"
        )

        Log.i(
            TAG,
            "sampleRates = ${
                hearingOutput.sampleRates.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "channelCounts = ${
                hearingOutput.channelCounts.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "channelMasks = ${
                hearingOutput.channelMasks.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )

        Log.i(
            TAG,
            "encodings = ${
                hearingOutput.encodings.joinToString(
                    prefix = "[",
                    postfix = "]"
                )
            }"
        )
    }

    private fun cleanupAudioObjects() {

        val currentRecorder =
            recorder

        if (currentRecorder != null) {

            try {

                if (
                    currentRecorder.recordingState ==
                    AudioRecord.RECORDSTATE_RECORDING
                ) {

                    currentRecorder.stop()
                }

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to stop AudioRecord.",
                    exception
                )
            }

            try {

                currentRecorder.release()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to release AudioRecord.",
                    exception
                )
            }
        }

        val currentPlayer =
            player

        if (currentPlayer != null) {

            try {

                if (
                    currentPlayer.playState ==
                    AudioTrack.PLAYSTATE_PLAYING
                ) {

                    currentPlayer.stop()
                }

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to stop AudioTrack.",
                    exception
                )
            }

            try {

                currentPlayer.release()

            } catch (
                exception: Exception
            ) {

                Log.w(
                    TAG,
                    "Unable to release AudioTrack.",
                    exception
                )
            }
        }

        recorder =
            null

        player =
            null
    }

    private fun reportError(
        message: String
    ) {

        Log.e(
            TAG,
            message
        )

        onError(
            message
        )
    }

    private data class AudioConfiguration(
        val sampleRate: Int,
        val recorderBufferSize: Int,
        val playerBufferSize: Int
    )

    companion object {

        private const val TAG =
            "UsbAudioRelay"
    }
}