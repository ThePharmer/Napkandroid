# Feature Specification: Architecture Foundation

**Feature Branch**: `1-architecture-foundation`
**Created**: 2025-11-05
**Status**: Draft

## Clarifications

### Session 2025-11-05

- Q: Dependency conflict resolution strategy - What approach should be used when dependency versions conflict? → A: Use Android BOM (Bill of Materials) and version catalogs to align dependency versions automatically
- Q: ProGuard rules handling for new libraries - How should ProGuard rules be discovered and validated for new dependencies? → A: Proactive discovery - Test release builds early, use R8 full mode, and validate with automated tests
- Q: Hilt annotation processing failure recovery - What should be done if Hilt kapt fails during compilation? → A: Incremental debugging - Clean build, invalidate caches, check for circular dependencies, enable verbose kapt logging
- Q: Build time baseline measurement - How should the baseline be established for SC-005's 30-second build time increase constraint? → A: Clean build - Measure full clean build time (./gradlew clean build) as baseline, no caching
- Q: API response structure - What does the Napkin.one API return when creating a thought? → A: Check API documentation - Consult Napkin.one API docs to determine actual response structure and status codes

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

- **Dependency version conflicts**: Use Android BOM (Bill of Materials) and version catalogs to automatically align dependency versions and minimize conflicts
- **ProGuard rules for new libraries**: Test release builds early in development using R8 full mode, run automated tests on release builds, and validate that all reflection-based libraries have appropriate keep rules
- **Hilt annotation processing failures**: Use incremental debugging approach - clean build, invalidate caches, check for circular dependencies in DI graph, and enable verbose kapt logging to identify root cause

## Requirements

### Functional Requirements

- **FR-001**: System MUST include all dependencies specified in constitution (Retrofit, OkHttp, Hilt, Coroutines, EncryptedSharedPreferences)
- **FR-002**: System MUST configure Hilt dependency injection with Application class
- **FR-003**: Project MUST include directory structure matching constitution specification
- **FR-004**: System MUST add INTERNET permission to AndroidManifest.xml
- **FR-005**: System MUST create base data models for API communication (ThoughtRequest). Note: ThoughtResponse model creation deferred to Spec 2 based on confirmed API contract
- **FR-006**: System MUST configure ProGuard rules for Retrofit and Gson/kotlinx.serialization
- **FR-007**: System MUST set up Hilt plugins and kapt configuration
- **FR-008**: System MUST create base sealed class for UiState pattern (Idle, Loading, Success, Error)
- **FR-009**: System MUST use Android BOM (Bill of Materials) for dependency version management to prevent conflicts. Version catalogs SHOULD be considered for future iterations
- **FR-010**: System MUST validate ProGuard rules by building and testing in release mode with R8 full mode enabled
- **FR-011**: System MUST enable verbose kapt logging in build configuration to facilitate troubleshooting of Hilt annotation processing issues
- **FR-012**: System MUST measure clean build time baseline before implementation and validate post-implementation build time against 30-second increase constraint
- **FR-013**: Project documentation MUST include verified Napkin.one API contract for request structure and authentication. Note: API response structure confirmed in contracts/napkin-api.md; ThoughtResponse model creation deferred to Spec 2

### Key Entities

- **ThoughtRequest**: API request model containing email, token, thought, sourceUrl (implemented in this spec)
- **ThoughtResponse**: API response model with thoughtId and url fields (structure confirmed in contracts/napkin-api.md, implementation deferred to Spec 2)
- **UiState<T>**: Sealed class representing UI states (Idle, Loading, Success<T>, Error) (implemented in this spec)
- **NapkinApplication**: Hilt application class for DI initialization

## Success Criteria

### Measurable Outcomes

- **SC-001**: Project compiles successfully with zero errors after dependency addition
- **SC-002**: Application launches and Hilt initializes without crashes
- **SC-003**: All constitutional directory structure exists (6 directories: data, ui, viewmodel, di, utils, and existing components)
- **SC-004**: Base models and classes can be imported and used in other modules
- **SC-005**: Clean build time (./gradlew clean build) increases by less than 30 seconds compared to pre-implementation baseline
- **SC-006**: ProGuard release build completes successfully with R8 full mode, passes automated tests, and produces no missing class warnings

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

### Dependency Version Management

To prevent dependency conflicts, use Android BOM (Bill of Materials):

**In `NapkinCollect/app/build.gradle.kts`**, add platform dependencies:
```kotlin
dependencies {
    // Use AndroidX BOM for version alignment
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // Compose dependencies without versions (managed by BOM)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Other dependencies as listed above with explicit versions
}
```

Consider migrating to Gradle version catalogs (libs.versions.toml) in future iterations for centralized version management.

### Hilt/Kapt Troubleshooting

If Hilt annotation processing fails, follow this incremental debugging procedure:

1. **Clean Build**:
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

2. **Invalidate Caches**: In Android Studio, `File > Invalidate Caches > Invalidate and Restart`

3. **Enable Verbose Kapt Logging** in `build.gradle.kts`:
   ```kotlin
   kapt {
       correctErrorTypes = true
       useBuildCache = false
       arguments {
           arg("verbose", "true")
       }
   }
   ```

4. **Check for Common Issues**:
   - Circular dependencies in DI modules
   - Missing `@HiltAndroidApp` annotation on Application class
   - Missing `@AndroidEntryPoint` on Activities/Fragments using injection
   - Conflicting kapt versions

5. **Review Build Output**: Kapt errors typically appear with detailed stack traces indicating the problematic class or module

6. **Verify Hilt Setup**: Ensure all Hilt components follow the canonical patterns from Hilt documentation

### Build Performance Measurement

To validate SC-005 (build time increase < 30 seconds), establish and measure baseline:

1. **Establish Baseline** (before implementation):
   ```bash
   ./gradlew clean
   time ./gradlew build
   ```
   Record the total build time (e.g., "Build completed in 1m 45s")

2. **Repeat Measurement**: Run clean build 3 times and take the average to account for system variance

3. **Post-Implementation Validation**:
   ```bash
   ./gradlew clean
   time ./gradlew build
   ```
   Compare new average build time against baseline

4. **Acceptable Result**: New build time ≤ (Baseline + 30 seconds)

5. **Documentation**: Record baseline and post-implementation times in implementation notes

**Example**:
- Baseline: 1m 45s (105 seconds average of 3 runs)
- Post-implementation: 2m 10s (130 seconds)
- Increase: 25 seconds ✓ (within 30-second constraint)

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

### API Contract Verification

Before creating the ThoughtResponse model, consult the Napkin.one API documentation:

**Reference**: https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts

**Required Information to Extract**:
1. **Success Response**:
   - HTTP status code (e.g., 200, 201)
   - Response body structure (JSON fields and types)
   - Any returned identifiers (e.g., thoughtId, createdAt)

2. **Error Response**:
   - HTTP error codes (e.g., 400, 401, 500)
   - Error message format
   - Error code/type fields (if any)

3. **Status Codes**:
   - 200/201: Success cases
   - 400: Invalid request (bad email, token, or thought format)
   - 401: Unauthorized (invalid credentials)
   - 500: Server error

**Implementation Note**: The ThoughtResponse data class should be created in Spec 2 (Send Thought Feature) based on findings from API documentation. This spec only establishes the foundation - actual API integration happens in Spec 2.

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

### ProGuard/R8 Validation Strategy

To proactively discover and validate ProGuard rules:

1. **Enable R8 Full Mode** in `gradle.properties`:
   ```properties
   android.enableR8.fullMode=true
   ```

2. **Early Release Build Testing**: Create release build variant early in development cycle, before implementing features

3. **Automated Testing on Release Builds**: Run unit and integration tests against release APK to catch reflection/serialization issues

4. **Validation Checklist**:
   - Build release APK successfully
   - Install and launch release APK on device/emulator
   - Run automated test suite against release build
   - Check logcat for R8 warnings during build
   - Verify no `ClassNotFoundException` or `NoSuchMethodException` at runtime

5. **Continuous Validation**: Test release builds as part of CI/CD pipeline (future enhancement in Spec 4)

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
