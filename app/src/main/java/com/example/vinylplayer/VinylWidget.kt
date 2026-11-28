package com.example.vinylplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews

object WidgetState {
    const val ACTION_PLAY_PAUSE = "com.example.vinylplayer.WIDGET_PLAY_PAUSE"
    const val ACTION_NEXT = "com.example.vinylplayer.WIDGET_NEXT"
    const val ACTION_PREV = "com.example.vinylplayer.WIDGET_PREV"

    fun update(ctx: Context) {
        runCatching {
            val i = Intent(ctx, VinylWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(ctx)
                    .getAppWidgetIds(ComponentName(ctx, VinylWidget::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            ctx.sendBroadcast(i)
        }
    }
}

class VinylWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val player = PlayerHolder.player(ctx)
        val md = player.currentMediaItem?.mediaMetadata
        val title = md?.title?.toString() ?: "VinylPlayer"
        val artist = md?.artist?.toString() ?: "Нет трека"
        val playing = player.isPlaying
        val art = md?.artworkUri?.path?.let {
            runCatching { BitmapFactory.decodeFile(it) }.getOrNull()
        }

        ids.forEach { id ->
            val v = RemoteViews(ctx.packageName, R.layout.widget_vinyl)
            v.setTextViewText(R.id.w_title, title)
            v.setTextViewText(R.id.w_artist, artist)
            v.setImageViewResource(
                R.id.w_btn,
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )
            if (art != null) v.setImageViewBitmap(R.id.w_art, art)
            else v.setImageViewResource(R.id.w_art, R.drawable.ic_play)

            v.setOnClickPendingIntent(R.id.w_btn, pi(ctx, WidgetState.ACTION_PLAY_PAUSE, 1))
            v.setOnClickPendingIntent(R.id.w_next, pi(ctx, WidgetState.ACTION_NEXT, 2))
            v.setOnClickPendingIntent(R.id.w_prev, pi(ctx, WidgetState.ACTION_PREV, 3))
            v.setOnClickPendingIntent(R.id.w_root, openApp(ctx, 4))
            mgr.updateAppWidget(id, v)
        }
    }

    private fun pi(ctx: Context, action: String, code: Int): PendingIntent {
        val i = Intent(ctx, WidgetReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openApp(ctx: Context, code: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            ctx, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class WidgetReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val player = PlayerHolder.player(ctx)
        when (intent.action) {
            WidgetState.ACTION_PLAY_PAUSE ->
                if (player.isPlaying) player.pause() else player.play()
            WidgetState.ACTION_NEXT -> player.seekToNextMediaItem()
            WidgetState.ACTION_PREV -> player.seekToPreviousMediaItem()
        }
        WidgetState.update(ctx)
    }
}
