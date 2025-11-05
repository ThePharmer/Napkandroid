# Quickstart Guide: Architecture Foundation

**Feature**: 001-architecture-foundation
**Audience**: New developers joining the Napkandroid project
**Time to Complete**: 30-45 minutes
**Prerequisites**: Android Studio installed, basic Kotlin knowledge

## Overview

This guide helps you understand the architectural foundation of Napkandroid and set up your development environment. After completing this guide, you'll be able to build the project, understand the MVVM architecture, and start contributing to feature development.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Architecture Overview](#architecture-overview)
3. [Directory Structure](#directory-structure)
4. [Key Components](#key-components)
5. [Development Workflow](#development-workflow)
6. [Common Tasks](#common-tasks)
7. [Troubleshooting](#troubleshooting)

---

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Napkandroid
```

### 2. Open in Android Studio

1. Launch Android Studio
2. **File > Open** → Select the `Napkandroid` directory
3. Wait for Gradle sync to complete (first sync takes 5-10 minutes)

### 3. Verify Environment

**Required Versions** (from plan.md):
- Android Studio: Electric Eel or newer
- Kotlin: 1.9.0
- Gradle: 8.2.0
- compileSdk: 34
- minSdk: 24 (Android 7.0+)
- targetSdk: 34

**Check Gradle versions**:
```bash
./gradlew --version
```

### 4. Build the Project

```bash
./gradlew build
```

**Expected Result**: Build should complete successfully in ~2-3 minutes.

**If build fails**: See [Troubleshooting](#troubleshooting) section below.

### 5. Run on Device/Emulator

1. Connect Android device (USB debugging enabled) OR start Android Emulator
2. In Android Studio: **Run > Run 'app'** (or press Shift+F10)
3. App should launch showing the main screen

**Success**: You're ready to start development!

---

## Architecture Overview

Napkandroid follows the **MVVM (Model-View-ViewModel)** architectural pattern with modern Android best practices.

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ MainActivity │  │  MainScreen  │  │  Components  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           │ observes StateFlow
                           ▼
┌─────────────────────────────────────────────────────────┐
│               ViewModel Layer (StateFlow)               │
│  ┌──────────────────────────────────────────────────┐   │
│  │          MainViewModel (Spec 2)                  │   │
│  │  - Manages UI state (UiState<T>)                 │   │
│  │  - Validates user input                          │   │
│  │  - Calls Repository                              │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
                           │ calls suspend functions
                           ▼
┌─────────────────────────────────────────────────────────┐
│            Repository Layer (Data Access)               │
│  ┌──────────────────────────────────────────────────┐   │
│  │         ThoughtRepository (Spec 2)               │   │
│  │  - Orchestrates API calls                        │   │
│  │  - Handles errors, returns Result<T>             │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │    SecurePreferencesRepository (Spec 3)          │   │
│  │  - Manages encrypted credentials                 │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                           │
                           │ uses
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   Data Layer (Models)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  UiState<T>  │  │ThoughtRequest│  │  API Service │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           │ network call
                           ▼
                  ┌──────────────────┐
                  │  Napkin.one API  │
                  └──────────────────┘
```

### Key Principles

1. **Separation of Concerns**:
   - UI (Composables): Display state, handle user interactions
   - ViewModel: Manage state, validate input, coordinate data flow
   - Repository: Data access logic, API calls, caching
   - Data Models: Plain data containers (immutable)

2. **Unidirectional Data Flow**:
   ```
   User Action → ViewModel → Repository → API
        ↑                                   │
        └───────── StateFlow ←──────────────┘
   ```

3. **Dependency Injection (Hilt)**:
   - ViewModels injected into Composables
   - Repositories injected into ViewModels
   - API services injected into Repositories
   - Testability through constructor injection

4. **Reactive State Management**:
   - ViewModels expose `StateFlow<UiState<T>>`
   - Composables collect state with `collectAsState()`
   - UI automatically recomposes when state changes

---

## Directory Structure

After Spec 1 implementation, the project structure will be:

```
NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/
├── data/                          # Data layer
│   ├── api/                       # Retrofit API interfaces
│   │   └── NapkinApiService.kt    # (Spec 2)
│   ├── model/                     # Data models
│   │   ├── ThoughtRequest.kt      # API request model ✅
│   │   ├── UiState.kt             # UI state sealed class ✅
│   │   └── ThoughtResponse.kt     # (Spec 2 - after API testing)
│   └── repository/                # Repository implementations
│       ├── ThoughtRepository.kt   # (Spec 2)
│       └── SecurePreferencesRepo  # (Spec 3)
│
├── ui/                            # UI layer (Compose)
│   ├── theme/                     # Compose theme (existing)
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── screens/                   # Screen-level composables
│   │   └── MainScreen.kt          # (Spec 2)
│   └── components/                # Reusable UI components
│       └── ThoughtInput.kt        # (Spec 2)
│
├── viewmodel/                     # ViewModels
│   ├── MainViewModel.kt           # (Spec 2)
│   └── SettingsViewModel.kt       # (Spec 3)
│
├── di/                            # Dependency Injection modules
│   ├── NetworkModule.kt           # (Spec 2 - Retrofit, OkHttp)
│   └── RepositoryModule.kt        # (Spec 2 - Repository bindings)
│
├── utils/                         # Shared utilities
│   └── Constants.kt               # (Future - app constants)
│
├── MainActivity.kt                # App entry point (existing)
└── NapkinApplication.kt           # Hilt application class ✅
```

**Legend**:
- ✅ Implemented in Spec 1 (Architecture Foundation)
- (Spec N) To be implemented in future specs

---

## Key Components

### 1. UiState<T> (Sealed Class)

**Location**: `data/model/UiState.kt`

**Purpose**: Type-safe representation of UI states (Idle, Loading, Success, Error)

**Usage**:
```kotlin
// In ViewModel
private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

// In Composable
when (val state = uiState.value) {
    is UiState.Idle -> ShowIdleUI()
    is UiState.Loading -> ShowLoadingUI()
    is UiState.Success -> ShowSuccessUI()
    is UiState.Error -> ShowErrorUI(state.message)
}
```

**Benefits**:
- Exhaustive `when` expressions (compile-time safety)
- Clear intent: UI always knows what to display
- Testable: Easy to verify state transitions

### 2. ThoughtRequest (Data Class)

**Location**: `data/model/ThoughtRequest.kt`

**Purpose**: API request model for creating thoughts

**Structure**:
```kotlin
data class ThoughtRequest(
    val email: String,
    val token: String,
    val thought: String,
    val sourceUrl: String
)
```

**Security**: Contains sensitive data (token) - never logged

### 3. NapkinApplication (Hilt Application Class)

**Location**: `NapkinApplication.kt`

**Purpose**: Initialize Hilt dependency injection framework

**Code**:
```kotlin
@HiltAndroidApp
class NapkinApplication : Application()
```

**Configuration**: Registered in `AndroidManifest.xml`:
```xml
<application
    android:name=".NapkinApplication"
    ...>
```

---

## Development Workflow

### Constitutional Development Process

1. **Specification** (`/specify`): Define feature requirements
2. **Clarification** (`/clarify`): Resolve ambiguities
3. **Planning** (`/plan`): Design implementation (this spec)
4. **Implementation** (`/implement`): Write code
5. **Validation** (`/checklist`): Verify quality gates
6. **Review**: Code review and testing
7. **Integration**: Merge to main branch

### Feature Branch Strategy

- **Branch naming**: `<number>-<short-name>` (e.g., `001-architecture-foundation`)
- **Main branch**: Stable, release-ready code only
- **Feature branches**: Work in isolation, merge via PR
- **Commit format**: Conventional commits (`feat:`, `fix:`, `refactor:`, etc.)

### Adding a New Feature (Example)

**Scenario**: Add a button to the main screen

1. **Identify Layer**:
   - UI change → Modify Composable in `ui/screens/MainScreen.kt`
   - Business logic → Update ViewModel in `viewmodel/MainViewModel.kt`
   - Data change → Update Repository or Model

2. **Follow MVVM**:
   ```kotlin
   // ❌ Don't: Put logic in Composable
   Button(onClick = {
       // API call logic here - WRONG!
   })

   // ✅ Do: Delegate to ViewModel
   Button(onClick = { viewModel.onButtonClick() })
   ```

3. **State Management**:
   ```kotlin
   // In ViewModel
   fun onButtonClick() {
       _uiState.value = UiState.Loading
       viewModelScope.launch {
           // Business logic
       }
   }

   // In Composable
   val uiState by viewModel.uiState.collectAsState()
   ```

4. **Testing**:
   - Write unit tests for ViewModel logic
   - Write UI tests for Composable interactions (Spec 4)

---

## Common Tasks

### Task 1: Run the App

```bash
# From command line
./gradlew installDebug

# Or in Android Studio
Run > Run 'app' (Shift+F10)
```

### Task 2: Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Task 3: Build Release APK

```bash
# Build release (with ProGuard/R8)
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

**Note**: Release build testing required (SC-006) - verify ProGuard rules

### Task 4: Clean Build (When Things Break)

```bash
# Clean and rebuild
./gradlew clean build

# Or in Android Studio
Build > Clean Project
Build > Rebuild Project
```

### Task 5: Check Dependency Tree

```bash
# View all dependencies and their versions
./gradlew app:dependencies
```

### Task 6: Measure Build Time (Baseline)

```bash
# Clean build with timing
./gradlew clean
time ./gradlew build

# Run 3 times, calculate average
# Record in plan.md for SC-005 validation
```

### Task 7: View Hilt-Generated Code

```bash
# After build, find generated code in:
app/build/generated/source/kapt/debug/com/taquangkhoi/napkincollect/
```

---

## Troubleshooting

### Problem: Gradle Sync Fails

**Symptom**: "Gradle sync failed" error in Android Studio

**Solutions**:
1. Check internet connection (dependencies need to download)
2. `File > Invalidate Caches > Invalidate and Restart`
3. Delete `.gradle` and `.idea` directories, reopen project
4. Verify Java version: `java -version` (should be JDK 11 or newer)

### Problem: Hilt Annotation Processing Errors

**Symptom**: Build fails with kapt errors like "Cannot find symbol: DaggerNapkinApplication"

**Solutions** (from research.md):
1. **Clean build**: `./gradlew clean`
2. **Invalidate caches**: Android Studio > File > Invalidate Caches
3. **Check Application class**:
   - Has `@HiltAndroidApp` annotation
   - Registered in AndroidManifest.xml: `android:name=".NapkinApplication"`
4. **Verify plugin order** in `build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.google.dagger.hilt.android")  # Before kapt
       id("kotlin-kapt")  # Must be last
   }
   ```
5. **Enable verbose kapt logging**: Already configured in build.gradle.kts
6. **Check for circular dependencies**: Review DI modules for cycles

### Problem: Build Time Too Long

**Symptom**: Build takes >5 minutes

**Solutions**:
1. **Check baseline**: Measure clean build time, compare against expected ~2-3 minutes
2. **Enable Gradle daemon**: Should be enabled by default
3. **Use build cache**: `org.gradle.caching=true` in gradle.properties
4. **Avoid clean builds**: Only clean when necessary
5. **Check kapt performance**: Review kapt logs for slow annotation processors

### Problem: ProGuard/R8 Release Build Crashes

**Symptom**: Debug build works, release build crashes at runtime

**Solutions**:
1. **Check logcat for errors**: Look for `ClassNotFoundException`, `NoSuchMethodException`
2. **Verify ProGuard rules**: Ensure rules in `proguard-rules.pro` include all reflection-based libraries
3. **Test incrementally**: Build release APK after each dependency addition
4. **Enable R8 full mode**: `android.enableR8.fullMode=true` in gradle.properties
5. **Review ProGuard rules** in spec.md Section: ProGuard Rules

### Problem: App Crashes on Launch

**Symptom**: App crashes immediately after launch

**Common Causes**:
1. **Hilt not initialized**: Check NapkinApplication is registered in AndroidManifest.xml
2. **Missing dependencies**: Run `./gradlew app:dependencies` to verify all deps resolved
3. **Min SDK issue**: Ensure device/emulator is API 24+ (Android 7.0+)

**Debug Steps**:
1. Check logcat for stack trace
2. Verify Application class has `@HiltAndroidApp`
3. Clean and rebuild project
4. Run on different device/emulator

### Problem: Network Calls Not Working

**Symptom**: API calls fail with network errors (Spec 2+)

**Solutions**:
1. **Check INTERNET permission**: Verify in AndroidManifest.xml:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```
2. **HTTPS enforcement**: Napkin.one API uses HTTPS, ensure URL is correct
3. **Check network connectivity**: Test on device with active internet
4. **Review Retrofit configuration**: Verify base URL, converters, interceptors
5. **Enable logging**: OkHttp logging interceptor should show requests/responses in logcat (debug builds)

---

## Next Steps

After completing this quickstart:

1. **Read the Constitution**: `/memory/constitution.md` v1.1.0
   - Understand core principles (security, simplicity, testing)
   - Review architectural patterns (MVVM, StateFlow, Hilt)
   - Learn coding standards (Kotlin conventions, error handling)

2. **Explore Specs**:
   - **Spec 1 (this spec)**: Architecture Foundation
   - **Spec 2**: Send Thought Feature (API integration, ViewModels)
   - **Spec 3**: Secure Settings (EncryptedSharedPreferences)
   - **Spec 4**: Quality Assurance (testing strategy)

3. **Review Data Model**: `data-model.md`
   - Understand UiState pattern
   - Learn ThoughtRequest structure
   - See data flow diagrams

4. **Study API Contract**: `contracts/napkin-api.md`
   - Napkin.one API request format
   - Error handling strategy
   - Security considerations

5. **Run Tests** (when available in Spec 4):
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

6. **Start Contributing**:
   - Pick a task from next spec's tasks.md
   - Create feature branch: `002-send-thought`
   - Follow constitutional development workflow
   - Submit PR for review

---

## Resources

### Project Documentation
- **Constitution**: `/memory/constitution.md` v1.1.0
- **Spec 1 (this)**: `./spec.md`, `./plan.md`
- **Data Model**: `./data-model.md`
- **API Contract**: `./contracts/napkin-api.md`
- **Research**: `./research.md`

### Android Development
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **MVVM Architecture**: https://developer.android.com/topic/architecture
- **StateFlow**: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
- **Hilt**: https://developer.android.com/training/dependency-injection/hilt-android
- **Coroutines**: https://developer.android.com/kotlin/coroutines

### Libraries Used
- **Retrofit**: https://square.github.io/retrofit/
- **OkHttp**: https://square.github.io/okhttp/
- **Gson**: https://github.com/google/gson
- **EncryptedSharedPreferences**: https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences

### Tools
- **Android Studio**: https://developer.android.com/studio
- **Gradle**: https://gradle.org/
- **Kotlin**: https://kotlinlang.org/

---

## FAQ

**Q: Where do I add new dependencies?**
A: In `NapkinCollect/app/build.gradle.kts` under `dependencies` block. Use Compose BOM for Compose libraries, explicit versions for third-party libs.

**Q: How do I add a new screen?**
A: Create a new Composable in `ui/screens/`, create corresponding ViewModel in `viewmodel/`, inject ViewModel into screen via Hilt.

**Q: Where do I put network calls?**
A: In Repository classes (`data/repository/`). ViewModels call repository functions, repositories call API services.

**Q: How do I handle errors?**
A: Use `UiState.Error` in ViewModels. Repositories return `Result<T>`. ViewModels map exceptions to user-friendly error messages.

**Q: What testing is required?**
A: Unit tests for ViewModels and Repositories (Spec 4). UI tests for critical flows. Manual testing checklist before release.

**Q: How do I store sensitive data?**
A: Use EncryptedSharedPreferences (implemented in Spec 3). Never use regular SharedPreferences for credentials.

**Q: What's the build time baseline?**
A: Measure with `./gradlew clean && time ./gradlew build`. Expected: ~2-3 minutes. Must increase < 30 seconds after dependencies added (SC-005).

---

**Document Version**: 1.0
**Last Updated**: 2025-11-05
**For Questions**: Refer to constitution or project maintainer
