package ai.medray.staff.ui.common

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.medray.staff.core.util.FileUtils
import ai.medray.staff.core.util.SelectedFile
import ai.medray.staff.data.model.Patient
import ai.medray.staff.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun UploadDocumentDialog(
    patient: Patient,
    initialKind: String = "REPORT",
    visitId: String? = null,
    isUploading: Boolean = false,
    uploadError: String? = null,
    onDismiss: () -> Unit,
    onUpload: (SelectedFile, kind: String, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedKind by remember(initialKind) { mutableStateOf(initialKind) }
    var selectedFile by remember { mutableStateOf<SelectedFile?>(null) }
    var notes by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isReadingFile by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isReadingFile = true
            localError = null
            coroutineScope.launch {
                val file = FileUtils.readSelectedFile(context, uri)
                isReadingFile = false
                if (file != null) {
                    if (FileUtils.isSupportedMimeType(file.mimeType)) {
                        selectedFile = file
                    } else {
                        localError = "Unsupported file type (${file.mimeType}). Please use PDF, JPEG, PNG, or WebP."
                    }
                } else {
                    localError = "Failed to read the selected file."
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            isReadingFile = true
            localError = null
            coroutineScope.launch {
                val file = FileUtils.readSelectedFile(context, uri)
                isReadingFile = false
                if (file != null) {
                    selectedFile = file
                } else {
                    localError = "Failed to process photo from camera."
                }
            }
        }
        pendingCameraUri = null
    }

    fun launchCamera() {
        try {
            val uri = FileUtils.createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Upload Patient Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = patient.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MedRayBlueLight,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "UHID: ${patient.uhid}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayBluePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 12.dp))

                // Document Category Selection
                Text(
                    text = "Document Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CategoryChip(
                        title = "Lab Report",
                        icon = Icons.Outlined.Science,
                        isSelected = selectedKind == "REPORT",
                        onClick = { selectedKind = "REPORT" },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryChip(
                        title = "General",
                        icon = Icons.Outlined.Description,
                        isSelected = selectedKind == "GENERAL",
                        onClick = { selectedKind = "GENERAL" },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryChip(
                        title = "Prescription",
                        icon = Icons.Outlined.Medication,
                        isSelected = selectedKind == "PRESCRIPTION",
                        onClick = { selectedKind = "PRESCRIPTION" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Source Selection / File Preview
                Text(
                    text = "Select or Capture File",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedFile == null) {
                    // Two options: Browse files (PDF/Images) or Scan via Camera
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            enabled = !isUploading && !isReadingFile,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = "Browse Files",
                                    tint = MedRayBluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Browse Files",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                                Text(
                                    text = "PDF, PNG, JPEG",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { launchCamera() },
                            enabled = !isUploading && !isReadingFile,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDFA)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = "Take Photo",
                                    tint = MedRayTealDark,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Take Photo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayTealDark
                                )
                                Text(
                                    text = "Point & shoot scan",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0D9488)
                                )
                            }
                        }
                    }

                    if (isReadingFile) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MedRayBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing file…", fontSize = 12.sp, color = Slate600)
                        }
                    }
                } else {
                    // Selected file preview card
                    val file = selectedFile!!
                    val isPdf = file.mimeType.contains("pdf", ignoreCase = true)

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPdf) Color(0xFFFEE2E2) else Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPdf) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = if (isPdf) Color(0xFFDC2626) else MedRayBluePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (isPdf) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isPdf) "PDF" else "IMAGE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPdf) Color(0xFFDC2626) else MedRayBluePrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = FileUtils.formatFileSize(file.sizeBytes),
                                        fontSize = 12.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedFile = null },
                                enabled = !isUploading
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove file",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Error message banner if any
                val activeError = localError ?: uploadError
                if (activeError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = activeError, fontSize = 12.sp, color = Color(0xFFDC2626), lineHeight = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description (optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. CBC Blood Report from Lal PathLabs", fontSize = 12.sp) },
                    enabled = !isUploading,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MedRayBluePrimary,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite
                    ),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isUploading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val file = selectedFile
                            if (file != null) {
                                onUpload(file, selectedKind, notes.ifBlank { null })
                            }
                        },
                        enabled = selectedFile != null && !isUploading && !isReadingFile,
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Document", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) MedRayBlueLight else PureWhite,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MedRayBluePrimary else Slate200
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MedRayBluePrimary else Slate500,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MedRayBluePrimary else Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
