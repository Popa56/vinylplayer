package com.example.vinylplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun Onboarding(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val spin = rememberSpin(true, 9000)

    Box(
        Modifier.fillMaxSize().background(BaseDark)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Магазин на углу",
                    text = "Здесь живут пластинки. Полка и ящик — два способа их листать. " +
                        "Неон на вывеске говорит, открыт ли магазин.",
                    art = {
                        Vinyl(Color(0xFF6B4FA0), { spin.value }, 180.dp, labelText = "SIDE A")
                    }
                )
                1 -> OnboardingPage(
                    title = "Кабинка прослушивания",
                    text = "Нажмите на альбом — конверт выедет из ящика, пластинка ляжет на " +
                        "проигрыватель, игла опустится. Можно крутить винил пальцем и ставить иглу вручную.",
                    art = {
                        Vinyl(Accent, { spin.value }, 160.dp, labelText = "SIDE B")
                    }
                )
                else -> OnboardingPage(
                    title = "Мелочи, которые важны",
                    text = "Треск иглы, щелчки, вибрация при перелистывании. Эквалайзер, " +
                        "избранное, свой цвет винила для каждого альбома. И виджет на главный экран.",
                    art = {
                        Vinyl(Color(0xFF2E7D6B), { spin.value }, 160.dp, labelText = "NEW")
                    }
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                Box(
                    Modifier.size(if (pager.currentPage == i) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (pager.currentPage == i) Accent else Color(0x44FFFFFF))
                )
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 40.dp, end = 40.dp)
                .fillMaxWidth().height(54.dp)
                .clip(RoundedCornerShape(50))
                .background(Accent)
                .clickable {
                    if (pager.currentPage == 2) onDone()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (pager.currentPage == 2) "Начать" else "Дальше",
                color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Пропустить",
            Modifier.align(Alignment.TopEnd).padding(20.dp).clickable { onDone() },
            color = Muted, fontSize = 13.sp
        )
    }
}

@Composable
private fun OnboardingPage(
    title: String,
    text: String,
    art: @Composable () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        art()
        Spacer(Modifier.height(40.dp))
        Text(
            title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text, color = Muted, fontSize = 14.sp, lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
