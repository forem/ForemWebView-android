package com.forem.webview.media

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil

/*
 * This subclass of [PlayerNotificationManager] customizes the controls available in the
 * notification by overriding the getActions method.
 */
class PodcastPlayerNotificationManager(
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
        /**
         * Creates the notification channel that notifications can be posted to.
         *
         * @param context a {@link Context}.
         * @param channelId the id of the channel. Must be unique.
         * @param channelName a string resource identifier for the user visible name of the channel.
         *     The recommended maximum length is 40 characters.
         * @param channelDescription a string resource identifier for the user visible description
         *     of the channel, or 0 if no description is provided. The recommended maximum length is
         *     300 characters.
         * @param notificationId unique notification id required to distinguish and control among
         *     multiple notifications.
         * @param mediaDescriptionAdapter contains the details like notification title and text.
         * @param playerNotificationManager manager to manage the state of notification.
         * @param customActionReceiver overrides the actions of notification.
         * @param smallIconResourceId a drawable resource identifier for small icon.
         * @param playActionIconResourceId a drawable resource identifier for play icon.
         * @param pauseActionIconResourceId a drawable resource identifier for pause icon.
         * @param stopActionIconResourceId a drawable resource identifier for stop icon.
         * @param rewindActionIconResourceId a drawable resource identifier for rewind icon.
         * @param fastForwardActionIconResourceId a drawable resource identifier for fast forward
         *     icon.
         * @param previousActionIconResourceId a drawable resource identifier for previous icon.
         * @param nextActionIconResourceId a drawable resource identifier for next icon.
         * @param groupKey the key of the group the media notification should belong to.
         * @return the audio notification player manager.
         */
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
        val stringActions = ArrayList<String>()
        stringActions.add(ACTION_REWIND)
        stringActions.add(
            if (shouldShowPauseButton(player)) {
                ACTION_PAUSE
            } else {
                ACTION_PLAY
            }
        )
        stringActions.add(ACTION_FAST_FORWARD)
        return stringActions
    }

    private fun shouldShowPauseButton(player: Player): Boolean {
        val state = player.playbackState
        return state != Player.STATE_ENDED && state != Player.STATE_IDLE && player.playWhenReady
    }
}
