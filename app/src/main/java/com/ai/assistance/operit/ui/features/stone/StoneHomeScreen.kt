package com.ai.assistance.operit.ui.features.stone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.ai.assistance.operit.ui.main.screens.Screen
import com.ai.assistance.operit.ui.main.screens.ScreenNavigationHandler

private data class Room(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val target: () -> Screen
)

private val rooms: List<Room> = listOf(
    Room("悄悄话", "CONVERSATIONS", Icons.Default.Email) { Screen.AiChat },
    Room("记忆档案", "MEMORY", Icons.Default.History) { Screen.MemoryBase },
    Room("工具箱", "TOOLS", Icons.Default.Extension) { Screen.Toolbox },
    Room("终端", "TERMINAL", Icons.Default.Terminal) { Screen.Terminal },
    Room("权限", "PERMISSIONS", Icons.Default.Security) { Screen.ToolPermission },
    Room("助理", "ASSISTANT", Icons.Default.Person) { Screen.AssistantConfig },
    Room("工作流", "WORKFLOW", Icons.Default.Build) { Screen.Workflow },
    Room("插件", "PACKAGES", Icons.Default.Extension) { Screen.Packages },
    Room("设置", "SETTINGS", Icons.Default.Settings) { Screen.Settings }
)

@Composable
fun StoneHomeScreen(navigateTo: ScreenNavigationHandler) {
    val paper = Color(0xFFF3EADA)
    val ink = Color(0xFF2B2118)
    val gold = Color(0xFFA9834A)
    val wine = Color(0xFF7A2B2F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 抬头
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 42.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Cubiculum.",
                    color = ink,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "一间只属于你们的房间",
                        color = ink.copy(alpha = 0.66f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(text = "藏", color = wine.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(wine.copy(alpha = 0.45f), gold.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "十个房间",
                    color = ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(rooms) { room ->
                    RoomCard(room) { navigateTo(room.target()) }
                }
            }
        }
    }
}

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit) {
    val cardBg = Color(0xFFFBF6E8)
    val ink = Color(0xFF2B2118)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE9DEC8)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = room.icon,
                    contentDescription = room.title,
                    tint = Color(0xFF6B5B45),
                    modifier = Modifier.fillMaxWidth(0.44f)
                )
            }
        }
        Text(
            text = room.title,
            color = ink,
            fontSize = 12.sp,
            maxLines = 1
        )
        Text(
            text = room.subtitle,
            color = ink.copy(alpha = 0.45f),
            fontSize = 7.sp,
            maxLines = 1
        )
    }
}
