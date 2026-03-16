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

        /** Default gain — 50% of full scale, leaving headroom for boost. */
        const val DEFAULT_GAIN = 2.0f
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
        // Sine wave is the correct choice at these carrier frequencies (12-19 kHz)
        // because SQUARE/TRIANGLE harmonics exceed the Nyquist limit (24 kHz at 48 kHz
        // sample rate), producing aliased noise instead of useful harmonics.
        // The real harmonics at 77.5/60/40/68.5 kHz are generated mechanically by the
        // speaker's non-linearity, which works best with a clean high-amplitude sine input.
        val signalShape = SignalShape.SIN
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

        playbackThread = Thread({
            Log.i(TAG, "Playback started: ${antennaType.displayName}, freq=$freq Hz")

            try {
                // Wait for the next second boundary so the AM modulation
                // pattern aligns with wall-clock seconds.
                val nowMs = System.currentTimeMillis()
                val waitMs = 1000L - (nowMs % 1000L)
                Thread.sleep(waitMs)

                track.play()

                // Use a render-time cursor that advances by exactly 1 second
                // per loop iteration.  This prevents the old bug where
                // re-reading the wall clock after a blocking write() caused
                // duplicate or skipped seconds.
                var renderTime = customTime ?: ZonedDateTime.now()
                var currentRecord: TimeSignalRecord? = null
                var currentRecordMinute = -1

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
                        amplitudeDeviation = amplitudeDeviation
                    )

                    // Scale amplitude linearly: gain/MAX_GAIN maps to [0..1]
                    // so the waveform shape is preserved exactly (no distortion).
                    applyLinearGain(pcmData, gain)

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

                    // Advance render cursor to the next second
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
