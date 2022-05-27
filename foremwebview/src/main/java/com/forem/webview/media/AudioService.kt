package com.forem.webview.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.forem.webview.BuildConfig
import com.forem.webview.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class AudioService : LifecycleService() {
    private val binder = AudioServiceBinder()

    private var currentPodcastUrl: String? = null
    private var episodeName: String? = null
    private var podcastName: String? = null
    private var imageUrl: String? = null

    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null

    inner class AudioServiceBinder : Binder() {
        val service: AudioService
            get() = this@AudioService
    }

    companion object {
        @MainThread
        fun newIntent(
            context: Context,
            episodeUrl: String
        ) = Intent(context, AudioService::class.java).apply {
            putExtra(argPodcastUrl, episodeUrl)
        }

        const val argPodcastUrl = "ARG_PODCAST_URL"
        const val playbackChannelId = "playback_channel"
        const val mediaSessionTag = "Forem"
        const val playbackNotificationId = 1
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        val newPodcastUrl = intent.getStringExtra(argPodcastUrl)

        if (currentPodcastUrl != newPodcastUrl) {
            currentPodcastUrl = newPodcastUrl
            preparePlayer()
        }

        return binder
    }

    override fun onCreate() {
        super.onCreate()

        player = SimpleExoPlayer.Builder(this).build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()

        player?.setAudioAttributes(audioAttributes, true)

        playerNotificationManager = PodcastPlayerNotificationManager.createWithNotificationChannel(
            applicationContext,
            playbackChannelId,
            R.string.app_name,
            R.string.playback_channel_description,
            playbackNotificationId,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): String {
                    return episodeName ?: getString(R.string.app_name)
                }

                @Nullable
                override fun createCurrentContentIntent(player: Player): PendingIntent? = null

                @Nullable
                override fun getCurrentContentText(player: Player): String {
                    return podcastName ?: getString(R.string.playback_channel_description)
                }

                @Nullable
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return null
                }
            },
            object : PlayerNotificationManager.NotificationListener {
                fun onNotificationStarted(
                    notificationId: Int,
                    notification: Notification
                ) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }

                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    if (ongoing) {
                        // Make sure the service will not get destroyed while playing media.
                        startForeground(notificationId, notification)
                    } else {
                        // Make notification cancellable.
                        stopForeground(false)
                    }
                }
            },
            object : PlayerNotificationManager.CustomActionReceiver {
                override fun createCustomActions(
                    context: Context,
                    instanceId: Int
                ): MutableMap<String, NotificationCompat.Action> {
                    val action = NotificationCompat.Action(
                        context.resources.getIdentifier(
                            "music_clear",
                            "drawable",
                            context.packageName
                        ),
                        "closeBar",
                        null
                    )
                    val actionMap: MutableMap<String, NotificationCompat.Action> = HashMap()
                    actionMap["closeBar"] = action
                    return actionMap
                }

                override fun getCustomActions(player: Player): MutableList<String> {
                    val customActions: MutableList<String> = ArrayList()
                    customActions.add("closeBar")
                    return customActions
                }

                override fun onCustomAction(player: Player, action: String, intent: Intent) {
                }
            },
            // TODO(#174): Can try exo-player icons too.
            R.drawable.ic_forem_notification,
            R.drawable.ic_baseline_play_arrow_24,
            R.drawable.ic_baseline_pause_24,
            R.drawable.ic_baseline_stop_24,
            R.drawable.ic_baseline_fast_rewind_24,
            R.drawable.ic_baseline_fast_forward_24,
            R.drawable.ic_baseline_skip_previous_24,
            R.drawable.ic_baseline_skip_next_24,
            null
        ).apply {
            setPlayer(player)
            invalidate()
        }

        // Show lock screen controls and let apps like Google assistant manager playback.
        mediaSession = MediaSessionCompat(this, mediaSessionTag)
        val builder = MediaMetadataCompat.Builder()
        builder.putString(MediaMetadata.METADATA_KEY_TITLE, episodeName)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, podcastName)
        mediaSession?.setMetadata(builder.build())
        playerNotificationManager?.setMediaSessionToken(mediaSession!!.sessionToken)
    }

    @MainThread
    fun play(audioUrl: String?, seconds: String?) {
        if (currentPodcastUrl != audioUrl) {
            currentPodcastUrl = audioUrl
            preparePlayer()
            seekTo("0")
        } else {
            seekTo(seconds)
        }
        player?.playWhenReady = true
    }

    @MainThread
    fun pause() {
        player?.playWhenReady = false
    }

    @MainThread
    fun mute(muted: String?) {
        muted?.toBoolean()?.let {
            if (it) {
                player?.volume = 0F
            } else {
                player?.volume = 1F
            }
        }
    }

    @MainThread
    fun volume(volume: String?) {
        volume?.toFloat()?.let {
            player?.volume = it
        }
    }

    @MainThread
    fun rate(rate: String?) {
        rate?.toFloat()?.let {
            player?.setPlaybackParameters(PlaybackParameters(it))
        }
    }

    @MainThread
    fun seekTo(seconds: String?) {
        seconds?.toFloat()?.let {
            player?.seekTo((it * 1000F).toLong())
        }
    }

    @MainThread
    fun loadMetadata(epName: String?, pdName: String?, url: String?) {
        episodeName = epName
        podcastName = pdName
        imageUrl = url
    }

    @MainThread
    fun currentTimeInSec(): Long {
        return player?.currentPosition ?: 0
    }

    @MainThread
    fun durationInSec(): Long {
        return player?.duration ?: 0L
    }

    @MainThread
    private fun preparePlayer() {
        player?.playWhenReady = false

        // Allows the data source to be seekable
        val extractorsFactory: DefaultExtractorsFactory =
            DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)

        val dataSourceFactory = DefaultDataSourceFactory(this, BuildConfig.FOREM_AGENT_EXTENSION)
        val streamUri = Uri.parse(currentPodcastUrl)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
            .createMediaSource(streamUri)
        player?.prepare(mediaSource)
    }
}
