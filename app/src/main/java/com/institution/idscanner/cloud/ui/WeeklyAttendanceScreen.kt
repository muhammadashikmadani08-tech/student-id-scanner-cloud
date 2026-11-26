package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel
import com.institution.idscanner.cloud.data.CloudRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val DHAKA_TZ: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")
private val DAY_NAMES = listOf(
    "শনিবার", "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার"
)

private data class WeekDay(
    val dayName: String,
    val dateMillis: Long,
    val dateString: String
)

@Composable
fun WeeklyAttendanceScreen(vm: AppViewModel, studentId: String) {
    val students by vm.students.collectAsState()
    val records by vm.attendanceFor(studentId).collectAsState(initial = emptyList())
    val student = students.firstOrNull { it.studentId == studentId }
    var weekOffset by remember { mutableStateOf(0) }

    val now = Calendar.getInstance(DHAKA_TZ)
    val todayString = CloudRepository.dateString(now.timeInMillis)
    val nowHour = now.get(Calendar.HOUR_OF_DAY)

    // Saturday is the first day of the week. weekOffset 0 = current week.
    val weekDays = remember(weekOffset) {
        val start = Calendar.getInstance(DHAKA_TZ)
        val back = (start.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY + 7) % 7
        start.add(Calendar.DAY_OF_YEAR, -back + weekOffset * 7)
        (0..6).map { i ->
            val day = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            WeekDay(
                dayName = DAY_NAMES[i],
                dateMillis = day.timeInMillis,
                dateString = CloudRepository.dateString(day.timeInMillis)
            )
        }
    }
    val byDate = remember(records) { records.associateBy { it.date } }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { timeZone = DHAKA_TZ } }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = DHAKA_TZ } }
    val weekLabel = if (weekDays.isNotEmpty()) {
        "${dateFmt.format(Date(weekDays.first().dateMillis))} — ${dateFmt.format(Date(weekDays.last().dateMillis))}"
    } else ""

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("সাপ্তাহিক হাজিরা", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (student == null) {
            Text("Student পাওয়া যায়নি।")
            return
        }
        Text(student.name, style = MaterialTheme.typography.titleLarge)
        Text("শ্রেণি: ${student.className}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { weekOffset -= 1 },
                modifier = Modifier.weight(1f)
            ) { Text("← আগের সপ্তাহ") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { weekOffset += 1 },
                modifier = Modifier.weight(1f)
            ) { Text("পরের সপ্তাহ →") }
        }
        Spacer(Modifier.height(8.dp))
        Text(weekLabel, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(weekDays) { day ->
                val record = byDate[day.dateString]
                val (statusText, statusColor) = when {
                    record != null -> {
                        val hourCal = Calendar.getInstance(DHAKA_TZ).apply { timeInMillis = record.scannedAt }
                        val session = if (hourCal.get(Calendar.HOUR_OF_DAY) < 12) "সকাল" else "বিকেল"
                        "উপস্থিত • ${timeFmt.format(Date(record.scannedAt))} ($session)" to Color(0xFF2E7D32)
                    }
                    day.dateString < todayString -> "অনুপস্থিত" to Color(0xFFC62828)
                    day.dateString == todayString && nowHour >= 20 -> "অনুপস্থিত" to Color(0xFFC62828)
                    else -> "এখনো বাকি" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(day.dayName, style = MaterialTheme.typography.titleMedium)
                            Text(dateFmt.format(Date(day.dateMillis)), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(statusText, color = statusColor, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "প্রতিদিন রাত ৮টার মধ্যে QR স্ক্যান করলে উপস্থিত হবে; স্ক্যান না হলে অনুপস্থিত দেখাবে।",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
