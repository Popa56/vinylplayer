package com.example.vinylplayer

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.abs

/**
 * Хранилище метаданных: избранное, индивидуальный цвет винила,
 * ручная подмена обложки для альбома/трека.
 * Всё лежит в SharedPreferences "meta" + файлы override-обложек в filesDir.
 */
object MetaStore {

    private const val PREFS = "meta"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ───────────────────────── Избранное ─────────────────────────
    // Ключ — uri трека (переживает перезапуск и пересборку библиотеки)

    fun favorites(ctx: Context): MutableSet<String> =
        prefs(ctx).getStringSet("fav", emptySet())?.toMutableSet() ?: mutableSetOf()

    fun isFavorite(ctx: Context, uri: Uri): Boolean =
        uri.toString() in favorites(ctx)

    fun setFavorite(ctx: Context, uri: Uri, on: Boolean) {
        val f = favorites(ctx)
        if (on) f += uri.toString() else f -= uri.toString()
        prefs(ctx).edit().putStringSet("fav", f).apply()
    }

    fun clearFavorites(ctx: Context) {
        prefs(ctx).edit().remove("fav").apply()
    }

    // ─────────────────────── Цвет винила альбома ───────────────────────

    fun vinylColor(ctx: Context, albumId: String): Color? {
        val v = prefs(ctx).getInt("vc_${albumId.hashCode()}", -1)
        return if (v == -1) null else Color(v.toUInt())
    }

    fun setVinylColor(ctx: Context, albumId: String, c: Color?) {
        prefs(ctx).edit().apply {
            if (c == null) remove("vc_${albumId.hashCode()}")
            else putInt("vc_${albumId.hashCode()}", c.value.toLong().toInt())
        }.apply()
    }

    // ─────────────────── Ручная подмена обложки ───────────────────
    // Своя обложка перекрывает обложку из папки и из тегов.

    fun coverOverride(ctx: Context, albumId: String): File? {
        val f = File(ctx.filesDir, "c_override_${albumId.hashCode()}.jpg")
        return if (f.exists()) f else null
    }

    fun setCoverOverride(ctx: Context, albumId: String, bytes: ByteArray) {
        File(ctx.filesDir, "c_override_${albumId.hashCode()}.jpg").writeBytes(bytes)
    }

    fun clearCoverOverride(ctx: Context, albumId: String) {
        File(ctx.filesDir, "c_override_${albumId.hashCode()}.jpg").delete()
    }

    // ───────────────────────── Палитра винила ─────────────────────────

    val VinylPalette: List<Color> = listOf(
        Color(0xFF6B4FA0), // фиолетовый
        Color(0xFFB03A48), // бордовый
        Color(0xFF2E7D6B), // изумруд
        Color(0xFFB8862E), // золото
        Color(0xFF3A5BA0), // синий
        Color(0xFF7A4A2A), // коричневый
        Color(0xFF8E4A8E), // маджента
        Color(0xFF2B2B35)  // графит
    )

    /** Цвет по умолчанию, если пользователь не выбрал свой. */
    fun defaultVinylColor(albumId: String): Color =
        VinylPalette[abs(albumId.hashCode()) % VinylPalette.size]

    /** Итоговый цвет пластинки для альбома. */
    fun vinylColorOr(ctx: Context, albumId: String): Color =
        vinylColor(ctx, albumId) ?: defaultVinylColor(albumId)
}

/** Итоговая обложка с учётом ручной подмены. */
fun resolveCover(ctx: Context, albumId: String): File =
    MetaStore.coverOverride(ctx, albumId) ?: albumCoverFile(ctx, albumId)
