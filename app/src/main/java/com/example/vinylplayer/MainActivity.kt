package com.example.vinylplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import java.io.File
import java.net.URL

private val Ink = Color(0xFF09090D)
private val Panel = Color(0xFF17151D)
private val Panel2 = Color(0xFF211B25)
private val Gold = Color(0xFFE6BC76)
private val Muted = Color(0xFFAAA3B1)
private val Wine = Color(0xFF4B182B)

data class Track(val uri: Uri, val title: String, val artist: String, val album: String)

fun coverFile(ctx: Context, t: Track) = File(ctx.filesDir, "cover_${(t.artist + "|" + t.album).hashCode()}.jpg")

private fun metadataTrack(ctx: Context, uri: Uri): Track {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(ctx, uri)
        Track(uri,
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Без названия",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "Неизвестный исполнитель",
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() } ?: "Без альбома")
    } catch (_: Exception) {
        Track(uri, uri.lastPathSegment?.substringAfterLast('/') ?: "Аудиофайл", "Неизвестный исполнитель", "Без альбома")
    } finally { runCatching { retriever.release() } }
}

fun loadTracks(ctx: Context): List<Track> {
    val list = mutableListOf<Track>()
    val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM)
    ctx.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
        "${MediaStore.Audio.Media.ARTIST}, ${MediaStore.Audio.Media.ALBUM}")?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            list += Track(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id.toString()).build(),
                c.getString(1) ?: "Без названия", c.getString(2) ?: "Неизвестный исполнитель",
                c.getString(3) ?: "Без альбома")
        }
    }
    return list
}

private fun folderAudioFiles(ctx: Context, treeUri: Uri): List<Uri> {
    val found = mutableListOf<Uri>()
    fun walk(parentId: String) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE)
        ctx.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val mime = cursor.getString(1) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) walk(id)
                else if (mime.startsWith("audio/")) found += DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
            }
        }
    }
    runCatching { walk(DocumentsContract.getTreeDocumentId(treeUri)) }
    return found
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VinylTheme { PlayerApp() } }
    }
}

@Composable
private fun VinylTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(primary = Gold, secondary = Color(0xFFC98A9E), background = Ink, surface = Panel)
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun PlayerApp() {
    val ctx = LocalContext.current
    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }
    val prefs = remember { ctx.getSharedPreferences("vinyl_library", Context.MODE_PRIVATE) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var current by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Track?>(null) }
    var coverVersion by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(1f) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) rotation.animateTo(rotation.value + 360f, tween(2600, easing = LinearEasing))
    }
    LaunchedEffect(player, current) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0 } ?: 0L
            delay(500)
        }
    }
    LaunchedEffect(player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })
    }

    fun saveImported(list: List<Track>) {
        val imported = (prefs.getStringSet("imported", emptySet()).orEmpty() + list.map { it.uri.toString() }).distinct()
        prefs.edit().putStringSet("imported", imported.toSet()).apply()
        tracks = (tracks + list).distinctBy { it.uri }
    }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val newTracks = uris.mapNotNull { uri ->
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            runCatching { metadataTrack(ctx, uri) }.getOrNull()
        }
        saveImported(newTracks)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val files = folderAudioFiles(ctx, uri)
            scopeImport(ctx, files) { saveImported(it) }
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) tracks = runCatching { loadTracks(ctx) }.getOrDefault(emptyList())
    }
    LaunchedEffect(Unit) {
        tracks = runCatching { loadTracks(ctx) }.getOrDefault(emptyList())
        val stored = prefs.getStringSet("imported", emptySet()).orEmpty()
        val restored = stored.mapNotNull { s -> runCatching { metadataTrack(ctx, Uri.parse(s)) }.getOrNull() }
        tracks = (tracks + restored).distinctBy { it.uri }
        if (Build.VERSION.SDK_INT >= 23 && ctx.checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }

    val pickerTrack = picker
    if (pickerTrack != null) {
        BackHandler { picker = null }
        CoverPicker(pickerTrack) { imageUrl ->
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                runCatching { URL(imageUrl).openStream().use { inp -> coverFile(ctx, pickerTrack).outputStream().use { inp.copyTo(it) } } }
                withContext(Dispatchers.Main) { coverVersion++; picker = null }
            }
        }
        return
    }

    val filtered = tracks.filter { "${it.title} ${it.artist} ${it.album}".contains(search, ignoreCase = true) }
    val albums = tracks.distinctBy { "${it.artist}|${it.album}" }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3B281C), Color(0xFF201810), Ink)))
            .padding(horizontal = 16.dp)
    ) {
        // Шапка музыкального магазина
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("VINYL STORE", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("Музыкальный магазин", color = Color(0xFFFFF1D5), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Пластинки для твоего настроения", color = Muted, fontSize = 12.sp)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF5A3C27)).clickable { multiPicker.launch(arrayOf("audio/*")) },
                contentAlignment = Alignment.Center
            ) { Text("＋", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Light) }
        }

        // Карточка проигрываемой пластинки
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2119))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)).background(Wine), contentAlignment = Alignment.Center) {
                    val activeTrack = current
                    if (activeTrack != null && coverFile(ctx, activeTrack).exists()) {
                        key(coverVersion) {
                            AsyncImage(model = coverFile(ctx, activeTrack), contentDescription = "Обложка",
                                modifier = Modifier.fillMaxSize())
                        }
                    } else Text("♫", color = Gold, fontSize = 30.sp)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("СЕЙЧАС ИГРАЕТ", color = Gold, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Text(current?.title ?: "Выбери пластинку", color = Color.White, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current?.artist ?: "Добавь музыку в магазин", color = Muted, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                        Text("|◀", color = Color.White, modifier = Modifier.clickable {
                            if (player.currentMediaItemIndex > 0) player.seekToPreviousMediaItem() else player.seekTo(0)
                        }.padding(end = 14.dp), fontSize = 16.sp)
                        Text(if (isPlaying) "Ⅱ" else "▶", color = Gold, modifier = Modifier.clickable {
                            if (current != null) { if (player.isPlaying) player.pause() else player.play() }
                        }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("▶|", color = Color.White, modifier = Modifier.clickable {
                            if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                            else if (tracks.isNotEmpty()) playTrack(tracks, (tracks.indexOf(current).coerceAtLeast(0) + 1) % tracks.size, player) { current = it }
                        }.padding(start = 14.dp), fontSize = 16.sp)
                    }
                }
            }
            Slider(
                value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                onValueChange = { if (duration > 0) player.seekTo((it * duration).toLong()) },
                enabled = duration > 0,
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Color(0xFF5B4A3A))
            )
        }

        // Витрина альбомов
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ВИТРИНА", color = Color(0xFFFFF1D5), fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("${albums.size} альб.", color = Muted, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(166.dp)) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(albums, key = { "${it.artist}|${it.album}" }) { albumTrack ->
                    Column(
                        Modifier.width(122.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF3B2A1D))
                            .clickable { playTrack(tracks, tracks.indexOf(albumTrack), player) { current = it } }
                            .padding(8.dp)
                    ) {
                        Box(Modifier.size(106.dp).clip(RoundedCornerShape(9.dp)).background(Wine), contentAlignment = Alignment.Center) {
                            val cover = coverFile(ctx, albumTrack)
                            if (cover.exists()) {
                                key(coverVersion) { AsyncImage(model = cover, contentDescription = "Обложка альбома", modifier = Modifier.fillMaxSize()) }
                            } else Text("♫", color = Gold, fontSize = 30.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(albumTrack.album, color = Color(0xFFFFF1D5), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(albumTrack.artist, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (albums.isEmpty()) item {
                    Box(Modifier.width(220.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Color(0xFF3B2A1D)), contentAlignment = Alignment.Center) {
                        Text("Пока пусто — добавь музыку", color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(7.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF8B5A35), Color(0xFF3A2416)))))
        }

        // Вся коллекция
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ВСЯ КОЛЛЕКЦИЯ", color = Color(0xFFFFF1D5), fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("${tracks.size} треков", color = Muted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск музыки", color = Muted) }, singleLine = true,
                shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF5B4634),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Gold
                )
            )
            Text("📁", color = Gold, fontSize = 22.sp,
                modifier = Modifier.clickable { folderPicker.launch(null) }.padding(start = 12.dp, end = 4.dp))
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filtered, key = { it.uri.toString() }) { t ->
                val active = current?.uri == t.uri
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(if (active) Color(0xFF4A3323) else Color(0xFF211A14).copy(alpha = 0.55f))
                        .clickable { playTrack(tracks, tracks.indexOf(t), player) { current = it } }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(Wine), contentAlignment = Alignment.Center) {
                        val cover = coverFile(ctx, t)
                        if (cover.exists()) key(coverVersion) { AsyncImage(model = cover, contentDescription = null, modifier = Modifier.fillMaxSize()) }
                        else Text("♫", color = Gold, fontSize = 21.sp)
                    }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text(t.title, color = if (active) Gold else Color(0xFFFFF1D5), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.artist, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (active && isPlaying) Text("♫", color = Gold, fontSize = 19.sp)
                }
            }
            if (filtered.isEmpty()) item {
                Text("Музыки пока нет. Нажми «＋» или выбери папку.", color = Muted,
                    modifier = Modifier.padding(16.dp), fontSize = 12.sp)
            }
        }
    }
}

private fun playTrack(list: List<Track>, index: Int, player: ExoPlayer, onSelected: (Track) -> Unit) {
    if (index !in list.indices) return
    val track = list[index]
    onSelected(track)
    player.setMediaItems(list.map { MediaItem.fromUri(it.uri) }, index, 0L)
    player.prepare()
    player.play()
}

private fun scopeImport(ctx: Context, uris: List<Uri>, onDone: (List<Track>) -> Unit) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        val result = uris.mapNotNull { runCatching { metadataTrack(ctx, it) }.getOrNull() }
        withContext(Dispatchers.Main) { onDone(result) }
    }
}

private fun timeLabel(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun VinylDisc(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(Color(0xFF3D3941), Color(0xFF111116), Color(0xFF050507)), center, radius), radius, center)
        for (i in 1..13) {
            val r = radius * (0.28f + i * 0.052f)
            if (r < radius) drawCircle(Color.White.copy(alpha = if (i % 3 == 0) 0.11f else 0.045f), r, center, style = Stroke(width = 1.dp.toPx()))
        }
        drawCircle(Color(0xFF29232B), radius * 0.30f, center)
        drawCircle(Gold.copy(alpha = 0.65f), radius * 0.30f, center, style = Stroke(width = 1.dp.toPx()))
        drawCircle(Color(0xFF050507), radius * 0.025f, center)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverPicker(track: Track, onImageChosen: (String) -> Unit) {
    val url = remember(track) { Uri.Builder().scheme("https").authority("covers.musichoarders.xyz")
        .appendQueryParameter("artist", track.artist).appendQueryParameter("album", track.album)
        .appendQueryParameter("sources", "discogs,musicbrainz,applemusic,itunes,deezer,bandcamp").build().toString() }
    Column(Modifier.fillMaxSize().background(Ink)) {
        Text("Долгое нажатие на обложку — выбрать её", Modifier.padding(12.dp), color = Gold, style = MaterialTheme.typography.bodySmall)
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { c -> WebView(c).apply {
            settings.javaScriptEnabled = true; settings.domStorageEnabled = true; webViewClient = WebViewClient()
            setOnLongClickListener {
                val result = hitTestResult
                val image = result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                if (image) { result.extra?.let { onImageChosen(it); true } ?: false } else false
            }
            loadUrl(url)
        } })
    }
}
