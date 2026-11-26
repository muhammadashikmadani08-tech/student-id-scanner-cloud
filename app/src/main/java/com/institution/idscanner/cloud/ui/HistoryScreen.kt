package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: AppViewModel, onProfile: (String) -> Unit) {
    val history by vm.history.collectAsState()
    val students by vm.students.collectAsState()
    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Scan History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text("এখনো কোনো স্ক্যান হয়নি।")
        } else {
            OutlinedButton(onClick = { vm.clearHistory() }, modifier = Modifier.fillMaxWidth()) {
                Text("সব History মুছুন")
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(history) { entry ->
                    val student = students.firstOrNull { it.studentId == entry.studentId }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(student?.name ?: entry.studentId, style = MaterialTheme.typography.titleMedium)
                            Text("ID: ${entry.studentId}  •  ${fmt.format(Date(entry.scannedAt))}", style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedButton(onClick = { onProfile(entry.studentId) }) { Text("Profile") }
                        IconButton(onClick = { vm.deleteHistory(entry.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete history")
                        }
                    }
                }
            }
        }
    }
}
