# Feature Specification: Secure Settings

**Feature Branch**: `3-secure-settings`
**Created**: 2025-11-05
**Status**: Draft
**Depends On**: Spec 1 (Architecture Foundation)

## User Scenarios & Testing

### User Story 1 - Initial Credential Configuration (Priority: P1)

As a new user, I need to configure my Napkin.one credentials (email and API token) so that I can start sending thoughts to my account.

**Why this priority**: Without credentials, the app cannot function. This is a prerequisite for using the core feature (sending thoughts).

**Independent Test**: Can be fully tested by launching app for first time, navigating to settings, entering valid credentials, saving, and verifying they persist across app restarts.

**Acceptance Scenarios**:

1. **Given** fresh app install, **When** user navigates to settings, **Then** email and token fields are empty
2. **Given** user enters email and token, **When** user taps Save, **Then** credentials are stored securely and success message displays
3. **Given** credentials are saved, **When** app is restarted, **Then** settings screen shows saved credentials (token masked)
4. **Given** user saves credentials, **When** navigating back to main screen, **Then** main screen can retrieve credentials for API calls

---

### User Story 2 - Credential Security and Privacy (Priority: P1)

As a user, I need my API credentials stored securely so that unauthorized apps or users cannot access my Napkin.one account.

**Why this priority**: Constitution mandates secure credential storage (EncryptedSharedPreferences). Critical for user trust and security.

**Independent Test**: Can be tested by examining SharedPreferences file directly and verifying contents are encrypted, not plain text.

**Acceptance Scenarios**:

1. **Given** credentials are saved, **When** examining app storage, **Then** SharedPreferences file contains encrypted data only
2. **Given** token is displayed in settings, **When** viewing token field, **Then** token is masked with bullets (e.g., "••••••••") unless user reveals it
3. **Given** app is uninstalled, **When** app is reinstalled, **Then** old credentials are not accessible (encryption key destroyed)

---

### User Story 3 - Credential Updating (Priority: P2)

As an existing user, I need to update my credentials when I change my Napkin.one API token so that I can continue using the app.

**Why this priority**: Users may need to rotate tokens for security. Important but less critical than initial setup.

**Independent Test**: Can be tested by changing saved credentials, verifying new values are used in subsequent API calls.

**Acceptance Scenarios**:

1. **Given** existing credentials, **When** user updates token and saves, **Then** new token is used for future API calls
2. **Given** user updates email, **When** save completes, **Then** new email is used in API requests
3. **Given** user clears credentials, **When** attempting to send thought, **Then** error directs user back to settings

---

### Edge Cases

- What happens if user enters invalid email format? (Validate before saving)
- What happens if token field is empty? (Validate - both fields required)
- What happens during configuration change while editing? (Preserve unsaved changes)
- What happens if encryption key becomes unavailable? (Prompt for re-entry)
- What happens if user taps Save without changing anything? (No-op or show "No changes" message)

## Requirements

### Functional Requirements

- **FR-001**: System MUST store credentials using EncryptedSharedPreferences per constitution
- **FR-002**: System MUST validate email format before saving
- **FR-003**: System MUST validate that both email and token are non-empty before saving
- **FR-004**: System MUST mask token field display (show bullets/dots instead of characters)
- **FR-005**: System MUST provide toggle to reveal/hide token in settings UI
- **FR-006**: System MUST show success message after saving credentials
- **FR-007**: System MUST show error message if save fails
- **FR-008**: System MUST preserve unsaved changes during configuration changes
- **FR-009**: System MUST provide "How to get API token" help link/text
- **FR-010**: System MUST expose credentials through CredentialsDataStore interface for repository layer
- **FR-011**: Settings screen MUST be accessible from main screen settings icon
- **FR-012**: System MUST handle case where encrypted storage is unavailable (show error to user)

### Key Entities

- **Credentials**: Data class containing email and token
- **CredentialsDataStore**: Interface for storing/retrieving credentials
- **EncryptedCredentialsDataStore**: Implementation using EncryptedSharedPreferences
- **SettingsViewModel**: ViewModel managing settings screen state
- **SettingsScreenUiState**: Data class representing settings screen state

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can configure credentials in under 2 minutes (including finding API token)
- **SC-002**: 100% of credentials stored are encrypted (verified through file inspection)
- **SC-003**: Credentials survive app restarts with 100% reliability
- **SC-004**: Token field is masked by default in 100% of cases
- **SC-005**: Invalid email formats are rejected 100% of the time before save
- **SC-006**: Settings screen navigation from main screen works in under 1 second

## Technical Specifications

### Data Models

**File**: `data/model/Credentials.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.model

data class Credentials(
    val email: String,
    val token: String
)
```

### CredentialsDataStore Interface

**File**: `data/repository/CredentialsDataStore.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.repository

import com.taquangkhoi.napkincollect.data.model.Credentials

interface CredentialsDataStore {
    suspend fun saveCredentials(credentials: Credentials): Result<Unit>
    suspend fun getCredentials(): Credentials?
    suspend fun clearCredentials(): Result<Unit>
}
```

### EncryptedSharedPreferences Implementation

**File**: `data/repository/EncryptedCredentialsDataStore.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.taquangkhoi.napkincollect.data.model.Credentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedCredentialsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : CredentialsDataStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "napkin_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveCredentials(credentials: Credentials): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .putString(KEY_EMAIL, credentials.email)
                    .putString(KEY_TOKEN, credentials.token)
                    .apply()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCredentials(): Credentials? {
        return withContext(Dispatchers.IO) {
            try {
                val email = sharedPreferences.getString(KEY_EMAIL, null)
                val token = sharedPreferences.getString(KEY_TOKEN, null)

                if (email != null && token != null) {
                    Credentials(email, token)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun clearCredentials(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .remove(KEY_EMAIL)
                    .remove(KEY_TOKEN)
                    .apply()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_TOKEN = "token"
    }
}
```

### Settings ViewModel

**File**: `viewmodel/SettingsViewModel.kt`

```kotlin
package com.taquangkhoi.napkincollect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taquangkhoi.napkincollect.data.model.Credentials
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.data.repository.CredentialsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsScreenUiState(
    val email: String = "",
    val token: String = "",
    val tokenVisible: Boolean = false,
    val saveState: UiState<Unit> = UiState.Idle
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val credentialsDataStore: CredentialsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsScreenUiState())
    val uiState: StateFlow<SettingsScreenUiState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            val credentials = credentialsDataStore.getCredentials()
            if (credentials != null) {
                _uiState.update {
                    it.copy(
                        email = credentials.email,
                        token = credentials.token
                    )
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun updateToken(token: String) {
        _uiState.update { it.copy(token = token) }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(tokenVisible = !it.tokenVisible) }
    }

    fun saveCredentials() {
        val currentState = _uiState.value

        // Validation
        if (currentState.email.isBlank()) {
            _uiState.update {
                it.copy(saveState = UiState.Error("Email is required"))
            }
            return
        }

        if (!isValidEmail(currentState.email)) {
            _uiState.update {
                it.copy(saveState = UiState.Error("Invalid email format"))
            }
            return
        }

        if (currentState.token.isBlank()) {
            _uiState.update {
                it.copy(saveState = UiState.Error("API token is required"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(saveState = UiState.Loading) }

            val credentials = Credentials(
                email = currentState.email.trim(),
                token = currentState.token.trim()
            )

            val result = credentialsDataStore.saveCredentials(credentials)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(saveState = UiState.Success(Unit)) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(saveState = UiState.Error(
                            error.message ?: "Failed to save credentials"
                        ))
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(saveState = UiState.Idle) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
```

### Settings Screen UI

**File**: `ui/screens/SettingsScreen.kt`

```kotlin
package com.taquangkhoi.napkincollect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle save state
    LaunchedEffect(uiState.saveState) {
        when (val state = uiState.saveState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Credentials saved successfully!")
                viewModel.clearError()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Napkin.one Credentials",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Enter your Napkin.one email and API token to sync your thoughts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = uiState.saveState !is UiState.Loading
            )

            OutlinedTextField(
                value = uiState.token,
                onValueChange = viewModel::updateToken,
                label = { Text("API Token") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (uiState.tokenVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleTokenVisibility) {
                        Icon(
                            imageVector = if (uiState.tokenVisible)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.tokenVisible)
                                "Hide token"
                            else
                                "Show token"
                        )
                    }
                },
                singleLine = true,
                enabled = uiState.saveState !is UiState.Loading
            )

            Text(
                text = "Get your API token from: napkin.one → Settings → API",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::saveCredentials,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.saveState !is UiState.Loading
            ) {
                if (uiState.saveState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = if (uiState.saveState is UiState.Loading) "Saving..." else "Save Credentials")
            }
        }
    }
}
```

### Navigation Update

Since navigation is needed between Main and Settings screens, add Compose Navigation:

**Add dependency to `build.gradle.kts`**:
```kotlin
implementation("androidx.navigation:navigation-compose:2.7.5")
```

**File**: `MainActivity.kt` (update with navigation)

```kotlin
package com.taquangkhoi.napkincollect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.taquangkhoi.napkincollect.ui.screens.MainScreen
import com.taquangkhoi.napkincollect.ui.screens.SettingsScreen
import com.taquangkhoi.napkincollect.ui.theme.NapkinCollectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NapkinCollectTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### Dependency Injection Module

**File**: `di/DataModule.kt`

```kotlin
package com.taquangkhoi.napkincollect.di

import com.taquangkhoi.napkincollect.data.repository.CredentialsDataStore
import com.taquangkhoi.napkincollect.data.repository.EncryptedCredentialsDataStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCredentialsDataStore(
        impl: EncryptedCredentialsDataStore
    ): CredentialsDataStore
}
```

## Assumptions

- Users can access Napkin.one settings to retrieve their API token
- Email validation using Android's Patterns.EMAIL_ADDRESS is sufficient
- Token format is opaque string (no validation beyond non-empty)
- EncryptedSharedPreferences is available on all supported devices (minSdk 24)
- Single user per device (no multi-account support needed)

## Out of Scope

- Multi-account support
- Token expiration handling
- OAuth or other authentication methods
- Credential import/export
- Account creation within the app
- Password/biometric protection for settings screen
- Token refresh or rotation automation

## Dependencies

- **Spec 1**: Architecture Foundation (required for dependencies, base classes, DI setup)

## References

- Napkandroid Constitution v1.1.0 (memory/constitution.md)
- Android EncryptedSharedPreferences: https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
- Napkin.one API settings: https://napkin.one (user must access manually)
- Jetpack Navigation Compose: https://developer.android.com/jetpack/compose/navigation
