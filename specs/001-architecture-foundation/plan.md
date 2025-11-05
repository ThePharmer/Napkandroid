# Implementation Plan: Architecture Foundation

**Branch**: `001-architecture-foundation` | **Date**: 2025-11-05 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-architecture-foundation/spec.md`

## Summary

Establish the architectural foundation for the Napkandroid project by adding all required dependencies (Retrofit, OkHttp, Hilt, Coroutines, EncryptedSharedPreferences), creating the constitutional directory structure (data/, ui/, viewmodel/, di/, utils/), and implementing base classes (UiState, ThoughtRequest, NapkinApplication). This foundation enables all future features to follow the MVVM architectural pattern defined in the constitution while maintaining security, testability, and code quality standards.

**Technical Approach**: Use Gradle dependency management with Android BOM for version alignment, configure Hilt for dependency injection with kapt, establish ProGuard rules for R8 optimization, and validate with release builds to ensure no runtime issues.

## Technical Context

**Language/Version**: Kotlin 1.9.0
**Primary Dependencies**: Retrofit 2.9.0, OkHttp 4.12.0, Hilt 2.48, Kotlin Coroutines 1.7.3, Jetpack Compose, EncryptedSharedPreferences (androidx.security:security-crypto)
**Storage**: EncryptedSharedPreferences for secure credential storage (API token, email)
**Testing**: JUnit 4.13.2, Espresso 3.5.1, Compose UI Test (androidx.compose.ui:ui-test-junit4)
**Target Platform**: Android API 24+ (Android 7.0+), compileSdk 34, targetSdk 34
**Project Type**: mobile (Android native with Jetpack Compose)
**Performance Goals**: Build time increase < 30 seconds from baseline, app launch without Hilt initialization errors
**Constraints**: Offline-first capable architecture, secure storage mandatory for credentials, HTTPS-only network, configuration must survive process death
**Scale/Scope**: Single-user mobile app, ~5 screens planned, foundational architecture for 4 feature specs

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Modern Android First
**Status**: ✅ PASS
**Evaluation**: All dependencies align with modern Android development (Kotlin, Compose, AndroidX, Material 3). No legacy Java or XML layouts being introduced.

### Principle II: User Privacy & Security
**Status**: ✅ PASS
**Evaluation**: EncryptedSharedPreferences ensures credentials stored securely. HTTPS enforced via Retrofit/OkHttp. INTERNET permission explicitly added. No debug logging of sensitive data planned.

### Principle III: Offline-First with Graceful Degradation
**Status**: ✅ PASS
**Evaluation**: UiState pattern includes explicit Loading/Error states for network feedback. Foundation prepares for future offline queueing (Spec 2 concern). Coroutines enable async network handling without blocking UI.

### Principle IV: Simplicity & Focus
**Status**: ✅ PASS
**Evaluation**: Minimal dependencies only - no unnecessary libraries. Foundation supports core goal (sending thoughts to Napkin.one). Directory structure is straightforward and follows constitutional organization.

### Principle V: Testing & Quality Assurance
**Status**: ✅ PASS
**Evaluation**: Testing framework dependencies included (JUnit, Espresso, Compose UI Test). ProGuard validation with R8 full mode ensures release quality. Build time constraint prevents excessive complexity.

### Architecture Pattern Compliance
**Status**: ✅ PASS
**Evaluation**: MVVM enforced via ViewModels, Repository pattern prepared (data/ structure), Hilt for DI testability, StateFlow for reactive state (constitution requirement).

### Development Standards Compliance
**Status**: ✅ PASS
**Evaluation**: Directory structure matches constitution exactly. Coroutines with proper dispatchers planned. StateFlow over LiveData as per constitution. Error handling with sealed UiState class.

**GATE RESULT**: ✅ ALL CHECKS PASS - Proceed to Phase 0 Research

## Project Structure

### Documentation (this feature)

```text
specs/001-architecture-foundation/
├── spec.md              # Feature specification (complete)
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output - dependency best practices, API contract research
├── data-model.md        # Phase 1 output - UiState, ThoughtRequest, entity relationships
├── quickstart.md        # Phase 1 output - Setup instructions for new developers
├── contracts/           # Phase 1 output - API contract documentation
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

```text
NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/
├── data/                          # Data layer (NEW - this spec)
│   ├── api/                       # Retrofit API interfaces (empty - prepared for Spec 2)
│   ├── model/                     # Data models (NEW)
│   │   ├── ThoughtRequest.kt      # API request model
│   │   └── UiState.kt             # Sealed class for UI states
│   └── repository/                # Repository pattern (empty - prepared for Spec 2)
├── ui/                            # UI layer (EXISTING, expand)
│   ├── theme/                     # EXISTING - Compose theme
│   ├── screens/                   # Composable screens (NEW - prepared for Spec 2)
│   └── components/                # Reusable composables (NEW - prepared for Spec 2)
├── viewmodel/                     # ViewModels (NEW - empty, prepared for Spec 2)
├── di/                            # Dependency injection modules (NEW - prepared for Spec 2)
├── utils/                         # Shared utilities (NEW - empty, prepared for future)
├── MainActivity.kt                # EXISTING - no changes this spec
└── NapkinApplication.kt           # NEW - Hilt application class

NapkinCollect/app/src/main/
└── AndroidManifest.xml            # MODIFIED - Add INTERNET permission, set android:name

NapkinCollect/app/
├── build.gradle.kts               # MODIFIED - Add dependencies, plugins, kapt config
└── proguard-rules.pro             # MODIFIED - Add rules for Retrofit, Gson, OkHttp, Hilt

NapkinCollect/
└── build.gradle.kts               # MODIFIED - Add Hilt plugin version

# Testing structure (prepared)
NapkinCollect/app/src/
├── test/                          # Unit tests (prepared for Spec 4)
└── androidTest/                   # Instrumented tests (prepared for Spec 4)
```

**Structure Decision**: Android mobile structure (Option 3 variant). This is a native Android application using modern Jetpack architecture. Source code lives in `NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/` with separation of concerns via data/, ui/, viewmodel/, di/, and utils/ packages as mandated by the constitution. Testing directories follow Android convention (test/ for unit, androidTest/ for instrumented).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations detected. This section intentionally left empty.*

## Phase 0: Research & Decisions

**Status**: See [research.md](./research.md)

**Research Topics**:
1. Android BOM vs Version Catalogs for dependency management
2. Hilt setup best practices and common annotation processing pitfalls
3. ProGuard/R8 configuration for Retrofit and Gson
4. Napkin.one API contract verification (response structure, status codes, error formats)
5. Build time optimization strategies for kapt/Hilt projects
6. EncryptedSharedPreferences implementation patterns

**Decisions Required**:
- Serialization library: Gson (already in spec) vs kotlinx.serialization
- StateFlow initialization patterns for UiState
- Directory structure for contracts/ (OpenAPI vs custom markdown)

## Phase 1: Design Artifacts

**Status**: See design documents in this directory

**Artifacts**:
- [data-model.md](./data-model.md) - Entity definitions, relationships, validation rules
- [contracts/](./contracts/) - API contract specifications
- [quickstart.md](./quickstart.md) - Developer onboarding guide

## Implementation Notes

### Build Time Baseline Measurement

**Pre-Implementation Baseline**: TBD (to be measured before any changes)
- Command: `./gradlew clean && time ./gradlew build`
- Measurement: Average of 3 runs
- Baseline time: [RECORD HERE]

**Post-Implementation Validation**: TBD (to be measured after implementation)
- Same command: `./gradlew clean && time ./gradlew build`
- Measurement: Average of 3 runs
- Post-implementation time: [RECORD HERE]
- Increase: [CALCULATE] (Must be < 30 seconds)

### ProGuard/R8 Validation Checklist

- [ ] Enable R8 full mode in gradle.properties
- [ ] Build release APK successfully
- [ ] Install release APK on physical device
- [ ] Launch app and verify Hilt initialization
- [ ] Check logcat for R8 warnings/errors
- [ ] Run automated test suite on release build
- [ ] Verify no ClassNotFoundException at runtime

### Risk Mitigation

**Risk**: Hilt annotation processing failures
**Mitigation**: Incremental debugging procedure documented in spec. Enable verbose kapt logging from start. Clean build after each change.

**Risk**: Dependency version conflicts
**Mitigation**: Use Compose BOM for androidx dependencies. Explicit version pinning for third-party libs. Test build after each dependency addition.

**Risk**: ProGuard rules incomplete
**Mitigation**: Early release build testing. R8 full mode validation. Automated tests on release variant.

**Risk**: Build time exceeds 30-second constraint
**Mitigation**: Measure baseline before changes. Incremental dependency addition. Consider kapt performance options if needed.

## Success Metrics

- **SC-001**: ✅ Project compiles successfully with zero errors
- **SC-002**: ✅ Application launches and Hilt initializes without crashes
- **SC-003**: ✅ All constitutional directory structure exists (6 directories)
- **SC-004**: ✅ Base models and classes can be imported and used
- **SC-005**: ✅ Build time increase < 30 seconds from baseline
- **SC-006**: ✅ ProGuard release build passes all validations

## Dependencies

**Upstream**: None (this is the foundation)
**Downstream**: All other specs depend on this foundation
- Spec 2 (Send Thought): Requires ViewModels, Repository pattern, API setup
- Spec 3 (Secure Settings): Requires EncryptedSharedPreferences setup
- Spec 4 (Quality Assurance): Requires testing framework setup

## References

- Constitution: `/memory/constitution.md` v1.1.0
- Spec: `./spec.md`
- Retrofit: https://square.github.io/retrofit/
- Hilt: https://dagger.dev/hilt/
- Napkin.one API: https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts
- Android BOM: https://developer.android.com/jetpack/compose/bom
- R8/ProGuard: https://developer.android.com/studio/build/shrink-code
