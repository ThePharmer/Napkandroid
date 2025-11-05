# Research: Architecture Foundation

**Feature**: 001-architecture-foundation
**Date**: 2025-11-05
**Status**: Complete

## Overview

This document captures research findings and technical decisions for establishing the architectural foundation of Napkandroid. All decisions align with the project constitution (v1.1.0) requirements for modern Android development with Kotlin, Jetpack Compose, and MVVM architecture.

## Research Topics

### 1. Dependency Management: Android BOM vs Version Catalogs

**Question**: What approach should be used for managing dependency versions in the project?

**Decision**: Use Android Compose BOM initially, migrate to Version Catalogs in future iteration

**Rationale**:
- **Compose BOM (Bill of Materials)**: Provides automatic version alignment for all AndroidX Compose dependencies, preventing version conflicts
- **Current State**: Project already uses Compose BOM (version 2023.08.00)
- **Immediate Need**: Adding third-party dependencies (Retrofit, Hilt, OkHttp) which are NOT covered by Compose BOM
- **Strategy**: Use explicit versions for third-party libraries, rely on BOM for AndroidX/Compose libraries

**Implementation**:
```kotlin
dependencies {
    // BOM manages versions for Compose libraries
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")  // No version - managed by BOM
    implementation("androidx.compose.material3:material3")

    // Explicit versions for third-party dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.google.dagger:hilt-android:2.48")
}
```

**Alternatives Considered**:
- **Version Catalogs (libs.versions.toml)**: More structured approach for centralized version management across multi-module projects. **Rejected for now** because:
  - Napkandroid is currently single-module
  - Migration can happen later without disrupting foundation
  - BOM already provides benefits for Compose dependencies
- **Gradle Platform**: Similar to BOM but requires manual maintenance. **Rejected** because Compose BOM is officially maintained by Google.

**Future Consideration**: Migrate to Version Catalogs when:
- Project grows to multi-module structure
- Team size increases (more developers need version consistency)
- Dependency management becomes complex

---

### 2. Hilt Setup Best Practices

**Question**: What are the best practices and common pitfalls for setting up Hilt dependency injection?

**Decision**: Follow canonical Hilt setup with kapt configuration and incremental debugging strategy

**Rationale**:
- Hilt is the constitution-mandated DI framework
- Kapt (Kotlin Annotation Processing Tool) is required for Hilt code generation
- Common failures occur during annotation processing, requiring proactive debugging setup

**Best Practices Identified**:

1. **Plugin Order Matters**:
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.google.dagger.hilt.android")  // After Android and Kotlin
       id("kotlin-kapt")  // Must be last
   }
   ```

2. **Kapt Configuration for Debugging**:
   ```kotlin
   kapt {
       correctErrorTypes = true  // Better error messages
       useBuildCache = false     // Disable during initial setup
       arguments {
           arg("verbose", "true")  // Detailed logging
       }
   }
   ```

3. **Application Class Setup**:
   ```kotlin
   @HiltAndroidApp
   class NapkinApplication : Application()
   ```
   - MUST be registered in AndroidManifest.xml: `android:name=".NapkinApplication"`

4. **Dependencies**:
   ```kotlin
   implementation("com.google.dagger:hilt-android:2.48")
   kapt("com.google.dagger:hilt-compiler:2.48")
   implementation("androidx.hilt:hilt-navigation-compose:1.1.0")  // For Compose integration
   ```

**Common Pitfalls to Avoid**:
- ❌ Missing `@HiltAndroidApp` annotation on Application class
- ❌ Not updating AndroidManifest.xml with custom Application class
- ❌ Circular dependencies in DI modules
- ❌ Mixing kapt and ksp (use kapt for Hilt)
- ❌ Wrong plugin order in build.gradle.kts

**Incremental Debugging Procedure** (from spec clarifications):
1. Clean build: `./gradlew clean`
2. Rebuild with dependencies: `./gradlew build --refresh-dependencies`
3. Invalidate caches in Android Studio
4. Enable verbose kapt logging (already configured above)
5. Check for circular dependencies in DI graph
6. Review build output for detailed error messages

**Alternatives Considered**:
- **Koin**: Lightweight DI framework without code generation. **Rejected** because:
  - Constitution specifically requires Hilt for testability
  - Compile-time safety preferred over runtime reflection
- **Manual DI**: No framework. **Rejected** because:
  - Doesn't scale for multiple ViewModels and Repositories
  - Poor testability without framework support

---

### 3. ProGuard/R8 Configuration for Retrofit and Gson

**Question**: What ProGuard/R8 rules are required for Retrofit, Gson, and OkHttp to work correctly in release builds?

**Decision**: Use comprehensive ProGuard rules with R8 full mode validation

**Rationale**:
- R8 (replacement for ProGuard) performs aggressive code shrinking and obfuscation
- Retrofit and Gson use reflection to serialize/deserialize JSON
- Without proper keep rules, reflection-based code fails at runtime in release builds
- Constitution requires ProGuard validation (SC-006)

**Required ProGuard Rules**:

```proguard
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all data models (critical for serialization)
-keep class com.taquangkhoi.napkincollect.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Hilt (generated code must be preserved)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
```

**R8 Full Mode Strategy** (from spec clarifications):
1. **Enable R8 Full Mode**: Add to `gradle.properties`:
   ```properties
   android.enableR8.fullMode=true
   ```
2. **Early Release Build Testing**: Build release APK after foundation setup, before feature implementation
3. **Automated Testing**: Run unit and integration tests against release build to catch runtime issues
4. **Validation Checklist**:
   - Build release APK successfully (no R8 errors)
   - Install release APK on physical device or emulator
   - Launch app and verify Hilt initialization (no crashes)
   - Check logcat for R8 warnings during build process
   - Verify no `ClassNotFoundException` or `NoSuchMethodException` at runtime
5. **Continuous Validation**: Integrate release build testing into CI/CD pipeline (Spec 4)

**Alternatives Considered**:
- **Disable R8/ProGuard**: Keep all code without optimization. **Rejected** because:
  - APK size would be significantly larger
  - Security through obscurity is lost
  - Industry standard practice for production apps
- **Minimal Rules**: Only keep what fails. **Rejected** because:
  - Reactive rather than proactive (find issues in production)
  - Constitution requires proactive validation

---

### 4. Napkin.one API Contract Verification

**Question**: What does the Napkin.one API return when creating a thought? (FR-013)

**Decision**: ✅ RESOLVED - API documentation provided by user

**Status**: ✅ CONFIRMED - Official API documentation received after initial research phase

**Confirmed API Contract**:

**Request Format**:
```json
{
  "email": "string",
  "token": "string",
  "thought": "string",
  "sourceUrl": "string"
}
```

**Request Headers**:
```
Accept: application/json
Content-Type: application/json
```

**Endpoint**: `https://app.napkin.one/api/createThought` (POST)

**Success Response** (HTTP 200):
```json
{
   "thoughtId": "-NV-DjhK61Ct4mMx-K06",
   "url": "https://app.napkin.one/t/-NV-DjhK61Ct4mMx-K06"
}
```

**Response Fields**:
- `thoughtId` (string): Unique identifier for the created thought (Firebase-style key format)
- `url` (string): Direct URL to view the thought in Napkin.one web app

**Key Findings**:
1. **Simpler than estimated**: No `success`, `message`, or `createdAt` fields - just thoughtId and url
2. **Firebase backend**: ThoughtId format (`-NV-DjhK61Ct4mMx-K06`) indicates Firebase Realtime Database
3. **Immediate access**: URL provided for instant verification/sharing of created thought
4. **Rate limiting**: Documentation notes API is "for single thoughts, not bulk imports" (suggests rate limits exist but not documented)

**Error Responses**: Not documented in official API docs (to be discovered during testing)

**Implementation Strategy** (Updated):
1. **Spec 1 (Architecture Foundation)**:
   - Create `ThoughtRequest` model ✅
   - Document confirmed response structure in contracts/ ✅
2. **Spec 2 (Send Thought Feature)**:
   - Create `ThoughtResponse` model with confirmed structure
   - Implement typed Retrofit API interface
   - Test with real Napkin.one account to verify
   - Discover and document error response formats through testing

**ThoughtResponse Model** (to be created in Spec 2):
```kotlin
data class ThoughtResponse(
    @SerializedName("thoughtId")
    val thoughtId: String,

    @SerializedName("url")
    val url: String
)
```

**Resolution Source**: User provided official API documentation content after initial 403 error during research phase

---

### 5. Build Time Optimization for Kapt/Hilt Projects

**Question**: How can build time be minimized when adding Hilt (which uses kapt)?

**Decision**: Accept kapt overhead, establish baseline measurement, monitor against 30-second constraint

**Rationale**:
- Kapt is required for Hilt (no alternative annotation processing for Hilt)
- Constitution constrains build time increase to < 30 seconds (SC-005)
- Baseline measurement is the primary control mechanism

**Baseline Measurement Strategy** (from spec clarifications):
1. **Establish Baseline** BEFORE any changes:
   ```bash
   ./gradlew clean
   time ./gradlew build
   ```
2. **Run 3 times**, calculate average (accounts for system variance)
3. **Record baseline** in plan.md Implementation Notes
4. **Post-Implementation**: Same procedure after all dependencies added
5. **Validate**: New build time ≤ (Baseline + 30 seconds)

**Example**:
```
Baseline: 1m 45s (105 seconds, average of 3 runs)
Post-implementation: 2m 10s (130 seconds)
Increase: 25 seconds ✓ PASS (within 30-second constraint)
```

**Optimization Strategies** (if needed):
1. **Kapt Performance Options**:
   ```kotlin
   kapt {
       useBuildCache = true  // Enable after initial setup verification
       javacOptions {
           option("-Xmaxerrs", 500)
       }
   }
   ```
2. **Gradle Daemon**: Ensure Gradle daemon is enabled (default)
3. **Parallel Execution**: Already enabled in modern Gradle
4. **Avoid Clean Builds**: Only clean when necessary (not for every build)

**Build Time Breakdown** (expected):
- Kapt processing: +10-15 seconds (Hilt annotation processing)
- Additional dependencies: +5-10 seconds (download, compile)
- ProGuard/R8 (release only): +5-10 seconds
- Total expected: ~20-30 seconds increase

**Alternatives Considered**:
- **KSP (Kotlin Symbol Processing)**: Faster alternative to kapt. **Rejected** because:
  - Hilt 2.48 requires kapt (KSP support is experimental)
  - Constitution requires stable, production-ready tools
- **Remove Hilt**: Reduce build time by removing DI framework. **Rejected** because:
  - Constitution mandates Hilt for testability
  - Build time constraint is specifically designed to accommodate necessary complexity

**Monitoring**:
- Measure build time after each major dependency addition
- If build time exceeds constraint, investigate with `./gradlew build --profile`

---

### 6. EncryptedSharedPreferences Implementation Patterns

**Question**: What are the best practices for implementing EncryptedSharedPreferences for secure credential storage?

**Decision**: Use EncryptedSharedPreferences with MasterKey for credentials, defer implementation to Spec 3

**Rationale**:
- Constitution requires secure storage for API token and email (Principle II)
- EncryptedSharedPreferences is Android's recommended API for secure local storage
- AES-256 encryption with hardware-backed keystore (when available)

**Implementation Pattern**:

```kotlin
// In Spec 3 (Secure Settings), create SecurePreferences repository:
class SecurePreferencesRepository(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "napkin_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(email: String, token: String) {
        encryptedPrefs.edit()
            .putString("user_email", email)
            .putString("api_token", token)
            .apply()
    }

    fun getCredentials(): Pair<String?, String?> {
        val email = encryptedPrefs.getString("user_email", null)
        val token = encryptedPrefs.getString("api_token", null)
        return Pair(email, token)
    }

    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
    }
}
```

**Dependency** (already in spec):
```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

**Best Practices**:
1. **MasterKey Management**: Use `MasterKey.Builder` with AES256_GCM scheme
2. **Key/Value Encryption**: Encrypt both keys and values (PrefKeyEncryptionScheme + PrefValueEncryptionScheme)
3. **Error Handling**: Handle `GeneralSecurityException` for devices without hardware keystore
4. **DI Integration**: Inject repository via Hilt for testability
5. **No Logging**: Never log encrypted or decrypted credentials (even in debug builds)

**Security Considerations**:
- **Hardware Backing**: Keystore uses hardware-backed security when available (TEE, Secure Element)
- **Root Detection**: Not implemented (out of scope for MVP, constitution doesn't require)
- **Biometric Protection**: Not implemented (out of scope for MVP)
- **Backup Exclusion**: Add to AndroidManifest.xml:
  ```xml
  <application
      android:allowBackup="false"
      android:fullBackupContent="false">
  ```

**Foundation Setup** (Spec 1):
- Add dependency: `androidx.security:security-crypto:1.1.0-alpha06`
- No implementation in Spec 1 (foundation only)
- Actual implementation in Spec 3 (Secure Settings)

**Alternatives Considered**:
- **Android Keystore Directly**: Use KeyStore API directly for key generation. **Rejected** because:
  - More complex implementation
  - EncryptedSharedPreferences is the recommended wrapper
  - No significant benefit over EncryptedSharedPreferences
- **SQLCipher**: Encrypted SQLite database. **Rejected** because:
  - Overkill for simple key-value storage
  - Additional dependency and complexity
  - Constitution requires simplicity

---

## Summary of Decisions

| Topic | Decision | Implementation Phase |
|-------|----------|---------------------|
| Dependency Management | Compose BOM + explicit versions for third-party libs | Spec 1 |
| Hilt Setup | Canonical setup with kapt + verbose logging | Spec 1 |
| ProGuard/R8 | Comprehensive rules + R8 full mode validation | Spec 1 |
| API Contract | ✅ Confirmed - thoughtId + url response structure | Spec 1 (documented), Spec 2 (implementation) |
| Build Time | Baseline measurement + 30s constraint monitoring | Spec 1 |
| EncryptedSharedPreferences | Standard pattern with MasterKey | Spec 3 |

## Unresolved Items

~~1. **Napkin.one API Response Structure**: ⚠️ BLOCKED by 403 on documentation URL~~
   - **Status**: ✅ RESOLVED - User provided official API documentation
   - **Resolution**: Response structure confirmed (thoughtId + url fields)
   - **See**: Section 4 above for complete API contract details

**All items resolved - No outstanding research questions**

## Constitution Compliance

All research findings and decisions comply with constitution v1.1.0:
- ✅ Modern Android First: All recommendations use current Android/Kotlin best practices
- ✅ User Privacy & Security: EncryptedSharedPreferences pattern ensures credential security
- ✅ Offline-First: Architecture prepares for network resilience patterns
- ✅ Simplicity & Focus: Minimal dependencies, standard patterns, no over-engineering
- ✅ Testing & Quality Assurance: R8 validation ensures release quality

## References

- Android Developers: Compose BOM: https://developer.android.com/jetpack/compose/bom
- Android Developers: Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Square: Retrofit: https://square.github.io/retrofit/
- Square: OkHttp: https://square.github.io/okhttp/
- Android Developers: R8: https://developer.android.com/studio/build/shrink-code
- Android Developers: EncryptedSharedPreferences: https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
- Napkandroid Constitution v1.1.0: `/memory/constitution.md`
- Architecture Foundation Spec: `./spec.md`
