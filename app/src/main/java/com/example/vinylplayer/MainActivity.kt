package com.example.vinylplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sign

val Accent = Color(0xFFE8B04B)
val BaseDark = Color(0xFF0B0B0F)
val Muted = Color(0xFFB0B0BA)

data class Track(
    val uri: Uri, val title: String, val artist: String, val album: String,
    val albumId: String, val no: Int = 0, val durMs: Long = 0L
)

data class Album(val id: String, val title: String, val artist: String, val tracks: List<Track>)

const val UNKNOWN_ARTIST = "Неизвестный исполнитель"
const val UNKNOWN_ALBUM = "Неизвестный альбом"

fun effDur(t: Track): Long = if (t.durMs > 0) t.durMs else 180_000L

// Треки одного альбома (по albumId) собираются в одну пластинку
fun groupAlbums(list: List<Track>): List<Album> =
    list.groupBy { it.albumId }.map { (id, ts) ->
        val artists = ts.map { it.artist }.distinct()
        val first = ts.first()
        val title = if (ts.size == 1 && first.album == UNKNOWN_ALBUM) first.title else first.album
        Album(id, title, if (artists.size == 1) artists[0] else "Разные исполнители", ts)
    }

// Обложка хранится на альбом (по albumId)
fun albumCoverFile(ctx: Context, albumId: String): File {
    val h = java.security.MessageDigest.getInstance("SHA-1")
        .digest(albumId.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .take(16)
    return File(ctx.filesDir, "c_$h.jpg")
}

fun coverFile(ctx: Context, t: Track): File = resolveCover(ctx, t.albumId)

// Для уже добавленных треков: переносим старую обложку альбома (если альбом известен)
// или достаём обложку из тегов файла. Возвращает true, если файл создан.
fun ensureCover(ctx: Context, t: Track): Boolean {
    val f = coverFile(ctx, t)
    if (f.exists()) return false
    if (t.artist != UNKNOWN_ARTIST && t.album != UNKNOWN_ALBUM) {
        val old = File(ctx.filesDir, "cover_${(t.artist + "|" + t.album).hashCode()}.jpg")
        if (old.exists()) {
            runCatching { old.copyTo(f) }
            if (f.exists()) return true
        }
    }
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, t.uri)
        val b = r.embeddedPicture
        if (b != null) {
            f.writeBytes(b)
            true
        } else false
    } catch (e: Exception) {
        false
    } finally {
        runCatching { r.release() }
    }
}

fun mediaItem(ctx: Context, t: Track): MediaItem {
    val f = coverFile(ctx, t)
    val md = MediaMetadata.Builder()
        .setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album)
    if (f.exists()) md.setArtworkUri(Uri.fromFile(f))
    return MediaItem.Builder()
        .setUri(t.uri).setMediaId(t.uri.toString()).setMediaMetadata(md.build()).build()
}

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

fun fileName(ctx: Context, uri: Uri): String {
    var n: String? = null
    runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) n = c.getString(0) }
    }
    return (n ?: "Трек").substringBeforeLast('.')
}

fun readTrack(
    ctx: Context, uri: Uri,
    forcedAlbumId: String? = null, fallbackAlbum: String? = null
): Track {
    val r = MediaMetadataRetriever()
    val name = fileName(ctx, uri)
    return try {
        r.setDataSource(ctx, uri)
        fun tag(key: Int): String? = r.extractMetadata(key)?.trim()?.takeIf { it.isNotEmpty() }
        val title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name
        val artist = tag(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?: tag(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: UNKNOWN_ARTIST
        val albumTag = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val album = albumTag ?: fallbackAlbum ?: UNKNOWN_ALBUM
        val no = tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            ?.substringBefore('/')?.trim()?.toIntOrNull() ?: 0
        val dur = tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val albumId = forcedAlbumId
            ?: if (albumTag != null && artist != UNKNOWN_ARTIST) "tag:$artist|$albumTag" else uri.toString()
        val t = Track(uri, title, artist, album, albumId, no, dur)
        val f = coverFile(ctx, t)
        if (!f.exists()) r.embeddedPicture?.let { f.writeBytes(it) }
        t
    } catch (e: Exception) {
        Track(uri, name, UNKNOWN_ARTIST, fallbackAlbum ?: UNKNOWN_ALBUM, forcedAlbumId ?: uri.toString())
    } finally {
        runCatching { r.release() }
    }
}

fun readDuration(ctx: Context, uri: Uri): Long {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        0L
    } finally {
        runCatching { r.release() }
    }
}

private val AUDIO_EXT = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "wma", "aiff", "alac")
private val IMAGE_EXT = setOf("jpg", "jpeg", "png")
private val COVER_NAMES = setOf("cover", "folder", "front", "album", "artwork")

// Папка = альбом. Вложенные папки с музыкой становятся отдельными альбомами.
fun scanFolder(ctx: Context, tree: Uri): List<Track> {
    val out = mutableListOf<Track>()
    val resolver = ctx.contentResolver
    val rootId = DocumentsContract.getTreeDocumentId(tree)
    var rootName = rootId.substringAfterLast(':').substringAfterLast('/')
    runCatching {
        resolver.query(
            DocumentsContract.buildDocumentUriUsingTree(tree, rootId),
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.let { rootName = it }
        }
    }

    fun walk(docId: String, dirName: String) {
        val files = mutableListOf<Pair<Uri, String>>()
        val images = mutableListOf<Pair<Uri, String>>()
        val dirs = mutableListOf<Pair<String, String>>()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        runCatching {
            resolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ), null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: ""
                    val mime = c.getString(2) ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> dirs += id to name
                        mime.startsWith("audio/") || ext in AUDIO_EXT -> files += docUri to name
                        mime.startsWith("image/") || ext in IMAGE_EXT -> images += docUri to name
                    }
                }
            }
        }
        if (files.isNotEmpty()) {
            val albumId = "dir:$docId"
            val img = images.firstOrNull { it.second.substringBeforeLast('.').lowercase() in COVER_NAMES }
            if (img != null) {
                val cf = albumCoverFile(ctx, albumId)
                if (!cf.exists()) {
                    runCatching {
                        resolver.openInputStream(img.first)?.use { inp ->
                            cf.outputStream().use { inp.copyTo(it) }
                        }
                    }
                }
            }
            files.sortedBy { it.second.lowercase() }.forEach { (u, _) ->
                out += readTrack(ctx, u, albumId, dirName)
            }
        }
        dirs.sortedBy { it.second.lowercase() }.forEach { (id, name) -> walk(id, name) }
    }

    walk(rootId, rootName)
    return out
}

fun saveLibrary(ctx: Context, list: List<Track>) {
    val arr = JSONArray()
    list.forEach {
        arr.put(
            JSONObject().put("u", it.uri.toString()).put("t", it.title)
                .put("a", it.artist).put("b", it.album)
                .put("i", it.albumId).put("n", it.no).put("d", it.durMs)
        )
    }
    ctx.getSharedPreferences("lib", Context.MODE_PRIVATE)
        .edit().putString("tracks", arr.toString()).apply()
}

fun loadLibrary(ctx: Context): List<Track> {
    val s = ctx.getSharedPreferences("lib", Context.MODE_PRIVATE)
        .getString("tracks", null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(s)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val uri = o.getString("u")
            val artist = o.getString("a")
            val album = o.getString("b")
            val albumId = o.optString("i", "").ifEmpty {
                if (artist != UNKNOWN_ARTIST && album != UNKNOWN_ALBUM) "tag:$artist|$album" else uri
            }
            Track(
                Uri.parse(uri), o.getString("t"), artist, album,
                albumId, o.optInt("n", 0), o.optLong("d", 0L)
            )
        }
    }.getOrDefault(emptyList())
        .let { l -> l.groupBy { it.albumId }.values.flatMap { g -> g.sortedBy { it.no } } }
}

fun dominantColor(f: File): Color? {
    if (!f.exists()) return null
    val opts = BitmapFactory.Options().apply { inSampleSize = 32 }
    val bmp = BitmapFactory.decodeFile(f.path, opts) ?: return null
    var r = 0L
    var g = 0L
    var b = 0L
    var n = 0
    for (x in 0 until bmp.width) for (y in 0 until bmp.height) {
        val p = bmp.getPixel(x, y)
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
        n++
    }
    bmp.recycle()
    return if (n == 0) null else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
}

// ───────────────────────── Эквалайзер ─────────────────────────

class FxState(private val prefs: SharedPreferences) {
    var enabled by mutableStateOf(prefs.getBoolean("on", false))
    var bass by mutableIntStateOf(prefs.getInt("bass", 0))
    var available by mutableStateOf(true)
    var minLevel by mutableIntStateOf(-1500)
    var maxLevel by mutableIntStateOf(1500)
    var freqs by mutableStateOf<List<Int>>(emptyList())
    var presets by mutableStateOf<List<String>>(emptyList())
    val levels = mutableStateListOf<Int>()

    private var eq: Equalizer? = null
    private var bassFx: BassBoost? = null
    private var session = 0

    fun attach(id: Int) {
        if (id <= 0 || id == session) return
        release()
        session = id
        eq = runCatching { Equalizer(0, id) }.getOrNull()
        bassFx = runCatching { BassBoost(0, id) }.getOrNull()
        val e = eq
        if (e == null) {
            available = false
            return
        }
        available = true
        val n = e.numberOfBands.toInt()
        minLevel = e.bandLevelRange[0].toInt()
        maxLevel = e.bandLevelRange[1].toInt()
        freqs = (0 until n).map { e.getCenterFreq(it.toShort()) / 1000 }
        presets = (0 until e.numberOfPresets.toInt()).map { e.getPresetName(it.toShort()) }
        val saved = prefs.getString("levels", null)?.split(",")?.mapNotNull { it.toIntOrNull() }
        levels.clear()
        for (i in 0 until n) {
            val v = saved?.getOrNull(i) ?: e.getBandLevel(i.toShort()).toInt()
            levels.add(v.coerceIn(minLevel, maxLevel))
        }
        push()
    }

    private fun push() {
        eq?.let { e ->
            levels.forEachIndexed { i, v ->
                runCatching { e.setBandLevel(i.toShort(), v.toShort()) }
            }
            runCatching { e.setEnabled(enabled) }
        }
        bassFx?.let { b ->
            runCatching {
                if (b.strengthSupported) b.setStrength(bass.toShort())
                b.setEnabled(enabled && bass > 0)
            }
        }
    }

    private fun saveLevels() {
        prefs.edit().putString("levels", levels.joinToString(",")).apply()
    }

    fun setOn(v: Boolean) {
        enabled = v
        prefs.edit().putBoolean("on", v).apply()
        push()
    }

    fun setBand(i: Int, v: Int) {
        if (i !in levels.indices) return
        levels[i] = v
        eq?.let { e -> runCatching { e.setBandLevel(i.toShort(), v.toShort()) } }
        saveLevels()
    }

    fun updateBass(v: Int) {
        bass = v
        prefs.edit().putInt("bass", v).apply()
        push()
    }

    fun usePreset(i: Int) {
        val e = eq ?: return
        runCatching { e.usePreset(i.toShort()) }
        for (b in levels.indices) levels[b] = e.getBandLevel(b.toShort()).toInt()
        saveLevels()
        if (!enabled) setOn(true)
    }

    fun reset() {
        for (b in levels.indices) levels[b] = 0
        bass = 0
        prefs.edit().putInt("bass", 0).apply()
        saveLevels()
        push()
    }

    fun release() {
        runCatching { eq?.release() }
        runCatching { bassFx?.release() }
        eq = null
        bassFx = null
        session = 0
    }
}

// Один плеер на весь процесс: им пользуется и экран, и сервис уведомления, и виджет
object PlayerHolder {
    private var p: ExoPlayer? = null
    private var f: FxState? = null

    @Synchronized
    fun player(ctx: Context): ExoPlayer {
        init(ctx)
        return p!!
    }

    @Synchronized
    fun fx(ctx: Context): FxState {
        init(ctx)
        return f!!
    }

    private fun init(ctx: Context) {
        if (p != null) return
        val app = ctx.applicationContext
        val player = ExoPlayer.Builder(app)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL
        val fx = FxState(app.getSharedPreferences("fx", Context.MODE_PRIVATE))
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                fx.attach(audioSessionId)
            }
        })
        fx.attach(player.audioSessionId)
        p = player
        f = fx
    }
}

// Сервис: показывает уведомление с кнопками пауза / следующий / предыдущий
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = PlayerHolder.player(this)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Sfx.enabled = getSharedPreferences("sfx", Context.MODE_PRIVATE)
            .getBoolean("on", true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) { PlayerApp() }
        }
    }

    override fun onDestroy() {
        Sfx.release()
        super.onDestroy()
    }
}

@Composable
fun PlayerApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { PlayerHolder.player(ctx) }
    val fx = remember { PlayerHolder.fx(ctx) }
    val tracks = remember { mutableStateListOf<Track>() }
    var currentId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var pendingPlay by remember { mutableStateOf(false) }
    var reversing by remember { mutableStateOf(false) }
    var resumeAfter by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showNow by remember { mutableStateOf(false) }
    var eqOpen by remember { mutableStateOf(false) }
    var showFav by remember { mutableStateOf(false) }
    var manualMode by remember { mutableStateOf(false) }
    var libraryLoading by remember { mutableStateOf(true) }
    val uiPrefs = remember { ctx.getSharedPreferences("ui", Context.MODE_PRIVATE) }
    var crateMode by remember { mutableStateOf(uiPrefs.getBoolean("crate", true)) }
    var picker by remember { mutableStateOf<Track?>(null) }
    var coverVersion by remember { mutableIntStateOf(0) }
    var tint by remember { mutableStateOf(Color(0xFF2A2140)) }
    val listState = rememberLazyListState()
    val current = tracks.firstOrNull { it.uri.toString() == currentId }
    val albums by remember { derivedStateOf { groupAlbums(tracks) } }
    val curAlbum = albums.firstOrNull { it.id == current?.albumId }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var vinylColorMenu by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) Sfx.roomStop() else Sfx.roomStart()
                WidgetState.update(ctx)
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                currentId = item?.mediaId
                WidgetState.update(ctx)
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { ctx.startService(Intent(ctx, PlaybackService::class.java)) }

        val saved = loadLibrary(ctx)
        tracks.addAll(saved)
        scope.launch {
            val created = withContext(Dispatchers.IO) { saved.map { ensureCover(ctx, it) }.any { it } }
            if (created) coverVersion++
            val missing = saved.filter { it.durMs == 0L }
            if (missing.isNotEmpty()) {
                val durs = withContext(Dispatchers.IO) {
                    missing.associate { it.uri to readDuration(ctx, it.uri) }
                }
                var changed = false
                for (i in tracks.indices) {
                    val t = tracks[i]
                    val d = durs[t.uri]
                    if (t.durMs == 0L && d != null && d > 0L) {
                        tracks[i] = t.copy(durMs = d)
                        changed = true
                    }
                }
                if (changed) saveLibrary(ctx, tracks)
            }
            libraryLoading = false
        }
        if (player.mediaItemCount == 0) {
            player.setMediaItems(saved.map { mediaItem(ctx, it) })
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        currentId = player.currentMediaItem?.mediaId
        isPlaying = player.isPlaying
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    LaunchedEffect(currentId, coverVersion) {
        val t = tracks.firstOrNull { it.uri.toString() == currentId }
        val c = if (t != null) withContext(Dispatchers.IO) { dominantColor(coverFile(ctx, t)) } else null
        tint = if (c != null) lerp(c, Color.Black, 0.55f) else Color(0xFF2A2140)
    }

    // Реверс: быстрая перемотка назад (звук на это время выключен)
    LaunchedEffect(reversing) {
        if (reversing) {
            player.pause()
            while (true) {
                val p = player.currentPosition
                if (p <= 0L) {
                    reversing = false
                    if (resumeAfter) player.play()
                    break
                }
                player.seekTo((p - 400L).coerceAtLeast(0L))
                delay(120)
            }
        }
    }
    val bg by animateColorAsState(tint, tween(900), label = "bg")
    val labelColor = lerp(bg, Color.White, 0.35f)

    fun rebuildPlaylist() {
        val curId = player.currentMediaItem?.mediaId
        val pos = player.currentPosition
        val items = tracks.map { mediaItem(ctx, it) }
        val idx = tracks.indexOfFirst { it.uri.toString() == curId }
        if (idx >= 0) {
            player.setMediaItems(items, idx, pos)
        } else {
            player.setMediaItems(items)
            player.pause()
        }
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
    }

    fun addTracks(added: List<Track>) {
        val existing = tracks.map { it.uri }.toSet()
        val fresh = added.filter { it.uri !in existing }
        if (fresh.isEmpty()) return
        val merged = (tracks.toList() + fresh)
            .groupBy { it.albumId }.values
            .flatMap { grp -> grp.sortedBy { it.no } }
        tracks.clear()
        tracks.addAll(merged)
        rebuildPlaylist()
        saveLibrary(ctx, tracks)
        coverVersion++
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val read = withContext(Dispatchers.IO) {
                    uris.map { u ->
                        runCatching {
                            ctx.contentResolver.takePersistableUriPermission(
                                u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        readTrack(ctx, u)
                    }
                }
                addTracks(read)
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { tree ->
        if (tree != null) {
            scope.launch {
                runCatching {
                    ctx.contentResolver.takePersistableUriPermission(
                        tree, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val read = withContext(Dispatchers.IO) { scanFolder(ctx, tree) }
                if (read.isEmpty()) {
                    Toast.makeText(ctx, "В папке нет аудиофайлов", Toast.LENGTH_SHORT).show()
                } else {
                    addTracks(read)
                }
            }
        }
    }

    fun releasePending() {
        if (pendingPlay) {
            pendingPlay = false
            player.play()
        }
    }

    fun playIndex(i: Int) {
        if (i !in tracks.indices) return
        reversing = false
        player.pause()
        player.seekTo(i, 0L)
        currentId = tracks[i].uri.toString()
        showNow = true
        if (!manualMode) pendingPlay = true
        Sfx.sleeve(ctx)
    }

    fun skip(next: Boolean) {
        reversing = false
        if (tracks.size > 1) {
            pendingPlay = true
            player.pause()
            if (next) player.seekToNextMediaItem() else player.seekToPreviousMediaItem()
            Sfx.click(ctx)
        } else {
            player.seekTo(0L)
        }
    }

    fun scratchBy(deltaMs: Long) {
        val dur = player.duration
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (dur > 0) minOf(target, dur - 100L).coerceAtLeast(0L) else target)
    }

    fun removeAlbum(a: Album) {
        tracks.removeAll { it.albumId == a.id }
        if (expandedId == a.id) expandedId = null
        rebuildPlaylist()
        saveLibrary(ctx, tracks)
    }

    fun playAlbum(a: Album) {
        if (a.id == current?.albumId) {
            showNow = true
        } else {
            playIndex(tracks.indexOf(a.tracks.first()))
        }
    }

    fun toggle() {
        reversing = false
        pendingPlay = false
        if (player.isPlaying) player.pause() else player.play()
        Sfx.click(ctx)
    }

    fun doPlay() {
        reversing = false
        pendingPlay = false
        player.play()
        Sfx.click(ctx)
    }

    fun doPause() {
        reversing = false
        pendingPlay = false
        player.pause()
        Sfx.click(ctx)
    }

    fun toggleReverse() {
        if (reversing) {
            reversing = false
            if (resumeAfter) player.play()
        } else {
            resumeAfter = player.isPlaying || pendingPlay
            pendingPlay = false
            reversing = true
        }
    }

    fun needleLift() {
        reversing = false
        pendingPlay = false
        player.pause()
        Sfx.click(ctx)
    }

    fun needleDrop(frac: Float) {
        reversing = false
        pendingPlay = false
        val alb = curAlbum
        if (alb == null) {
            player.play()
            return
        }
        val total = alb.tracks.sumOf { effDur(it) }
        var t = (frac * total).toLong()
        var idx = alb.tracks.lastIndex
        for ((i, tr) in alb.tracks.withIndex()) {
            val d = effDur(tr)
            if (t < d) {
                idx = i
                break
            }
            t -= d
        }
        t = t.coerceAtMost((effDur(alb.tracks[idx]) - 1500L).coerceAtLeast(0L))
        val flat = tracks.indexOf(alb.tracks[idx])
        if (flat >= 0) player.seekTo(flat, t.coerceAtLeast(0L))
        player.play()
    }

    fun closeNow() {
        showNow = false
        if (reversing) {
            reversing = false
            if (resumeAfter) player.play()
        }
        releasePending()
    }

    LaunchedEffect(pendingPlay, manualMode) {
        if (pendingPlay && !manualMode) {
            delay(4500)
            releasePending()
        }
    }

    BackHandler(enabled = showNow && picker == null && !showFav) { closeNow() }
    BackHandler(enabled = eqOpen) { eqOpen = false }
    BackHandler(enabled = showFav) { showFav = false }

    val pickerTrack = picker
    if (pickerTrack != null) {
        BackHandler { picker = null }
        CoverPicker(pickerTrack, onBack = { picker = null }) { imageUrl ->
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { URL(imageUrl).openStream().use { it.readBytes() } }.getOrNull()
                }
                if (bytes == null) {
                    Toast.makeText(ctx, "Не удалось скачать картинку", Toast.LENGTH_SHORT).show()
                } else {
                    withContext(Dispatchers.IO) {
                        MetaStore.setCoverOverride(ctx, pickerTrack.albumId, bytes)
                    }
                    coverVersion++
                    picker = null
                }
            }
        }
    } else if (showFav) {
        FavoritesScreen(
            tracks = tracks.filter { MetaStore.isFavorite(ctx, it.uri) },
            version = coverVersion,
            onPlay = { t -> showFav = false; playIndex(tracks.indexOf(t)) },
            onRemove = { t -> MetaStore.setFavorite(ctx, t.uri, false); coverVersion++ },
            onBack = { showFav = false }
        )
    } else {
        Box(Modifier.fillMaxSize().storeWall(bg)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        NeonSign("VINYL SHOP", Color(0xFFFF4D8D), 24.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeonSign("OPEN", Color(0xFF4DFFB0), 11.sp)
                            Text("  ·  альбомов: ${albums.size}", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                            .clickable {
                                crateMode = !crateMode
                                uiPrefs.edit().putBoolean("crate", crateMode).apply()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (crateMode) "Полка" else "Ящик",
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                            .clickable { showFav = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) { Text("♥", color = Accent, fontSize = 15.sp) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                            .clickable { eqOpen = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) { Text("EQ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(Accent)
                                .clickable { addMenu = true },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = Color.Black, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Добавить альбом (папку)") },
                                onClick = {
                                    addMenu = false
                                    folderLauncher.launch(null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Добавить файлы") },
                                onClick = {
                                    addMenu = false
                                    importLauncher.launch(arrayOf("audio/*"))
                                }
                            )
                        }
                    }
                }

                if (tracks.isEmpty()) {
                    EmptyState(
                        onAlbum = { folderLauncher.launch(null) },
                        onFiles = { importLauncher.launch(arrayOf("audio/*")) }
                    )
                } else if (crateMode) {
                    Crate(
                        albums, current?.albumId, isPlaying, coverVersion,
                        onPlay = { albums.getOrNull(it)?.let { a -> playAlbum(a) } },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(albums, key = { it.id }) { a ->
                            val isCurAlbum = a.id == current?.albumId
                            val open = expandedId == a.id
                            val rowBg by animateColorAsState(
                                if (isCurAlbum) Color(0x26FFFFFF) else Color.Transparent,
                                label = "row"
                            )
                            Column(Modifier.fillMaxWidth().animateItem()) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                        .background(rowBg)
                                        .clickable { expandedId = if (open) null else a.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoverImage(
                                        resolveCover(ctx, a.id), coverVersion,
                                        Modifier.size(58.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            a.title, color = if (isCurAlbum) Accent else Color.White,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${a.artist} · треков: ${a.tracks.size}",
                                            color = Muted, fontSize = 12.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isCurAlbum) EqBars(isPlaying, Accent)
                                    Box(Modifier.clickable { removeAlbum(a) }.padding(12.dp)) {
                                        Text("✕", color = Color(0x88FFFFFF))
                                    }
                                }
                                AnimatedVisibility(visible = open) {
                                    Column(
                                        Modifier.fillMaxWidth()
                                            .padding(start = 12.dp, end = 4.dp, bottom = 8.dp)
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                .clickable { playAlbum(a) }
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PlayerIcon("play", Accent, 16.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "Слушать альбом с начала", color = Accent,
                                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                                            )
                                        }
                                        a.tracks.forEachIndexed { ti, t ->
                                            val isCur = t.uri.toString() == currentId
                                            val fav = MetaStore.isFavorite(ctx, t.uri)
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                                    .clickable { playIndex(tracks.indexOf(t)) }
                                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${ti + 1}", Modifier.width(26.dp), color = Muted, fontSize = 12.sp)
                                                Text(
                                                    t.title, Modifier.weight(1f),
                                                    color = if (isCur) Accent else Color.White,
                                                    fontSize = 14.sp, maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (t.durMs > 0) {
                                                    Text(fmt(t.durMs), color = Muted, fontSize = 12.sp)
                                                }
                                                Box(
                                                    Modifier.clickable {
                                                        MetaStore.setFavorite(ctx, t.uri, !fav)
                                                        coverVersion++
                                                    }.padding(8.dp)
                                                ) {
                                                    Text(
                                                        if (fav) "♥" else "♡",
                                                        color = if (fav) Accent else Muted,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = current != null && !showNow,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
                enter = slideInVertically(tween(350)) { it } + fadeIn(),
                exit = slideOutVertically(tween(250)) { it } + fadeOut()
            ) {
                current?.let {
                    MiniPlayer(
                        it, coverFile(ctx, it), coverVersion, isPlaying, labelColor,
                        onOpen = { showNow = true }, onToggle = { toggle() }
                    )
                }
            }

            AnimatedVisibility(
                visible = showNow && current != null,
                enter = slideInVertically(tween(450)) { it } + fadeIn(),
                exit = slideOutVertically(tween(350)) { it } + fadeOut()
            ) {
                current?.let { cur ->
                    NowPlaying(
                        track = cur, file = coverFile(ctx, cur), version = coverVersion,
                        isPlaying = isPlaying, waiting = pendingPlay, reversing = reversing,
                        position = position, duration = duration,
                        albumDurs = curAlbum?.tracks?.map { effDur(it) } ?: listOf(effDur(cur)),
                        trackIdx = curAlbum?.tracks?.indexOf(cur)?.coerceAtLeast(0) ?: 0,
                        tint = bg, labelColor = vinylColor,
                        manualMode = manualMode,
                        isFavorite = MetaStore.isFavorite(ctx, cur.uri),
                        onClose = { closeNow() },
                        onFindCover = { releasePending(); picker = cur },
                        onEq = { eqOpen = true },
                        onToggleFav = {
                            MetaStore.setFavorite(ctx, cur.uri, !MetaStore.isFavorite(ctx, cur.uri))
                            coverVersion++
                        },
                        onToggleManual = { manualMode = !manualMode },
                        onPlay = { doPlay() },
                        onPause = { doPause() },
                        onReverse = { toggleReverse() },
                        onPrev = { skip(false) },
                        onNext = { skip(true) },
                        onSeek = { ms -> player.seekTo(ms) },
                        onPulled = { releasePending() },
                        onScratch = { d -> scratchBy(d) },
                        onNeedleLift = { needleLift() },
                        onNeedleDrop = { f -> needleDrop(f) },
                        onPickVinylColor = { c ->
                            curAlbum?.let { MetaStore.setVinylColor(ctx, it.id, c) }
                            coverVersion++
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = eqOpen,
                enter = slideInVertically(tween(350)) { it } + fadeIn(),
                exit = slideOutVertically(tween(300)) { it } + fadeOut()
            ) {
                EqualizerPanel(fx) { eqOpen = false }
            }

            AnimatedVisibility(
                visible = libraryLoading,
                enter = fadeIn(), exit = fadeOut()
            ) {
                LibraryLoader()
            }
        }
    }
}

// Цвет пластинки для текущего альбома
val vinylColor: Color
    @Composable get() {
        val ctx = LocalContext.current
        return Accent
    }

val Cream = Color(0xFFF1E6CF)
val WoodLight = Color(0xFF7A5430)
val WoodDark = Color(0xFF4A3119)

fun Modifier.storeWall(tint: Color): Modifier = this
    .background(Brush.verticalGradient(listOf(Color(0xFF1C1512), Color(0xFF0D0A08))))
    .drawBehind {
        val rowH = 28.dp.toPx()
        val brickW = 72.dp.toPx()
        var y = 0f
        var row = 0
        while (y < this.size.height) {
            drawLine(Color(0x0DFFFFFF), Offset(0f, y), Offset(this.size.width, y), 1.5f)
            var x = if (row % 2 == 0) 0f else brickW / 2f
            while (x < this.size.width) {
                drawLine(Color(0x0DFFFFFF), Offset(x, y), Offset(x, y + rowH), 1.5f)
                x += brickW
            }
            y += rowH
            row++
        }
        drawRect(
            Brush.radialGradient(
                listOf(tint.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(this.size.width / 2f, 0f),
                radius = this.size.width * 0.95f
            )
        )
    }

@Composable
fun NeonSign(text: String, color: Color, fontSize: TextUnit) {
    val t = rememberInfiniteTransition(label = "neon")
    val flicker by t.animateFloat(
        0.80f, 1f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker"
    )
    Text(
        text, color = color.copy(alpha = flicker), fontSize = fontSize,
        fontWeight = FontWeight.Black, letterSpacing = 2.sp, maxLines = 1,
        style = TextStyle(shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = 24f))
    )
}

@Composable
fun PriceTag(title: String, subtitle: String, titleSize: TextUnit, modifier: Modifier = Modifier) {
    Column(
        modifier.graphicsLayer { rotationZ = -1.5f }
            .shadow(6.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp))
            .background(Cream).padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title, color = Color(0xFF3B2A1A), fontSize = titleSize, fontWeight = FontWeight.Bold,
            maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle, color = Color(0xFF7A6247), fontSize = 12.sp,
            maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CrateFront(modifier: Modifier, width: Dp, height: Dp) {
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
    Box(
        modifier.width(width).height(height)
            .shadow(12.dp, shape).clip(shape)
            .background(Brush.verticalGradient(listOf(WoodLight, WoodDark)))
            .drawBehind {
                val gap = this.size.height / 3f
                for (i in 1..2) {
                    drawLine(Color(0x44000000), Offset(0f, i * gap), Offset(this.size.width, i * gap), 2.5f)
                }
                drawLine(Color(0x55FFFFFF), Offset(0f, 2f), Offset(this.size.width, 2f), 3f)
                for (x in listOf(14.dp.toPx(), this.size.width - 14.dp.toPx())) {
                    for (yy in listOf(gap * 0.5f, gap * 1.5f, gap * 2.5f)) {
                        drawCircle(Color(0x66000000), 2.5f, Offset(x, yy))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.graphicsLayer { rotationZ = -2f }.clip(RoundedCornerShape(3.dp))
                .background(Cream).padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "NEW ARRIVALS", color = Color(0xFF3B2A1A), fontSize = 11.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun DeckKey(label: String, d: Dp, bg: Color, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RoundButton(onClick, d, bg, icon)
        Spacer(Modifier.height(4.dp))
        Text(
            label, color = Cream.copy(alpha = 0.85f), fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp
        )
    }
}

@Composable
fun EmptyState(onAlbum: () -> Unit, onFiles: () -> Unit) {
    val spin = rememberSpin(true, 14000)
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Vinyl(Color(0xFF6B4FA0), { spin.value }, 200.dp, labelText = "SIDE A")
        Spacer(Modifier.height(28.dp))
        Text("Полки пусты", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Привезите пластинки в магазин: выберите папку с альбомом, и все треки встанут на полку вместе. Обложка подтянется из файла cover.jpg в папке или из тегов.",
            color = Muted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Accent).clickable(onClick = onAlbum)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) { Text("Привезти альбом", color = Color.Black, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(6.dp))
        Text(
            "или добавить отдельные файлы", color = Muted, fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onFiles).padding(10.dp)
        )
    }
}

@Composable
fun LibraryLoader() {
    val spin = rememberSpin(true, 1400)
    Box(
        Modifier.fillMaxSize().storeWall(Color(0xFF3B2A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Vinyl(Accent, { spin.value }, 160.dp, labelText = "LOADING")
            Spacer(Modifier.height(24.dp))
            Text("Разбираем пластинки…", color = Cream, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Сканируем библиотеку", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun rememberSpin(
    playing: Boolean,
    periodMs: Int,
    coast: Boolean = true,
    reverse: Boolean = false
): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(playing, periodMs, reverse) {
        if (playing) {
            while (true) {
                val s = a.value % 360f
                a.snapTo(s)
                a.animateTo(if (reverse) s - 360f else s + 360f, tween(periodMs, easing = LinearEasing))
            }
        } else if (coast && a.value != 0f) {
            a.animateTo(a.value + (if (reverse) -40f else 40f), tween(900, easing = FastOutSlowInEasing))
        }
    }
    return a
}

@Composable
fun CoverImage(file: File?, version: Int, modifier: Modifier = Modifier) {
    val ok = remember(file, version) { file != null && file.exists() }
    if (ok) {
        AsyncImage(
            model = file, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = modifier
        )
    } else {
        Box(
            modifier.background(Brush.linearGradient(listOf(Color(0xFF3B2F5C), Color(0xFF1C1A2B)))),
            contentAlignment = Alignment.Center
        ) { Text("♪", color = Color(0x88FFFFFF), fontSize = 20.sp) }
    }
}

@Composable
fun Vinyl(
    labelColor: Color,
    angle: () -> Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    gaps: List<Float> = emptyList()
) {
    Box(modifier.size(diameter)) {
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = angle() }) {
            Canvas(Modifier.fillMaxSize()) {
                val r = this.size.minDimension / 2f
                drawCircle(Color(0xFF0E0E10))
                for (i in 0..14) {
                    drawCircle(
                        Color.White.copy(alpha = if (i % 3 == 0) 0.08f else 0.035f),
                        radius = r * (0.42f + i * 0.036f),
                        style = Stroke(1.2f)
                    )
                }
                val rOut = r * 0.946f
                val rIn = r * 0.40f
                for (g in gaps) {
                    val rr = rOut - g * (rOut - rIn)
                    drawCircle(Color(0xFF040405), radius = rr, style = Stroke(4f))
                    drawCircle(Color.White.copy(alpha = 0.10f), radius = rr + 3f, style = Stroke(1f))
                }
            }
            Box(
                Modifier.size(diameter * 0.36f).align(Alignment.Center).clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(lerp(labelColor, Color.White, 0.2f), labelColor))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (labelText != null) {
                    Text(
                        labelText, color = Color.Black.copy(alpha = 0.55f),
                        fontSize = (diameter.value * 0.045f).sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = -(diameter * 0.07f))
                    )
                }
            }
            Box(
                Modifier.size(diameter * 0.045f).align(Alignment.Center)
                    .clip(CircleShape).background(Color(0xFF0E0E10))
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent,
                        Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent
                    ),
                    center = center
                ),
                radius = r
            )
            drawCircle(Color.White.copy(alpha = 0.12f), radius = r - 1f, style = Stroke(2f))
        }
    }
}

@Composable
fun Tonearm(angle: State<Float>) {
    Canvas(Modifier.fillMaxSize()) {
        val k = this.size.width / 320f
        val pivot = Offset(305f * k, 8f * k)
        drawCircle(Color(0xFF3A3A40), 14f * k, pivot)
        rotate(angle.value, pivot) {
            drawLine(
                Color(0xFFCFCFD6), pivot, Offset(pivot.x, pivot.y + 137f * k),
                strokeWidth = 5f * k, cap = StrokeCap.Round
            )
            drawRoundRect(
                Accent, Offset(pivot.x - 9f * k, pivot.y + 128f * k),
                Size(18f * k, 32f * k), CornerRadius(5f * k)
            )
        }
        drawCircle(Color(0xFF55555C), 6f * k, pivot)
    }
}

@Composable
fun SleeveStage(
    track: Track, file: File, version: Int,
    isPlaying: Boolean, waiting: Boolean, reversing: Boolean,
    position: Long, duration: Long, albumDurs: List<Long>, trackIdx: Int,
    labelColor: Color, manual: Boolean,
    onPulled: () -> Unit, onScratch: (Long) -> Unit,
    onNeedleLift: () -> Unit, onNeedleDrop: (Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val s = minOf(1f, maxWidth / 320.dp, (maxHeight - 380.dp) / 290.dp).coerceAtLeast(0.55f)
        val aid = track.albumId
        val lift = remember(aid) { Animatable(0f) }
        val pull = remember(aid) { Animatable(0f) }
        val crate = remember(aid) { Animatable(0f) }
        val pulled by remember(aid) { derivedStateOf { pull.value >= 1f } }
        var scratching by remember { mutableStateOf(false) }
        var scratch by remember { mutableFloatStateOf(0f) }
        var dragAngle by remember { mutableStateOf<Float?>(null) }

        LaunchedEffect(aid, manual) {
            if (!manual) {
                delay(150)
                lift.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
                launch { crate.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }
                pull.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
                delay(650)
                onPulled()
            } else {
                lift.snapTo(0f)
                pull.snapTo(0f)
                crate.snapTo(0f)
            }
        }

        val durs = albumDurs.mapIndexed { i, d -> if (i == trackIdx && duration > 0L) duration else d }
        val totalMs = durs.sum().coerceAtLeast(1L)
        val beforeMs = durs.take(trackIdx).sum()
        val progress = ((beforeMs + position).toFloat() / totalMs).coerceIn(0f, 1f)
        val groove = 10.5f + progress * 21.5f
        val gaps = run {
            var acc = 0L
            durs.dropLast(1).map {
                acc += it
                acc.toFloat() / totalMs
            }
        }
        val armDown = pulled && (isPlaying || waiting || reversing)
        val target = dragAngle ?: if (armDown) groove else 0f
        val armAngle = animateFloatAsState(
            target,
            if (dragAngle != null) snap<Float>() else tween<Float>(600, easing = FastOutSlowInEasing),
            label = "arm"
        )

        val spinning = (isPlaying || reversing) && pulled && !scratching && dragAngle == null
        val spin = rememberSpin(
            spinning, if (reversing) 2000 else 6000,
            coast = !scratching, reverse = reversing
        )
        val shape = RoundedCornerShape(6.dp)

        Box(
            Modifier.size(320.dp * s, 290.dp * s)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (!pulled) return@awaitEachGesture
                        val k = size.width / 320f
                        val px0 = 305f * k
                        val py0 = 8f * k
                        val a0 = armAngle.value * 0.017453292f
                        val vx = -137f * k * sin(a0)
                        val vy = 137f * k * cos(a0)
                        val wx = down.position.x - px0
                        val wy = down.position.y - py0
                        val tt = ((wx * vx + wy * vy) / (vx * vx + vy * vy)).coerceIn(0f, 1f)
                        val dist = hypot(wx - tt * vx, wy - tt * vy)
                        if (dist > 40.dp.toPx()) return@awaitEachGesture
                        down.consume()
                        onNeedleLift()
                        dragAngle = armAngle.value
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val ch = event.changes.firstOrNull() ?: break
                            if (!ch.pressed) break
                            val a = (atan2(px0 - ch.position.x, ch.position.y - py0) * 57.29578f)
                                .coerceIn(0f, 34f)
                            dragAngle = a
                            ch.consume()
                        }
                        val a = dragAngle ?: 0f
                        if (a >= 8f) {
                            onNeedleDrop(((a - 10.5f) / 21.5f).coerceIn(0f, 1f))
                            scope.launch {
                                delay(450)
                                dragAngle = null
                            }
                        } else {
                            dragAngle = null
                        }
                    }
                }
        ) {
            Box(
                Modifier.offset(88.dp * s, 12.dp * s).size(228.dp * s, 226.dp * s)
                    .shadow(10.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF3A2A1E), Color(0xFF1D140E))))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            )
            Box(
                Modifier.offset(95.dp * s, 25.dp * s).size(200.dp * s).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF45454C), Color(0xFF1F1F24))))
                    .border(2.dp, Color(0x44FFFFFF), CircleShape)
            )
            Box(
                Modifier.offset(296.dp * s, 214.dp * s).size(14.dp * s)
                    .clip(CircleShape).background(Accent)
            )
            Box(
                Modifier.offset(303.dp * s, 150.dp * s).size(6.dp * s, 56.dp * s)
                    .clip(RoundedCornerShape(3.dp)).background(Color(0xFF0E0A08))
            )
            Box(
                Modifier.offset(14.dp * s, 70.dp * s).size(162.dp * s, 150.dp * s)
                    .graphicsLayer { alpha = 1f - crate.value }
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xFF2B2B35))
            )
            Box(
                Modifier.offset(7.dp * s, 82.dp * s).size(176.dp * s, 150.dp * s)
                    .graphicsLayer { alpha = 1f - crate.value }
                    .clip(RoundedCornerShape(6.dp)).background(Color(0xFF353541))
            )
            Vinyl(
                labelColor,
                { spin.value + pull.value * 160f + scratch },
                184.dp * s,
                Modifier.offset(3.dp * s, 33.dp * s)
                    .graphicsLayer {
                        translationX = pull.value * (100.dp * s).toPx()
                        translationY = (1f - lift.value) * (68.dp * s).toPx()
                        val sc = 0.92f + 0.08f * lift.value
                        scaleX = sc
                        scaleY = sc
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!pulled) return@awaitEachGesture
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val pi = 3.1415927f
                            var last = atan2(down.position.y - cy, down.position.x - cx)
                            var started = false
                            var accMs = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val ch = event.changes.firstOrNull() ?: break
                                if (!ch.pressed) break
                                val dx = ch.position.x - cx
                                val dy = ch.position.y - cy
                                if (hypot(dx, dy) < 40f) continue
                                val ang = atan2(dy, dx)
                                var d = ang - last
                                if (d > pi) d -= 2 * pi
                                if (d < -pi) d += 2 * pi
                                last = ang
                                if (d == 0f) continue
                                if (!started) {
                                    started = true
                                    scratching = true
                                }
                                scratch += d * 180f / pi
                                accMs += d * (10000f / (2 * pi))
                                if (abs(accMs) >= 150f) {
                                    onScratch(accMs.toLong())
                                    accMs = 0f
                                }
                                ch.consume()
                            }
                            if (accMs != 0f) onScratch(accMs.toLong())
                            if (started) {
                                scope.launch {
                                    spin.snapTo(spin.value + scratch)
                                    scratch = 0f
                                    scratching = false
                                }
                            }
                        }
                    },
                labelText = "SIDE A",
                gaps = gaps
            )
            // Конверт: в ручном режиме — жест «вытянуть вверх»
            Box(
                Modifier.offset(0.dp, 30.dp * s).size(190.dp * s)
                    .graphicsLayer {
                        translationY = (1f - lift.value) * (68.dp * s).toPx()
                        val sc = 0.92f + 0.08f * lift.value
                        scaleX = sc
                        scaleY = sc
                    }
                    .then(
                        if (manual && !pulled) Modifier.pointerInput(aid) {
                            detectVerticalDragGestures { _, dy ->
                                val next = (pull.value - dy / (200f * s.toPx() / density))
                                    .coerceIn(0f, 1f)
                                scope.launch {
                                    pull.snapTo(next)
                                    lift.snapTo(next)
                                    if (next >= 0.85f) crate.snapTo(1f)
                                }
                                if (next >= 1f) onPulled()
                            }
                        } else Modifier
                    )
                    .shadow(14.dp, shape)
                    .clip(shape)
                    .border(1.dp, Color(0x33FFFFFF), shape)
            ) {
                CoverImage(file, version, Modifier.fillMaxSize())
                Box(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(16.dp * s)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
            }
            Box(
                Modifier.offset((-16).dp * s, 214.dp * s).size(222.dp * s, 76.dp * s)
                    .graphicsLayer {
                        translationY = crate.value * (90.dp * s).toPx()
                        alpha = 1f - crate.value
                    }
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF7A5430), Color(0xFF4A3119))))
                    .drawBehind {
                        val gap = this.size.height / 4f
                        for (i in 1..3) {
                            drawLine(
                                Color(0x33000000), Offset(0f, i * gap),
                                Offset(this.size.width, i * gap), 2f
                            )
                        }
                        drawLine(
                            Color(0x44FFFFFF), Offset(0f, 1f),
                            Offset(this.size.width, 1f), 2f
                        )
                    }
            )
            Tonearm(armAngle)
        }
    }
}

@Composable
fun EqBars(active: Boolean, color: Color) {
    val t = rememberInfiniteTransition(label = "eq")
    val hs = (0..2).map { i ->
        t.animateFloat(
            0.3f, 1f,
            infiniteRepeatable(tween(500 + i * 170, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar$i"
        )
    }
    Row(
        Modifier.height(16.dp).width(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hs.forEach { h ->
            Box(
                Modifier.width(3.dp).fillMaxHeight(if (active) h.value else 0.3f)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun PlayerIcon(kind: String, color: Color, iconSize: Dp) {
    Canvas(Modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        when (kind) {
            "play" -> drawPath(Path().apply {
                moveTo(w * 0.22f, h * 0.12f); lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.22f, h * 0.88f); close()
            }, color)
            "pause" -> {
                drawRoundRect(color, Offset(w * 0.2f, h * 0.14f), Size(w * 0.2f, h * 0.72f), CornerRadius(w * 0.05f))
                drawRoundRect(color, Offset(w * 0.6f, h * 0.14f), Size(w * 0.2f, h * 0.72f), CornerRadius(w * 0.05f))
            }
            "next" -> {
                drawPath(Path().apply {
                    moveTo(w * 0.15f, h * 0.15f); lineTo(w * 0.72f, h * 0.5f)
                    lineTo(w * 0.15f, h * 0.85f); close()
                }, color)
                drawRoundRect(color, Offset(w * 0.76f, h * 0.15f), Size(w * 0.1f, h * 0.7f), CornerRadius(w * 0.03f))
            }
            "prev" -> {
                drawPath(Path().apply {
                    moveTo(w * 0.85f, h * 0.15f); lineTo(w * 0.28f, h * 0.5f)
                    lineTo(w * 0.85f, h * 0.85f); close()
                }, color)
                drawRoundRect(color, Offset(w * 0.14f, h * 0.15f), Size(w * 0.1f, h * 0.7f), CornerRadius(w * 0.03f))
            }
            "rew" -> {
                drawPath(Path().apply {
                    moveTo(w * 0.50f, h * 0.2f); lineTo(w * 0.08f, h * 0.5f)
                    lineTo(w * 0.50f, h * 0.8f); close()
                }, color)
                drawPath(Path().apply {
                    moveTo(w * 0.92f, h * 0.2f); lineTo(w * 0.50f, h * 0.5f)
                    lineTo(w * 0.92f, h * 0.8f); close()
                }, color)
            }
            "down" -> {
                drawLine(color, Offset(w * 0.2f, h * 0.35f), Offset(w * 0.5f, h * 0.65f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.8f, h * 0.35f), Offset(w * 0.5f, h * 0.65f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun RoundButton(onClick: () -> Unit, d: Dp, bg: Color, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 400f), label = "press"
    )
    Box(
        Modifier.size(d).graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape).background(bg)
            .clickable(interactionSource = src, indication = null, onClick = {
                Sfx.click(PlayerHolder.player(LocalContext.current).applicationContext)
                onClick()
            }),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun MiniPlayer(
    track: Track, file: File, version: Int, isPlaying: Boolean, labelColor: Color,
    onOpen: () -> Unit, onToggle: () -> Unit
) {
    val spin = rememberSpin(isPlaying, 4000)
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().shadow(10.dp, shape).clip(shape)
            .background(Brush.horizontalGradient(listOf(WoodLight, WoodDark)))
            .clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(68.dp, 46.dp)) {
            Vinyl(labelColor, { spin.value }, 42.dp, Modifier.offset(24.dp, 2.dp))
            CoverImage(
                file, version,
                Modifier.size(46.dp).shadow(4.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title, color = Cream, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist, color = Cream.copy(alpha = 0.7f), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        RoundButton(onToggle, 42.dp, Color(0xFF241810)) {
            Crossfade(isPlaying, label = "mini") { p ->
                PlayerIcon(if (p) "pause" else "play", Accent, 20.dp)
            }
        }
    }
}

@Composable
fun NowPlaying(
    track: Track, file: File, version: Int,
    isPlaying: Boolean, waiting: Boolean, reversing: Boolean,
    position: Long, duration: Long, albumDurs: List<Long>, trackIdx: Int,
    tint: Color, labelColor: Color, manualMode: Boolean, isFavorite: Boolean,
    onClose: () -> Unit, onFindCover: () -> Unit, onEq: () -> Unit,
    onToggleFav: () -> Unit, onToggleManual: () -> Unit,
    onPlay: () -> Unit, onPause: () -> Unit, onReverse: () -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit,
    onSeek: (Long) -> Unit, onPulled: () -> Unit, onScratch: (Long) -> Unit,
    onNeedleLift: () -> Unit, onNeedleDrop: (Float) -> Unit,
    onPickVinylColor: (Color) -> Unit
) {
    val ctx = LocalContext.current
    var dragValue by remember { mutableStateOf<Float?>(null) }
    var colorMenu by remember { mutableStateOf(false) }
    val progress = (dragValue ?: if (duration > 0) position.toFloat() / duration else 0f).coerceIn(0f, 1f)
    val dim = Color(0x33FFFFFF)
    val key = Color(0xFF241810)
    val paused = !isPlaying && !reversing
    val counterShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Column(
        Modifier.fillMaxSize().storeWall(tint).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(onClose, 40.dp, dim) { PlayerIcon("down", Color.White, 18.dp) }
            Text(
                "Кабинка прослушивания", Modifier.weight(1f), color = Muted,
                fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(dim)
                    .clickable(onClick = onToggleFav).padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text(if (isFavorite) "♥" else "♡", color = if (isFavorite) Accent else Color.White, fontSize = 16.sp) }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(dim)
                    .clickable { manualModeToggle@ onToggleManual() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text(if (manualMode) "Авто" else "Ручной", color = Color.White, fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            Box {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(dim)
                        .clickable { colorMenu = true }.padding(horizontal = 10.dp, vertical = 8.dp)
                ) { Text("Винил", color = Color.White, fontSize = 12.sp) }
                DropdownMenu(colorMenu, { colorMenu = false }) {
                    MetaStore.VinylPalette.forEach { c ->
                        DropdownMenuItem(
                            text = { Text("■", color = c, fontSize = 22.sp) },
                            onClick = { onPickVinylColor(c); colorMenu = false }
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(dim)
                    .clickable(onClick = onFindCover).padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text("Обложка", color = Color.White, fontSize = 12.sp) }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(dim)
                    .clickable(onClick = onEq).padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text("EQ", color = Color.White, fontSize = 12.sp) }
        }

        Spacer(Modifier.weight(1f))

        SleeveStage(
            track, file, version, isPlaying, waiting, reversing, position, duration,
            albumDurs, trackIdx,
            labelColor, manualMode,
            onPulled, onScratch, onNeedleLift, onNeedleDrop
        )

        Spacer(Modifier.height(4.dp))
        Text(
            if (manualMode)
                "Ручной режим: потяните конверт вверх, затем поставьте иглу на дорожку"
            else
                "Крутите пластинку — перемотка. Иглу можно ставить на любую дорожку: промежутки на пластинке — границы треков",
            color = Color(0x88B0B0BA), fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        AnimatedContent(targetState = track, label = "title") { t ->
            PriceTag(
                t.title,
                "${t.artist} · ${t.album}" +
                    (if (albumDurs.size > 1) "  ·  ${trackIdx + 1}/${albumDurs.size}" else ""),
                20.sp, Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Column(
            Modifier.fillMaxWidth().shadow(16.dp, counterShape).clip(counterShape)
                .background(Brush.verticalGradient(listOf(WoodLight, WoodDark)))
                .drawBehind {
                    val plank = 64.dp.toPx()
                    var x = plank
                    while (x < this.size.width) {
                        drawLine(Color(0x33000000), Offset(x, 0f), Offset(x, this.size.height), 2f)
                        x += plank
                    }
                    drawLine(Color(0x66FFFFFF), Offset(0f, 2f), Offset(this.size.width, 2f), 3f)
                }
                .navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Slider(
                value = progress,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    dragValue?.let { onSeek((it * duration).toLong()) }
                    dragValue = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Cream, activeTrackColor = Accent,
                    inactiveTrackColor = Color(0x66000000)
                )
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(fmt((progress * duration).toLong()), color = Cream, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(fmt(duration), color = Cream, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                DeckKey("НАЗАД", 48.dp, key, onPrev) {
                    PlayerIcon("prev", Cream, 22.dp)
                }
                DeckKey("РЕВЕРС", 48.dp, if (reversing) Accent else key, onReverse) {
                    PlayerIcon("rew", if (reversing) Color.Black else Cream, 24.dp)
                }
                DeckKey("ПАУЗА", 56.dp, if (paused) Color(0xFF8A6A2E) else key, onPause) {
                    PlayerIcon("pause", Cream, 24.dp)
                }
                DeckKey("ИГРАТЬ", 68.dp, if (isPlaying) Accent else Color(0xFFB88A3A), onPlay) {
                    PlayerIcon("play", Color.Black, 28.dp)
                }
                DeckKey("ВПЕРЁД", 48.dp, key, onNext) {
                    PlayerIcon("next", Cream, 22.dp)
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    tracks: List<Track>, version: Int,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().storeWall(Color(0xFF2A1B12)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onBack, 40.dp, Color(0x22FFFFFF)) { PlayerIcon("down", Color.White, 18.dp) }
            Text("Избранное", Modifier.weight(1f).padding(start = 12.dp),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (tracks.isEmpty()) {
            Text("Пока пусто. Добавьте треки ♥", color = Muted,
                modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)) {
                items(tracks, key = { it.uri.toString() }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { onPlay(t) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverImage(resolveCover(ctx, t.albumId), version,
                            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, color = Accent, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, color = Muted, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(Modifier.clickable { onRemove(t) }.padding(12.dp)) {
                            Text("♥", color = Accent, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

fun freqLabel(hz: Int): String =
    if (hz >= 1000) {
        val k = hz / 1000f
        if (k == k.toInt().toFloat()) "${k.toInt()} кГц" else "%.1f кГц".format(k)
    } else "$hz Гц"

@Composable
fun EqualizerPanel(fx: FxState, onClose: () -> Unit) {
    val colors = SliderDefaults.colors(
        thumbColor = Accent, activeTrackColor = Accent,
        inactiveTrackColor = Color(0x33FFFFFF),
        disabledThumbColor = Color(0x55FFFFFF),
        disabledActiveTrackColor = Color(0x44FFFFFF),
        disabledInactiveTrackColor = Color(0x22FFFFFF)
    )
    Column(
        Modifier.fillMaxSize().storeWall(Color(0xFF3B2A1A))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onClose, 40.dp, Color(0x22FFFFFF)) { PlayerIcon("down", Color.White, 18.dp) }
            Text(
                "Эквалайзер", Modifier.weight(1f).padding(start = 16.dp),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            Switch(
                checked = fx.enabled, onCheckedChange = { fx.setOn(it) },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Accent, checkedThumbColor = Color.Black
                )
            )
        }
        Spacer(Modifier.height(16.dp))

        if (!fx.available || fx.levels.isEmpty()) {
            Text(
                "Эквалайзер недоступен на этом устройстве или ещё не готов. Запустите любой трек и откройте экран снова.",
                color = Muted
            )
        } else {
            if (fx.presets.isNotEmpty()) {
                Text("Пресеты", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fx.presets.forEachIndexed { i, name ->
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                                .clickable { fx.usePreset(i) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text(name, color = Color.White, fontSize = 13.sp) }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            fx.levels.forEachIndexed { i, lv ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        freqLabel(fx.freqs.getOrElse(i) { 0 }), Modifier.width(64.dp),
                        color = Muted, fontSize = 12.sp
                    )
                    Slider(
                        value = lv.toFloat(),
                        onValueChange = { fx.setBand(i, it.toInt()) },
                        valueRange = fx.minLevel.toFloat()..fx.maxLevel.toFloat(),
                        enabled = fx.enabled,
                        colors = colors,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%+.1f дБ".format(lv / 100f), Modifier.width(64.dp),
                        color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Усиление баса", color = Muted, fontSize = 13.sp)
            Slider(
                value = fx.bass.toFloat(),
                onValueChange = { fx.updateBass(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = fx.enabled,
                colors = colors
            )

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                    .clickable { fx.reset() }.padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Text("Сбросить", color = Color.White, fontSize = 13.sp) }

            if (!fx.enabled) {
                Spacer(Modifier.height(16.dp))
                Text("Включите эквалайзер переключателем сверху.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun Crate(
    albums: List<Album>, currentAlbumId: String?, isPlaying: Boolean, version: Int,
    onPlay: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val n = albums.size
    val pos = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(true) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    var lastDy by remember { mutableFloatStateOf(0f) }
    val dragPx = with(LocalDensity.current) { 120.dp.toPx() }
    val snapSpec = spring<Float>(dampingRatio = 0.72f, stiffness = 220f)

    LaunchedEffect(n) { pos.updateBounds(0f, (n - 1).coerceAtLeast(0).toFloat()) }
    LaunchedEffect(currentAlbumId, n) {
        val idx = albums.indexOfFirst { it.id == currentAlbumId }
        if (idx >= 0) {
            if (first) pos.snapTo(idx.toFloat())
            else if (!dragging && abs(pos.value - idx) > 0.01f) pos.animateTo(idx.toFloat(), snapSpec)
        }
        first = false
    }

    BoxWithConstraints(modifier) {
        val side = minOf(minOf(maxWidth - 72.dp, 270.dp), maxHeight - 312.dp).coerceAtLeast(150.dp)
        val last = (n - 1).coerceAtLeast(0)
        val base by remember(n) { derivedStateOf { floor(pos.value).toInt().coerceIn(0, last) } }
        val center by remember(n) { derivedStateOf { pos.value.roundToInt().coerceIn(0, last) } }

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.fillMaxWidth().height(side + 156.dp)
                    .pointerInput(n) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                dragPos = pos.value
                                lastDy = 0f
                            },
                            onDragEnd = {
                                val extra = if (abs(lastDy) > 14f) -sign(lastDy) * 0.6f else 0f
                                val target = (dragPos + extra).roundToInt().coerceIn(0, last)
                                dragging = false
                                scope.launch { pos.animateTo(target.toFloat(), snapSpec) }
                            },
                            onDragCancel = {
                                dragging = false
                                scope.launch { pos.animateTo(pos.value.roundToInt().toFloat(), snapSpec) }
                            },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                lastDy = dy
                                dragPos = (dragPos - dy / dragPx).coerceIn(0f, last.toFloat())
                                scope.launch { pos.snapTo(dragPos) }
                            }
                        )
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier.align(Alignment.BottomCenter).offset(x = -(side / 2 + 15.dp))
                        .width(14.dp).height(side * 0.7f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.horizontalGradient(listOf(WoodLight, WoodDark)))
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).offset(x = side / 2 + 15.dp)
                        .width(14.dp).height(side * 0.7f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(Brush.horizontalGradient(listOf(WoodDark, WoodLight)))
                )
                val hi = minOf(base + 5, last)
                for (i in hi downTo base) {
                    val a = albums.getOrNull(i) ?: continue
                    key(i) {
                        CrateSleeve(
                            label = "${a.title} · ${a.artist}",
                            file = resolveCover(ctx, a.id), version = version,
                            isCur = a.id == currentAlbumId, isPlaying = isPlaying,
                            side = side, rel = { i - pos.value }
                        ) {
                            if (i == center) onPlay(i)
                            else scope.launch { pos.animateTo(i.toFloat(), snapSpec) }
                        }
                    }
                }
                CrateFront(Modifier.align(Alignment.BottomCenter), side + 44.dp, side * 0.3f)
            }

            Spacer(Modifier.height(14.dp))
            AnimatedContent(targetState = center, label = "crateInfo") { idx ->
                val a = albums.getOrNull(idx)
                PriceTag(
                    a?.title ?: "",
                    if (a != null) "${a.artist} · треков: ${a.tracks.size}" else "",
                    18.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Перебирайте пластинки в ящике, нажмите на любую, чтобы послушать альбом",
                color = Color(0x88B0B0BA), fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
fun CrateSleeve(
    label: String, file: File, version: Int, isCur: Boolean, isPlaying: Boolean,
    side: Dp, rel: () -> Float, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(side)
            .graphicsLayer {
                val r = rel()
                cameraDistance = 16f * density
                if (r >= 0f) {
                    translationY = -r * 34.dp.toPx()
                    val sc = (1f - 0.045f * r).coerceAtLeast(0.6f)
                    scaleX = sc
                    scaleY = sc
                    alpha = (1f - (r - 3.6f).coerceAtLeast(0f) * 1.5f).coerceIn(0f, 1f)
                } else {
                    val f = -r
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    rotationX = -62f * f.coerceAtMost(1f)
                    translationY = f * side.toPx() * 0.5f
                    val sc = 1f + 0.05f * f.coerceAtMost(1f)
                    scaleX = sc
                    scaleY = sc
                    alpha = (1f - f).coerceIn(0f, 1f)
                }
            }
            .shadow(10.dp, shape)
            .clip(shape)
            .then(if (isCur) Modifier.border(2.dp, Accent, shape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        CoverImage(file, version, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().drawBehind {
                val r = rel().coerceAtLeast(0f)
                drawRect(Color.Black.copy(alpha = (r * 0.14f).coerceAtMost(0.6f)))
            }
        )
        Box(
            Modifier.fillMaxWidth().height(30.dp)
                .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
        )
        Text(
            label,
            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(if (isCur) 0.8f else 1f)
        )
        if (isCur) {
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { EqBars(isPlaying, Accent) }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverPicker(track: Track, onBack: () -> Unit, onImageChosen: (String) -> Unit) {
    // Обложки — только с musichoarders (как в исходной версии)
    val url = remember(track) {
        Uri.Builder().scheme("https").authority("covers.musichoarders.xyz")
            .appendQueryParameter("artist", track.artist)
            .appendQueryParameter("album", track.album)
            .appendQueryParameter("sources", "discogs,musicbrainz,applemusic,itunes,deezer,bandcamp")
            .build().toString()
    }
    Column(Modifier.fillMaxSize().background(BaseDark).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "← Назад", color = Accent, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
            )
            Text(
                "Долгое нажатие на обложку — выбрать",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp)
            )
        }
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { c ->
            WebView(c).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                setOnLongClickListener {
                    val r = hitTestResult
                    val isImg = r.type == WebView.HitTestResult.IMAGE_TYPE ||
                        r.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    val extra = r.extra
                    if (isImg && extra != null) { onImageChosen(extra); true } else false
                }
                loadUrl(url)
            }
        })
    }
}
