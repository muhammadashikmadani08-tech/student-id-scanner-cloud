package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.institution.idscanner.cloud.data.Student
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/** Cloud photos are https:// URLs (Coil loads the string directly); a
 *  just-picked photo is a content:// URI string; legacy absolute file
 *  paths need a File for Coil. */
internal fun photoModel(stored: String): Any =
    if (stored.startsWith("/")) File(stored) else stored

@Composable
fun StudentInfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StudentCard(student: Student, institutionName: String, onClick: (() -> Unit)? = null) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (student.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoModel(student.photoUrl), contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(64.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text(
                            student.name.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(student.name, style = MaterialTheme.typography.titleMedium)
                Text("ID: ${student.studentId}", style = MaterialTheme.typography.bodyMedium)
                if (student.className.isNotBlank())
                    Text("শ্রেণি: ${student.className}  •  রোল: ${student.roll}", style = MaterialTheme.typography.bodyMedium)
                if (institutionName.isNotBlank())
                    Text(institutionName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
fun StudentDetailCard(
    student: Student,
    institutionName: String,
    scanTimeMillis: Long? = null,
    scanTimeFormat: SimpleDateFormat? = null
) {
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Student Found", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            if (scanTimeMillis != null && scanTimeFormat != null) {
                Spacer(Modifier.height(4.dp))
                Text("স্ক্যানের সময়: ${scanTimeFormat.format(Date(scanTimeMillis))}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            if (student.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoModel(student.photoUrl), contentDescription = "Student photo",
                    modifier = Modifier.size(120.dp).clip(CircleShape).align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(10.dp))
            }
            StudentInfoRow("Student ID", student.studentId)
            StudentInfoRow("নাম", student.name)
            StudentInfoRow("পিতার নাম", student.fatherName)
            StudentInfoRow("মাতার নাম", student.motherName)
            StudentInfoRow("শ্রেণি", student.className)
            StudentInfoRow("রোল", student.roll)
            StudentInfoRow("সেশন", student.session)
            StudentInfoRow("বিভাগ/শাখা", student.section)
            StudentInfoRow("মোবাইল", student.phone)
            StudentInfoRow("প্রতিষ্ঠান", institutionName)
        }
    }
}
