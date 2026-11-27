package com.example.vinylplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

val Accent = Color(0xFFE8B04B)
val BaseDark = Color(0xFF0B0B0F)
val Muted = Color(0xFFB0B0BA)

data class Track(val uri: Uri, val title: String, val artist: String, val album: String)

fun coverFile(ctx: Context, t: Track) =
    File(ctx.filesDir, "cover_${(t.artist + "|" + t.album).hashCode()}.jpg")

fun mediaItem(t: Track): MediaItem =
    MediaItem.Builder().setUri(t.uri).setMediaId(t.uri.toString()).build()

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

fun readTrack(ctx: Context, uri: Uri): Track {
    val unknownArtist = "Неизвестный исполнитель"
    val unknownAlbum = "Неизвестный альбом"
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(ctx, uri)
        val title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?: fileName(ctx, uri)
        val artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            ?: unknownArtist
        val album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: unknownAlbum
        val t = Track(uri, title, artist, album)
        val f = coverFile(ctx, t)
        if (!f.exists()) r.embeddedPicture?.let { f.writeBytes(it) }
        t
    } catch (e: Exception) {
        Track(uri, fileName(ctx, uri), unknownArtist, unknownAlbum)
    } finally {
        runCatching { r.release() }
    }
}

fun saveLibrary(ctx: Context, list: List<Track>) {
    val arr = JSONArray()
    list.forEach {
        arr.put(
            JSONObject().put("u", it.uri.toString()).put("t", it.title)
                .put("a", it.artist).put("b", it.album)
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
            Track(Uri.parse(o.getString("u")), o.getString("t"), o.getString("a"), o.getString("b"))
        }
    }.getOrDefault(emptyList())
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) { PlayerApp() }
        }
    }
}

@Composable
fun PlayerApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply { repeatMode = Player.REPEAT_MODE_ALL }
    }
    val tracks = remember { mutableStateListOf<Track>() }
    var currentId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var pendingPlay by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showNow by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Track?>(null) }
    var coverVersion by remember { mutableIntStateOf(0) }
    var tint by remember { mutableStateOf(Color(0xFF2A2140)) }
    val listState = rememberLazyListState()
    val current = tracks.firstOrNull { it.uri.toString() == currentId }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                currentId = item?.mediaId
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }

    LaunchedEffect(Unit) {
        val saved = loadLibrary(ctx)
        tracks.addAll(saved)
        player.setMediaItems(saved.map { mediaItem(it) })
        player.prepare()
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
    val bg by animateColorAsState(tint, tween(900), label = "bg")
    val brush = Brush.verticalGradient(listOf(bg, BaseDark))
    val labelColor = lerp(bg, Color.White, 0.35f)

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val existing = tracks.map { it.uri }.toSet()
            scope.launch {
                val added = withContext(Dispatchers.IO) {
                    uris.filter { it !in existing }.map { u ->
                        runCatching {
                            ctx.contentResolver.takePersistableUriPermission(
                                u, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        readTrack(ctx, u)
                    }
                }
                if (added.isNotEmpty()) {
                    tracks.addAll(added)
                    player.addMediaItems(added.map { mediaItem(it) })
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    saveLibrary(ctx, tracks)
                    coverVersion++
                }
            }
        }
    }

    // Музыка стартует, когда пластинка достана и игла опущена
    fun releasePending() {
        if (pendingPlay) {
            pendingPlay = false
            player.play()
        }
    }

    fun playIndex(i: Int) {
        pendingPlay = true
        player.pause()
        player.seekTo(i, 0L)
        currentId = tracks[i].uri.toString()
        showNow = true
    }

    fun skip(next: Boolean) {
        if (tracks.size > 1) {
            pendingPlay = true
            player.pause()
            if (next) player.seekToNextMediaItem() else player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
    }

    fun removeTrack(t: Track) {
        val i = tracks.indexOf(t)
        if (i >= 0) {
            tracks.removeAt(i)
            player.removeMediaItem(i)
            saveLibrary(ctx, tracks)
        }
    }

    fun toggle() {
        pendingPlay = false
        if (player.isPlaying) player.pause() else player.play()
    }

    fun closeNow() {
        showNow = false
        releasePending()
    }

    BackHandler(enabled = showNow && picker == null) { closeNow() }

    val pickerTrack = picker
    if (pickerTrack != null) {
        BackHandler { picker = null }
        CoverPicker(pickerTrack, onBack = { picker = null }) { imageUrl ->
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        URL(imageUrl).openStream().use { inp ->
                            coverFile(ctx, pickerTrack).outputStream().use { inp.copyTo(it) }
                        }
                    }.isSuccess
                }
                if (!ok) Toast.makeText(ctx, "Не удалось скачать картинку", Toast.LENGTH_SHORT).show()
                coverVersion++
                picker = null
            }
        }
    } else {
        Box(Modifier.fillMaxSize().background(brush)) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Коллекция", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Треков: ${tracks.size}", color = Muted, fontSize = 13.sp)
                    }
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Accent)
                            .clickable { importLauncher.launch(arrayOf("audio/*")) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text("+ Добавить", color = Color.Black, fontWeight = FontWeight.SemiBold) }
                }

                if (tracks.isEmpty()) {
                    EmptyState { importLauncher.launch(arrayOf("audio/*")) }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(tracks, key = { it.uri.toString() }) { t ->
                            val isCur = t.uri.toString() == currentId
                            val rowBg by animateColorAsState(
                                if (isCur) Color(0x26FFFFFF) else Color.Transparent,
                                label = "row"
                            )
                            Row(
                                Modifier.fillMaxWidth().animateItem()
                                    .clip(RoundedCornerShape(16.dp)).background(rowBg)
                                    .clickable { playIndex(tracks.indexOf(t)) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverImage(
                                    coverFile(ctx, t), coverVersion,
                                    Modifier.size(54.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        t.title, color = if (isCur) Accent else Color.White,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${t.artist} · ${t.album}", color = Muted, fontSize = 12.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isCur) Equalizer(isPlaying, Accent)
                                Box(Modifier.clickable { removeTrack(t) }.padding(12.dp)) {
                                    Text("✕", color = Color(0x88FFFFFF))
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
                        isPlaying = isPlaying, waiting = pendingPlay,
                        position = position, duration = duration,
                        background = brush, labelColor = labelColor,
                        onClose = { closeNow() },
                        onFindCover = { releasePending(); picker = cur },
                        onPlayPause = { toggle() },
                        onNext = { skip(true) },
                        onPrev = { skip(false) },
                        onSeek = { ms -> player.seekTo(ms) },
                        onPulled = { releasePending() }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(onAdd: () -> Unit) {
    val spin = rememberSpin(true, 14000)
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Vinyl(Color(0xFF6B4FA0), { spin.value }, 200.dp, labelText = "SIDE A")
        Spacer(Modifier.height(28.dp))
        Text("Здесь пока пусто", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Добавьте аудиофайлы с телефона. Обложки подтянутся из тегов, а при желании можно найти их на сайте.",
            color = Muted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Accent).clickable(onClick = onAdd)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) { Text("Добавить музыку", color = Color.Black, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun rememberSpin(playing: Boolean, periodMs: Int): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                val s = a.value % 360f
                a.snapTo(s)
                a.animateTo(s + 360f, tween(periodMs, easing = LinearEasing))
            }
        } else if (a.value != 0f) {
            a.animateTo(a.value + 40f, tween(900, easing = FastOutSlowInEasing))
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

// Пластинка: чёрный винил с канавками и цветной этикеткой
@Composable
fun Vinyl(
    labelColor: Color,
    angle: () -> Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
    labelText: String? = null
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
fun Tonearm(down: Boolean) {
    val angle by animateFloatAsState(
        if (down) 15f else 0f, tween(900, easing = FastOutSlowInEasing), label = "arm"
    )
    Canvas(Modifier.fillMaxSize()) {
        val k = this.size.width / 320f
        val pivot = Offset(305f * k, 8f * k)
        drawCircle(Color(0xFF3A3A40), 14f * k, pivot)
        rotate(angle, pivot) {
            drawLine(
                Color(0xFFCFCFD6), pivot, Offset(pivot.x, pivot.y + 137f * k),
                strokeWidth = 5f * k, cap = StrokeCap.Round
            )
            drawRoundRect(
                Accent, Offset(pivot.x - 8f * k, pivot.y + 130f * k),
                Size(16f * k, 28f * k), CornerRadius(4f * k)
            )
        }
        drawCircle(Color(0xFF55555C), 6f * k, pivot)
    }
}

// Конверт с обложкой; пластинка выезжает из него, потом опускается игла
@Composable
fun SleeveStage(
    track: Track, file: File, version: Int,
    isPlaying: Boolean, waiting: Boolean,
    labelColor: Color, onPulled: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val s = minOf(1f, maxWidth / 320.dp)
        val intro = remember(track) { Animatable(0f) }
        val pull = remember(track) { Animatable(0f) }
        val pulled by remember(track) { derivedStateOf { pull.value >= 1f } }

        LaunchedEffect(track) {
            delay(150)
            intro.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            pull.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
            delay(650)
            onPulled()
        }

        val spin = rememberSpin(isPlaying && pulled, 6000)

        Box(Modifier.size(320.dp * s, 250.dp * s)) {
            // пластинка (под конвертом)
            Vinyl(
                labelColor,
                { spin.value + pull.value * 160f },
                184.dp * s,
                Modifier.offset(3.dp * s, 33.dp * s).graphicsLayer {
                    translationX = pull.value * (100.dp * s).toPx()
                },
                labelText = "SIDE A"
            )
            // конверт с обложкой
            Box(
                Modifier.offset(0.dp, 30.dp * s).size(190.dp * s)
                    .graphicsLayer {
                        val v = intro.value.coerceIn(0f, 1f)
                        scaleX = 0.85f + 0.15f * v
                        scaleY = 0.85f + 0.15f * v
                        alpha = v
                    }
                    .shadow(14.dp, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
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
            Tonearm(pulled && (isPlaying || waiting))
        }
    }
}

@Composable
fun Equalizer(active: Boolean, color: Color) {
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
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun MiniPlayer(
    track: Track, file: File, version: Int, isPlaying: Boolean, labelColor: Color,
    onOpen: () -> Unit, onToggle: () -> Unit
) {
    val spin = rememberSpin(isPlaying, 4000)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Color(0xE61B1B22)).clickable(onClick = onOpen).padding(10.dp),
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
                track.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RoundButton(onToggle, 42.dp, Accent) {
            Crossfade(isPlaying, label = "mini") { p ->
                PlayerIcon(if (p) "pause" else "play", Color.Black, 20.dp)
            }
        }
    }
}

@Composable
fun NowPlaying(
    track: Track, file: File, version: Int,
    isPlaying: Boolean, waiting: Boolean,
    position: Long, duration: Long,
    background: Brush, labelColor: Color,
    onClose: () -> Unit, onFindCover: () -> Unit,
    onPlayPause: () -> Unit, onNext: () -> Unit, onPrev: () -> Unit,
    onSeek: (Long) -> Unit, onPulled: () -> Unit
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val progress = (dragValue ?: if (duration > 0) position.toFloat() / duration else 0f).coerceIn(0f, 1f)

    Column(
        Modifier.fillMaxSize().background(background)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onClose, 40.dp, Color(0x22FFFFFF)) { PlayerIcon("down", Color.White, 18.dp) }
            Text(
                "Сейчас играет", Modifier.weight(1f), color = Muted,
                fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0x22FFFFFF))
                    .clickable(onClick = onFindCover).padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("Обложка", color = Color.White, fontSize = 13.sp) }
        }

        Spacer(Modifier.weight(1f))

        SleeveStage(track, file, version, isPlaying, waiting, labelColor, onPulled)

        Spacer(Modifier.height(28.dp))

        AnimatedContent(targetState = track, label = "title") { t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    t.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${t.artist} · ${t.album}", color = Muted, fontSize = 14.sp,
                    maxLines = 1, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Slider(
            value = progress,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeek((it * duration).toLong()) }
                dragValue = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Accent, activeTrackColor = Accent,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(fmt((progress * duration).toLong()), color = Muted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(fmt(duration), color = Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(onPrev, 56.dp, Color(0x22FFFFFF)) { PlayerIcon("prev", Color.White, 24.dp) }
            RoundButton(onPlayPause, 76.dp, Accent) {
                Crossfade(isPlaying, label = "pp") { p ->
                    PlayerIcon(if (p) "pause" else "play", Color.Black, 32.dp)
                }
            }
            RoundButton(onNext, 56.dp, Color(0x22FFFFFF)) { PlayerIcon("next", Color.White, 24.dp) }
        }

        Spacer(Modifier.weight(1f))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverPicker(track: Track, onBack: () -> Unit, onImageChosen: (String) -> Unit) {
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
