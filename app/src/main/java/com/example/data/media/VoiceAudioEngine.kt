package com.example.data.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

data class VoiceRecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitudes: List<Float> = emptyList(),
    val lastRecordedUri: String? = null,
    val lastRecordedDurationMs: Long = 0L,
    val error: String? = null
)

data class VoicePlaybackState(
    val activeUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val error: String? = null
)

class VoiceAudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext

    private val _recorderState = MutableStateFlow(VoiceRecorderState())
    val recorderState: StateFlow<VoiceRecorderState> = _recorderState.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var meterJob: Job? = null
    private var activeSegmentStartMs: Long = 0L
    private var accumulatedMs: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    fun startRecording() {
        stopPlayback()
        releaseRecorder()

        val dir = File(appContext.filesDir, "voice_notes").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")

        try {
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentOutputFile = file
            accumulatedMs = 0L
            activeSegmentStartMs = System.currentTimeMillis()
            _recorderState.value = VoiceRecorderState(
                isRecording = true,
                isPaused = false,
                elapsedMs = 0L,
                amplitudes = emptyList()
            )
            startAmplitudePolling()
        } catch (e: Exception) {
            file.delete()
            releaseRecorder()
            _recorderState.value = VoiceRecorderState(
                error = "Microphone error: ${e.localizedMessage ?: "Unable to start recording"}"
            )
        }
    }

    fun pauseRecording() {
        val recorder = mediaRecorder ?: return
        val current = _recorderState.value
        if (!current.isRecording || current.isPaused) return
        try {
            recorder.pause()
            accumulatedMs += (System.currentTimeMillis() - activeSegmentStartMs).coerceAtLeast(0L)
            meterJob?.cancel()
            _recorderState.value = current.copy(isPaused = true, elapsedMs = accumulatedMs)
        } catch (e: Exception) {
            _recorderState.value = current.copy(error = e.localizedMessage ?: "Unable to pause")
        }
    }

    fun resumeRecording() {
        val recorder = mediaRecorder ?: return
        val current = _recorderState.value
        if (!current.isRecording || !current.isPaused) return
        try {
            recorder.resume()
            activeSegmentStartMs = System.currentTimeMillis()
            _recorderState.value = current.copy(isPaused = false, error = null)
            startAmplitudePolling()
        } catch (e: Exception) {
            _recorderState.value = current.copy(error = e.localizedMessage ?: "Unable to resume")
        }
    }

    fun stopAndSaveRecording(): Pair<String, Long>? {
        val recorder = mediaRecorder ?: return null
        val file = currentOutputFile ?: return null
        val state = _recorderState.value
        val totalMs = if (state.isPaused) {
            accumulatedMs
        } else {
            accumulatedMs + (System.currentTimeMillis() - activeSegmentStartMs).coerceAtLeast(0L)
        }.coerceAtLeast(500L)

        meterJob?.cancel()
        return try {
            recorder.stop()
            releaseRecorder()
            val uriString = Uri.fromFile(file).toString()
            _recorderState.value = VoiceRecorderState(
                isRecording = false,
                isPaused = false,
                elapsedMs = totalMs,
                amplitudes = state.amplitudes,
                lastRecordedUri = uriString,
                lastRecordedDurationMs = totalMs
            )
            uriString to totalMs
        } catch (e: Exception) {
            file.delete()
            releaseRecorder()
            _recorderState.value = VoiceRecorderState(
                error = "Recording too short or interrupted (${e.localizedMessage ?: "retry"})"
            )
            null
        }
    }

    fun discardRecording() {
        meterJob?.cancel()
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        releaseRecorder()
        currentOutputFile?.delete()
        currentOutputFile = null
        _recorderState.value = VoiceRecorderState()
    }

    fun clearLastRecording() {
        _recorderState.value = _recorderState.value.copy(
            lastRecordedUri = null,
            lastRecordedDurationMs = 0L,
            amplitudes = emptyList(),
            elapsedMs = 0L,
            error = null
        )
    }

    fun togglePlayback(uriString: String) {
        val current = _playbackState.value
        if (current.activeUri == uriString && mediaPlayer != null) {
            val player = mediaPlayer ?: return
            if (current.isPlaying) {
                player.pause()
                playbackJob?.cancel()
                _playbackState.value = current.copy(
                    isPlaying = false,
                    positionMs = player.currentPosition.toLong()
                )
            } else {
                player.start()
                _playbackState.value = current.copy(isPlaying = true)
                startPlaybackPolling()
            }
            return
        }

        stopPlayback()
        try {
            val uri = Uri.parse(uriString)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(appContext, uri)
                prepare()
                start()
            }
            player.setOnCompletionListener {
                playbackJob?.cancel()
                _playbackState.value = VoicePlaybackState(
                    activeUri = uriString,
                    isPlaying = false,
                    positionMs = 0L,
                    durationMs = it.duration.toLong().coerceAtLeast(0L)
                )
            }
            mediaPlayer = player
            _playbackState.value = VoicePlaybackState(
                activeUri = uriString,
                isPlaying = true,
                positionMs = 0L,
                durationMs = player.duration.toLong().coerceAtLeast(0L)
            )
            startPlaybackPolling()
        } catch (e: Exception) {
            stopPlayback()
            _playbackState.value = VoicePlaybackState(
                error = "Playback failed: ${e.localizedMessage ?: "Audio file missing"}"
            )
        }
    }

    fun seekTo(uriString: String, positionMs: Long) {
        val player = mediaPlayer
        if (player != null && _playbackState.value.activeUri == uriString) {
            val clamped = positionMs.toInt().coerceIn(0, player.duration.coerceAtLeast(0))
            player.seekTo(clamped)
            _playbackState.value = _playbackState.value.copy(positionMs = clamped.toLong())
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        _playbackState.value = VoicePlaybackState()
    }

    private fun startAmplitudePolling() {
        meterJob?.cancel()
        meterJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val recorder = mediaRecorder ?: break
                val state = _recorderState.value
                if (!state.isRecording || state.isPaused) break
                val rawAmp = try {
                    recorder.maxAmplitude
                } catch (_: Exception) {
                    0
                }
                val normalized = min(1f, (rawAmp / 18000f).coerceIn(0.06f, 1f))
                val updatedAmps = (state.amplitudes + normalized).takeLast(48)
                val elapsed = accumulatedMs + (System.currentTimeMillis() - activeSegmentStartMs).coerceAtLeast(0L)
                _recorderState.value = state.copy(
                    elapsedMs = elapsed,
                    amplitudes = updatedAmps
                )
                delay(75L)
            }
        }
    }

    private fun startPlaybackPolling() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val player = mediaPlayer ?: break
                val isPlayingNow = try {
                    player.isPlaying
                } catch (_: Exception) {
                    false
                }
                if (!isPlayingNow) break
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    positionMs = player.currentPosition.toLong().coerceAtLeast(0L),
                    durationMs = player.duration.toLong().coerceAtLeast(0L)
                )
                delay(120L)
            }
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    fun releaseAll() {
        meterJob?.cancel()
        releaseRecorder()
        stopPlayback()
    }
}
