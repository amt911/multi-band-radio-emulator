package com.example.multibandradioemulator.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.multibandradioemulator.model.AntennaType
import com.example.multibandradioemulator.audio.bpc.BpcRenderer
import com.example.multibandradioemulator.audio.dcf77.Dcf77Renderer
import com.example.multibandradioemulator.audio.jjy.JjyRenderer
import com.example.multibandradioemulator.audio.msf.MsfRenderer
import com.example.multibandradioemulator.audio.wwvb.WwvbRenderer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

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

    /** Amplitude gain factor, read by the playback thread each second. Thread-safe. */
    @Volatile
    var gain: Float = DEFAULT_GAIN

    val playing: Boolean get() = isPlaying.get()

    companion object {
        private const val TAG = "RadioSignalPlayer"
        private const val SAMPLE_RATE = 48000

        /** Maximum gain value (= full 16-bit scale). */
        const val MAX_GAIN = 4.0f

        /** Default gain — full power for best reception. */
        const val DEFAULT_GAIN = MAX_GAIN
    }

    private fun getRenderer(antennaType: AntennaType): TimeSignalRenderer {
        return when (antennaType) {
            AntennaType.DCF77 -> Dcf77Renderer()
            AntennaType.MSF -> MsfRenderer()
            AntennaType.WWVB -> WwvbRenderer()
            AntennaType.JJY40 -> JjyRenderer(is60kHz = false)
            AntennaType.JJY60 -> JjyRenderer(is60kHz = true)
            AntennaType.BPC -> BpcRenderer()
        }
    }

    /**
     * Start playing the time signal for the specified antenna type.
     * Playback will continue until [stop] is called.
     * Set [gain] property before or during playback to adjust amplitude.
     */
    fun start(antennaType: AntennaType, customTime: ZonedDateTime? = null) {
        if (isPlaying.getAndSet(true)) return // Already playing

        val renderer = getRenderer(antennaType)
        val freq = renderer.carrierFrequencies[1].toDouble() // Use middle frequency
        val signalShape = SignalShape.SIN
        val amplitudeDeviation = renderer.amplitudeDeviation

        // Compute subharmonic and quantization scale (cf. TimeStation's approach).
        // Quantizing the sine wave to sampleRate/subharmonic levels creates a
        // "staircase" waveform whose harmonics land exactly at integer multiples
        // of the carrier frequency — including the real broadcast frequency
        // (e.g. 5th harmonic of 15500 Hz = 77500 Hz for DCF77).
        val targetHz = (antennaType.frequencyKHz * 1000).toInt()
        val subharmonic = (targetHz.toDouble() / freq).roundToInt()
        val quantScale = SAMPLE_RATE / subharmonic

        val bufferSize = maxOf(
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            SAMPLE_RATE * 2 / 5 // ~200ms buffer for low output latency
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

        playbackThread = Thread({
            Log.i(TAG, "Playback started: ${antennaType.displayName}, freq=$freq Hz")

            try {
                // Estimate AudioTrack output latency from buffer size.
                // bufferSize is in bytes; 2 bytes per sample at 16-bit mono.
                val bufferLatencyMs = bufferSize.toLong() * 1000 / (SAMPLE_RATE * 2)

                // Wait for the next second boundary MINUS the buffer latency
                // so the AM modulation pattern reaches the speaker exactly
                // at the wall-clock second boundary.
                val nowMs = System.currentTimeMillis()
                val nextSecondMs = nowMs + (1000L - (nowMs % 1000L))
                val waitMs = maxOf(0L, nextSecondMs - bufferLatencyMs - nowMs)
                Thread.sleep(waitMs)

                track.play()

                // renderTime represents the wall-clock time at which audio
                // will be heard (≈ next second boundary after buffer latency).
                var renderTime = customTime ?: ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(nextSecondMs),
                    ZoneId.systemDefault()
                )
                var currentRecord: TimeSignalRecord? = null
                var currentRecordMinute = -1

                // Running sample counter for phase-continuous carrier generation.
                // Monotonically increases — never resets — so the sine wave has
                // no phase discontinuity at minute boundaries.
                var sampleOffset = 0L

                while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
                    val renderSecond = renderTime.second
                    val minuteKey = renderTime.hour * 60 + renderTime.minute

                    // Generate a new record at each minute boundary.
                    // DCF77 encodes the NEXT minute; others encode the current one.
                    if (currentRecord == null || minuteKey != currentRecordMinute) {
                        val recordTime = if (renderer.encodesNextMinute) {
                            renderTime.plusMinutes(1).withSecond(0).withNano(0)
                        } else {
                            renderTime.withSecond(0).withNano(0)
                        }
                        currentRecord = renderer.makeTimeSignalRecord(recordTime)
                        currentRecordMinute = minuteKey
                        Log.d(TAG, "New record for minute $minuteKey (encoding ${recordTime.hour}:${recordTime.minute}), second=$renderSecond")
                    }

                    // Render one second of audio
                    val pcmData = renderer.renderSecondPcm(
                        record = currentRecord!!,
                        secondIndex = renderSecond,
                        freq = freq,
                        sampleRate = SAMPLE_RATE,
                        signalShape = signalShape,
                        amplitudeDeviation = amplitudeDeviation,
                        sampleOffset = sampleOffset
                    )

                    // Quantize to boost harmonics at target radio frequency
                    applyQuantization(pcmData, quantScale)

                    // Scale amplitude linearly: gain/MAX_GAIN maps to [0..1]
                    val currentGain = gain
                    if (currentGain < MAX_GAIN) {
                        applyLinearGain(pcmData, currentGain)
                    }

                    // Write PCM data to AudioTrack.  The call blocks when the
                    // internal buffer is full, which naturally paces playback
                    // at real-time speed — no manual sleep needed.
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

                    // Advance render cursor and sample offset
                    sampleOffset += SAMPLE_RATE
                    renderTime = renderTime.plusSeconds(1)
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

    /**
     * Reduce the effective resolution of each 16-bit LE sample to [quantScale]
     * discrete levels. This deliberately introduces staircase distortion whose
     * harmonics fall at exact integer multiples of the carrier frequency,
     * boosting the component at the real broadcast frequency (e.g. 77.5 kHz
     * for DCF77). Technique from TimeStation (cf. jjy.luxferre.top).
     */
    private fun applyQuantization(pcm: ByteArray, quantScale: Int) {
        val maxVal = Short.MAX_VALUE.toDouble()
        var i = 0
        while (i < pcm.size - 1) {
            val lo = pcm[i].toInt() and 0xFF
            val hi = pcm[i + 1].toInt()
            val sample = ((hi shl 8) or lo).toDouble() / maxVal
            val quantized = (sample * quantScale).toInt().toDouble() / quantScale
            val pcmValue = (quantized * maxVal).roundToInt()
            pcm[i] = (pcmValue and 0xFF).toByte()
            pcm[i + 1] = ((pcmValue shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    /**
     * Scale every 16-bit LE sample by [gain] / [MAX_GAIN].
     *
     * Because the renderer outputs at full 16-bit scale, dividing by
     * MAX_GAIN creates headroom. The user's gain setting then linearly
     * fills that headroom — at [MAX_GAIN] the output reaches full scale.
     * The waveform shape is preserved exactly: no clipping, no distortion.
     */
    private fun applyLinearGain(pcm: ByteArray, gain: Float) {
        val scale = gain / MAX_GAIN
        var i = 0
        while (i < pcm.size - 1) {
            val lo = pcm[i].toInt() and 0xFF
            val hi = pcm[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val scaled = (sample * scale).roundToInt()
            pcm[i] = (scaled and 0xFF).toByte()
            pcm[i + 1] = ((scaled shr 8) and 0xFF).toByte()
            i += 2
        }
    }
}
