package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.font.FontStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PatientEntity
import com.example.data.consolidated
import com.example.data.PostEntity
import com.example.data.PatientRepository
import com.example.ui.PatientRoundsViewModel
import com.example.ui.PatientRoundsViewModelFactory
import com.example.ui.UiEvent
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Persistence layer hooks
        val database = AppDatabase.getDatabase(this)
        val repository = PatientRepository(database.patientDao(), database.postDao())

        setContent {
            MyApplicationTheme {
                // Instantiating patient VM with factory
                val viewModel: PatientRoundsViewModel = viewModel(
                    factory = PatientRoundsViewModelFactory(repository)
                )

                val context = LocalContext.current

                // Seed preferences
                LaunchedEffect(Unit) {
                    viewModel.loadPreferences(context)
                }

                // Handle one-time shared flow events (Toast / PDF intents)
                LaunchedEffect(viewModel.eventFlow) {
                    viewModel.eventFlow.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowToast -> {
                                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is UiEvent.OpenPdf -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(event.fileUri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No PDF viewer found on device.", Toast.LENGTH_LONG).show()
                                }
                            }
                            is UiEvent.SharePdf -> {
                                try {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, event.fileUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Faster Obs PDF"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Sharing failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(4000)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    PatientRoundsApp(viewModel = viewModel)

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 800))
                    ) {
                        MedicalRoundsSplashScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MedicalRoundsSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3E47),
                        Color(0xFF122328)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Icon
            Image(
                painter = painterResource(id = R.drawable.img_app_icon_v3),
                contentDescription = "Faster Obs App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .scale(1.35f, 1.0f)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(40.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Title
            Text(
                text = "Faster Obs",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.SansSerif
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Designer text
            Text(
                text = "Designed by Dr.Andrew Mamdouh",
                color = Color(0xFFBBE1EC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRoundsApp(viewModel: PatientRoundsViewModel) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val allPatients by viewModel.allPatients.collectAsStateWithLifecycle()
    val editingPatient by viewModel.editingPatient.collectAsStateWithLifecycle()

    var isExtractingPdf by remember { mutableStateOf(false) }
    var extractionError by remember { mutableStateOf<String?>(null) }

    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val selectedPostId by viewModel.selectedPostId.collectAsStateWithLifecycle()
    val selectedPost by viewModel.selectedPost.collectAsStateWithLifecycle()

    val pdfFontSize by viewModel.pdfFontSize.collectAsStateWithLifecycle()
    val pdfAccentColor by viewModel.pdfAccentColor.collectAsStateWithLifecycle()
    
    var showDeleteConfirmAll by remember { mutableStateOf(false) }
    var patientToDelete by remember { mutableStateOf<PatientEntity?>(null) }
    var showPdfStyleDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showPostSelectorMenu by remember { mutableStateOf(false) }
    var showManagePostsDialog by remember { mutableStateOf(false) }
    var showRenameActivePostDialog by remember { mutableStateOf(false) }
    var renameActivePostText by remember { mutableStateOf("") }

    val printCount = patients.count { it.isSelectedForPrint }
    val pagesNeeded = (printCount + 5) / 6

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Medical Clipboard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Post Sheets PDF",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (patients.isNotEmpty()) {
                                Text(
                                    text = "$printCount patient${if (printCount != 1) "s" else ""} selected (${pagesNeeded} page${if (pagesNeeded != 1) "s" else ""})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (patients.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.generateAndOpenPdf(context) },
                            modifier = Modifier.testTag("generate_pdf_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Open PDF")
                        }
                        IconButton(onClick = { viewModel.generateAndSharePdf(context) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
                        }
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All Patients") },
                            onClick = {
                                viewModel.selectAllPatientsForPrint(true)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Deselect All Patients") },
                            onClick = {
                                viewModel.selectAllPatientsForPrint(false)
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("PDF Styling Settings") },
                            onClick = {
                                showPdfStyleDialog = true
                                menuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Shared Patient Data") },
                            onClick = {
                                showImportDialog = true
                                menuExpanded = false
                            }
                        )
                        if (patients.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Share All Patients in Post") },
                                onClick = {
                                    val shareText = com.example.utils.PatientSharing.serializeList(patients)
                                    if (shareText.isNotEmpty()) {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share All Patients in Post")
                                        context.startActivity(shareIntent)
                                    }
                                    menuExpanded = false
                                }
                            )
                        }
                        if (allPatients.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Share All Patients (All Posts)") },
                                onClick = {
                                    val shareText = com.example.utils.PatientSharing.serializeList(allPatients)
                                    if (shareText.isNotEmpty()) {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share All Patients")
                                        context.startActivity(shareIntent)
                                    }
                                    menuExpanded = false
                                }
                            )
                        }
                        if (posts.isNotEmpty()) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear All Patients in Post", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDeleteConfirmAll = true
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (posts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.startCreatingPatient() },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_patient_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Patient")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (posts.isEmpty()) {
                // Empty state for Posts
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No Posts",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Create Your First Post",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clinical records are organized inside Posts. Every Post has its own dedicated patients lists and formatted print documents.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    var tempTitle by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Clinical Post Name") },
                        placeholder = { Text("e.g. OB/GYN, Emergency, Post A") },
                        singleLine = true,
                        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().testTag("initial_post_title_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (tempTitle.isNotBlank()) {
                                viewModel.createPost(tempTitle)
                                tempTitle = ""
                            }
                        },
                        modifier = Modifier.testTag("initial_post_create_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Clinical Post")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active Post bar at the top!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ACTIVE CLINICAL POST",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { showPostSelectorMenu = true }
                                                .weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = selectedPost?.title ?: "Select Post...",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.testTag("active_post_title")
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Post",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        selectedPost?.let { currentPost ->
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    renameActivePostText = currentPost.title
                                                    showRenameActivePostDialog = true
                                                },
                                                modifier = Modifier.size(24.dp).testTag("edit_active_post_name_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Post Name",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Button(
                                    onClick = { showCreatePostDialog = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("new_post_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Post", fontSize = 12.sp)
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${patients.size} Patient${if (patients.size != 1) "s" else ""} in this Post",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { showManagePostsDialog = true },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.testTag("manage_posts_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manage All Posts", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Reverse PDF extraction card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("reverse_pdf_extraction_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "REVERSE PATIENT SCANNER (PDF)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Upload any rounds PDF to decode patient data and automatically populate clinical fields.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            val coroutineScope = rememberCoroutineScope()
                            val pdfPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri: Uri? ->
                                if (uri != null) {
                                    coroutineScope.launch {
                                        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                                        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                                            extractionError = "Please add your GEMINI_API_KEY inside the Secrets panel of Google AI Studio."
                                            return@launch
                                        }
                                        
                                        isExtractingPdf = true
                                        extractionError = null
                                        val activePostId = selectedPostId ?: 0
                                        
                                        val result = com.example.utils.GeminiPdfExtractor.extractPatientFromPdf(
                                            context = context,
                                            uri = uri,
                                            postId = activePostId,
                                            apiKey = apiKey
                                        )
                                        
                                        isExtractingPdf = false
                                        if (result.isSuccess) {
                                            val patientsList = result.getOrNull()
                                            if (!patientsList.isNullOrEmpty()) {
                                                if (patientsList.size == 1) {
                                                    viewModel.startEditingPatient(patientsList[0])
                                                } else {
                                                    viewModel.importPatients(patientsList)
                                                }
                                            } else {
                                                extractionError = "No patient records were extracted."
                                            }
                                        } else {
                                            extractionError = result.exceptionOrNull()?.message ?: "Unknown extraction error"
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("import_pdf_trigger")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Import PDF",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import PDF", fontSize = 12.sp)
                            }
                        }
                    }

                    if (patients.isEmpty()) {
                        // Empty state inside selected post
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Medical rounds placeholder",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Patients In This Post",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap the '+' button below to add clinical records for this post.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp, start = 12.dp, end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(
                                items = patients,
                                key = { _, patient -> patient.id }
                            ) { index, patient ->
                                PatientCardItem(
                                    patient = patient,
                                    allPatients = allPatients,
                                    posts = posts,
                                    onToggleSelection = { viewModel.togglePatientPrintSelection(patient) },
                                    onEdit = { viewModel.startEditingPatient(patient) },
                                    onDelete = { patientToDelete = patient },
                                    isFirst = index == 0,
                                    isLast = index == patients.lastIndex,
                                    onMoveUp = { viewModel.movePatientUp(patient) },
                                    onMoveDown = { viewModel.movePatientDown(patient) },
                                    onShare = {
                                        val shareText = com.example.utils.PatientSharing.serialize(patient)
                                        if (shareText.isNotEmpty()) {
                                            val sendIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Clinical Patient Code")
                                            context.startActivity(shareIntent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Patient configuration editor modal overlays
            if (editingPatient != null) {
                PatientEditorDialog(
                    patient = editingPatient!!,
                    onDismiss = { viewModel.stopEditing() },
                    onStateChange = { updater -> viewModel.updateEditingState(updater) },
                    onSave = { viewModel.savePatient() }
                )
            }

            if (showImportDialog) {
                ImportPatientDialog(
                    onDismiss = { showImportDialog = false },
                    onImport = { importedPatients ->
                        viewModel.importPatients(importedPatients)
                        showImportDialog = false
                    },
                    activePostId = selectedPostId ?: (posts.firstOrNull()?.id)
                )
            }

            if (showDeleteConfirmAll) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmAll = false },
                    title = { Text("Clear All Records in Post?") },
                    text = { Text("This will permanently delete all clinical patient entries inside the currently active Post from local storage.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllPatients()
                                showDeleteConfirmAll = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear Post")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmAll = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (patientToDelete != null) {
                AlertDialog(
                    onDismissRequest = { patientToDelete = null },
                    title = { Text("Confirm Deletion") },
                    text = { Text("Are you sure you want to permanently delete the patient '${patientToDelete?.name?.ifEmpty { "Unnamed Patient" }}'?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                patientToDelete?.let { viewModel.deletePatient(it) }
                                patientToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { patientToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showPdfStyleDialog) {
                PdfStyleDialog(
                    currentFontSize = pdfFontSize,
                    currentAccentColor = pdfAccentColor,
                    onDismiss = { showPdfStyleDialog = false },
                    onSave = { size, color ->
                        viewModel.updatePdfPreferences(context, size, color)
                        showPdfStyleDialog = false
                    }
                )
            }

            // Post dialogues
            if (showCreatePostDialog) {
                var newPostTitleLocal by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreatePostDialog = false },
                    title = { Text("Create New Clinical Post") },
                    text = {
                        Column {
                            Text("Provide a title to organize clinical patient profiles under a unique category.", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPostTitleLocal,
                                onValueChange = { newPostTitleLocal = it },
                                label = { Text("Post Title") },
                                placeholder = { Text("e.g. Ward A, Delivery Room") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("dialog_new_post_name")
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newPostTitleLocal.isNotBlank()) {
                                    viewModel.createPost(newPostTitleLocal)
                                    showCreatePostDialog = false
                                }
                            },
                            modifier = Modifier.testTag("dialog_new_post_confirm")
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePostDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showRenameActivePostDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameActivePostDialog = false },
                    title = { Text("Rename Clinical Post") },
                    text = {
                        Column {
                            Text("Update the name of this clinical post.", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = renameActivePostText,
                                onValueChange = { renameActivePostText = it },
                                label = { Text("Post Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("dialog_rename_active_post_name")
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val currentPost = selectedPost
                                if (currentPost != null && renameActivePostText.isNotBlank()) {
                                    viewModel.renamePost(currentPost, renameActivePostText)
                                    showRenameActivePostDialog = false
                                }
                            },
                            modifier = Modifier.testTag("dialog_rename_active_post_confirm")
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameActivePostDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showPostSelectorMenu) {
                AlertDialog(
                    onDismissRequest = { showPostSelectorMenu = false },
                    title = { Text("Switch Active Post") },
                    text = {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(posts) { post ->
                                val isSelected = post.id == selectedPostId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectPost(post.id)
                                            showPostSelectorMenu = false
                                        }
                                        .testTag("switch_post_item_${post.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.List,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = post.title,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPostSelectorMenu = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (showManagePostsDialog) {
                var editingPostLocal by remember { mutableStateOf<PostEntity?>(null) }
                var renameTextLocal by remember { mutableStateOf("") }
                
                AlertDialog(
                    onDismissRequest = { showManagePostsDialog = false },
                    title = { Text("Manage Clinical Posts") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (editingPostLocal == null) {
                                Text("Select a post below to edit or delete it:", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(posts) { post ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = post.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(
                                                onClick = {
                                                    editingPostLocal = post
                                                    renameTextLocal = post.title
                                                },
                                                modifier = Modifier.size(36.dp).testTag("post_rename_btn_${post.id}")
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deletePost(post) },
                                                modifier = Modifier.size(36.dp).testTag("post_delete_btn_${post.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete, 
                                                    contentDescription = "Delete", 
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("Rename Post Title:", fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = renameTextLocal,
                                    onValueChange = { renameTextLocal = it },
                                    label = { Text("New Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("post_rename_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { editingPostLocal = null }) {
                                        Text("Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val postToUpdate = editingPostLocal
                                            if (postToUpdate != null && renameTextLocal.isNotBlank()) {
                                                viewModel.renamePost(postToUpdate, renameTextLocal)
                                                editingPostLocal = null
                                            }
                                        },
                                        modifier = Modifier.testTag("post_rename_confirm")
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showManagePostsDialog = false },
                            modifier = Modifier.testTag("close_manage_posts")
                        ) {
                            Text("Done")
                        }
                    }
                )
            }

            if (isExtractingPdf) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.width(280.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Decoding Patient PDF...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Gemini is scanning document visuals and extracting clinical parameters.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (extractionError != null) {
                AlertDialog(
                    onDismissRequest = { extractionError = null },
                    title = { Text("Extraction Unsuccessful") },
                    text = { Text(extractionError ?: "An unexpected error occurred during PDF scanning.") },
                    confirmButton = {
                        TextButton(onClick = { extractionError = null }) {
                            Text("Acknowledge")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PatientCardItem(
    patient: PatientEntity,
    allPatients: List<PatientEntity> = emptyList(),
    posts: List<PostEntity> = emptyList(),
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_item_card_${patient.id}")
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        border = BorderStroke(
            1.dp,
            if (patient.isSelectedForPrint) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (patient.isSelectedForPrint) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = patient.isSelectedForPrint,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = patient.name.ifEmpty { "Unnamed Patient" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (patient.abo.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(vertical = 2.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = patient.abo,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (patient.diagnosis.isNotEmpty()) {
                        Text(
                            text = com.example.utils.ColorTagParser.parseToAnnotatedString(
                                patient.diagnosis,
                                MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move patient up",
                        tint = if (isFirst) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move patient down",
                        tint = if (isLast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit patient particulars",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share complete patient profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // LMP & EDD highlights
            if (patient.lmp.isNotEmpty() || patient.edd.isNotEmpty() || patient.us.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (patient.lmp.isNotEmpty()) {
                        Column {
                            Text("LMP", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(patient.lmp, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (patient.edd.isNotEmpty()) {
                        Column {
                            Text("EDD", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(patient.edd, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    // Display calculated on-screen Gestational Age!
                    val gaStr = com.example.utils.GestationalAgeCalculator.calculateGestationalAge(patient.lmp, patient.edd)
                    if (!gaStr.isNullOrEmpty()) {
                        Column {
                            Text("GA", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(vertical = 2.dp, horizontal = 6.dp)
                            ) {
                                Text(
                                    text = gaStr,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (patient.us.isNotEmpty()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("U/S Findings", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(patient.us, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Expandable details (Selected Lab profiles and general notes)
            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                // Key Lab Parameters Grid (Unified Chronological Comparison Schedule for All Labs)
                val patientTimelineRecords = remember(patient.labRecordsJson, patient.createdAt) {
                    val list = com.example.data.LabRecordSerializer.fromJson(patient.labRecordsJson)
                    if (list.isEmpty()) {
                        listOf(
                            com.example.data.LabRecord(
                                id = "initial",
                                createdAt = patient.createdAt,
                                hb = patient.hb,
                                hct = patient.hct,
                                plt = patient.plt,
                                wbc = patient.wbc,
                                urea = patient.urea,
                                creat = patient.creat,
                                sgot = patient.sgot,
                                sgpt = patient.sgpt,
                                inr = patient.inr,
                                rbs = patient.rbs,
                                na = patient.na,
                                k = patient.k,
                                alb = patient.alb,
                                acetone = patient.acetone,
                                sugar = patient.sugar,
                                pus = patient.pus
                            )
                        )
                    } else {
                        list.sortedBy { it.createdAt }
                    }
                }

                val columns = remember {
                    listOf(
                        "Hb" to { r: com.example.data.LabRecord -> r.hb },
                        "Hct" to { r: com.example.data.LabRecord -> r.hct },
                        "WBC" to { r: com.example.data.LabRecord -> r.wbc },
                        "PLT" to { r: com.example.data.LabRecord -> r.plt },
                        "Urea" to { r: com.example.data.LabRecord -> r.urea },
                        "Creat" to { r: com.example.data.LabRecord -> r.creat },
                        "SGOT" to { r: com.example.data.LabRecord -> r.sgot },
                        "SGPT" to { r: com.example.data.LabRecord -> r.sgpt },
                        "INR" to { r: com.example.data.LabRecord -> r.inr },
                        "RBS" to { r: com.example.data.LabRecord -> r.rbs },
                        "Na" to { r: com.example.data.LabRecord -> r.na },
                        "K" to { r: com.example.data.LabRecord -> r.k },
                        "Alb" to { r: com.example.data.LabRecord -> r.alb },
                        "Acet" to { r: com.example.data.LabRecord -> r.acetone },
                        "Sug" to { r: com.example.data.LabRecord -> r.sugar },
                        "Pus" to { r: com.example.data.LabRecord -> r.pus }
                    )
                }

                fun getEffectiveValue(records: List<com.example.data.LabRecord>, index: Int, selectField: (com.example.data.LabRecord) -> String): Pair<String, Boolean> {
                    val rawVal = selectField(records[index])
                    if (rawVal.isNotBlank()) {
                        return Pair(rawVal, false)
                    }
                    for (prev in index - 1 downTo 0) {
                        val prevVal = selectField(records[prev])
                        if (prevVal.isNotBlank()) {
                            return Pair(prevVal, true)
                        }
                    }
                    return Pair("", false)
                }

                val scrollStateTable = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LABORATORY METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (scrollStateTable.value < scrollStateTable.maxValue) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Swipe right to see more",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Scroll right indicator",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollStateTable)
                        ) {
                            Column {
                                // Header row
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date Header
                                    Text(
                                        text = "Date",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(72.dp).padding(start = 8.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    
                                    // Column Headers
                                    columns.forEach { (label, _) ->
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(54.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                
                                // Rows
                                patientTimelineRecords.forEachIndexed { index, rec ->
                                    val rowBg = if (index % 2 == 1) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    }
                                    
                                    val dateLabel = SimpleDateFormat("d-M", Locale.getDefault()).format(Date(rec.createdAt))
                                    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rec.createdAt))
                                    
                                    val isCurrent = rec.createdAt == patient.createdAt
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    
                                    Row(
                                        modifier = Modifier
                                            .background(rowBg)
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Date cell
                                        Column(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .padding(start = 8.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dateLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = timeLabel,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        
                                        // Value cells
                                        columns.forEach { (_, selectField) ->
                                            val (effectiveValue, isCarriedForward) = getEffectiveValue(patientTimelineRecords, index, selectField)
                                            
                                            Box(
                                                modifier = Modifier.width(54.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (effectiveValue.isBlank()) {
                                                    Text(
                                                        text = "-",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                } else {
                                                    Text(
                                                        text = effectiveValue,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        fontStyle = if (isCarriedForward) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                                        color = when {
                                                            isCurrent && !isCarriedForward -> MaterialTheme.colorScheme.primary
                                                            isCarriedForward -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Soft fade scrim on the right to visually indicate overflow and scrollability
                        if (scrollStateTable.value < scrollStateTable.maxValue) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                if (patient.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NOTES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = patient.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to collapse",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to view labs & notes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LabValueText(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifEmpty { "—" },
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = FontWeight.Normal,
            color = if (value.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PatientEditorDialog(
    patient: PatientEntity,
    onDismiss: () -> Unit,
    onStateChange: ((PatientEntity) -> PatientEntity) -> Unit,
    onSave: () -> Unit
) {
    val lContext = LocalContext.current
    // We can group editor text inputs in 3 clinical tabs
    var selectedTab by remember { mutableIntStateOf(0) }

    val parsedRecords = remember(patient.labRecordsJson, patient.createdAt) {
        val list = com.example.data.LabRecordSerializer.fromJson(patient.labRecordsJson)
        if (list.isEmpty()) {
            listOf(
                com.example.data.LabRecord(
                    id = "initial_${patient.id}",
                    createdAt = patient.createdAt,
                    hb = patient.hb,
                    hct = patient.hct,
                    plt = patient.plt,
                    wbc = patient.wbc,
                    urea = patient.urea,
                    creat = patient.creat,
                    sgot = patient.sgot,
                    sgpt = patient.sgpt,
                    inr = patient.inr,
                    rbs = patient.rbs,
                    na = patient.na,
                    k = patient.k,
                    alb = patient.alb,
                    acetone = patient.acetone,
                    sugar = patient.sugar,
                    pus = patient.pus
                )
            )
        } else {
            list.sortedBy { it.createdAt }
        }
    }

    var selectedRecordId by remember(patient.id, parsedRecords.size) {
        mutableStateOf<String?>(parsedRecords.lastOrNull()?.id)
    }

    // fallback check
    if (selectedRecordId == null || parsedRecords.none { it.id == selectedRecordId }) {
        selectedRecordId = parsedRecords.lastOrNull()?.id
    }

    val activeRecord = parsedRecords.find { it.id == selectedRecordId } ?: parsedRecords.lastOrNull()

    val updateActiveRecord = { updater: (com.example.data.LabRecord) -> com.example.data.LabRecord ->
        if (activeRecord != null) {
            val updatedRecords = parsedRecords.map { rec ->
                if (rec.id == activeRecord.id) updater(rec) else rec
            }
            
            val latestByDate = updatedRecords.maxByOrNull { it.createdAt } ?: activeRecord
            val consolidated = updatedRecords.consolidated()
            
            onStateChange { currentPatient ->
                currentPatient.copy(
                    labRecordsJson = com.example.data.LabRecordSerializer.toJson(updatedRecords),
                    hb = consolidated.hb,
                    hct = consolidated.hct,
                    plt = consolidated.plt,
                    wbc = consolidated.wbc,
                    urea = consolidated.urea,
                    creat = consolidated.creat,
                    sgot = consolidated.sgot,
                    sgpt = consolidated.sgpt,
                    inr = consolidated.inr,
                    rbs = consolidated.rbs,
                    na = consolidated.na,
                    k = consolidated.k,
                    alb = consolidated.alb,
                    acetone = consolidated.acetone,
                    sugar = consolidated.sugar,
                    pus = consolidated.pus,
                    createdAt = if (activeRecord.id == latestByDate.id) latestByDate.createdAt else currentPatient.createdAt
                )
            }
        }
    }

    var diagnosisValue by remember(patient.id) {
        mutableStateOf(TextFieldValue(patient.diagnosis))
    }

    LaunchedEffect(patient.diagnosis) {
        if (patient.diagnosis != diagnosisValue.text) {
            diagnosisValue = diagnosisValue.copy(text = patient.diagnosis)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Forces dialog to grow as full-screen modal
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (patient.id == 0) "New Patient Record" else "Edit Patient Record",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("cancel_patient_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onSave,
                            modifier = Modifier.testTag("save_patient_button")
                        ) {
                            Text(
                                "SAVE",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Tab select header
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Biographics", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Laboratory Matrices", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Clinical Notes", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // Main form content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Section 1: Biographics, ABO, Obstetric
                            Text(
                                "PATIENT & MILITARY GENERAL DETAILS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = patient.name,
                                onValueChange = { name -> onStateChange { it.copy(name = name) } },
                                label = { Text("Patient Full Name") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // ABO Selection Chips Row
                            Text("ABO Blood Group", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                bloodGroups.forEach { group ->
                                    val isSelected = patient.abo.equals(group, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onStateChange { it.copy(abo = group) } },
                                        label = { Text(group) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "OBSTETRIC / INORGANIC DATES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = patient.lmp,
                                    onValueChange = { lmp -> onStateChange { it.copy(lmp = lmp) } },
                                    label = { Text("LMP Date") },
                                    placeholder = { Text("e.g. DD/MM/YYYY") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = patient.edd,
                                    onValueChange = { edd -> onStateChange { it.copy(edd = edd) } },
                                    label = { Text("EDD Milestone") },
                                    placeholder = { Text("e.g. DD/MM/YYYY") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Dynamic Gestational Age Calculator option
                            val gaLong = com.example.utils.GestationalAgeCalculator.calculateGestationalAgeLong(patient.lmp, patient.edd)
                            if (!gaLong.isNullOrEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Calculated Gestational Age:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = gaLong,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val cleanGa = com.example.utils.GestationalAgeCalculator.calculateGestationalAge(patient.lmp, patient.edd) ?: ""
                                                val suffix = ""
                                                if (cleanGa.isNotEmpty()) {
                                                    val updatedDiag = com.example.utils.GestationalAgeCalculator.updateGaInDiagnosis(patient.diagnosis, cleanGa, forceAppend = true)
                                                    onStateChange { it.copy(diagnosis = updatedDiag) }
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Append GA to Diagnosis", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val applyFormattingTag = { tagName: String ->
                                val sel = diagnosisValue.selection
                                val text = diagnosisValue.text
                                val startTag = "[$tagName]"
                                val endTag = "[/$tagName]"
                                
                                val minIdx = minOf(sel.start, sel.end)
                                val maxIdx = maxOf(sel.start, sel.end)
                                
                                val adjustedRange = com.example.utils.ColorTagParser.adjustSelectionForTags(text, minIdx, maxIdx)
                                val adStart = adjustedRange.first
                                val adEnd = adjustedRange.last
                                
                                val (newText, newSelection) = if (adStart < adEnd) {
                                    val selectedText = text.substring(adStart, adEnd)
                                    val wrapped = "$startTag$selectedText$endTag"
                                    val t = text.take(adStart) + wrapped + text.substring(adEnd)
                                    val newCursorPos = adStart + wrapped.length
                                    t to TextRange(newCursorPos)
                                } else {
                                    val cursor = adStart
                                    val emptyTags = "$startTag$endTag"
                                    val t = text.take(cursor) + emptyTags + text.substring(cursor)
                                    val newCursorPos = cursor + startTag.length
                                    t to TextRange(newCursorPos)
                                }
                                
                                diagnosisValue = TextFieldValue(text = newText, selection = newSelection)
                                onStateChange { it.copy(diagnosis = newText) }
                            }

                            val removeFormattingAction = {
                                val sel = diagnosisValue.selection
                                val text = diagnosisValue.text
                                val (newText, newSelectionRange) = com.example.utils.ColorTagParser.removeFormatting(
                                    text,
                                    sel.start,
                                    sel.end
                                )
                                diagnosisValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newSelectionRange.first, newSelectionRange.last)
                                )
                                onStateChange { it.copy(diagnosis = newText) }
                            }

                            // High-fidelity formatting bar for bold styles, highlights, and formatting removal
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Styles, Highlights and Alignment
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Style:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    // Bold Text wrap
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .clickable { applyFormattingTag("bold") }
                                            .padding(horizontal = 10.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "B",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = "Highlight (H):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    // Highlight Yellow
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFFFF00))
                                            .clickable { applyFormattingTag("hyellow") }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Yellow",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    }
                                    
                                    // Highlight Pink
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF00B3))
                                            .clickable { applyFormattingTag("hpink") }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Pink",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Clear formatting
                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                            .clickable { removeFormattingAction() }
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Remove formatting",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "Clear",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = diagnosisValue,
                                onValueChange = { newValue ->
                                    diagnosisValue = newValue
                                    if (newValue.text != patient.diagnosis) {
                                        onStateChange { it.copy(diagnosis = newValue.text) }
                                    }
                                },
                                label = { Text("Clinical Diagnosis / Indication") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                                visualTransformation = com.example.utils.ColorTagParser.ColorTagVisualTransformation(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = patient.us,
                                onValueChange = { us -> onStateChange { it.copy(us = us) } },
                                label = { Text("Ultrasound Findings (U/S)") },
                                placeholder = { Text("AFI, Placenta, EFW details...") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        1 -> {
                            // Section 2: Laboratory matrix (Hb, Urea, etc.)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "COORDINATE LABORATORY PROFILES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Manage and select multiple dated lab records.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                                                Button(
                                    onClick = {
                                        val calendar = java.util.Calendar.getInstance()
                                        android.app.DatePickerDialog(
                                            lContext,
                                            { _, year, month, dayOfMonth ->
                                                calendar.set(java.util.Calendar.YEAR, year)
                                                calendar.set(java.util.Calendar.MONTH, month)
                                                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                                
                                                val chosenTime = calendar.timeInMillis
                                                val newRecord = com.example.data.LabRecord(
                                                    createdAt = chosenTime
                                                )
                                                val updatedRecords = parsedRecords + newRecord
                                                val latestByDate = updatedRecords.maxByOrNull { it.createdAt } ?: newRecord
                                                val consolidated = updatedRecords.consolidated()
                                                
                                                selectedRecordId = newRecord.id
                                                
                                                onStateChange { currentPatient ->
                                                    currentPatient.copy(
                                                        labRecordsJson = com.example.data.LabRecordSerializer.toJson(updatedRecords),
                                                        hb = consolidated.hb,
                                                        hct = consolidated.hct,
                                                        plt = consolidated.plt,
                                                        wbc = consolidated.wbc,
                                                        urea = consolidated.urea,
                                                        creat = consolidated.creat,
                                                        sgot = consolidated.sgot,
                                                        sgpt = consolidated.sgpt,
                                                        inr = consolidated.inr,
                                                        rbs = consolidated.rbs,
                                                        na = consolidated.na,
                                                        k = consolidated.k,
                                                        alb = consolidated.alb,
                                                        acetone = consolidated.acetone,
                                                        sugar = consolidated.sugar,
                                                        pus = consolidated.pus
                                                    )
                                                }
                                            },
                                            calendar.get(java.util.Calendar.YEAR),
                                            calendar.get(java.util.Calendar.MONTH),
                                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add lab", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Labs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Horizontal list of dated record tabs with a custom elegant chip design
                            val scrollStateComponents = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollStateComponents)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                parsedRecords.forEach { rec ->
                                    val recordDateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(rec.createdAt))
                                    val isSelected = rec.id == activeRecord?.id
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { selectedRecordId = rec.id }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = recordDateStr,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (parsedRecords.size > 1) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete this lab",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            // Delete this lab record
                                                            val updatedRecords = parsedRecords.filter { it.id != rec.id }
                                                            val consolidated = updatedRecords.consolidated()
                                                            onStateChange { currentPatient ->
                                                                currentPatient.copy(
                                                                    labRecordsJson = com.example.data.LabRecordSerializer.toJson(updatedRecords),
                                                                    hb = consolidated.hb,
                                                                    hct = consolidated.hct,
                                                                    plt = consolidated.plt,
                                                                    wbc = consolidated.wbc,
                                                                    urea = consolidated.urea,
                                                                    creat = consolidated.creat,
                                                                    sgot = consolidated.sgot,
                                                                    sgpt = consolidated.sgpt,
                                                                    inr = consolidated.inr,
                                                                    rbs = consolidated.rbs,
                                                                    na = consolidated.na,
                                                                    k = consolidated.k,
                                                                    alb = consolidated.alb,
                                                                    acetone = consolidated.acetone,
                                                                    sugar = consolidated.sugar,
                                                                    pus = consolidated.pus
                                                                )
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            ListItemSeparatorHeader("BLOOD COUNT & PLATELETS")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Hb (Hemoglobin)", activeRecord?.hb ?: "", Modifier.weight(1f)) { hb -> updateActiveRecord { it.copy(hb = hb) } }
                                MatrixInput("Hct (Hematocrit)", activeRecord?.hct ?: "", Modifier.weight(1f)) { hct -> updateActiveRecord { it.copy(hct = hct) } }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Platelets (PLT)", activeRecord?.plt ?: "", Modifier.weight(1f)) { plt -> updateActiveRecord { it.copy(plt = plt) } }
                                MatrixInput("WBC (White Blood Count)", activeRecord?.wbc ?: "", Modifier.weight(1f)) { wbc -> updateActiveRecord { it.copy(wbc = wbc) } }
                            }

                            ListItemSeparatorHeader("RENAL & HEPATOBILIARY METABOLIC")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Blood Urea Nitrogen", activeRecord?.urea ?: "", Modifier.weight(1f)) { urea -> updateActiveRecord { it.copy(urea = urea) } }
                                MatrixInput("Creatinine (Creat.)", activeRecord?.creat ?: "", Modifier.weight(1f)) { creat -> updateActiveRecord { it.copy(creat = creat) } }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("SGOT (AST)", activeRecord?.sgot ?: "", Modifier.weight(1f)) { sgot -> updateActiveRecord { it.copy(sgot = sgot) } }
                                MatrixInput("SGPT (ALT)", activeRecord?.sgpt ?: "", Modifier.weight(1f)) { sgpt -> updateActiveRecord { it.copy(sgpt = sgpt) } }
                            }

                            ListItemSeparatorHeader("ELECTROLYTES & GLYCEMIC INDEXES")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("INR Coagulation", activeRecord?.inr ?: "", Modifier.weight(1f)) { inr -> updateActiveRecord { it.copy(inr = inr) } }
                                MatrixInput("Random Blood Sugar", activeRecord?.rbs ?: "", Modifier.weight(1f)) { rbs -> updateActiveRecord { it.copy(rbs = rbs) } }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Serum Sodium (Na)", activeRecord?.na ?: "", Modifier.weight(1f)) { na -> updateActiveRecord { it.copy(na = na) } }
                                MatrixInput("Serum Potassium (K)", activeRecord?.k ?: "", Modifier.weight(1f)) { k -> updateActiveRecord { it.copy(k = k) } }
                            }

                            ListItemSeparatorHeader("URINALYSIS & PROTEIN CO-INDEXES")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Albumin (Alb)", activeRecord?.alb ?: "", Modifier.weight(1f)) { alb -> updateActiveRecord { it.copy(alb = alb) } }
                                MatrixInput("Ketones (Acetone)", activeRecord?.acetone ?: "", Modifier.weight(1f)) { acetone -> updateActiveRecord { it.copy(acetone = acetone) } }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixInput("Urine Sugar", activeRecord?.sugar ?: "", Modifier.weight(1f)) { sugar -> updateActiveRecord { it.copy(sugar = sugar) } }
                                MatrixInput("Urine Pus Cells", activeRecord?.pus ?: "", Modifier.weight(1f)) { pus -> updateActiveRecord { it.copy(pus = pus) } }
                            }
                        }

                        2 -> {
                            // Section 3: Notes
                            Text(
                                "CLINICAL EVOLUTION & PROGRESS NOTES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = patient.notes,
                                onValueChange = { notes -> onStateChange { it.copy(notes = notes) } },
                                label = { Text("Evolution / Care Instructions / Plan") },
                                placeholder = { Text("E.g. Schedule Doppler weekly, check blood pressure, monitor fetal kicks...") },
                                minLines = 6,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListItemSeparatorHeader(title: String) {
    Spacer(modifier = Modifier.padding(top = 4.dp))
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun MatrixInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            focusedContainerColor = Color.Transparent
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PdfStyleDialog(
    currentFontSize: Float,
    currentAccentColor: String,
    onDismiss: () -> Unit,
    onSave: (fontSize: Float, accentColorHex: String) -> Unit
) {
    var selectedFontSize by remember { mutableFloatStateOf(currentFontSize) }
    var selectedAccentColor by remember { mutableStateOf(currentAccentColor) }

    val fontSizesList = listOf(
        Triple("Small (8.0pt)", 8.0f, "Compact grids for maximum detail overlap"),
        Triple("Medium (9.5pt)", 9.5f, "Default size optimized for readability"),
        Triple("Large (11.0pt)", 11.0f, "Enlarged fonts for visual accessibility")
    )

    val colorsList = listOf(
        Pair("Medical Blue", "#005B94"),
        Pair("Dark Charcoal", "#1F2022"),
        Pair("Teal Green", "#00796B"),
        Pair("Burgundy", "#880E4F"),
        Pair("Forest Pine", "#1B5E20"),
        Pair("Royal Purple", "#4A148C")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    "PDF Styling Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Configure how your generated inpatient rounds sheets are printed and styled format parameters dynamically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // 1. Font Size Heading
                Text(
                    "PDF PRINT FONT SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    fontSizesList.forEach { (label, value, desc) ->
                        val isSelected = selectedFontSize == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            onClick = { selectedFontSize = value },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFontSize = value }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 2. Primary Accent Color Heading
                Text(
                    "PDF HEADER ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colorsList.forEach { (colorName, hexCode) ->
                        val isSelected = selectedAccentColor.equals(hexCode, ignoreCase = true)
                        val colorVal = parseColorHex(hexCode)
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            onClick = { selectedAccentColor = hexCode },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colorVal)
                                )
                                Text(
                                    text = colorName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Soft Preview Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(parseColorHex(selectedAccentColor))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PREVIEW BANNER (${selectedFontSize}pt font)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedFontSize, selectedAccentColor) }
            ) {
                Text("APPLY STYLING")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

fun parseColorHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF005B94)
    }
}

@Composable
fun ImportPatientDialog(
    onDismiss: () -> Unit,
    onImport: (List<PatientEntity>) -> Unit,
    activePostId: Int?
) {
    var codeText by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    val parsedPatients = remember(codeText) {
        if (codeText.isNotBlank()) {
            com.example.utils.PatientSharing.deserializeList(codeText)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Import Shared Patient Data")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Paste the shared patient profile JSON code (single patient or complete list) below to add them to your clinical rounds list.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Paste action
                Button(
                    onClick = {
                        val text = clipboardManager.getText()?.text
                        if (!text.isNullOrBlank()) {
                            codeText = text
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Paste Clipboard",
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text("Auto-Paste from Clipboard", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Patient Data Code (JSON)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    placeholder = { Text("Paste JSON here...") }
                )

                if (parsedPatients != null && parsedPatients.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp), 
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "✔ Valid Patient Profile(s) found: ${parsedPatients.size} patient(s)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            parsedPatients.take(3).forEach { p ->
                                Text(
                                    "• ${p.name.ifEmpty { "Unnamed Patient" }}${if (p.diagnosis.isNotEmpty()) " (${p.diagnosis})" else ""}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (parsedPatients.size > 3) {
                                Text(
                                    "• and ${parsedPatients.size - 3} more...",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (codeText.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "❌ Invalid patient code. Please make sure you copied the correct JSON block from the other user.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedPatients?.let {
                        val activeId = activePostId ?: 0
                        onImport(it.map { p -> p.copy(postId = activeId) })
                    }
                },
                enabled = parsedPatients != null && parsedPatients.isNotEmpty() && activePostId != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Import (${parsedPatients?.size ?: 0})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

