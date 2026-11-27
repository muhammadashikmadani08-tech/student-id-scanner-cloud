package com.institution.idscanner.cloud.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel
import com.institution.idscanner.cloud.data.Student

@Composable
fun StudentFormScreen(vm: AppViewModel, existing: Student?, onDone: () -> Unit) {
    var id by remember { mutableStateOf(existing?.studentId ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var father by remember { mutableStateOf(existing?.fatherName ?: "") }
    var mother by remember { mutableStateOf(existing?.motherName ?: "") }
    var className by remember { mutableStateOf(existing?.className ?: "") }
    var roll by remember { mutableStateOf(existing?.roll ?: "") }
    var session by remember { mutableStateOf(existing?.session ?: "") }
    var section by remember { mutableStateOf(existing?.section ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var guardianEmail by remember { mutableStateOf(existing?.guardianEmail ?: "") }
    var photoUrl by remember { mutableStateOf(existing?.photoUrl ?: "") }
    var pickedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf("") }

    // A picked photo is kept as its picker Uri and saved when the form
    // is saved: uploaded to Firebase Storage if available, otherwise
    // copied into internal storage on this phone only (it will not
    // sync to other phones in this build).
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            pickedPhotoUri = uri
            photoUrl = uri.toString()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(if (existing == null) "Add Student" else "Edit Student", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        @Composable
        fun Field(v: String, onV: (String) -> Unit, label: String, enabled: Boolean = true) {
            OutlinedTextField(value = v, onValueChange = onV, label = { Text(label) }, enabled = enabled, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        Field(id, { id = it }, "Student ID *", enabled = existing == null)
        Field(name, { name = it }, "নাম *")
        Field(father, { father = it }, "পিতার নাম")
        Field(mother, { mother = it }, "মাতার নাম")
        Field(className, { className = it }, "শ্রেণি")
        Field(roll, { roll = it }, "রোল")
        Field(session, { session = it }, "সেশন")
        Field(section, { section = it }, "বিভাগ/শাখা")
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("গার্ডিয়ানের মোবাইল নাম্বার") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "গার্ডিয়ান অ্যাপে OTP লগইনের জন্য এই নম্বরটি ব্যবহার হবে (মা অথবা বাবা — একটি নম্বর দিন)।",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = guardianEmail, onValueChange = { guardianEmail = it },
            label = { Text("গার্ডিয়ানের ইমেইল (লগইনের জন্য)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "মাদ্রাসা থেকে গার্ডিয়ানকে এই ইমেইল ও পাসওয়ার্ড দেওয়া হবে; গার্ডিয়ান অ্যাপে এই ইমেইল দিয়ে লগইন করবেন। খালি রাখা যাবে।",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { photoPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text(if (photoUrl.isBlank()) "ছবি বেছে নিন (ঐচ্ছিক)" else "ছবি পরিবর্তন করুন")
        }
        if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (id.isBlank() || name.isBlank()) { error = "Student ID ও নাম অবশ্যই দিতে হবে।"; return@Button }
                // Guardian email is optional; when given it must be a
                // plausible address: trimmed + lowercased, with "@" and
                // a "." after the "@". An invalid value blocks the save
                // so no broken email-keyed guardian link is created.
                val normalisedEmail = guardianEmail.trim().lowercase()
                val atIndex = normalisedEmail.indexOf("@")
                val emailValid = normalisedEmail.isEmpty() ||
                    (atIndex > 0 && normalisedEmail.indexOf(".", atIndex) > atIndex + 1)
                if (!emailValid) { error = "সঠিক ইমেইল দিন (যেমন: example@gmail.com) অথবা খালি রাখুন।"; return@Button }
                // Keep the existing cloud URL when no new photo was picked;
                // a just-picked photo is uploaded by the ViewModel and its
                // URL replaces the temporary picker Uri in the saved record.
                val student = Student(
                    studentId = id.trim(),
                    name = name.trim(),
                    fatherName = father.trim(),
                    motherName = mother.trim(),
                    className = className.trim(),
                    roll = roll.trim(),
                    session = session.trim(),
                    section = section.trim(),
                    phone = phone.trim(),
                    photoUrl = existing?.photoUrl ?: "",
                    guardianEmail = normalisedEmail
                )
                vm.saveStudent(student, pickedPhotoUri)
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save করুন") }
    }
}

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    studentId: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onAttendance: () -> Unit = {}
) {
    val students by vm.students.collectAsState()
    val institutionName by vm.institutionName.collectAsState()
    val student = students.firstOrNull { it.studentId == studentId }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (student == null) {
            Text("Student পাওয়া যায়নি।", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("ফিরে যান") }
        } else {
            Text("Student Profile", style = MaterialTheme.typography.headlineMedium)
            StudentDetailCard(student, institutionName)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAttendance, modifier = Modifier.fillMaxWidth()) { Text("সাপ্তাহিক হাজিরা দেখুন") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit করুন (Admin)") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.deleteStudent(student); onDelete() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Delete করুন (Admin)") }
        }
    }
}
