package com.example.ui.editor

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File

/**
 * Helper for recording local m4a voice-note attachments and playing them back offline.
 */
class VoiceNoteRecorderHelper(private val context: Context) {
  private var recorder: MediaRecorder? = null
  private var player: MediaPlayer? = null
  private var activeFile: File? = null
  private var startMillis: Long = 0L

  val isRecording: Boolean
    get() = recorder != null

  fun startRecording(): File? {
    stopRecording()
    return try {
      val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
      val outFile = File(dir, "voice_${System.currentTimeMillis()}.m4a")
      val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }
      mediaRecorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128_000)
        setAudioSamplingRate(44_100)
        setOutputFile(outFile.absolutePath)
        prepare()
        start()
      }
      recorder = mediaRecorder
      activeFile = outFile
      startMillis = System.currentTimeMillis()
      outFile
    } catch (_: Throwable) {
      recorder = null
      activeFile = null
      null
    }
  }

  /**
   * Stops active recording and returns the saved [File] along with its duration in milliseconds.
   */
  fun stopRecording(): Pair<File, Long>? {
    val currentRecorder = recorder ?: return null
    val file = activeFile ?: return null
    val duration = (System.currentTimeMillis() - startMillis).coerceAtLeast(500L)
    return try {
      currentRecorder.stop()
      currentRecorder.release()
      recorder = null
      activeFile = null
      file to duration
    } catch (_: Throwable) {
      runCatching { currentRecorder.release() }
      recorder = null
      activeFile = null
      null
    }
  }

  fun playVoiceNote(uriString: String, onFinished: () -> Unit = {}) {
    stopPlayback()
    try {
      val mp = MediaPlayer().apply {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" || uriString.startsWith("/")) {
          setDataSource(uri.path ?: uriString)
        } else {
          setDataSource(context, uri)
        }
        setOnCompletionListener {
          stopPlayback()
          onFinished()
        }
        prepare()
        start()
      }
      player = mp
    } catch (_: Throwable) {
      stopPlayback()
      onFinished()
    }
  }

  fun stopPlayback() {
    runCatching {
      player?.stop()
      player?.release()
    }
    player = null
  }

  fun releaseAll() {
    stopRecording()
    stopPlayback()
  }
}
