package com.example.vinylplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class Track(val uri: Uri, val title: String, val artist: String, val album: String)

fun coverFile(ctx: Context, t: Track) =
    File(ctx.filesDir, "cover_${(t.artist + "|" + t.album).hashCode()}.jpg")

fun loadTracks(ctx: Context): List<Track> {
    val list = mutableListOf<Track>()
    val proj = arrayOf(
        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM
    )
    ctx.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
        "${MediaStore.Audio.Media.ARTIST}, ${MediaStore.Audio.Media.ALBUM}"
    )?.use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            list += Track(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendPath(id.toString()).build(),
                c.getString(1) ?: "—", c.getString(2) ?: "—", c.getString(3) ?: "—"
            )
        }
    }
    return list
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface { PlayerApp() } } }
    }
}

@Composable
fun PlayerApp() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var current by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Track?>(null) }
    var coverVersion by remember { mutableIntStateOf(0) }

    val perm = if (Build.VERSION.SDK_INT >= 33)
        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> if (ok) tracks = loadTracks(ctx) }
    LaunchedEffect(Unit) { launcher.launch(perm) }

    val pickerTrack = picker
    if (pickerTrack != null) {
        BackHandler { picker = null }
        CoverPicker(pickerTrack) { imageUrl ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching {
                        URL(imageUrl).openStream().use { inp ->
                            coverFile(ctx, pickerTrack).outputStream().use { inp.copyTo(it) }
                        }
                    }
                }
                coverVersion++
                picker = null
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        current?.let { t ->
            val f = coverFile(ctx, t)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                key(coverVersion) {
                    AsyncImage(
                        model = if (f.exists()) f else null,
                        contentDescription = null,
                        modifier = Modifier.size(240.dp)
                    )
                }
                Text(t.title, style = MaterialTheme.typography.titleMedium)
                Text("${t.artist} — ${t.album}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                    }) { Text(if (isPlaying) "Пауза" else "Играть") }
                    OutlinedButton(onClick = { picker = t }) { Text("Найти обложку") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        LazyColumn {
            items(tracks) { t ->
                Text(
                    "${t.artist} — ${t.title}",
                    Modifier.fillMaxWidth().clickable {
                        current = t
                        player.setMediaItem(MediaItem.fromUri(t.uri))
                        player.prepare(); player.play(); isPlaying = true
                    }.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverPicker(track: Track, onImageChosen: (String) -> Unit) {
    val url = remember(track) {
        Uri.Builder().scheme("https").authority("covers.musichoarders.xyz")
            .appendQueryParameter("artist", track.artist)
            .appendQueryParameter("album", track.album)
            .appendQueryParameter("sources", "discogs,musicbrainz,applemusic,itunes,deezer,bandcamp")
            .build().toString()
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Долгое нажатие на обложку — выбрать её",
            Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall
        )
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
