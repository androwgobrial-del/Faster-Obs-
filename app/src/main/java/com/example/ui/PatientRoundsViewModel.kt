package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PatientEntity
import com.example.data.PostEntity
import com.example.data.PatientRepository
import com.example.utils.PdfGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class OpenPdf(val fileUri: Uri) : UiEvent
    data class SharePdf(val fileUri: Uri) : UiEvent
}

class PatientRoundsViewModel(private val repository: PatientRepository) : ViewModel() {

    // List of posts observed reactively from Room
    val posts: StateFlow<List<PostEntity>> = repository.allPosts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // List of all patient records observed reactively from Room for historical trends
    val allPatients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected Post ID
    private val _selectedPostId = MutableStateFlow<Int?>(null)
    val selectedPostId: StateFlow<Int?> = _selectedPostId.asStateFlow()

    // Current selected Post Entity
    val selectedPost: StateFlow<PostEntity?> = combine(posts, _selectedPostId) { postList, postId ->
        postList.find { it.id == postId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // List of patients for the selected post
    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<PatientEntity>> = _selectedPostId
        .flatMapLatest { postId ->
            if (postId == null) {
                flowOf(emptyList())
            } else {
                repository.getPatientsForPost(postId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var isGaUpdated = false

    init {
        // Automatically select the first post when the list loads for the first time
        viewModelScope.launch {
            posts.collect { list ->
                if (_selectedPostId.value == null && list.isNotEmpty()) {
                    _selectedPostId.value = list.first().id
                }
            }
        }
        triggerAutoGaUpdates()
    }

    private fun triggerAutoGaUpdates() {
        viewModelScope.launch {
            if (isGaUpdated) return@launch
            isGaUpdated = true
            try {
                val patientList = repository.allPatients.first()
                for (patient in patientList) {
                    if (patient.lmp.isNotEmpty() || patient.edd.isNotEmpty()) {
                        val currentGa = com.example.utils.GestationalAgeCalculator.calculateGestationalAge(patient.lmp, patient.edd) ?: ""
                        if (currentGa.isNotEmpty()) {
                            val newDiag = com.example.utils.GestationalAgeCalculator.updateGaInDiagnosis(patient.diagnosis, currentGa, forceAppend = false)
                            if (newDiag != patient.diagnosis) {
                                repository.updatePatient(patient.copy(diagnosis = newDiag))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Current Patient being edited/created. Null means form not displayed.
    private val _editingPatient = MutableStateFlow<PatientEntity?>(null)
    val editingPatient: StateFlow<PatientEntity?> = _editingPatient.asStateFlow()

    // PDF Print Preferences state
    private val _pdfFontSize = MutableStateFlow(9.5f)
    val pdfFontSize: StateFlow<Float> = _pdfFontSize.asStateFlow()

    private val _pdfAccentColor = MutableStateFlow("#005B94")
    val pdfAccentColor: StateFlow<String> = _pdfAccentColor.asStateFlow()

    // Shared Flow for one-time side-effects
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    fun selectPost(postId: Int) {
        _selectedPostId.value = postId
    }

    fun createPost(title: String) {
        viewModelScope.launch {
            if (title.trim().isEmpty()) {
                _eventFlow.emit(UiEvent.ShowToast("Post title cannot be empty"))
                return@launch
            }
            val newPost = PostEntity(title = title.trim())
            val newId = repository.insertPost(newPost)
            _selectedPostId.value = newId.toInt()
            _eventFlow.emit(UiEvent.ShowToast("Post '$title' created!"))
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch {
            repository.deletePost(post)
            if (_selectedPostId.value == post.id) {
                _selectedPostId.value = posts.value.firstOrNull { it.id != post.id }?.id
            }
            _eventFlow.emit(UiEvent.ShowToast("Post deleted"))
        }
    }

    fun renamePost(post: PostEntity, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.trim().isEmpty()) {
                _eventFlow.emit(UiEvent.ShowToast("Post title cannot be empty"))
                return@launch
            }
            repository.updatePost(post.copy(title = newTitle.trim()))
            _eventFlow.emit(UiEvent.ShowToast("Post renamed to '$newTitle'"))
        }
    }

    fun loadPreferences(context: Context) {
        val prefs = context.getSharedPreferences("pdf_preferences", Context.MODE_PRIVATE)
        _pdfFontSize.value = prefs.getFloat("font_size", 9.5f)
        _pdfAccentColor.value = prefs.getString("accent_color", "#005B94") ?: "#005B94"
    }

    fun updatePdfPreferences(context: Context, fontSize: Float, accentColorHex: String) {
        _pdfFontSize.value = fontSize
        _pdfAccentColor.value = accentColorHex
        context.getSharedPreferences("pdf_preferences", Context.MODE_PRIVATE)
            .edit()
            .putFloat("font_size", fontSize)
            .putString("accent_color", accentColorHex)
            .apply()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowToast("Style settings applied successfully!"))
        }
    }

    fun startCreatingPatient() {
        val currentPostId = _selectedPostId.value
        if (currentPostId == null) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Please create a Post first!"))
            }
            return
        }
        _editingPatient.value = PatientEntity(postId = currentPostId)
    }

    fun startEditingPatient(patient: PatientEntity) {
        _editingPatient.value = patient
    }

    fun stopEditing() {
        _editingPatient.value = null
    }

    fun updateEditingState(updater: (PatientEntity) -> PatientEntity) {
        val current = _editingPatient.value ?: return
        _editingPatient.value = updater(current)
    }

    fun savePatient() {
        val patient = _editingPatient.value ?: return
        viewModelScope.launch {
            val currentGa = com.example.utils.GestationalAgeCalculator.calculateGestationalAge(patient.lmp, patient.edd) ?: ""
            val finalDiag = if (currentGa.isNotEmpty()) {
                com.example.utils.GestationalAgeCalculator.updateGaInDiagnosis(patient.diagnosis, currentGa, forceAppend = false)
            } else {
                patient.diagnosis
            }
            val finalPatient = patient.copy(diagnosis = finalDiag)

            if (finalPatient.id == 0) {
                repository.insertPatient(finalPatient)
                _eventFlow.emit(UiEvent.ShowToast("Patient record added!"))
            } else {
                repository.updatePatient(finalPatient)
                _eventFlow.emit(UiEvent.ShowToast("Patient record updated!"))
            }
            _editingPatient.value = null
        }
    }

    fun importPatients(patientsList: List<PatientEntity>) {
        if (patientsList.isEmpty()) return
        viewModelScope.launch {
            try {
                patientsList.forEach { patient ->
                    val currentGa = com.example.utils.GestationalAgeCalculator.calculateGestationalAge(patient.lmp, patient.edd) ?: ""
                    val finalDiag = if (currentGa.isNotEmpty()) {
                        com.example.utils.GestationalAgeCalculator.updateGaInDiagnosis(patient.diagnosis, currentGa, forceAppend = false)
                    } else {
                        patient.diagnosis
                    }
                    val finalPatient = patient.copy(diagnosis = finalDiag, id = 0, createdAt = System.currentTimeMillis())
                    repository.insertPatient(finalPatient)
                }
                _eventFlow.emit(UiEvent.ShowToast("Imported ${patientsList.size} patient record(s) successfully!"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowToast("Failed to import patients: ${e.message}"))
            }
        }
    }

    fun deletePatient(patient: PatientEntity) {
        viewModelScope.launch {
            repository.deletePatient(patient)
            _eventFlow.emit(UiEvent.ShowToast("Patient deleted successfully"))
        }
    }

    fun movePatientUp(patient: PatientEntity) {
        val currentList = patients.value
        val index = currentList.indexOfFirst { it.id == patient.id }
        if (index <= 0) return
        val targetPatient = currentList[index - 1]
        viewModelScope.launch {
            try {
                val patientTime = patient.createdAt
                val targetTime = targetPatient.createdAt
                val newPatientTime = if (patientTime == targetTime) targetTime + 1 else targetTime
                val newTargetTime = if (patientTime == targetTime) patientTime - 1 else patientTime
                repository.updatePatient(patient.copy(createdAt = newPatientTime))
                repository.updatePatient(targetPatient.copy(createdAt = newTargetTime))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun movePatientDown(patient: PatientEntity) {
        val currentList = patients.value
        val index = currentList.indexOfFirst { it.id == patient.id }
        if (index == -1 || index >= currentList.size - 1) return
        val targetPatient = currentList[index + 1]
        viewModelScope.launch {
            try {
                val patientTime = patient.createdAt
                val targetTime = targetPatient.createdAt
                val newPatientTime = if (patientTime == targetTime) targetTime - 1 else targetTime
                val newTargetTime = if (patientTime == targetTime) patientTime + 1 else patientTime
                repository.updatePatient(patient.copy(createdAt = newPatientTime))
                repository.updatePatient(targetPatient.copy(createdAt = newTargetTime))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePatientPrintSelection(patient: PatientEntity) {
        viewModelScope.launch {
            repository.updateSelection(patient.id, !patient.isSelectedForPrint)
        }
    }

    fun selectAllPatientsForPrint(select: Boolean) {
        val postId = _selectedPostId.value ?: return
        viewModelScope.launch {
            repository.updateAllSelectionsForPost(postId, select)
        }
    }

    fun clearAllPatients() {
        val postId = _selectedPostId.value ?: return
        viewModelScope.launch {
            repository.deleteAllPatientsForPost(postId)
            _eventFlow.emit(UiEvent.ShowToast("All patient records deleted"))
        }
    }

    fun generateAndOpenPdf(context: Context) {
        val currentList = patients.value
        val printList = currentList.filter { it.isSelectedForPrint }
        if (printList.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Please select at least 1 patient to print!"))
            }
            return
        }

        val activePost = selectedPost.value ?: return

        viewModelScope.launch {
            try {
                val pdfFile = PdfGenerator.generatePatientRoundsPdf(
                    context = context, 
                    postTitle = activePost.title,
                    patients = currentList,
                    fontSize = _pdfFontSize.value,
                    accentColorHex = _pdfAccentColor.value
                )
                val authority = "${context.packageName}.fileprovider"
                val fileUri = FileProvider.getUriForFile(context, authority, pdfFile)
                _eventFlow.emit(UiEvent.OpenPdf(fileUri))
            } catch (e: Exception) {
                e.printStackTrace()
                _eventFlow.emit(UiEvent.ShowToast("Failed to generate PDF: ${e.localizedMessage}"))
            }
        }
    }

    fun generateAndSharePdf(context: Context) {
        val currentList = patients.value
        val printList = currentList.filter { it.isSelectedForPrint }
        if (printList.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Please select at least 1 patient to print!"))
            }
            return
        }

        val activePost = selectedPost.value ?: return

        viewModelScope.launch {
            try {
                val pdfFile = PdfGenerator.generatePatientRoundsPdf(
                    context = context, 
                    postTitle = activePost.title,
                    patients = currentList,
                    fontSize = _pdfFontSize.value,
                    accentColorHex = _pdfAccentColor.value
                )
                val authority = "${context.packageName}.fileprovider"
                val fileUri = FileProvider.getUriForFile(context, authority, pdfFile)
                _eventFlow.emit(UiEvent.SharePdf(fileUri))
            } catch (e: Exception) {
                e.printStackTrace()
                _eventFlow.emit(UiEvent.ShowToast("Failed to share PDF: ${e.localizedMessage}"))
            }
        }
    }
}

class PatientRoundsViewModelFactory(private val repository: PatientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientRoundsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientRoundsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
