package com.forem.webview.media

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil

/*
 * This subclass of PlayerNotificationManager customizes the controls available in the
 * notification by overriding the getActions method.
 */
public class PodcastPlayerNotificationManager(
    context: Context,
    channelId: String,
    notificationId: Int,
    mediaDescriptionAdapter: MediaDescriptionAdapter,
    playerNotificationManager: NotificationListener,
    customActionReceiver: CustomActionReceiver,
    smallIconResourceId: Int,
    playActionIconResourceId: Int,
    pauseActionIconResourceId: Int,
    stopActionIconResourceId: Int,
    rewindActionIconResourceId: Int,
    fastForwardActionIconResourceId: Int,
    previousActionIconResourceId: Int,
    nextActionIconResourceId: Int,
    groupKey: String?
) : PlayerNotificationManager(
    context,
    channelId,
    notificationId,
    mediaDescriptionAdapter,
    playerNotificationManager,
    customActionReceiver,
    smallIconResourceId,
    playActionIconResourceId,
    pauseActionIconResourceId,
    stopActionIconResourceId,
    rewindActionIconResourceId,
    fastForwardActionIconResourceId,
    previousActionIconResourceId,
    nextActionIconResourceId,
    groupKey
) {

    companion object {
        fun createWithNotificationChannel(
            context: Context,
            channelId: String,
            channelName: Int,
            channelDescription: Int,
            notificationId: Int,
            mediaDescriptionAdapter: MediaDescriptionAdapter,
            playerNotificationManager: NotificationListener,
            customActionReceiver: CustomActionReceiver,
            smallIconResourceId: Int,
            playActionIconResourceId: Int,
            pauseActionIconResourceId: Int,
            stopActionIconResourceId: Int,
            rewindActionIconResourceId: Int,
            fastForwardActionIconResourceId: Int,
            previousActionIconResourceId: Int,
            nextActionIconResourceId: Int,
            groupKey: String?
        ): PodcastPlayerNotificationManager {

            NotificationUtil.createNotificationChannel(
                context,
                channelId,
                channelName,
                channelDescription,
                NotificationUtil.IMPORTANCE_LOW
            )
            return PodcastPlayerNotificationManager(
                context,
                channelId,
                notificationId,
                mediaDescriptionAdapter,
                playerNotificationManager,
                customActionReceiver,
                smallIconResourceId,
                playActionIconResourceId,
                pauseActionIconResourceId,
                stopActionIconResourceId,
                rewindActionIconResourceId,
                fastForwardActionIconResourceId,
                previousActionIconResourceId,
                nextActionIconResourceId,
                groupKey
            )
        }
    }

    override fun getActions(player: Player): List<String> {
        var stringActions: List<String> = ArrayList()
        stringActions += ACTION_REWIND
        stringActions += if (shouldShowPauseButton(player)) {
            ACTION_PAUSE
        } else {
            ACTION_PLAY
        }
        stringActions += ACTION_FAST_FORWARD
        return stringActions
    }

    private fun shouldShowPauseButton(player: Player): Boolean {
        val state = player.playbackState
        return state != Player.STATE_ENDED && state != Player.STATE_IDLE && player.playWhenReady
    }
}
