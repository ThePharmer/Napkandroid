# Feature Specification: Architecture Foundation

**Feature Branch**: `1-architecture-foundation`
**Created**: 2025-11-05
**Status**: Draft

## User Scenarios & Testing

### User Story 1 - Development Infrastructure Ready (Priority: P1)

As a developer, I need the project configured with all required dependencies and architectural foundations so that I can implement features following the constitution's MVVM pattern.

**Why this priority**: Without this foundation, no feature development can proceed in compliance with the constitution. This is the prerequisite for all other work.

**Independent Test**: Can be fully tested by verifying the project compiles with all new dependencies, Hilt application runs without errors, and base architectural classes are available for import.

**Acceptance Scenarios**:

1. **Given** clean project state, **When** build is executed, **Then** all dependencies resolve and project compiles successfully
2. **Given** Hilt is configured, **When** application starts, **Then** dependency injection framework initializes without errors
3. **Given** base classes exist, **When** importing ViewModels and repositories, **Then** all required base types are available

---

### User Story 2 - Code Organization Matches Constitution (Priority: P1)

As a developer, I need the codebase organized according to the constitution's structure so that code is maintainable and follows established patterns.

**Why this priority**: Proper organization from the start prevents technical debt and ensures consistency with constitutional requirements.

**Independent Test**: Can be tested by verifying all directories exist as specified in constitution (data/, ui/, viewmodel/, di/, utils/) and sample classes compile in correct locations.

**Acceptance Scenarios**:

1. **Given** constitution directory structure, **When** examining project, **Then** all required directories exist
2. **Given** sample data models, **When** placed in data/model/, **Then** they are accessible from other layers
3. **Given** DI modules, **When** placed in di/, **Then** Hilt can discover and process them

---

### Edge Cases

- What happens when dependency versions conflict?
- How does the system handle ProGuard rules for new libraries?
- What if Hilt annotation processing fails?

## Requirements

### Functional Requirements

- **FR-001**: System MUST include all dependencies specified in constitution (Retrofit, OkHttp, Hilt, Coroutines, EncryptedSharedPreferences)
- **FR-002**: System MUST configure Hilt dependency injection with Application class
- **FR-003**: Project MUST include directory structure matching constitution specification
- **FR-004**: System MUST add INTERNET permission to AndroidManifest.xml
- **FR-005**: System MUST create base data models for API communication (ThoughtRequest, ThoughtResponse)
- **FR-006**: System MUST configure ProGuard rules for Retrofit and Gson/kotlinx.serialization
- **FR-007**: System MUST set up Hilt plugins and kapt configuration
- **FR-008**: System MUST create base sealed class for UiState pattern (Idle, Loading, Success, Error)

### Key Entities

- **ThoughtRequest**: API request model containing email, token, thought, sourceUrl
- **ThoughtResponse**: API response model (if needed based on API contract)
- **UiState<T>**: Sealed class representing UI states (Idle, Loading, Success<T>, Error)
- **NapkinApplication**: Hilt application class for DI initialization

## Success Criteria

### Measurable Outcomes

- **SC-001**: Project compiles successfully with zero errors after dependency addition
- **SC-002**: Application launches and Hilt initializes without crashes
- **SC-003**: All constitutional directory structure exists (6 directories: data, ui, viewmodel, di, utils, and existing components)
- **SC-004**: Base models and classes can be imported and used in other modules
- **SC-005**: Build time increases by less than 30 seconds compared to current baseline
- **SC-006**: ProGuard release build completes successfully with no missing class warnings

## Technical Specifications

### Dependencies to Add

**In `NapkinCollect/app/build.gradle.kts`**:

```kotlin
dependencies {
    // Existing dependencies remain...

    // ViewModel & Lifecycle (Constitution requirement)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Networking (Constitution Section: API Integration)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Coroutines (Constitution Section: Kotlin Coroutines)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Dependency Injection (Constitution requirement)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Secure Storage (Constitution Section: User Privacy & Security)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")
}
```

**Add plugins**:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}
```

**Project-level `build.gradle.kts`**:
```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

### Directory Structure

Create the following directories:
```
NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/
├── data/
│   ├── api/           # Retrofit API interfaces
│   ├── model/         # Data classes for API requests/responses
│   └── repository/    # Repository implementations
├── ui/                # (Already exists with theme/)
│   ├── theme/        # (Already exists)
│   ├── screens/      # Screen composables
│   └── components/   # (Move existing MainScreen here)
├── viewmodel/        # ViewModels
├── di/               # Dependency injection modules
└── utils/            # Shared utilities
```

### Base Classes to Create

**1. UiState.kt** (`data/model/UiState.kt`):
```kotlin
package com.taquangkhoi.napkincollect.data.model

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

**2. ThoughtRequest.kt** (`data/model/ThoughtRequest.kt`):
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

**3. NapkinApplication.kt** (in package root):
```kotlin
package com.taquangkhoi.napkincollect

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NapkinApplication : Application()
```

**4. Update AndroidManifest.xml**:
```xml
<manifest>
    <!-- Add INTERNET permission -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".NapkinApplication"
        ...
    >
```

### ProGuard Rules

Add to `proguard-rules.pro`:
```proguard
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data models
-keep class com.taquangkhoi.napkincollect.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
```

## Assumptions

- Gradle sync will complete successfully after dependency additions
- Current Kotlin version (1.9.0) is compatible with all added dependencies
- Target SDK 34 is compatible with all libraries
- No existing code conflicts with Hilt annotations

## Out of Scope

- Implementation of actual features (ViewModels, repositories with logic)
- Network calls or API integration (covered in Spec 2)
- Settings screen implementation (covered in Spec 3)
- Test implementation (covered in Spec 4)
- Migration of existing MainActivity to use new architecture

## Dependencies

- None (this is the foundation for all other specs)

## References

- Napkandroid Constitution v1.1.0 (memory/constitution.md)
- Retrofit documentation: https://square.github.io/retrofit/
- Hilt documentation: https://dagger.dev/hilt/
- Napkin.one API: https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts
