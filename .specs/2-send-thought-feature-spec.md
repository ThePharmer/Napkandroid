# Feature Specification: Send Thought Feature

**Feature Branch**: `2-send-thought-feature`
**Created**: 2025-11-05
**Status**: Draft
**Depends On**: Spec 1 (Architecture Foundation)

## User Scenarios & Testing

### User Story 1 - Quick Thought Capture (Priority: P1)

As a user, I want to quickly enter a thought and send it to Napkin.one so that I can capture ideas without interrupting my workflow.

**Why this priority**: This is the core value proposition of the app - quick thought capture. All other features support this primary use case.

**Independent Test**: Can be fully tested by entering text in the thought field, tapping Send, and verifying the thought appears in the Napkin.one web interface with correct content and metadata.

**Acceptance Scenarios**:

1. **Given** user has configured credentials, **When** user enters thought text and taps Send, **Then** thought is successfully sent to Napkin.one and success message displays
2. **Given** network request is in progress, **When** user is waiting, **Then** loading spinner displays and Send button is disabled
3. **Given** thought was sent successfully, **When** send completes, **Then** thought text is cleared and user can enter a new thought
4. **Given** user enters thought with source URL, **When** send completes, **Then** both thought and source URL are included in the API request

---

### User Story 2 - Error Handling and Feedback (Priority: P1)

As a user, I want to know if my thought failed to send so that I can retry without losing my content.

**Why this priority**: Mobile networks are unreliable. Users must know submission status to trust the app with their thoughts.

**Independent Test**: Can be tested by disconnecting network, attempting to send, and verifying clear error message displays with thought text preserved for retry.

**Acceptance Scenarios**:

1. **Given** no network connection, **When** user taps Send, **Then** error message displays "No internet connection" and thought text is preserved
2. **Given** API returns error (401/403), **When** send fails, **Then** error message displays "Invalid credentials - check settings" with link to settings
3. **Given** API returns server error (500), **When** send fails, **Then** error message displays "Server error - please try again"
4. **Given** request times out, **When** timeout occurs, **Then** error message displays "Request timed out - please retry"

---

### User Story 3 - Optional Source Attribution (Priority: P2)

As a user, I want to optionally include a source URL with my thought so that I can track where ideas came from.

**Why this priority**: Source attribution adds context but is not required for core thought capture. Secondary to basic send functionality.

**Independent Test**: Can be tested by sending thought with and without source URL, verifying both scenarios work correctly.

**Acceptance Scenarios**:

1. **Given** thought with empty source field, **When** send completes, **Then** empty string is sent for sourceUrl field
2. **Given** thought with URL in source field, **When** send completes, **Then** URL is included in API request
3. **Given** thought with arbitrary source text (not URL), **When** send completes, **Then** text is sent as-is (no URL validation required)

---

### Edge Cases

- What happens when thought text is empty? (Validate before sending)
- What happens if thought is extremely long? (Should API handle or truncate?)
- What happens during configuration change (screen rotation) during send? (Preserve state)
- What happens if user taps Send multiple times rapidly? (Debounce or disable button)
- What happens if credentials are not configured? (Show error directing to settings)

## Requirements

### Functional Requirements

- **FR-001**: System MUST validate that thought text is not empty before sending
- **FR-002**: System MUST display loading indicator during network requests
- **FR-003**: System MUST disable Send button while request is in progress
- **FR-004**: System MUST retrieve credentials from secure storage (EncryptedSharedPreferences)
- **FR-005**: System MUST construct API request with email, token, thought, and sourceUrl
- **FR-006**: System MUST handle network errors and display user-friendly messages
- **FR-007**: System MUST preserve thought text on configuration changes (screen rotation)
- **FR-008**: System MUST clear thought and source fields after successful send
- **FR-009**: System MUST show success message (Snackbar or Toast) on successful send
- **FR-010**: System MUST log network errors (for debugging) without exposing credentials
- **FR-011**: System MUST enforce network request timeout of 30 seconds
- **FR-012**: System MUST handle missing credentials by directing user to settings

### Key Entities

- **NapkinApi**: Retrofit API interface defining createThought endpoint
- **ThoughtRepository**: Repository handling API calls and error mapping
- **MainViewModel**: ViewModel managing UI state and orchestrating send logic
- **MainScreenUiState**: Data class representing main screen state (thought, source, loading, error)
- **CredentialsDataStore**: Interface for retrieving stored credentials

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can send a thought to Napkin.one in under 5 seconds (including network time on good connection)
- **SC-002**: 100% of network errors display user-friendly messages (no raw exception text shown)
- **SC-003**: Thought text survives configuration changes (screen rotation) without data loss
- **SC-004**: Send button prevents duplicate submissions (disabled during request)
- **SC-005**: Success rate of 100% on valid credentials with working network
- **SC-006**: All sent thoughts appear in Napkin.one web interface with correct content

## Technical Specifications

### Retrofit API Interface

**File**: `data/api/NapkinApi.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.api

import com.taquangkhoi.napkincollect.data.model.ThoughtRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NapkinApi {
    @POST("api/createThought")
    suspend fun createThought(
        @Body request: ThoughtRequest
    ): Response<Unit>
}
```

### Repository Implementation

**File**: `data/repository/ThoughtRepository.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.repository

import com.taquangkhoi.napkincollect.data.api.NapkinApi
import com.taquangkhoi.napkincollect.data.model.ThoughtRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtRepository @Inject constructor(
    private val api: NapkinApi
) {
    suspend fun sendThought(
        email: String,
        token: String,
        thought: String,
        sourceUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = ThoughtRequest(
                email = email,
                token = token,
                thought = thought,
                sourceUrl = sourceUrl
            )

            val response = api.createThought(request)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = when (response.code()) {
                    401, 403 -> "Invalid credentials. Please check your settings."
                    500, 502, 503 -> "Server error. Please try again later."
                    else -> "Failed to send thought. Please try again."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "No internet connection. Please check your network."
                e.message?.contains("timeout") == true ->
                    "Request timed out. Please try again."
                else -> "Network error. Please try again."
            }
            Result.failure(Exception(errorMessage))
        }
    }
}
```

### ViewModel Implementation

**File**: `viewmodel/MainViewModel.kt`

```kotlin
package com.taquangkhoi.napkincollect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.data.repository.ThoughtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainScreenUiState(
    val thought: String = "",
    val source: String = "",
    val sendState: UiState<Unit> = UiState.Idle
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ThoughtRepository,
    private val credentialsDataStore: CredentialsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    fun updateThought(thought: String) {
        _uiState.update { it.copy(thought = thought) }
    }

    fun updateSource(source: String) {
        _uiState.update { it.copy(source = source) }
    }

    fun sendThought() {
        val currentState = _uiState.value

        // Validation
        if (currentState.thought.isBlank()) {
            _uiState.update {
                it.copy(sendState = UiState.Error("Please enter a thought"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(sendState = UiState.Loading) }

            val credentials = credentialsDataStore.getCredentials()
            if (credentials == null) {
                _uiState.update {
                    it.copy(sendState = UiState.Error("Please configure credentials in settings"))
                }
                return@launch
            }

            val result = repository.sendThought(
                email = credentials.email,
                token = credentials.token,
                thought = currentState.thought,
                sourceUrl = currentState.source
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            thought = "",
                            source = "",
                            sendState = UiState.Success(Unit)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(sendState = UiState.Error(error.message ?: "Unknown error"))
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(sendState = UiState.Idle) }
    }
}
```

### UI Integration

**File**: `ui/screens/MainScreen.kt` (refactored from components/)

```kotlin
package com.taquangkhoi.napkincollect.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.taquangkhoi.napkincollect.R
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle UI state changes
    LaunchedEffect(uiState.sendState) {
        when (val state = uiState.sendState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar("Thought sent successfully!")
                viewModel.clearError()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = if (state.message.contains("credentials")) "Settings" else null
                )
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.napkin_logo_analogue_black),
                        contentDescription = "Logo",
                        modifier = Modifier.height(32.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
        ) {
            OutlinedTextField(
                value = uiState.thought,
                onValueChange = viewModel::updateThought,
                label = { Text("Enter your thought") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = uiState.sendState !is UiState.Loading
            )

            OutlinedTextField(
                value = uiState.source,
                onValueChange = viewModel::updateSource,
                label = { Text("Source of Thought") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.sendState !is UiState.Loading
            )

            Button(
                onClick = viewModel::sendThought,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = uiState.sendState !is UiState.Loading
            ) {
                if (uiState.sendState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = if (uiState.sendState is UiState.Loading) "Sending..." else "Send")
            }
        }
    }
}
```

### Dependency Injection Module

**File**: `di/NetworkModule.kt`

```kotlin
package com.taquangkhoi.napkincollect.di

import com.taquangkhoi.napkincollect.data.api.NapkinApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://app.napkin.one/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNapkinApi(retrofit: Retrofit): NapkinApi {
        return retrofit.create(NapkinApi::class.java)
    }
}
```

### MainActivity Update

**File**: `MainActivity.kt` (update to use Hilt)

```kotlin
package com.taquangkhoi.napkincollect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.taquangkhoi.napkincollect.ui.screens.MainScreen
import com.taquangkhoi.napkincollect.ui.theme.NapkinCollectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NapkinCollectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
```

## Assumptions

- Credentials are available from CredentialsDataStore (implemented in Spec 3)
- For testing purposes, hardcoded credentials can be used temporarily
- API returns HTTP 200 on success with empty body
- Network timeout of 30 seconds is acceptable
- Thought text has no strict character limit (API handles validation)

## Out of Scope

- Offline thought queuing (future enhancement per constitution)
- Credential configuration UI (covered in Spec 3)
- Share intent handling from other apps
- Thought history or local storage
- Analytics or usage tracking

## Dependencies

- **Spec 1**: Architecture Foundation (required for dependencies, base classes, DI setup)
- **Spec 3**: Secure Settings (credentials storage interface - can use mock for testing)

## References

- Napkandroid Constitution v1.1.0 (memory/constitution.md)
- Napkin.one API Documentation: https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts
- Retrofit documentation: https://square.github.io/retrofit/
- Kotlin Coroutines guide: https://kotlinlang.org/docs/coroutines-guide.html
