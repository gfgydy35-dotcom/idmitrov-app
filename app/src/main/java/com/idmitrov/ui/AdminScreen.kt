package com.idmitrov.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.idmitrov.data.News
import com.idmitrov.supabase.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val newsList = remember { mutableStateListOf<News>() }
    val isLoading = remember { mutableStateOf(false) }

    val showAddDialog = remember { mutableStateOf(false) }
    val title = remember { mutableStateOf("") }
    val content = remember { mutableStateOf("") }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val isUploading = remember { mutableStateOf(false) }
    val uploadProgress = remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri.value = uri
    }

    fun loadNews() {
        scope.launch {
            try {
                val result = SupabaseManager.client.postgrest["news"]
                    .select()
                    .order("id", ascending = false)
                    .decodeList<News>()
                newsList.clear()
                newsList.addAll(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteNews(news: News) {
        scope.launch {
            try {
                SupabaseManager.client.postgrest["news"]
                    .delete {
                        filter("id", "eq", news.id)
                    }
                newsList.remove(news)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNewsWithImage(titleText: String, contentText: String, imageUri: Uri?) {
        scope.launch {
            isUploading.value = true
            uploadProgress.value = "Загрузка фото..."
            try {
                var imageUrl = ""

                if (imageUri != null) {
                    uploadProgress.value = "Загрузка фото..."
                    val fileName = "news_${System.currentTimeMillis()}.jpg"
                    val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                    if (bytes != null) {
                        SupabaseManager.client.storage.from("news-images")
                            .upload(fileName, bytes)
                        imageUrl = SupabaseManager.client.storage.from("news-images")
                            .publicUrl(fileName)
                        uploadProgress.value = "Фото загружено!"
                    }
                }

                uploadProgress.value = "Сохранение новости..."
                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .format(Date())

                val newsData = mapOf(
                    "title" to titleText,
                    "content" to contentText,
                    "image_url" to imageUrl,
                    "created_at" to date
                )

                SupabaseManager.client.postgrest["news"]
                    .insert(newsData)

                uploadProgress.value = "Готово!"
                title.value = ""
                content.value = ""
                selectedImageUri.value = null
                showAddDialog.value = false
                loadNews()
            } catch (e: Exception) {
                uploadProgress.value = "Ошибка: ${e.message}"
                e.printStackTrace()
            }
            isUploading.value = false
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
                title = { Text("Админ-панель") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog.value = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить новость")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Всего новостей: ${newsList.size}",
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(newsList) { news ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = news.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = news.content.take(50) + "...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(
                                    onClick = { deleteNews(news) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!isUploading.value) {
                    showAddDialog.value = false
                    title.value = ""
                    content.value = ""
                    selectedImageUri.value = null
                }
            },
            title = { Text("Добавить новость") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title.value,
                        onValueChange = { title.value = it },
                        label = { Text("Заголовок") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading.value
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content.value,
                        onValueChange = { content.value = it },
                        label = { Text("Текст новости") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        enabled = !isUploading.value
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploading.value,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImageUri.value != null) "Фото выбрано ✅" else "Выбрать фото")
                    }

                    selectedImageUri.value?.let { uri ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = uri,
                            contentDescription = "Превью",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                    }

                    if (isUploading.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator()
                        Text(
                            text = uploadProgress.value,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.value.isNotBlank() && content.value.isNotBlank() && !isUploading.value) {
                            addNewsWithImage(title.value, content.value, selectedImageUri.value)
                        }
                    },
                    enabled = title.value.isNotBlank() && content.value.isNotBlank() && !isUploading.value
                ) {
                    Text(if (isUploading.value) "Загрузка..." else "Добавить")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (!isUploading.value) {
                            showAddDialog.value = false
                            title.value = ""
                            content.value = ""
                            selectedImageUri.value = null
                        }
                    },
                    enabled = !isUploading.value
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}
