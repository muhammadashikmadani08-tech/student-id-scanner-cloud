package com.institution.idscanner.cloud

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.institution.idscanner.cloud.auth.AuthManager
import com.institution.idscanner.cloud.data.AttendanceRecord
import com.institution.idscanner.cloud.data.CloudRepository
import com.institution.idscanner.cloud.data.CloudSettings
import com.institution.idscanner.cloud.data.ScanHistory
import com.institution.idscanner.cloud.data.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val repo: CloudRepository,
    private val settings: CloudSettings,
    private val auth: AuthManager,
    private val context: Context
) : ViewModel() {
    val students: StateFlow<List<Student>> = repo.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history: StateFlow<List<ScanHistory>> = repo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val institutionName = settings.institutionName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val adminPin = settings.adminPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1234")

    /** The signed-in Google account's email (read-only; it is the account). */
    val currentUserEmail: StateFlow<String> = auth.currentUser
        .map { it?.email.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), auth.email.orEmpty())

    /** Attendance records for one student, oldest first. */
    fun attendanceFor(studentId: String): Flow<List<AttendanceRecord>> =
        repo.attendanceFor(studentId)

    suspend fun markAttendance(studentId: String) {
        repo.markAttendance(studentId)
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    val searchResults: StateFlow<List<Student>> = MutableStateFlow(emptyList())

    fun setQuery(q: String) {
        _query.value = q
        viewModelScope.launch {
            (searchResults as MutableStateFlow).value = repo.search(q).first()
        }
    }

    /** Returns the student (or null). On a hit, records both the scan
     *  history entry and today's attendance. */
    suspend fun lookupScanned(raw: String): Student? {
        val student = repo.findByScanned(raw)
        if (student != null) {
            repo.recordScan(student.studentId)
            repo.markAttendance(student.studentId)
        }
        return student
    }

    suspend fun getStudent(id: String) = repo.findByScanned(id)

    /**
     * Saves a student. Pass [newPhotoUri] when the user just picked a photo:
     * it is uploaded to Firebase Storage if available, otherwise saved
     * locally on this phone (see CloudRepository.upsert).
     */
    fun saveStudent(student: Student, newPhotoUri: Uri? = null) = viewModelScope.launch {
        repo.upsert(student, newPhotoUri, context)
    }

    fun deleteStudent(student: Student) = viewModelScope.launch {
        repo.delete(student, context)
    }

    fun importStudents(list: List<Student>) = viewModelScope.launch { repo.upsertAll(list) }
    fun deleteHistory(id: String) = viewModelScope.launch { repo.deleteHistory(id) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }
    fun setInstitutionName(name: String) = viewModelScope.launch { settings.setInstitutionName(name) }
    fun setAdminPin(pin: String) = viewModelScope.launch { settings.setAdminPin(pin) }

    fun signOut() = viewModelScope.launch { auth.signOut() }
}
