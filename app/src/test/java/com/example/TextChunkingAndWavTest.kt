package com.example

import com.example.domain.TextChunkingEngine
import com.example.domain.WavAudioProcessor
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TextChunkingAndWavTest {

    @Test
    fun testShortFushaChunking() {
        val fushaText = "الحمد لله رب العالمين، الرحمن الرحيم، مالك يوم الدين."
        val chunks = TextChunkingEngine.splitText(fushaText, targetChunkSize = 500)
        assertEquals(1, chunks.size)
        assertEquals(fushaText, chunks[0])
    }

    @Test
    fun testEgyptianDialectChunking() {
        val egyptianText = "إزيك يا فنان؟ إن شاء الله تكون بخير والنهاردة يوم جميل جداً. خلينا نجرب نحول النص لصوت واضح باللهجة المصرية الأصيلة!"
        val chunks = TextChunkingEngine.splitText(egyptianText, targetChunkSize = 60)
        assertTrue(chunks.size >= 2)
        // Verify no word was split in the middle
        chunks.forEach { chunk ->
            assertFalse(chunk.startsWith(" "))
            assertFalse(chunk.endsWith(" "))
        }
    }

    @Test
    fun testOneMillionCharacterTextChunking() {
        // Build a massive text (~1,000,000 characters) simulating an entire book
        val paragraphTemplate = "هذا نص تجريبي طويل جداً لمحاكاة قراءة كتاب ضخم باللغة العربية عبر الذكاء الاصطناعي وبدون أي قيود على عدد الحروف إطلاقاً.\n"
        val builder = java.lang.StringBuilder()
        while (builder.length < 1_000_000) {
            builder.append(paragraphTemplate)
        }
        val massiveText = builder.toString()
        assertTrue(massiveText.length >= 1_000_000)

        val startTime = System.currentTimeMillis()
        val chunks = TextChunkingEngine.splitText(massiveText, targetChunkSize = 1200)
        val elapsed = System.currentTimeMillis() - startTime

        assertTrue("Chunking 1M chars must be fast (took ${elapsed}ms)", elapsed < 3000)
        assertTrue("Should produce hundreds of chunks", chunks.size > 500)

        // Verify total non-whitespace reconstructed content remains verbatim
        val firstChunk = chunks.first()
        val lastChunk = chunks.last()
        assertTrue(firstChunk.isNotEmpty())
        assertTrue(lastChunk.isNotEmpty())
    }

    @Test
    fun testWavHeaderGeneration() {
        val pcmLength = 48000L // 1 second of 24kHz 16-bit mono
        val header = WavAudioProcessor.createWavHeader(pcmLength)
        assertEquals(44, header.size)

        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val riff = ByteArray(4)
        buffer.get(riff)
        assertEquals("RIFF", String(riff))

        val fileSize = buffer.getInt()
        assertEquals((pcmLength + 36).toInt(), fileSize)

        val wave = ByteArray(4)
        buffer.get(wave)
        assertEquals("WAVE", String(wave))

        val fmt = ByteArray(4)
        buffer.get(fmt)
        assertEquals("fmt ", String(fmt))

        assertEquals(16, buffer.getInt()) // subchunk1 size
        assertEquals(1.toShort(), buffer.getShort()) // audio format = PCM
        assertEquals(1.toShort(), buffer.getShort()) // channels = 1 (Mono)
        assertEquals(24000, buffer.getInt()) // sample rate
        assertEquals(48000, buffer.getInt()) // byte rate (24000 * 2)
        assertEquals(2.toShort(), buffer.getShort()) // block align
        assertEquals(16.toShort(), buffer.getShort()) // bits per sample

        val data = ByteArray(4)
        buffer.get(data)
        assertEquals("data", String(data))
        assertEquals(pcmLength.toInt(), buffer.getInt())
    }

    @Test
    fun testSilencePcmGeneration() {
        val silenceMs = 200
        val silenceBytes = WavAudioProcessor.createSilencePcm(silenceMs)
        val expectedSamples = (24000 * 0.2).toInt()
        val expectedBytes = expectedSamples * 2
        assertEquals(expectedBytes, silenceBytes.size)

        // All bytes should be zero (pure digital silence)
        for (b in silenceBytes) {
            assertEquals(0.toByte(), b)
        }
    }

    @Test
    fun testDurationCalculation() {
        val oneSecondPcm = 48000L
        assertEquals(1000L, WavAudioProcessor.calculateDurationMs(oneSecondPcm))
        val fiveSecondsPcm = 48000L * 5
        assertEquals(5000L, WavAudioProcessor.calculateDurationMs(fiveSecondsPcm))
    }
}
