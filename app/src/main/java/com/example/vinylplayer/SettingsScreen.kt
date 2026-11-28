package com.example.vinylplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var sounds by remember { mutableStateOf(Settings.soundsEnabled) }
    var haptic by remember { mutableStateOf(Settings.hapticEnabled) }
    var onlyVinyl by remember { mutableStateOf(Settings.onlyVinylDefault) }
    var keepScreen by remember { mutableStateOf(Settings.keepScreenOn) }

    Column(
        Modifier.fillMaxSize()
            .background(BaseDark)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onBack, 40.dp, Color(0x22FFFFFF)) {
                PlayerIcon("down", Color.White, 18.dp)
            }
            Text(
                "Настройки",
                Modifier.weight(1f).padding(start = 16.dp),
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingHeader("Звук")
        SettingRow("Звуки интерфейса", "Щелчки, шуршание, треск иглы", sounds) {
            sounds = it
            Settings.soundsEnabled = it
        }
        SettingRow("Вибрация", "Отклик при перелистывании и игле", haptic) {
            haptic = it
            Settings.hapticEnabled = it
        }

        Spacer(Modifier.height(24.dp))
        SettingHeader("Поведение")
        SettingRow(
            "Открывать в режиме «Только винил»",
            "Сразу проигрыватель, без магазина и списков", onlyVinyl
        ) {
            onlyVinyl = it
            Settings.onlyVinylDefault = it
        }
        SettingRow(
            "Не гасить экран в проигрывателе",
            "Экран не выключается, пока открыт плеер", keepScreen
        ) {
            keepScreen = it
            Settings.keepScreenOn = it
        }

        Spacer(Modifier.height(24.dp))
        SettingHeader("О приложении")
        Text(
            "VinylPlayer — виниловый плеер с душой.\n" +
                "Пластинки, игла, треск и магазин на углу.",
            color = Muted, fontSize = 13.sp, lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("Версия 1.0", color = Color(0x66B0B0BA), fontSize = 12.sp)
    }
}

@Composable
private fun SettingHeader(title: String) {
    Text(
        title.uppercase(),
        color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                subtitle, color = Muted, fontSize = 12.sp, maxLines = 2,
                textAlign = TextAlign.Start
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Accent, checkedThumbColor = Color.Black
            )
        )
    }
}
