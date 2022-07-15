package com.forem.webview.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.bumptech.glide.Glide
import com.forem.webview.BuildConfig
import com.forem.webview.R
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.ExecutionException
import javax.net.ssl.HttpsURLConnection

/**
 * Class which helps us play the podcasts in foreground as well as background.
 *
 * This class is responsible for starting and terminating the podcast.
 */
class AudioService : LifecycleService() {
    private val binder = AudioServiceBinder()

    private var currentPodcastUrl: String? = null
    private var episodeName: String? = null
    private var podcastName: String? = null
    private var imageUrl: String? = null

    private var player: SimpleExoPlayer? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private var mediaSession: MediaSessionCompat? = null

    /** Binder to use AudioService. */
    inner class AudioServiceBinder : Binder() {
        val service: AudioService
            get() = this@AudioService
    }

    companion object {
        private const val argPodcastUrl = "ARG_PODCAST_URL"
        private const val playbackChannelId = "playback_channel"
        private const val mediaSessionTag = "Forem"
        private const val notificationId = 1

        /**
         * Creates a new intent which calls AudioService on main thread.
         *
         * @param context the context of the activity or fragment.
         * @param episodeUrl the url of the podcast which needs to be played.
         * @return an [AudioService] intent.
         */
        @MainThread
        fun newIntent(
            context: Context,
            episodeUrl: String
        ) = Intent(context, AudioService::class.java).apply {
            putExtra(argPodcastUrl, episodeUrl)
        }
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
            notificationId,
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
                    if (imageUrl == null) return null

                    var largeIconBitmap: Bitmap? = null
                    val thread = Thread {
                        try {
                            val uri = Uri.parse(imageUrl)
                            val bitmap = Glide.with(applicationContext)
                                .asBitmap()
                                .load(uri)
                                .submit().get()

                            largeIconBitmap = bitmap
                            callback.onBitmap(bitmap)
                        } catch (e: ExecutionException) {
                            e.printStackTrace()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    thread.start()

                    return largeIconBitmap
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
                    // TODO: Handle onNotificationCancelled
                    Log.d("TAG", "onNotificationCancelled: $notificationId, $dismissedByUser")
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

    private fun largeImage(): Bitmap? {
        Log.d("TAG", "largeImage: $imageUrl")
        var image: Bitmap? = null
        if (imageUrl.isNullOrEmpty()) {
            return null
        }
        try {
            val url = URL(imageUrl)
            image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        } catch (e: IOException) {
            Log.e("TAG", "largeImage", e)
        }
        return image

    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Plays the podcast on main thread.
     *
     * @param audioUrl the url of the podcast.
     * @param seconds starting time of the padcast.
     */
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

    /** Function to pause the podcast. */
    @MainThread
    fun pause() {
        player?.playWhenReady = false
    }

    fun clearNotification() {
        player?.release()
    }

    /**
     * Function to mute the podcast.
     *
     * @param muted the url of the podcast.
     */
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

    /**
     * Function to change the volume of the podcast.
     *
     * @param volume the volume which needs to be set to podcast.
     */
    @MainThread
    fun volume(volume: String?) {
        volume?.toFloat()?.let {
            player?.volume = it
        }
    }

    /**
     * Function to change the playback speed of podcast.
     *
     * @param rate the rate at which the podcast needs to be played.
     */
    @MainThread
    fun rate(rate: String?) {
        rate?.toFloat()?.let {
            player?.setPlaybackParameters(PlaybackParameters(it))
        }
    }

    /**
     * This function helps to skip to specified time in podcast.
     *
     * @param seconds the time in the audio at which the podcast should play.
     */
    @MainThread
    fun seekTo(seconds: String?) {
        seconds?.toFloat()?.let {
            player?.seekTo((it * 1000F).toLong())
        }
    }

    /**
     * This function helps to show the meta data of podcast in notification.
     *
     * @param episodeName the name of the episode which gets displayed in notification.
     * @param pdName the name of the podcast which gets displayed in notification.
     * @param url the image which needs to be displayed in the notification.
     */
    @MainThread
    fun loadMetadata(epName: String?, pdName: String?, url: String?) {
        episodeName = epName
        podcastName = pdName
        imageUrl = url
    }

    /**
     * Get current value of time in seconds for audio position.
     *
     * @return the time at which currently audio is playing in seconds.
     */
    @MainThread
    fun currentTimeInSec(): Long {
        return player?.currentPosition ?: 0
    }

    /**
     * Get the total duration of podcast in seconds.
     *
     * @return the total duration of podcast in seconds.
     */
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
            .createMediaSource(MediaItem.fromUri(streamUri))
        player?.prepare(mediaSource)
    }
}
