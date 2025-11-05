# Data Model: Architecture Foundation

**Feature**: 001-architecture-foundation
**Date**: 2025-11-05
**Status**: Complete - All models confirmed, to be implemented in Spec 2

## Overview

This document defines the core data models for the Napkandroid application's architectural foundation. These models establish patterns for state management (UiState) and API communication (ThoughtRequest, ThoughtResponse). All models follow Kotlin best practices and constitutional requirements for immutability and type safety. API response structure confirmed from official Napkin.one documentation.

## Entity Diagram

```
┌─────────────────────────────────┐
│   UiState<T> (Sealed)           │
│                                 │
│  ├─ Idle                        │
│  ├─ Loading                     │
│  ├─ Success<T>(data: T)         │
│  └─ Error(message: String)      │
└─────────────────────────────────┘
                │
                │ used by
                ▼
┌─────────────────────────────────┐
│   ViewModels                    │  (Spec 2)
│  (MainViewModel, etc.)          │
└─────────────────────────────────┘
                │
                │ sends
                ▼
┌─────────────────────────────────┐
│   ThoughtRequest                │
│                                 │
│  - email: String                │
│  - token: String                │
│  - thought: String              │
│  - sourceUrl: String            │
└─────────────────────────────────┘
                │
                │ HTTP POST
                ▼
┌─────────────────────────────────┐
│   Napkin.one API                │
│   POST /api/createThought       │
└─────────────────────────────────┘
                │
                │ returns
                ▼
┌─────────────────────────────────┐
│   ThoughtResponse ✅            │
│                                 │
│  - thoughtId: String            │
│  - url: String                  │
└─────────────────────────────────┘
                │
                │ wrapped in
                ▼
┌─────────────────────────────────┐
│   UiState<ThoughtResponse>      │
│   (Success or Error)            │
└─────────────────────────────────┘
```

## Entities

### 1. UiState<T>

**Purpose**: Represent all possible UI states in a type-safe, exhaustive manner

**Type**: Sealed class (Generic)

**Location**: `data/model/UiState.kt`

**Definition**:
```kotlin
package com.taquangkhoi.napkincollect.data.model

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

**Fields**:

| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| data | T | Success payload (generic) | Type-safe generic parameter |
| message | String | Error message for user display | Non-empty for Error state |

**States**:

| State | When Used | UI Behavior | Next States |
|-------|-----------|-------------|-------------|
| Idle | Initial state before any action | Show default/empty UI | → Loading (user action)<br>→ Success (cached data)<br>→ Error (validation) |
| Loading | Operation in progress (network call, etc.) | Show loading spinner/progress | → Success (operation succeeds)<br>→ Error (operation fails) |
| Success<T> | Operation completed successfully | Show success UI with data | → Loading (refresh action)<br>→ Idle (reset) |
| Error | Operation failed | Show error message, retry option | → Loading (retry)<br>→ Idle (dismiss) |

**State Transitions**:
```
[Idle] ──user sends thought──> [Loading]
         │
         ├──API success──> [Success<Unit>]
         │
         └──API failure──> [Error("Network error")]

[Success] ──user sends again──> [Loading]

[Error] ──user retries──> [Loading]
        └──user dismisses──> [Idle]
```

**Validation Rules**:
- Error state MUST have non-empty message
- Success state MUST have non-null data (enforced by type system)
- States are immutable (sealed class + data class properties)

**Usage Pattern** (from Constitution):
```kotlin
// ViewModel
private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

fun sendThought() {
    _uiState.value = UiState.Loading
    viewModelScope.launch {
        try {
            repository.sendThought(request)
            _uiState.value = UiState.Success(Unit)
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
        }
    }
}

// Composable
when (val state = uiState.value) {
    is UiState.Idle -> { /* Show input form */ }
    is UiState.Loading -> { /* Show loading spinner */ }
    is UiState.Success -> { /* Show success message */ }
    is UiState.Error -> { /* Show error: state.message */ }
}
```

**Relationships**:
- Used by all ViewModels (one-to-many)
- Generic parameter T varies by use case:
  - `UiState<Unit>` for operations with no return data (send thought)
  - `UiState<List<String>>` for data retrieval (future: thought history)
  - `UiState<Credentials>` for settings (Spec 3)

**Testing Considerations**:
- Exhaustive when expressions ensure all states handled
- Sealed class prevents external subclasses (closed hierarchy)
- Unit testable: `assert(state is UiState.Success)`

---

### 2. ThoughtRequest

**Purpose**: API request model for creating a thought via Napkin.one API

**Type**: Data class

**Location**: `data/model/ThoughtRequest.kt`

**Definition**:
```kotlin
package com.taquangkhoi.napkincollect.data.model

import com.google.gson.annotations.SerializedName

data class ThoughtRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("token")
    val token: String,

    @SerializedName("thought")
    val thought: String,

    @SerializedName("sourceUrl")
    val sourceUrl: String
)
```

**Fields**:

| Field | Type | Required | Description | Validation Rules |
|-------|------|----------|-------------|------------------|
| email | String | Yes | User's Napkin.one email | Must match user's account email |
| token | String | Yes | API authentication token | Non-empty, stored securely |
| thought | String | Yes | Thought content to capture | Non-empty, max length TBD |
| sourceUrl | String | Yes | Source URL for context | Valid URL format or empty string |

**Validation Rules** (enforced in ViewModel/Repository, NOT in model):
1. **email**:
   - Format: Valid email address (regex: `^[A-Za-z0-9+_.-]+@(.+)$`)
   - Source: Retrieved from EncryptedSharedPreferences
   - Validation: ViewModel validates before creating request

2. **token**:
   - Format: Non-empty string
   - Source: Retrieved from EncryptedSharedPreferences
   - Security: Never logged, transmitted over HTTPS only
   - Validation: Repository checks non-empty before network call

3. **thought**:
   - Format: Non-empty string
   - Min length: 1 character (after trim)
   - Max length: TBD (no constraint in API docs)
   - Validation: UI enforces non-empty, ViewModel trims whitespace

4. **sourceUrl**:
   - Format: Valid URL or empty string
   - Example: "https://example.com/article" or ""
   - Validation: Optional in UI, empty string if not provided
   - Note: Constitution requires source URL tracking for thought context

**JSON Serialization Example**:
```json
{
  "email": "user@example.com",
  "token": "abc123token",
  "thought": "Great idea for a productivity app",
  "sourceUrl": "https://example.com/inspiration"
}
```

**Usage Pattern**:
```kotlin
// In Repository
suspend fun sendThought(email: String, token: String, thought: String, sourceUrl: String): Result<Unit> {
    val request = ThoughtRequest(
        email = email,
        token = token,
        thought = thought.trim(),
        sourceUrl = sourceUrl
    )

    return withContext(Dispatchers.IO) {
        try {
            apiService.createThought(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Relationships**:
- Created by Repository (ThoughtRepository in Spec 2)
- Sent via Retrofit API interface (NapkinApiService in Spec 2)
- Fields populated from:
  - `email`, `token`: SecurePreferencesRepository (Spec 3)
  - `thought`, `sourceUrl`: User input via MainScreen (Spec 2)

**Security Considerations**:
- Contains sensitive data (token, email)
- MUST NOT be logged in any build variant (debug or release)
- Transmitted over HTTPS only (enforced by Retrofit/OkHttp)
- Never persisted to disk (created on-demand for API calls)

**Testing Considerations**:
- Immutable data class (all fields val)
- Easy to create test instances
- Gson serialization testable with JSON strings

---

### 3. ThoughtResponse

**Purpose**: API response model for thought creation

**Type**: Data class

**Location**: `data/model/ThoughtResponse.kt` (Spec 2)

**Status**: ✅ CONFIRMED - API documentation provided, to be implemented in Spec 2

**Definition**:
```kotlin
package com.taquangkhoi.napkincollect.data.model

import com.google.gson.annotations.SerializedName

data class ThoughtResponse(
    @SerializedName("thoughtId")
    val thoughtId: String,

    @SerializedName("url")
    val url: String
)
```

**Fields**:

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| thoughtId | String | Yes | Unique identifier for the created thought | "-NV-DjhK61Ct4mMx-K06" |
| url | String | Yes | Direct URL to view the thought in Napkin.one | "https://app.napkin.one/t/-NV-DjhK61Ct4mMx-K06" |

**Validation Rules**:
- Both fields are non-nullable (enforced by Kotlin type system)
- thoughtId format: Firebase-style key (starts with `-`, alphanumeric)
- url format: Full HTTPS URL to Napkin.one domain

**JSON Response Example** (from official API documentation):
```json
{
   "thoughtId": "-NV-DjhK61Ct4mMx-K06",
   "url": "https://app.napkin.one/t/-NV-DjhK61Ct4mMx-K06"
}
```

**Usage Pattern**:
```kotlin
// In Repository
suspend fun sendThought(request: ThoughtRequest): Result<ThoughtResponse> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.createThought(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// In ViewModel
fun sendThought() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        val result = repository.sendThought(request)
        _uiState.value = if (result.isSuccess) {
            val thoughtResponse = result.getOrNull()!!
            // Can use thoughtResponse.url for sharing or verification
            UiState.Success(thoughtResponse)
        } else {
            UiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
}
```

**Relationships**:
- Returned by Repository (ThoughtRepository in Spec 2)
- Received via Retrofit API interface (NapkinApiService in Spec 2)
- Can be wrapped in `UiState<ThoughtResponse>` for UI state management
- URL field can be used for:
  - Verification (open in browser to confirm thought created)
  - Sharing (share URL with other apps)
  - Analytics (track successful creations)

**Key Insights**:
- **Simpler than expected**: Only 2 fields (no `success`, `message`, `createdAt`)
- **Firebase backend**: ThoughtId format indicates Napkin.one uses Firebase Realtime Database
- **Immediate verification**: URL provides direct link for instant confirmation
- **No timestamps**: Server-side timestamp not included in response (may be internal only)

**Testing Considerations**:
- Easy to create test instances (simple 2-field data class)
- URL field can be validated with regex: `^https://app\.napkin\.one/t/[A-Za-z0-9_-]+$`
- thoughtId format: `^-[A-Za-z0-9_-]+$`
- Mock responses simple due to minimal fields

---

## Data Flow

### Send Thought Flow (Foundation + Spec 2)

```
User Input
    │
    ▼
[MainScreen Composable]
    │ user clicks send
    ▼
[MainViewModel]
    │ validates input
    │ updates state: UiState.Loading
    ▼
[ThoughtRepository]
    │ creates ThoughtRequest
    │ gets credentials from SecurePreferencesRepository
    ▼
[Retrofit API Service]
    │ serializes ThoughtRequest to JSON
    │ POST to https://app.napkin.one/api/createThought
    ▼
[Napkin.one API]
    │ processes thought
    │ returns response
    ▼
[ThoughtRepository]
    │ deserializes response
    │ returns Result<Unit>
    ▼
[MainViewModel]
    │ updates state: UiState.Success or UiState.Error
    ▼
[MainScreen Composable]
    │ observes state change
    │ shows success/error UI
```

### State Management Flow

```
StateFlow<UiState<Unit>> in ViewModel
    │
    ├─ Initial: UiState.Idle
    │
    ├─ User Action: → UiState.Loading
    │
    ├─ API Success: → UiState.Success(Unit)
    │       │
    │       └─ UI shows: "Thought sent successfully!"
    │
    └─ API Failure: → UiState.Error("Network error")
            │
            └─ UI shows: Error message + Retry button
```

## Validation Summary

| Entity | Field | Validation Location | Rules |
|--------|-------|-------------------|-------|
| UiState | message | Compile-time | Type system ensures non-null for Error |
| ThoughtRequest | email | ViewModel | Email format regex |
| ThoughtRequest | token | Repository | Non-empty check |
| ThoughtRequest | thought | ViewModel | Non-empty after trim |
| ThoughtRequest | sourceUrl | ViewModel | Valid URL or empty string |

**Validation Philosophy** (from Constitution):
- Data classes are simple, immutable containers (no validation logic inside)
- ViewModels validate user input before creating requests
- Repositories validate business rules before API calls
- Type system enforces non-null constraints
- Sealed classes provide exhaustive state handling

## Future Extensions

### Spec 2 (Send Thought Feature)
- Add `ThoughtResponse` model
- Add Retrofit API interface: `NapkinApiService`
- Add Repository: `ThoughtRepository`
- Add ViewModel: `MainViewModel` with UiState<Unit>

### Spec 3 (Secure Settings)
- Add `Credentials` data class (email + token)
- Add `SecurePreferencesRepository`
- Add ViewModel: `SettingsViewModel` with UiState<Credentials>

### Spec 4 (Quality Assurance)
- Add test data builders for ThoughtRequest
- Add test fixtures for UiState states
- Add API mock responses

### Future Enhancements (Post-MVP)
- Add `Thought` entity for offline queueing
- Add `SyncStatus` sealed class (Pending, Synced, Failed)
- Add Room database for offline storage

## Constitutional Compliance

✅ **Immutability**: All data classes use `val`, sealed class prevents modification
✅ **Type Safety**: Generic UiState<T> provides compile-time type checking
✅ **Error Handling**: UiState.Error provides explicit error state
✅ **Security**: ThoughtRequest marked as sensitive (no logging)
✅ **Kotlin Best Practices**: Data classes, sealed classes, explicit types
✅ **MVVM Pattern**: Clear separation - models in data/, used by viewmodel/ and ui/

## References

- Constitution: `/memory/constitution.md` v1.1.0 (Sections: Kotlin Coding Standards, Error Handling, State Management)
- Feature Spec: `./spec.md` (FR-005, FR-008, Key Entities)
- Research: `./research.md` (Section 4: API Contract, Section 6: EncryptedSharedPreferences)
- API Contract: `./contracts/napkin-api.md` (to be created after API testing)
