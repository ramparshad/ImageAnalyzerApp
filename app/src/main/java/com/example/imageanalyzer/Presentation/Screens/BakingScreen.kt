package com.example.imageanalyzer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.imageanalyzer.Presentation.Screens.UiState
import com.example.imageanalyzer.Presentation.ViewModel.BakingViewModel
import com.example.jcmodule.RippleEffectProgressBar
import java.util.Locale


@Composable
fun BakingScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: BakingViewModel= hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // State for selected image and prompt
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("Results will appear here") }

    // TextToSpeech setup
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Initialize TextToSpeech
    LaunchedEffect(Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }
                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        result = "Text-to-speech error"
                    }
                })
                ttsInitialized = true
            } else {
                result = "Text-to-speech initialization failed"
            }
        }
    }

    // Clean up TextToSpeech when composable is disposed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                textToSpeech.stop()
                textToSpeech.shutdown()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            result = "Camera permission denied"
        }
    }

    // Permission launcher for audio recording
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            result = "Microphone permission denied"
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            selectedImageBitmap = it
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedImageBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
            } catch (e: Exception) {
                result = "Error loading image: ${e.localizedMessage}"
            }
        }
    }

    // Speech-to-text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        try {
            if (activityResult.resultCode == android.app.Activity.RESULT_OK) {
                val data = activityResult.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    prompt = results[0]
                } else {
                    result = "No speech recognized"
                }
            } else {
                result = "Speech recognition failed"
            }
        } catch (e: Exception) {
            result = "Speech recognition error: ${e.localizedMessage}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 3.dp, start = 5.dp, end = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Image Analyzer",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Blue
            )
            IconButton(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToProfile ) {
                Icon(
                    imageVector = Icons.Filled.ManageAccounts,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
Row (modifier = Modifier.fillMaxWidth()
    .padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
){
    // Image display
    selectedImageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Selected image",
            modifier = Modifier
                .size(200.dp)
                .border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary))

        )
    } ?: Text(
        text = "No image selected",
        modifier = Modifier
            .padding(16.dp)
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.5f),
            border = BorderStroke(2.dp, color = Color.DarkGray)
        ) {
            // Camera and Gallery buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        val permission =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.CAMERA
                            } else {
                                Manifest.permission.CAMERA
                            }
                        if (ContextCompat.checkSelfPermission(
                                context,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            cameraLauncher.launch()
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "Take Photo",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileOpen,
                        contentDescription = "Pick from Gallery",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

        // Cool-looking search TextField with all icons below
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Enter prompt") },
                    placeholder = { Text("e.g., 'Tell me about this image'") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

        // Speak button
                    IconButton(
                        onClick = {
                            if (ttsInitialized) {
                                textToSpeech?.speak(
                                    (uiState as? UiState.Success)?.outputText ?: "",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "utteranceId"
                                )
                                isSpeaking = true
                            } else {
                                result = "Text-to-speech not initialized"
                            }
                        },
                        enabled = ttsInitialized && uiState is UiState.Success && !isSpeaking
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

        // Stop speaking button
                    IconButton(
                        onClick = {
                            textToSpeech?.stop()
                            isSpeaking = false
                        },
                        enabled = ttsInitialized && isSpeaking
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicOff,
                            contentDescription = "Stop Speaking",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }


                    // Mic button for speech recognition
                    IconButton(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.RECORD_AUDIO
                            } else {
                                Manifest.permission.RECORD_AUDIO
                            }
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt")
                                    }
                                    try {
                                        speechRecognizerLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        result = "Failed to start speech recognition: ${e.localizedMessage}"
                                    }
                                } else {
                                    result = "Speech recognition not available on this device"
                                }
                            } else {
                                audioPermissionLauncher.launch(permission)
                            }
                        },
                        enabled = true
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Speak Prompt",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

        // Search button
                    IconButton(
                        onClick = {
                            selectedImageBitmap?.let { bitmap ->
                                viewModel.sendPrompt(bitmap, prompt)
                            } ?: run {
                                result = "Please select an image first"
                            }
                        },
                        enabled = prompt.isNotEmpty() && selectedImageBitmap != null
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Result display
        when (uiState) {
            is UiState.Loading -> {
                RippleEffectProgressBar(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    size = 110.dp,
                    color = Color.Blue
                )
            }
            is UiState.Error -> {
                SelectionContainer {
                    Text(
                        text = (uiState as UiState.Error).errorMessage,
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
            is UiState.Success -> {
                SelectionContainer {
                    Text(
                        text = (uiState as UiState.Success).outputText,
                        fontSize =20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
            is UiState.Initial -> {
                SelectionContainer {
                    Text(
                        text = result,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

