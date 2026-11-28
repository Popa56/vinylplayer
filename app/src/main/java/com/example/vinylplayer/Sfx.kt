package com.example.vinylplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Звуки интерфейса, синтезированные на лету (без res/raw).
 *  - click  : короткий тихий щелчок
 *  - sleeve : мягкое шуршание конверта
 *  - room   : очень тихая петля «комнаты» (пока музыка на паузе)
 */
object Sfx {

    var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SR = 44100

    private var room: AudioTrack? = null

    // ───────────────────────── Публичное API ─────────────────────────

    fun click(ctx: Context) {
        if (!enabled) return
        scope.launch {
            val samples = ShortArray(SR / 40) // ~25 мс
            for (i in samples.indices) {
                val t = i.toFloat() / SR
                val env = exp(-t * 180f)
                val s = (sin(2.0 * PI * 1800.0 * t) * 0.4 +
                         sin(2.0 * PI * 2400.0 * t) * 0.2).toFloat()
                samples[i] = (s * env * Short.MAX_VALUE * 0.35f).toInt().toShort()
            }
            playOnce(samples, volume = 0.35f)
        }
    }

    fun sleeve(ctx: Context) {
        if (!enabled) return
        scope.launch {
            // шуршание: фильтрованный шум с нарастанием и затуханием
            val n = SR / 3 // ~330 мс
            val samples = ShortArray(n)
            var lp = 0f
            for (i in 0 until n) {
                val t = i.toFloat() / n
                val env = (if (t < 0.25f) t / 0.25f else (1f - t) / 0.75f).coerceIn(0f, 1f)
                val noise = Random.nextFloat() * 2f - 1f
                lp = lp * 0.82f + noise * 0.18f // простой low-pass
                samples[i] = (lp * env * Short.MAX_VALUE * 0.22f).toInt().toShort()
            }
            playOnce(samples, volume = 0.5f)
        }
    }

    /** Тихая петля «комнаты». Идемпотентно. */
    fun roomStart() {
        if (!enabled || room != null) return
        val durSec = 4
        val n = SR * durSec
        val buf = ShortArray(n)
        var lp1 = 0f
        var lp2 = 0f
        // коричневый шум — звучит как тихая комната
        var brown = 0f
        for (i in 0 until n) {
            val w = Random.nextFloat() * 2f - 1f
            brown = (brown + 0.02f * w).coerceIn(-1f, 1f)
            lp1 = lp1 * 0.97f + brown * 0.03f
            lp2 = lp2 * 0.9f + lp1 * 0.1f
            // очень тихо
            buf[i] = (lp2 * Short.MAX_VALUE * 0.06f).toInt().toShort()
        }
        // плавные края для бесшовной петли
        val fade = SR / 10
        for (i in 0 until fade) {
            val k = i.toFloat() / fade
            buf[i] = (buf[i] * k).toInt().toShort()
            buf[n - 1 - i] = (buf[n - 1 - i] * k).toInt().toShort()
        }
        room = buildTrack().also { tr ->
            runCatching {
                tr.write(buf, 0, buf.size)
                tr.setLoopPoints(0, buf.size, -1)
                tr.play()
            }
        }
    }

    fun roomStop() {
        room?.let { tr ->
            runCatching { tr.stop() }
            runCatching { tr.release() }
        }
        room = null
    }

    fun release() {
        roomStop()
    }

    // ───────────────────────── Внутреннее ─────────────────────────

    private fun buildTrack(): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(SR, AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

    private fun playOnce(samples: ShortArray, volume: Float) {
        val tr = runCatching { buildTrack() }.getOrNull() ?: return
        runCatching {
            tr.write(samples, 0, samples.size)
            tr.setVolume(volume)
            tr.play()
        }
        // освободим после проигрывания
        scope.launch {
            kotlinx.coroutines.delay((samples.size * 1000L / SR) + 200)
            runCatching { tr.stop() }
            runCatching { tr.release() }
        }
    }
}
