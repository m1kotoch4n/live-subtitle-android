package com.faqxd.livesub.android.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Port of `pcm_processor.py:PCM16Downsampler`.
 *
 * Convert multi-channel Float32 PCM at an arbitrary sample rate to mono
 * 16 kHz PCM16 little-endian bytes, suitable for Gemini Live's
 * `audio/pcm;rate=16000` realtime input.
 *
 * Uses linear interpolation (same as the Python implementation); Android's
 * AudioRecord normally hands us 16 kHz mono already when we request it, so
 * in practice the resampler is rarely exercised — but we keep it for parity
 * with the Windows / macOS versions that capture at the device's native rate.
 */
class PCM16Downsampler(private val targetRate: Int = 16000) {

    /**
     * @param samples Float32 PCM samples, shape (frames,) or (frames, channels)
     *        flattened row-major (i.e. `samples[frame * channels + ch]`).
     * @param srcRate Native sample rate of the input.
     * @param channels Number of interleaved channels in `samples`.
     * @return Mono 16 kHz PCM16 bytes (little-endian).
     */
    fun convert(samples: FloatArray, srcRate: Int, channels: Int = 1): ByteArray {
        if (samples.isEmpty() || srcRate <= 0 || channels <= 0) return ByteArray(0)

        // Downmix to mono
        val mono: FloatArray = if (channels == 1) {
            samples
        } else {
            val frameCount = samples.size / channels
            FloatArray(frameCount) { f ->
                var sum = 0f
                for (ch in 0 until channels) sum += samples[f * channels + ch]
                sum / channels
            }
        }

        // Resample (linear interpolation)
        val resampled: FloatArray = if (srcRate == targetRate) {
            mono
        } else {
            val ratio = targetRate.toFloat() / srcRate
            val outCount = max(1, (mono.size * ratio).toInt())
            FloatArray(outCount) { i ->
                val srcPos = i / ratio
                val lower = min(mono.size - 1, max(0, srcPos.toInt()))
                val upper = min(mono.size - 1, lower + 1)
                val frac = srcPos - lower
                mono[lower] + (mono[upper] - mono[lower]) * frac
            }
        }

        // Clip + convert to int16 LE
        val out = ByteArray(resampled.size * 2)
        val shortBuf: ShortBuffer =
            ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        for (i in resampled.indices) {
            val v = resampled[i]
            val clamped = when {
                v > 1f -> 1f
                v < -1f -> -1f
                else -> v
            }
            shortBuf.put((clamped * 32767f).toInt().toShort())
        }
        return out
    }

    /** Convenience: decode raw Float32 little-endian bytes first. */
    fun convertBytes(float32Le: ByteArray, srcRate: Int, channels: Int = 1): ByteArray {
        if (float32Le.size % 4 != 0) return ByteArray(0)
        val floats = FloatArray(float32Le.size / 4)
        val buf = ByteBuffer.wrap(float32Le).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        buf.get(floats)
        return convert(floats, srcRate, channels)
    }
}

/**
 * Port of `pcm_processor.py:PCM16Chunker`.
 *
 * Accumulate PCM16 bytes and emit fixed-size chunks (default 3200 bytes =
 * 100 ms of 16 kHz mono int16). Thread-safe.
 */
class PCM16Chunker(
    private val chunkSize: Int = 3200,
    private val onChunk: (ByteArray) -> Unit,
) {
    private val lock = Any()
    private var pending: ByteArray = ByteArray(0)

    fun append(data: ByteArray) {
        if (data.isEmpty()) return
        synchronized(lock) {
            val buf = pending + data
            var offset = 0
            while (buf.size - offset >= chunkSize) {
                onChunk(buf.copyOfRange(offset, offset + chunkSize))
                offset += chunkSize
            }
            pending = if (offset > 0) buf.copyOfRange(offset, buf.size) else buf
        }
    }

    fun reset() {
        synchronized(lock) {
            pending = ByteArray(0)
        }
    }
}
