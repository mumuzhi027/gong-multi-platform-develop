package com.sky31.gongmultiplatform.ui.screen.gongAi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sky31.gongmultiplatform.model.GongAiBlock
import com.sky31.gongmultiplatform.model.GongAiUiMessage
import com.sky31.gongmultiplatform.ui.viewModel.GongAiViewModel
import gongmultiplatform.composeapp.generated.resources.Res
import gongmultiplatform.composeapp.generated.resources.baseline_arrow_back_24
import gongmultiplatform.composeapp.generated.resources.gonggong_logo
import org.jetbrains.compose.resources.painterResource
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun GongAiScreen(onBack: () -> Unit) {
    val viewModel = getKoin().get<GongAiViewModel>()
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val introAccepted by viewModel.introAccepted.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var showPrivacy by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(Res.drawable.baseline_arrow_back_24), "返回")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("拱拱AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Beta", modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("一句话，帮你办校园事", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = viewModel::clearConversation) { Text("清空") }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("问课表、成绩，或说想办什么…") },
                    enabled = !loading,
                    maxLines = 4,
                    shape = RoundedCornerShape(18.dp)
                )
                Button(
                    onClick = { val value = input; input = ""; viewModel.send(value) },
                    enabled = input.isNotBlank() && !loading,
                    modifier = Modifier.height(52.dp)
                ) { Text("发送") }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "拱拱AI目前处于测试阶段，执行重要操作前会由你确认。",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(messages, key = { it.id }) { message -> GongAiMessage(message, viewModel) }
            if (loading) item {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("拱拱正在处理…", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            error?.let { message -> item { GongAiBlockCard(GongAiBlock(type = "warning", text = message), viewModel) } }
            item { Spacer(Modifier.height(6.dp)) }
        }
    }

    if (!introAccepted) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(painterResource(Res.drawable.gonggong_logo), null, modifier = Modifier.size(42.dp)) },
            title = { Text("欢迎使用拱拱AI") },
            text = {
                Text("拱拱AI可以根据你的需求查询和办理校园事务。AI不会获取你的校园账号密码、登录 Cookie 或 Token；涉及请假、预约等提交操作时，都会在执行前由你确认。")
            },
            confirmButton = { Button(onClick = viewModel::acceptIntro) { Text("开始使用") } },
            dismissButton = { TextButton(onClick = { showPrivacy = true }) { Text("拱拱AI隐私说明") } }
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("拱拱AI隐私说明") },
            text = { Text("AI仅接收理解当前问题所需的最少信息。校园密码、Cookie、访问令牌和刷新令牌永不发送给模型；成绩等敏感查询优先由服务器确定性计算；附件默认不提供给模型。你可以随时清空对话记录。") },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("我知道了") } }
        )
    }
}

@Composable
private fun GongAiMessage(message: GongAiUiMessage, viewModel: GongAiViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start
    ) {
        if (message.text.isNotBlank()) {
            Surface(
                color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(if (message.fromUser) .82f else .92f)
            ) { Text(message.text, modifier = Modifier.padding(13.dp), style = MaterialTheme.typography.bodyMedium) }
        }
        message.blocks.forEach { block -> GongAiBlockCard(block, viewModel) }
    }
}

@Composable
private fun GongAiBlockCard(block: GongAiBlock, viewModel: GongAiViewModel) {
    val warning = block.type == "warning"
    Card(
        modifier = Modifier.fillMaxWidth(.94f).padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (block.title.isNotBlank()) Text(block.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (block.text.isNotBlank()) Text(block.text, style = MaterialTheme.typography.bodyMedium)
            block.fields.forEach { field ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(field.label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.4f))
                    Text(field.value, modifier = Modifier.weight(.6f))
                }
            }
            block.items.take(30).forEach { item ->
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (item.subtitle.isNotBlank()) Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.meta.isNotEmpty()) Text(item.meta.entries.joinToString("  ") { "${it.key}：${it.value}" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            block.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (block.type == "action_preview" && block.actionId.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.confirm(block.actionId) }) { Text(block.confirmText.ifBlank { "确认提交" }) }
                    OutlinedButton(onClick = { viewModel.cancel(block.actionId) }) { Text(block.cancelText.ifBlank { "取消" }) }
                }
                Text("确认后参数将锁定且只执行一次", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

