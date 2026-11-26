package com.institution.idscanner.cloud.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.institution.idscanner.cloud.auth.AuthManager
import kotlinx.coroutines.launch

/**
 * Shown first whenever nobody is signed in. Real Google sign-in: the
 * signed-in Gmail's Firebase UID becomes the User ID for all cloud data.
 */
@Composable
fun LoginScreen(auth: AuthManager) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        busy = true
        scope.launch {
            error = auth.handleSignInResult(result.data).orEmpty()
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎓", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Student ID Scanner Cloud",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "আপনার Gmail দিয়ে লগইন করুন। একই Gmail দিয়ে যেকোনো ফোনে লগইন করলে একই ছাত্র, হাজিরা ও হিস্ট্রি পাওয়া যাবে।",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        if (busy) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val intent = auth.signInIntent()
                    if (intent == null) {
                        error = "Google লগইন সেটআপ হয়নি। আসল google-services.json ফাইলটি অ্যাপে বসাতে হবে (README-CLOUD.md দেখুন)।"
                    } else {
                        launcher.launch(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Google দিয়ে লগইন করুন") }
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "লগইনের জন্য ইন্টারনেট প্রয়োজন। লগইন করা থাকলে আগের সিঙ্ক করা ডাটা অফলাইনেও দেখা যায়।",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
