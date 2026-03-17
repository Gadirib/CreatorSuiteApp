package com.example.creatorsuiteapp.ui.screens.rec

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.*
import android.media.audiofx.NoiseSuppressor
import android.media.audiofx.PresetReverb
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*


class RealtimeAudioProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioProcessor"
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_COUNT = 1
        private const val BIT_RATE = 128_000
        private const val MIME = MediaFormat.MIMETYPE_AUDIO_AAC
    }

    data class Config(
        val noiseSuppressionOn: Boolean = false,
        val pitchSemitones: Float = 0f,
        val vocalVolume: Float = 1f,
        val delayMs: Float = 0f,
        val reverbPreset: Int = 0,
        val reverbLevel: Float = 0f,
        val soundResId: Int? = null,
        val soundVolume: Float = 0f
    )

    private var config = Config()
    private var processorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var cachedSoundPcm: ShortArray? = null
    private var cachedSoundResId: Int? = null

    private var videoOnlyFile: File? = null
    private var audioFile: File? = null

    @Volatile private var isRunning = false
    private var audioStartTimeUs = 0L


    fun configure(
        noiseOn: Boolean,
        pitchValue: Float,
        vocalVolume: Float,
        delayValue: Float,
        selectedEffect: String,
        effectLevel: Float,
        selectedSound: String,
        soundVolume: Float
    ) {
        config = Config(
            noiseSuppressionOn = noiseOn,
            pitchSemitones = pitchValue,
            vocalVolume = vocalVolume * 2f,
            delayMs = delayValue * 500f,
            reverbPreset = effectNameToPreset(selectedEffect),
            reverbLevel = effectLevel,
            soundResId = soundNameToRes(context, selectedSound),
            soundVolume = soundVolume
        )
        Log.d(TAG, "Config: $config")
    }

    
    fun start(): String {
        val videoFile = File(context.cacheDir, "rec_video_${System.currentTimeMillis()}.mp4")
        videoOnlyFile = videoFile
        audioFile = File(context.cacheDir, "rec_audio_${System.currentTimeMillis()}.aac")

        isRunning = true
        audioStartTimeUs = System.nanoTime() / 1000L

        processorJob = scope.launch {
            runAudioPipeline()
        }

        return videoFile.absolutePath
    }

    
    suspend fun stop(): Uri? = withContext(Dispatchers.IO) {
        isRunning = false
        processorJob?.join()

        val videoFile = videoOnlyFile ?: return@withContext null
        val audioRaw = audioFile ?: return@withContext null

        delay(300)

        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.w(TAG, "Video file missing or empty: ${videoFile.absolutePath}")
            return@withContext null
        }

        muxVideoAndAudio(videoFile, audioRaw)
    }

    fun release() {
        isRunning = false
        scope.cancel()
    }


    @SuppressLint("MissingPermission")
    private fun runAudioPipeline() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING)
            .coerceAtLeast(4096)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE, CHANNELS, ENCODING, bufferSize * 2
        )

        var noiseSuppressor: NoiseSuppressor? = null
        if (config.noiseSuppressionOn && NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(audioRecord.audioSessionId)
                noiseSuppressor?.enabled = true
                Log.d(TAG, "NoiseSuppressor enabled")
            } catch (e: Exception) {
                Log.w(TAG, "NoiseSuppressor not supported: ${e.message}")
            }
        }

        var reverb: PresetReverb? = null
        if (config.reverbPreset != PresetReverb.PRESET_NONE.toInt() && config.reverbLevel > 0f) {
            try {
                reverb = PresetReverb(0, audioRecord.audioSessionId)
                reverb.preset = config.reverbPreset.toShort()
                reverb.enabled = true
                Log.d(TAG, "Reverb enabled: preset=${config.reverbPreset}")
            } catch (e: Exception) {
                Log.w(TAG, "Reverb not supported: ${e.message}")
            }
        }

        val delaySamples = ((config.delayMs / 1000f) * SAMPLE_RATE).toInt().coerceIn(0, SAMPLE_RATE * 2)
        val delayBuffer = if (delaySamples > 0) ShortArray(delaySamples) { 0 } else null
        var delayWritePos = 0

        val soundPcm: ShortArray? = try {
            when {
                config.soundResId == null || config.soundVolume <= 0f -> null
                config.soundResId == cachedSoundResId -> cachedSoundPcm
                else -> decodeSoundToPcm(config.soundResId!!).also {
                    cachedSoundPcm = it
                    cachedSoundResId = config.soundResId
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sound decode skipped: ${e.message}"); null
        }
        var soundPos = 0

        val encoder = setupAacEncoder() ?: run {
            audioRecord.release()
            return
        }

        val outputFile = audioFile ?: run { audioRecord.release(); return }
        val fos = outputFile.outputStream().buffered()

        audioRecord.startRecording()
        encoder.start()

        val readBuf = ShortArray(minOf(bufferSize / 2, 2048))
        val bufferInfo = MediaCodec.BufferInfo()

        Log.d(TAG, "Audio pipeline started")

        try {
            while (isRunning) {
                val read = audioRecord.read(readBuf, 0, readBuf.size)
                if (read <= 0) continue

                for (i in 0 until read) {
                    readBuf[i] = (readBuf[i] * config.vocalVolume).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val pitched = if (config.pitchSemitones != 0f) {
                    pitchShift(readBuf, read, config.pitchSemitones)
                } else readBuf.copyOf(read)

                val delayed = if (delayBuffer != null && delaySamples > 0) {
                    applyDelay(pitched, pitched.size, delayBuffer, delaySamples, delayWritePos).also {
                        delayWritePos = (delayWritePos + pitched.size) % delaySamples
                    }
                } else pitched

                val mixed = if (soundPcm != null && soundPcm.isNotEmpty()) {
                    val out = delayed.copyOf()
                    for (i in out.indices) {
                        val s = soundPcm[soundPos % soundPcm.size]
                        out[i] = (out[i] + (s * config.soundVolume)).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        soundPos++
                    }
                    out
                } else delayed

                val inputIdx = encoder.dequeueInputBuffer(10_000L)
                if (inputIdx >= 0) {
                    val inputBuf = encoder.getInputBuffer(inputIdx) ?: continue
                    inputBuf.clear()
                    val byteArray = shortsToBytes(mixed)
                    inputBuf.put(byteArray, 0, byteArray.size.coerceAtMost(inputBuf.capacity()))
                    val ptsUs = (System.nanoTime() / 1000L) - audioStartTimeUs
                    encoder.queueInputBuffer(inputIdx, 0, byteArray.size.coerceAtMost(inputBuf.capacity()), ptsUs, 0)
                }

                drainEncoder(encoder, bufferInfo, fos)
            }

            val inputIdx = encoder.dequeueInputBuffer(10_000L)
            if (inputIdx >= 0) {
                encoder.queueInputBuffer(inputIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(encoder, bufferInfo, fos, eos = true)

        } catch (e: Exception) {
            Log.e(TAG, "Audio pipeline error", e)
        } finally {
            fos.close()
            encoder.stop()
            encoder.release()
            noiseSuppressor?.release()
            reverb?.release()
            audioRecord.stop()
            audioRecord.release()
            Log.d(TAG, "Audio pipeline stopped, file size=${outputFile.length()}")
        }
    }

    private fun pitchShift(input: ShortArray, count: Int, semitones: Float): ShortArray {
        val ratio = 2f.pow(semitones / 12f)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val srcIdx = (i * ratio).toInt()
            out[i] = if (srcIdx < count) input[srcIdx] else 0
        }
        return out
    }

    private fun applyDelay(
        input: ShortArray, count: Int,
        delayBuf: ShortArray, bufSize: Int, writePos: Int
    ): ShortArray {
        val out = ShortArray(count)
        for (i in 0 until count) {
            val readPos = ((writePos + i) % bufSize)
            val delayed = delayBuf[readPos]
            val combined = (input[i] + delayed * 0.5f).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            out[i] = combined
            delayBuf[(writePos + i) % bufSize] = input[i]
        }
        return out
    }

    private fun setupAacEncoder(): MediaCodec? {
        return try {
            val format = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            MediaCodec.createEncoderByType(MIME).also { it.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }
        } catch (e: Exception) {
            Log.e(TAG, "AAC encoder setup failed", e)
            null
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        info: MediaCodec.BufferInfo,
        fos: java.io.OutputStream,
        eos: Boolean = false
    ) {
        while (true) {
            val outIdx = encoder.dequeueOutputBuffer(info, if (eos) 10_000L else 0L)
            if (outIdx < 0) break
            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                encoder.releaseOutputBuffer(outIdx, false)
                continue
            }
            val buf = encoder.getOutputBuffer(outIdx) ?: run {
                encoder.releaseOutputBuffer(outIdx, false); continue
            }
            val adtsData = ByteArray(info.size + 7)
            addAdtsHeader(adtsData, adtsData.size)
            buf.position(info.offset)
            buf.get(adtsData, 7, info.size)
            fos.write(adtsData)
            encoder.releaseOutputBuffer(outIdx, false)
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
        }
    }

    private fun addAdtsHeader(packet: ByteArray, packetLen: Int) {
        val freqIdx = 4
        val chanCfg = 1
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((0 shl 6) or (freqIdx shl 2) or (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) or (packetLen shr 11)).toByte()
        packet[4] = ((packetLen and 0x7FF) shr 3).toByte()
        packet[5] = (((packetLen and 7) shl 5) or 0x1F).toByte()
        packet[6] = 0xFC.toByte()
    }

    private suspend fun muxVideoAndAudio(videoFile: File, audioADTS: File): Uri? = withContext(Dispatchers.IO) {
        if (!audioADTS.exists() || audioADTS.length() == 0L) {
            Log.w(TAG, "Audio file empty, returning video only")
            return@withContext saveVideoToMediaStore(videoFile)
        }

        val outputFile = File(context.cacheDir, "final_${System.currentTimeMillis()}.mp4")

        try {
            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var videoTrack = -1
            for (i in 0 until videoExtractor.trackCount) {
                val fmt = videoExtractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    videoTrack = muxer.addTrack(fmt)
                    videoExtractor.selectTrack(i)
                    break
                }
            }

            val audioFormat = MediaFormat.createAudioFormat(MIME, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            }
            val audioTrack = muxer.addTrack(audioFormat)

            muxer.start()

            val videoBuf = ByteBuffer.allocate(256 * 1024)
            val videoInfo = MediaCodec.BufferInfo()
            while (true) {
                videoInfo.size = videoExtractor.readSampleData(videoBuf, 0)
                if (videoInfo.size < 0) break
                videoInfo.presentationTimeUs = videoExtractor.sampleTime
                videoInfo.flags = if (videoExtractor.sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                muxer.writeSampleData(videoTrack, videoBuf, videoInfo)
                videoExtractor.advance()
            }

            val adtsBytes = audioADTS.readBytes()
            var offset = 0
            var ptsUs = 0L
            val samplesPerFrame = 1024L
            val audioBuf = ByteBuffer.allocate(8192)
            val audioInfo = MediaCodec.BufferInfo()

            while (offset < adtsBytes.size - 7) {
                if (adtsBytes[offset] != 0xFF.toByte() || adtsBytes[offset + 1] != 0xF9.toByte()) {
                    offset++; continue
                }
                val frameLen = ((adtsBytes[offset + 3].toInt() and 0x03) shl 11) or
                        ((adtsBytes[offset + 4].toInt() and 0xFF) shl 3) or
                        ((adtsBytes[offset + 5].toInt() and 0xFF) shr 5)
                if (frameLen < 8 || offset + frameLen > adtsBytes.size) break

                val rawFrame = adtsBytes.copyOfRange(offset + 7, offset + frameLen)
                audioBuf.clear()
                audioBuf.put(rawFrame)
                audioBuf.flip()
                audioInfo.offset = 0
                audioInfo.size = rawFrame.size
                audioInfo.presentationTimeUs = ptsUs
                audioInfo.flags = 0
                muxer.writeSampleData(audioTrack, audioBuf, audioInfo)
                ptsUs += samplesPerFrame * 1_000_000L / SAMPLE_RATE
                offset += frameLen
            }

            muxer.stop()
            muxer.release()
            videoExtractor.release()

            Log.d(TAG, "Mux complete: ${outputFile.length()} bytes")
            saveVideoToMediaStore(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Mux failed", e)
            saveVideoToMediaStore(videoFile)
        }
    }

    private fun saveVideoToMediaStore(file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "CreatorSuite_${System.currentTimeMillis()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CreatorSuite")
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        return uri
    }

    private fun decodeSoundToPcm(resId: Int): ShortArray? {
        return try {
            val afd = context.resources.openRawResourceFd(resId)
            val extractor = MediaExtractor()
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            var audioTrackIdx = -1
            for (i in 0 until extractor.trackCount) {
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIdx = i; break
                }
            }
            if (audioTrackIdx < 0) return null

            extractor.selectTrack(audioTrackIdx)
            val fmt = extractor.getTrackFormat(audioTrackIdx)
            val decoder = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(fmt, null, null, 0)
            decoder.start()

            val pcm = mutableListOf<Short>()
            val info = MediaCodec.BufferInfo()
            var eos = false
            val maxSamples = SAMPLE_RATE * 2

            while (!eos && pcm.size < maxSamples) {
                val inIdx = decoder.dequeueInputBuffer(5_000L)
                if (inIdx >= 0) {
                    val inBuf = decoder.getInputBuffer(inIdx)!!
                    val sz = extractor.readSampleData(inBuf, 0)
                    if (sz < 0) {
                        decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        eos = true
                    } else {
                        decoder.queueInputBuffer(inIdx, 0, sz, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
                val outIdx = decoder.dequeueOutputBuffer(info, 5_000L)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)!!
                    outBuf.position(info.offset)
                    val shortBuf = outBuf.order(ByteOrder.nativeOrder()).asShortBuffer()
                    while (shortBuf.hasRemaining() && pcm.size < maxSamples) pcm.add(shortBuf.get())
                    decoder.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            decoder.stop(); decoder.release(); extractor.release()
            pcm.toShortArray()
        } catch (e: Exception) {
            Log.e(TAG, "Sound decode failed", e); null
        }
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        shorts.forEach { bb.putShort(it) }
        return bytes
    }

    private fun effectNameToPreset(name: String): Int = when (name) {
        "Small Room" -> PresetReverb.PRESET_SMALLROOM.toInt() and 0xFFFF
        "Medium Room" -> PresetReverb.PRESET_MEDIUMROOM.toInt() and 0xFFFF
        "Cathedral" -> PresetReverb.PRESET_PLATE.toInt() and 0xFFFF
        "Large Room" -> PresetReverb.PRESET_LARGEROOM.toInt() and 0xFFFF
        "Medium Hall" -> PresetReverb.PRESET_MEDIUMHALL.toInt() and 0xFFFF
        "Large Hall" -> PresetReverb.PRESET_LARGEHALL.toInt() and 0xFFFF
        "Medium Chamber" -> PresetReverb.PRESET_LARGEROOM.toInt() and 0xFFFF
        else -> PresetReverb.PRESET_NONE.toInt() and 0xFFFF
    }

    private fun soundNameToRes(context: Context, name: String): Int? {
        if (name == "None") return null
        val resName = "sound_${name.lowercase().replace(" ", "_")}"
        val id = context.resources.getIdentifier(resName, "raw", context.packageName)
        return if (id != 0) id else null
    }
}