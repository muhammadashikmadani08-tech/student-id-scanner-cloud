package com.institution.idscanner.cloud.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel
import com.institution.idscanner.cloud.data.CloudRepository
import com.institution.idscanner.cloud.data.Holiday
import com.institution.idscanner.cloud.util.AttendanceCsv
import com.institution.idscanner.cloud.util.CsvUtil
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

/** UTF-8 byte-order mark, so Excel shows Bengali text correctly. */
private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

@Composable
fun AdminScreen(
    vm: AppViewModel,
    onStudents: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pin by vm.adminPin.collectAsState()
    var entered by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val students by vm.students.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { vm.importStudents(CsvUtil.parse(it)) }
                message = "CSV import সম্পন্ন হয়েছে। ডাটা ক্লাউডে জমা হয়েছে।"
            }.onFailure { message = "Import ব্যর্থ: ${it.message}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(CsvUtil.toCsv(students).toByteArray(Charsets.UTF_8))
                }
                message = "CSV export সম্পন্ন হয়েছে।"
            }.onFailure { message = "Export ব্যর্থ: ${it.message}" }
        }
    }
    val dayAttendanceExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                val csv = AttendanceCsv.dayCsv(students, vm.getAllAttendance())
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(UTF8_BOM)
                    it.write(csv.toByteArray(Charsets.UTF_8))
                }
                message = "আজকের হাজিরা export সম্পন্ন হয়েছে।"
            }.onFailure { message = "Export ব্যর্থ: ${it.message}" }
        }
    }
    val weekAttendanceExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                val csv = AttendanceCsv.weekCsv(students, vm.getAllAttendance())
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(UTF8_BOM)
                    it.write(csv.toByteArray(Charsets.UTF_8))
                }
                message = "সাপ্তাহিক হাজিরা export সম্পন্ন হয়েছে।"
            }.onFailure { message = "Export ব্যর্থ: ${it.message}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Admin Section", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (!unlocked) {
            Text("Admin PIN দিন (ডিফল্ট: 1234)। Settings থেকে PIN বদলানো যায়।")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = entered, onValueChange = { entered = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { unlocked = entered == pin; if (!unlocked) message = "ভুল PIN।" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unlock করুন") }
            if (message.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(message) }
        } else {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add Student") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onStudents, modifier = Modifier.fillMaxWidth()) { Text("Students দেখুন / Edit / Delete") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("CSV/Excel (CSV) থেকে Import করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { exportLauncher.launch("students.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text("CSV-এ Export করুন")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { dayAttendanceExportLauncher.launch("attendance-${CloudRepository.dateString()}.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text("আজকের হাজিরা Export করুন (CSV)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { weekAttendanceExportLauncher.launch("attendance-week-${AttendanceCsv.weekStartDate()}.csv") }, modifier = Modifier.fillMaxWidth()) {
                Text("সাপ্তাহিক হাজিরা Export করুন (CSV)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text("প্রতিষ্ঠানের নাম ও PIN পরিবর্তন করুন")
            }
            if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message) }
            Spacer(Modifier.height(10.dp))
            Text(
                "এই ক্লাউড ভার্সনে ডাটা Firebase-এ জমা থাকে, তাই আলাদা Database Backup/Restore লাগে না — একই Gmail দিয়ে লগইন করলেই সব ফোনে একই ডাটা পাওয়া যায়। Excel ফাইল সরাসরি নয় — Excel-এ File > Save As > CSV UTF-8 করে import করুন।",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, onDone: () -> Unit) {
    // PIN-gated: this screen is only reachable from AdminScreen, after
    // the Admin PIN has been entered (see AppNav "settings" route).
    val institutionName by vm.institutionName.collectAsState()
    val pin by vm.adminPin.collectAsState()
    val signedInEmail by vm.currentUserEmail.collectAsState()
    val logoPath by vm.logoPath.collectAsState()
    val holidays by vm.holidays.collectAsState()
    val scope = rememberCoroutineScope()
    var holidayDate by remember { mutableStateOf("") }
    var holidayTitle by remember { mutableStateOf("") }
    var holidayMessage by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf(true) }
    if (first && institutionName.isNotBlank()) { name = institutionName; first = false }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.saveLogo(uri)
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("লগইন করা অ্যাকাউন্ট (এই অ্যাকাউন্টের ডাটাই দেখা যাচ্ছে):")
        Spacer(Modifier.height(4.dp))
        Text(
            if (signedInEmail.isBlank()) "লগইন করা হয়নি" else signedInEmail,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "অ্যাকাউন্ট বদলাতে লগআউট করে অন্য Gmail দিয়ে লগইন করুন। প্রতিটি Gmail-এর ডাটা সম্পূর্ণ আলাদা থাকে।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Text("প্রতিষ্ঠানের নাম (ওয়েলকাম পেইজ ও Home Screen-এ দেখাবে)।")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("প্রতিষ্ঠানের নাম") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("প্রতিষ্ঠানের লোগো (ওয়েলকাম পেইজে দেখাবে)।")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!logoPath.isNullOrBlank()) {
                AsyncImage(
                    model = photoModel(logoPath!!), contentDescription = "বর্তমান লোগো",
                    modifier = Modifier.size(72.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.QrCodeScanner, contentDescription = null,
                    modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                OutlinedButton(onClick = { logoPicker.launch("image/*") }) { Text("লোগো বদলান") }
                if (!logoPath.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { vm.clearLogo() }) { Text("লোগো মুছুন") }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "লোগো শুধু এই ফোনে দেখাবে — অন্য ফোনে যাবে না। নাম বদলালে সব ফোনে বদলাবে।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text("বর্তমান Admin PIN: $pin")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newPin, onValueChange = { newPin = it }, label = { Text("নতুন PIN (4 সংখ্যা)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text("ছুটির দিনের তালিকা", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "ছুটির দিনগুলো এখানে যোগ করুন। একই তারিখে আবার যোগ করলে আগের ছুটির নাম বদলে যাবে।",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        if (holidays.isEmpty()) {
            Text("কোনো ছুটির দিন যোগ করা হয়নি।", style = MaterialTheme.typography.bodyMedium)
        } else {
            holidays.forEach { holiday ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${holiday.date} — ${holiday.title}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching { vm.deleteHoliday(holiday.date) }
                                    .onFailure { holidayMessage = "মুছতে ব্যর্থ: ${it.message}" }
                            }
                        }
                    ) { Text("মুছুন") }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = holidayDate, onValueChange = { holidayDate = it },
            label = { Text("তারিখ (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text("যেমন: 2026-12-25", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = holidayTitle, onValueChange = { holidayTitle = it },
            label = { Text("ছুটির নাম/কারণ") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val date = holidayDate.trim()
                val title = holidayTitle.trim()
                when {
                    !isValidHolidayDate(date) -> holidayMessage = "সঠিক তারিখ দিন (yyyy-MM-dd, যেমন: 2026-12-25)।"
                    title.isBlank() -> holidayMessage = "ছুটির নাম/কারণ দিতে হবে।"
                    else -> {
                        vm.addHoliday(Holiday(date = date, title = title))
                        holidayDate = ""
                        holidayTitle = ""
                        holidayMessage = "ছুটি যোগ করা হয়েছে।"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("ছুটি যোগ করুন") }
        if (holidayMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(holidayMessage, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) vm.setInstitutionName(name.trim())
                if (newPin.length >= 4) vm.setAdminPin(newPin.trim())
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save করুন") }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { vm.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("লগআউট করুন (Google)") }
        Spacer(Modifier.height(8.dp))
        Text(
            "লগআউট করলে ফোনের ডাটা মুছে যায় না — আবার লগইন করলে ক্লাউড থেকে সব ফিরে আসবে।",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** True only for a real calendar date written as yyyy-MM-dd. */
private fun isValidHolidayDate(value: String): Boolean {
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)) return false
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value) != null
    } catch (_: Exception) {
        false
    }
}
