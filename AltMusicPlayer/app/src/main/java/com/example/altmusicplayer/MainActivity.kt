package com.example.altmusicplayer

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts

class MusicPlayerActivity : AppCompatActivity() {

    private lateinit var playButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTrackTextView: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentSongIndex = 0
    private val songs = arrayListOf(R.raw.djuma, R.raw.domino, R.raw.cuore)
    private val songNames = arrayOf("Djuma - Les Djinns", "Oxia - Domino", "Rivero - Il mio Cuore")
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var vinylImageView: ImageView
    private var vinylAnimator: ObjectAnimator? = null
    private lateinit var upcomingTracksListView: ListView


    private val directorySelectionRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            Log.d("MusicPlayerActivity", "Selected directory URI: $uri")
            // Use the Uri to access the selected directory. You might want to save this Uri or load files from it.
            // Remember to ask for persistable permission if you want to keep access across restarts
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        setupButtonListeners()
        setupSeekBar()
        initializeUI()

    }

    private fun initializeUI() {
        playButton = findViewById(R.id.button_play)
        nextButton = findViewById(R.id.button_next)
        prevButton = findViewById(R.id.button_previous)
        seekBar = findViewById(R.id.track_seek_bar)
        currentTrackTextView = findViewById(R.id.current_track)
        vinylImageView = findViewById(R.id.vinyl_avatar)
        updateCurrentTrackName()


    }

    private fun setupButtonListeners() {
        playButton.setOnClickListener {
            togglePlayPause()
        }

        nextButton.setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            prepareMediaPlayer()
            mediaPlayer?.start() // Start the new song immediately
            playButton.setImageResource(R.drawable.pause) // Update play button to show "pause"
            isPlaying = true // Update isPlaying flag
            updateCurrentTrackName()
        }

        prevButton.setOnClickListener {
            currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
            prepareMediaPlayer()
            mediaPlayer?.start() // Start the new song immediately
            playButton.setImageResource(R.drawable.pause) // Update play button to show "pause"
            isPlaying = true // Update isPlaying flag
            updateCurrentTrackName()
        }
    }

    private fun togglePlayPause() {
        if (mediaPlayer == null) {
            prepareMediaPlayer()
        }
        if (!isPlaying) {
            mediaPlayer?.start()
            playButton.setImageResource(R.drawable.pause)
            isPlaying = true
            startSeekBarUpdate()
            startVinylRotation()
        } else {
            mediaPlayer?.pause()
            playButton.setImageResource(R.drawable.play)
            isPlaying = false
            pauseVinylRotation()
        }
    }

    private fun prepareMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, songs[currentSongIndex]).apply {
            setOnCompletionListener {
                this@MusicPlayerActivity.isPlaying = false
                playButton.setImageResource(R.drawable.play)
                seekBar.progress = 0
                updateCurrentTrackName()
                stopVinylRotation()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MusicPlayerActivity", "Error occurred during playback: What: $what Extra: $extra")
                true
            }
            seekBar.max = duration
        }
        updateCurrentTrackName()
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startSeekBarUpdate() {
        handler.postDelayed(updateSeekBarTask, 0)
    }

    private val updateSeekBarTask = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (isPlaying && it.isPlaying) {
                    seekBar.progress = it.currentPosition
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun updateCurrentTrackName() {
        if (songNames.isNotEmpty() && currentSongIndex in songNames.indices) {
            val currentTrackText = getString(R.string.current_track) + "\n"
            val songName = songNames[currentSongIndex]
            val fullText = currentTrackText + songName
            val spannableString = SpannableString(fullText)
            val colorSpan = ForegroundColorSpan(ContextCompat.getColor(this, R.color.song_name_color))
            spannableString.setSpan(colorSpan, currentTrackText.length, fullText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            currentTrackTextView.text = spannableString
        }
    }

    private fun startVinylRotation() {
        if (vinylAnimator == null) {
            vinylAnimator = ObjectAnimator.ofFloat(vinylImageView, "rotation", 0f, 360f).apply {
                duration = 10000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
            }
        }
        vinylAnimator?.start()
    }


    private fun pauseVinylRotation() {
        vinylAnimator?.pause()
    }

    private fun stopVinylRotation() {
        vinylAnimator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarTask)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
