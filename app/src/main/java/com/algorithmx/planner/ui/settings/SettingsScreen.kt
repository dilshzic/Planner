package com.algorithmx.planner.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.algorithmx.planner.worker.CalendarSyncWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- Google Sign-In Config ---
    // TODO: REPLACE THIS STRING with your Web Client ID from Firebase Console -> Authentication -> Sign-in method -> Google
    val webClientId = "YOUR_WEB_CLIENT_ID_HERE"

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                Firebase.auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            Toast.makeText(context, "Welcome ${account.displayName}!", Toast.LENGTH_SHORT).show()
                            viewModel.setDebugError("") // Clear errors

                            // Trigger Initial Sync
                            val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
                            WorkManager.getInstance(context).enqueue(request)
                        } else {
                            val msg = "Firebase Auth Failed: ${authTask.exception?.message}"
                            viewModel.setDebugError(msg)
                            Toast.makeText(context, "Auth Failed", Toast.LENGTH_LONG).show()
                        }
                    }
            } catch (e: ApiException) {
                // IMPORTANT: This code tells you WHY it failed
                // 10 = Developer Error (Wrong Client ID or Missing SHA-1)
                // 12500 = SHA-1 Missing
                val errorMsg = "Google Sign-In Error: ${e.statusCode}"
                viewModel.setDebugError(errorMsg)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Debug Error Box ---
        if (!state.lastError.isNullOrEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = state.lastError!!, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // --- Account Section ---
        Text(text = "Account", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(16.dp))

                if (state.isAuthenticated) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = state.displayName ?: "User", style = MaterialTheme.typography.titleMedium)
                        Text(text = state.userEmail ?: "", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(text = "Not Signed In", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (state.isAuthenticated) {
                    OutlinedButton(onClick = { viewModel.signOut() }) {
                        Text("Sign Out")
                    }
                } else {
                    Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }) {
                        Text("Sign In with Google")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Sync Section ---
        Text(text = "Cloud & Data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Status: ${if (state.isAuthenticated) "Active" else "Offline Mode"}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                        Toast.makeText(context, "Syncing...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = state.isAuthenticated
                ) {
                    Text("Force Cloud Sync")
                }
            }
        }
    }
}