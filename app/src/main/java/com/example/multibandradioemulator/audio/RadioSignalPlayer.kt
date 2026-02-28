package com.example.multibandradioemulator.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.multibandradioemulator.model.AntennaType
import com.example.multibandradioemulator.audio.bpc.BpcRenderer
import com.example.multibandradioemulator.audio.dcf77.Dcf77Renderer
import com.example.multibandradioemulator.audio.jjy.JjyRenderer
import com.example.multibandradioemulator.audio.wwvb.WwvbRenderer
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages real-time audio playback of time signal broadcasts via the device speaker.
 *
 * Generates PCM audio second-by-second using the selected protocol renderer,
 * synchronized to the system clock. Uses Android's AudioTrack in streaming mode.
 */
class RadioSignalPlayer {

    private val isPlaying = AtomicBoolean(false)
    private var playbackThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    val playing: Boolean get() = isPlaying.get()

    companion object {
        private const val TAG = "RadioSignalPlayer"
        private const val SAMPLE_RATE = 48000
    }

    private fun getRenderer(antennaType: AntennaType): TimeSignalRenderer {
        return when (antennaType) {
            AntennaType.DCF77 -> Dcf77Renderer()
            AntennaType.WWVB -> WwvbRenderer()
            AntennaType.JJY -> JjyRenderer()
            AntennaType.BPC -> BpcRenderer()
        }
    }

    /**
     * Start playing the time signal for the specified antenna type.
     * Playback will continue until [stop] is called.
     */
    fun start(antennaType: AntennaType) {
        if (isPlaying.getAndSet(true)) return // Already playing

        val renderer = getRenderer(antennaType)
        val freq = renderer.carrierFrequencies[1].toDouble() // Use middle frequency
        val signalShape = SignalShape.SQUARE // Square wave for stronger harmonics
        val amplitudeDeviation = renderer.amplitudeDeviation

        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            SAMPLE_RATE * 2 // At least 1 second of buffer
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        playbackThread = Thread({
            Log.i(TAG, "Playback started: ${antennaType.displayName}, freq=$freq Hz")

            try {
                var currentRecord: TimeSignalRecord? = null
                var currentRecordMinute = -1

                while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
                    val now = ZonedDateTime.now()
                    val currentSecond = now.second

                    // Generate a new record at the start of each minute.
                    // Time signal protocols encode the NEXT minute's time,
                    // so we add 1 minute when creating the record.
                    val minuteKey = now.hour * 60 + now.minute
                    if (currentRecord == null || minuteKey != currentRecordMinute) {
                        val nextMinute = now.plusMinutes(1).withSecond(0).withNano(0)
                        currentRecord = renderer.makeTimeSignalRecord(nextMinute)
                        currentRecordMinute = minuteKey
                        Log.d(TAG, "New record for minute $minuteKey (encoding ${nextMinute.hour}:${nextMinute.minute}), second=$currentSecond")
                    }

                    // Render and play one second of audio
                    val pcmData = renderer.renderSecondPcm(
                        record = currentRecord,
                        secondIndex = currentSecond,
                        freq = freq,
                        sampleRate = SAMPLE_RATE,
                        signalShape = signalShape,
                        amplitudeDeviation = amplitudeDeviation
                    )

                    // Write PCM data to AudioTrack (blocking)
                    var offset = 0
                    var remaining = pcmData.size
                    while (remaining > 0 && isPlaying.get()) {
                        val written = track.write(pcmData, offset, remaining)
                        if (written < 0) {
                            Log.e(TAG, "AudioTrack write error: $written")
                            break
                        }
                        offset += written
                        remaining -= written
                    }

                    // Synchronize to the next second boundary
                    val elapsed = System.currentTimeMillis() % 1000
                    val sleepMs = if (elapsed < 50) 0L else maxOf(0L, 1000L - elapsed - 50)
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs)
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                Log.e(TAG, "Playback error", e)
            } finally {
                Log.i(TAG, "Playback stopped")
            }
        }, "radio-signal-playback").apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
            start()
        }
    }

    /**
     * Stop playback and release audio resources.
     */
    fun stop() {
        if (!isPlaying.getAndSet(false)) return

        playbackThread?.interrupt()
        playbackThread = null

        audioTrack?.let { track ->
            try {
                track.stop()
                track.flush()
                track.release()
            } catch (_: Exception) {
            }
        }
        audioTrack = null
    }

    /**
     * Release all resources. Call when done with this player.
     */
    fun release() {
        stop()
    }
}
