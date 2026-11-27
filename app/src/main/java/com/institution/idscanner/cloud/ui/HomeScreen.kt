package com.institution.idscanner.cloud.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.AppViewModel

@Composable
fun HomeScreen(vm: AppViewModel, onNavigate: (String) -> Unit) {
    val institutionName by vm.institutionName.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(12.dp))
        // Institution logo placeholder: replace the text below / add a logo image in
        // res/drawable and use Image() here to brand the app.
        Text("🎓", style = MaterialTheme.typography.displayMedium)
        Text(
            institutionName, style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text("Student ID Card Scanner", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onNavigate("scanner/$SCAN_MODE_ATTENDANCE") },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("  হাজিরা স্ক্যান", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onNavigate("scanner/$SCAN_MODE_INFO") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
            Text("  তথ্য দেখুন (শুধু তথ্য, হাজিরা হবে না)")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { onNavigate("search") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Search, contentDescription = null); Text("  Search Student")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { onNavigate("students") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.People, contentDescription = null); Text("  Students")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { onNavigate("history") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.History, contentDescription = null); Text("  Scan History")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { onNavigate("admin") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null); Text("  Admin")
        }
        Spacer(Modifier.height(20.dp))
        Text("ডাটা ক্লাউডে জমা থাকে — একই Gmail দিয়ে যেকোনো ফোনে লগইন করলে একই ডাটা পাওয়া যায়।", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}
