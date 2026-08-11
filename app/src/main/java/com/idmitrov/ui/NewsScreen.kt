package com.idmitrov.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.idmitrov.data.News
import com.idmitrov.supabase.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    onNavigateToAdmin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val newsList = remember { mutableStateListOf<News>() }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    fun loadNews() {
        scope.launch {
            isLoading.value = true
            try {
                val result = SupabaseManager.client.postgrest["news"]
                    .select()
                    .order("id", ascending = false)
                    .decodeList<News>()
                newsList.clear()
                newsList.addAll(result)
                errorMessage.value = null
            } catch (e: Exception) {
                errorMessage.value = "Ошибка загрузки: ${e.message}"
            }
            isLoading.value = false
        }
    }

    LaunchedEffect(Unit) {
        loadNews()
        scope.launch {
            SupabaseManager.client.postgrest["news"]
                .select()
                .decodeList<News>()
                .collect { result ->
                    newsList.clear()
                    newsList.addAll(result)
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("idmitrov — Новости Дмитрова") },
                actions = {
                    IconButton(onClick = onNavigateToAdmin) {
                        Icon(Icons.Default.Settings, contentDescription = "Админ-панель")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading.value && newsList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage.value != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage.value!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadNews() }) {
                            Text("Обновить")
                        }
                    }
                }
            }
            newsList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Новостей пока нет")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newsList) { news ->
                        NewsCard(news = news)
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCard(news: News) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (news.image_url.isNotBlank()) {
                AsyncImage(
                    model = news.image_url,
                    contentDescription = "Изображение к новости",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = news.content,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(news.created_at),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateStr
    }
}
