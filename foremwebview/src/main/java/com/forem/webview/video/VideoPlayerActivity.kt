package com.forem.webview.video

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.forem.webview.ForemWebViewSession
import com.forem.webview.R
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import java.util.Timer
import java.util.TimerTask

/** Activity which displays video using ExoPlayer on top of WebViewFragment. */
class VideoPlayerActivity : AppCompatActivity(), Player.Listener {

    companion object {
        const val VIDEO_URL_INTENT_EXTRA = "VideoPlayerActivity.video_url"
        const val VIDEO_TIME_INTENT_EXTRA = "VideoPlayerActivity.video_time"

        /**
         * Creates a new instance of intent for [VideoPlayerActivity].
         *
         * @param context the source activity context.
         * @param url the video which needs to be played.
         * @param time the time at video the video should start from.
         * @return the intent for [VideoPlayerActivity] with extras.
         */
        fun newInstance(context: Context, url: String, time: String): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            // This flag makes sure that there is only one instance of this activity.
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.putExtra(VIDEO_URL_INTENT_EXTRA, url)
            intent.putExtra(VIDEO_TIME_INTENT_EXTRA, time)
            return intent
        }
    }

    private lateinit var playerView: PlayerView
    private lateinit var player: SimpleExoPlayer
    private val timer = Timer()

    private var pictureInPictureParamsBuilder: PictureInPictureParams.Builder? = null

    private val foremWebViewSession: ForemWebViewSession by lazy {
        ForemWebViewSession.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.video_player_activity)

        playerView = findViewById(R.id.player_view)

        val url = intent.getStringExtra(VIDEO_URL_INTENT_EXTRA)
        val time = intent.getStringExtra(VIDEO_TIME_INTENT_EXTRA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
            val aspectRatio = Rational(16, 9)
            pictureInPictureParamsBuilder!!.setAspectRatio(aspectRatio)
            playerView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || right != oldRight || top != oldTop ||
                    bottom != oldBottom && pictureInPictureParamsBuilder != null
                ) {
                    val sourceRectHint = Rect()
                    playerView.getGlobalVisibleRect(sourceRectHint)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setPictureInPictureParams(
                            pictureInPictureParamsBuilder!!.build()
                        )
                    }
                }
            }
        }

        val streamUri = Uri.parse(url)
        val mediaSource = HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(streamUri))

        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        player.prepare(mediaSource)
        player.seekTo(time!!.toLong() * 1000)
        player.playWhenReady = true

        timer.schedule(TimerUpdate(), 0, 1000)
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            pauseVideo()
        }
        super.onPause()
    }

    override fun onDestroy() {
        destroyVideo()
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        pictureInPictureMode()
    }

    // Reference: https://www.techotopia.com/index.php/An_Android_Picture-in-Picture_Tutorial
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            playerView.hideController()
        } else {
            playerView.showController()
        }
    }

    // Reference: https://stackoverflow.com/a/72392781
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            // User clicked on close button of PiP window
            destroyVideo()
            finishAndRemoveTask()
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    private fun pictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(pictureInPictureParamsBuilder!!.build())
        }
    }

    private fun pauseVideo() {
        if (::player.isInitialized) {
            player.pause()
        }
        foremWebViewSession.videoPlayerPaused()
    }

    private fun destroyVideo() {
        if (::player.isInitialized) {
            player.playWhenReady = false
        }
        timer.cancel()
        foremWebViewSession.videoPlayerPaused()
    }

    private fun timeUpdate() {
        if (::player.isInitialized && player.isPlaying) {
            val milliseconds = player.currentPosition
            val currentTime = (milliseconds / 1000).toString()
            foremWebViewSession.videoPlayerTimerUpdate(currentTime)
        }
    }

    /** Helper timer class which updates the status of video player continuously on backend. */
    inner class TimerUpdate : TimerTask() {
        override fun run() {
            runOnUiThread {
                timeUpdate()
            }
        }
    }
}
