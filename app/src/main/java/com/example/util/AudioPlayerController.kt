package com.example.util

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentFilePath: String? = null,
    val isLoaded: Boolean = false
)

class AudioPlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer != null) return

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val dur = duration.coerceAtLeast(0L)
                            _playerState.value = _playerState.value.copy(
                                durationMs = dur,
                                isLoaded = true
                            )
                        }
                        Player.STATE_ENDED -> {
                            _playerState.value = _playerState.value.copy(
                                isPlaying = false,
                                currentPositionMs = _playerState.value.durationMs
                            )
                            stopProgressUpdates()
                        }
                        else -> Unit
                    }
                }
            })
        }
    }

    fun loadAudio(filePath: String, autoPlay: Boolean = false, initialSpeed: Float = 1.0f) {
        val file = File(filePath)
        if (!file.exists()) return

        val player = exoPlayer ?: return
        val uri = Uri.fromFile(file)
        val mediaItem = MediaItem.fromUri(uri)

        player.setMediaItem(mediaItem)
        player.playbackParameters = PlaybackParameters(initialSpeed, 1.0f)
        player.prepare()

        _playerState.value = PlayerState(
            isPlaying = autoPlay,
            currentPositionMs = 0L,
            durationMs = 0L,
            playbackSpeed = initialSpeed,
            currentFilePath = filePath,
            isLoaded = true
        )

        if (autoPlay) {
            player.play()
        }
    }

    fun play() {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
            player.play()
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _playerState.value.durationMs)
        exoPlayer?.seekTo(bounded)
        _playerState.value = _playerState.value.copy(currentPositionMs = bounded)
    }

    fun seekRelative(offsetMs: Long) {
        val current = exoPlayer?.currentPosition ?: 0L
        seekTo(current + offsetMs)
    }

    fun setSpeed(speed: Float) {
        val bounded = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(bounded, 1.0f)
        _playerState.value = _playerState.value.copy(playbackSpeed = bounded)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playerState.value.durationMs
                    )
                }
                delay(100)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressUpdates()
        exoPlayer?.release()
        exoPlayer = null
    }
}
