package com.example.domain

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object WavAudioProcessor {

    const val SAMPLE_RATE = 24000
    const val CHANNELS = 1 // Mono
    const val BITS_PER_SAMPLE = 16
    const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
    const val BYTE_RATE = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE // 48000 bytes/sec

    /**
     * Creates a 44-byte RIFF WAV header for standard 16-bit Mono PCM.
     */
    fun createWavHeader(pcmDataLength: Long): ByteArray {
        val totalDataLen = pcmDataLength + 36
        val buffer = ByteBuffer.allocate(44)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk descriptor
        buffer.put('R'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.put('F'.code.toByte())
        buffer.putInt((totalDataLen and 0xFFFFFFFFL).toInt())
        buffer.put('W'.code.toByte())
        buffer.put('A'.code.toByte())
        buffer.put('V'.code.toByte())
        buffer.put('E'.code.toByte())

        // "fmt " sub-chunk
        buffer.put('f'.code.toByte())
        buffer.put('m'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put(' '.code.toByte())
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat: 1 = PCM
        buffer.putShort(CHANNELS.toShort()) // Mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(BYTE_RATE)
        buffer.putShort((CHANNELS * BYTES_PER_SAMPLE).toShort()) // BlockAlign = 2
        buffer.putShort(BITS_PER_SAMPLE.toShort())

        // "data" sub-chunk
        buffer.put('d'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.put('t'.code.toByte())
        buffer.put('a'.code.toByte())
        buffer.putInt((pcmDataLength and 0xFFFFFFFFL).toInt())

        return buffer.array()
    }

    /**
     * Generates silence PCM samples for the specified milliseconds.
     */
    fun createSilencePcm(durationMs: Int): ByteArray {
        val samples = (SAMPLE_RATE.toDouble() * (durationMs.toDouble() / 1000.0)).toInt()
        val byteCount = samples * CHANNELS * BYTES_PER_SAMPLE
        return ByteArray(byteCount) // All zeros is pure digital silence in 16-bit signed PCM
    }

    /**
     * Initializes a new temporary audio recording file with a placeholder 44-byte header.
     */
    fun startWavFile(targetFile: File): FileOutputStream {
        if (targetFile.exists()) targetFile.delete()
        targetFile.parentFile?.mkdirs()
        val fos = FileOutputStream(targetFile)
        // Write blank 44-byte header placeholder
        fos.write(ByteArray(44))
        return fos
    }

    /**
     * Updates the RIFF WAV header with the actual final PCM bytes written.
     */
    fun finalizeWavFile(targetFile: File) {
        val totalFileLength = targetFile.length()
        val pcmDataLength = if (totalFileLength >= 44) totalFileLength - 44 else 0
        val header = createWavHeader(pcmDataLength)

        RandomAccessFile(targetFile, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    /**
     * Calculates duration in milliseconds from PCM data length.
     */
    fun calculateDurationMs(pcmDataLength: Long): Long {
        if (BYTE_RATE <= 0) return 0L
        return (pcmDataLength * 1000L) / BYTE_RATE
    }

    /**
     * Pitch-preserving time stretch / speed modification on a 16-bit Mono WAV file using
     * Synchronized Overlap-Add (SOLA) algorithm implemented in pure Kotlin.
     */
    fun applySpeedToWavFile(
        sourceWavFile: File,
        targetWavFile: File,
        speed: Float
    ) {
        if (speed in 0.98f..1.02f) {
            // Speed is 1.0x - direct stream copy
            sourceWavFile.copyTo(targetWavFile, overwrite = true)
            return
        }

        if (sourceWavFile.length() <= 44) {
            sourceWavFile.copyTo(targetWavFile, overwrite = true)
            return
        }

        val pcmInput = ByteArray((sourceWavFile.length() - 44).toInt())
        FileInputStream(sourceWavFile).use { fis ->
            fis.skip(44)
            fis.read(pcmInput)
        }

        // Convert PCM bytes to Short samples
        val sampleCount = pcmInput.size / 2
        val inputSamples = ShortArray(sampleCount)
        ByteBuffer.wrap(pcmInput).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inputSamples)

        val outputSamples = solaTimeStretch(inputSamples, speed)

        // Write output WAV
        val outputPcmBytes = ByteArray(outputSamples.size * 2)
        ByteBuffer.wrap(outputPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputSamples)

        if (targetWavFile.exists()) targetWavFile.delete()
        targetWavFile.parentFile?.mkdirs()

        FileOutputStream(targetWavFile).use { fos ->
            fos.write(createWavHeader(outputPcmBytes.size.toLong()))
            fos.write(outputPcmBytes)
        }
    }

    /**
     * Synchronized Overlap-Add (SOLA) time scale modification.
     * Alters playback duration without modifying pitch.
     */
    private fun solaTimeStretch(input: ShortArray, speed: Float): ShortArray {
        val boundedSpeed = speed.coerceIn(0.25f, 3.0f)
        val windowSize = (SAMPLE_RATE * 0.025).toInt() // 25ms window (~600 samples)
        val synthesisHop = (windowSize / 2) // 50% overlap
        val analysisHop = (synthesisHop * boundedSpeed).toInt().coerceAtLeast(1)
        val searchWindow = (windowSize * 0.35).toInt() // Search range around expected position

        val estimatedOutputSize = (input.size / boundedSpeed).toInt() + windowSize
        val output = FloatArray(estimatedOutputSize)
        val weight = FloatArray(estimatedOutputSize)

        // Hanning window function
        val hanning = FloatArray(windowSize) { i ->
            (0.5 - 0.5 * kotlin.math.cos(2.0 * Math.PI * i / (windowSize - 1))).toFloat()
        }

        var inPos = 0
        var outPos = 0

        while (inPos + windowSize + searchWindow < input.size && outPos + windowSize < estimatedOutputSize) {
            // Find best cross-correlation alignment to prevent phase cancellations
            var bestOffset = 0
            var maxCorrelation = Long.MIN_VALUE

            val searchStart = inPos
            val searchEnd = min(input.size - windowSize, inPos + searchWindow)

            for (offset in 0 until (searchEnd - searchStart)) {
                var correlation: Long = 0
                val step = 4 // Subsample for performance
                var i = 0
                while (i < windowSize) {
                    val targetSample = input[searchStart + offset + i].toLong()
                    val prevOutSample = output[outPos + i].toLong()
                    correlation += targetSample * prevOutSample
                    i += step
                }
                if (correlation > maxCorrelation) {
                    maxCorrelation = correlation
                    bestOffset = offset
                }
            }

            val actualInPos = searchStart + bestOffset
            for (i in 0 until windowSize) {
                val win = hanning[i]
                output[outPos + i] += input[actualInPos + i] * win
                weight[outPos + i] += win
            }

            inPos += analysisHop
            outPos += synthesisHop
        }

        // Normalize output with accumulated window weights
        val result = ShortArray(outPos)
        for (i in 0 until outPos) {
            val w = weight[i]
            val sample = if (w > 0.0001f) output[i] / w else output[i]
            result[i] = sample.coerceIn(-32768f, 32767f).toInt().toShort()
        }
        return result
    }
}
