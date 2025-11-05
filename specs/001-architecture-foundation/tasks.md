# Tasks: Architecture Foundation

**Feature**: 001-architecture-foundation
**Branch**: `001-architecture-foundation`
**Date**: 2025-11-05
**Status**: Ready for Implementation

## Overview

This document defines all implementation tasks for the Architecture Foundation feature, organized by user story to enable independent implementation and testing. Each task follows the strict checklist format with task IDs, parallelization markers, and story labels.

**Implementation Strategy**: MVP-first, incremental delivery. Complete User Story 1 (Development Infrastructure) first, then User Story 2 (Code Organization). Both stories are P1 and required for the foundation.

**Tests**: NOT included - This is foundational work with verification steps instead of automated tests. Spec 4 (Quality Assurance) will add comprehensive testing.

---

## Phase 1: Setup (Project Initialization)

**Goal**: Establish baseline metrics and prepare for implementation.

**Tasks**:

- [ ] T001 Measure baseline build time with `./gradlew clean && time ./gradlew build` (run 3 times, record average in NapkinCollect/app/build.gradle.kts comments)
- [ ] T002 Verify current project structure with `ls -la NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/`
- [ ] T003 Create backup of current build.gradle.kts files at NapkinCollect/app/build.gradle.kts and NapkinCollect/build.gradle.kts

**Completion Criteria**:
- Baseline build time recorded
- Current project state documented
- Backup created for rollback if needed

---

## Phase 2: Foundational Tasks (Blocking Prerequisites)

**Goal**: Add all required dependencies and configurations that are prerequisites for both user stories.

**Tasks**:

- [ ] T004 Add Hilt plugin version to NapkinCollect/build.gradle.kts (version 2.48)
- [ ] T005 Add plugins to NapkinCollect/app/build.gradle.kts (com.google.dagger.hilt.android, kotlin-kapt)
- [ ] T006 [P] Add ViewModel and Lifecycle dependencies to NapkinCollect/app/build.gradle.kts (lifecycle-viewmodel-compose:2.7.0, lifecycle-runtime-compose:2.7.0)
- [ ] T007 [P] Add Retrofit and OkHttp dependencies to NapkinCollect/app/build.gradle.kts (retrofit:2.9.0, converter-gson:2.9.0, okhttp:4.12.0, logging-interceptor:4.12.0)
- [ ] T008 [P] Add Kotlin Coroutines dependencies to NapkinCollect/app/build.gradle.kts (kotlinx-coroutines-android:1.7.3, kotlinx-coroutines-core:1.7.3)
- [ ] T009 [P] Add Hilt dependencies to NapkinCollect/app/build.gradle.kts (hilt-android:2.48, hilt-compiler:2.48 with kapt, hilt-navigation-compose:1.1.0)
- [ ] T010 [P] Add EncryptedSharedPreferences dependency to NapkinCollect/app/build.gradle.kts (security-crypto:1.1.0-alpha06)
- [ ] T011 [P] Add Gson dependency to NapkinCollect/app/build.gradle.kts (gson:2.10.1)
- [ ] T012 Configure kapt settings in NapkinCollect/app/build.gradle.kts (correctErrorTypes=true, useBuildCache=false, verbose logging)
- [ ] T013 Add ProGuard rules for Retrofit to NapkinCollect/app/proguard-rules.pro
- [ ] T014 [P] Add ProGuard rules for Gson to NapkinCollect/app/proguard-rules.pro
- [ ] T015 [P] Add ProGuard rules for OkHttp to NapkinCollect/app/proguard-rules.pro
- [ ] T016 [P] Add ProGuard rules for Hilt to NapkinCollect/app/proguard-rules.pro
- [ ] T017 [P] Add ProGuard rules for data models to NapkinCollect/app/proguard-rules.pro (keep all classes in data.model package)
- [ ] T018 Enable R8 full mode in NapkinCollect/gradle.properties (android.enableR8.fullMode=true)
- [ ] T019 Run Gradle sync and verify all dependencies resolve successfully

**Completion Criteria**:
- All dependencies added to build.gradle.kts
- Hilt plugins configured
- ProGuard rules complete
- R8 full mode enabled
- Gradle sync successful

**Parallel Opportunities**: Tasks T006-T011 (dependency additions) and T013-T017 (ProGuard rules) can be done in parallel as they modify different sections of files.

---

## Phase 3: User Story 1 - Development Infrastructure Ready (P1)

**Story Goal**: Configure project with all required dependencies and architectural foundations so that features can be implemented following the constitution's MVVM pattern.

**Independent Test Criteria**:
1. Project compiles successfully with zero errors after dependency addition
2. Application launches and Hilt initializes without crashes
3. Base architectural classes are available for import

**Tasks**:

- [ ] T020 [US1] Create NapkinApplication.kt in NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/NapkinApplication.kt
- [ ] T021 [US1] Add @HiltAndroidApp annotation to NapkinApplication class
- [ ] T022 [US1] Update AndroidManifest.xml to add INTERNET permission in NapkinCollect/app/src/main/AndroidManifest.xml
- [ ] T023 [US1] Update AndroidManifest.xml to set android:name=".NapkinApplication" in <application> tag
- [ ] T024 [US1] Build project with `./gradlew build` and verify zero compilation errors
- [ ] T025 [US1] Run application and verify Hilt initializes without crashes (check logcat for Hilt initialization messages)
- [ ] T026 [US1] Verify no Hilt annotation processing errors in build output

**Completion Criteria**:
- ✅ SC-002: Application launches and Hilt initializes without crashes
- ✅ FR-002: Hilt dependency injection configured with Application class
- ✅ FR-004: INTERNET permission added to AndroidManifest.xml
- ✅ FR-007: Hilt plugins and kapt configuration complete

**Parallel Opportunities**: T020-T023 are independent file modifications that could be done in parallel.

---

## Phase 4: User Story 2 - Code Organization Matches Constitution (P1)

**Story Goal**: Organize codebase according to the constitution's structure so that code is maintainable and follows established patterns.

**Independent Test Criteria**:
1. All required directories exist as specified in constitution (data/, ui/, viewmodel/, di/, utils/)
2. Sample data models compile in correct locations
3. Hilt can discover and process DI modules

**Tasks**:

- [ ] T027 [P] [US2] Create data/api/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/data/api/
- [ ] T028 [P] [US2] Create data/model/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/data/model/
- [ ] T029 [P] [US2] Create data/repository/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/data/repository/
- [ ] T030 [P] [US2] Create ui/screens/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/ui/screens/
- [ ] T031 [P] [US2] Create ui/components/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/ui/components/
- [ ] T032 [P] [US2] Create viewmodel/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/viewmodel/
- [ ] T033 [P] [US2] Create di/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/di/
- [ ] T034 [P] [US2] Create utils/ directory at NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/utils/
- [ ] T035 [US2] Create UiState.kt in NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/data/model/UiState.kt
- [ ] T036 [US2] Define sealed class UiState<T> with Idle, Loading, Success<T>, Error states
- [ ] T037 [US2] Create ThoughtRequest.kt in NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/data/model/ThoughtRequest.kt
- [ ] T038 [US2] Define ThoughtRequest data class with @SerializedName annotations (email, token, thought, sourceUrl)
- [ ] T039 [US2] Create .gitkeep files in empty directories (api/, repository/, screens/, components/, viewmodel/, di/, utils/)
- [ ] T040 [US2] Verify all directories exist with `find NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect -type d`
- [ ] T041 [US2] Verify UiState can be imported in a test file or MainActivity
- [ ] T042 [US2] Verify ThoughtRequest can be imported and instantiated

**Completion Criteria**:
- ✅ SC-003: All constitutional directory structure exists (6 directories: data, ui, viewmodel, di, utils, and existing components)
- ✅ SC-004: Base models and classes can be imported and used in other modules
- ✅ FR-003: Project includes directory structure matching constitution specification
- ✅ FR-005: Base data models for API communication created (ThoughtRequest)
- ✅ FR-008: Base sealed class for UiState pattern created

**Parallel Opportunities**: T027-T034 (directory creation) can all be done in parallel. T035-T038 (file creation) can be done in parallel after directories exist.

---

## Phase 5: Validation & Polish

**Goal**: Validate that all success criteria are met and the foundation is ready for future features.

**Tasks**:

- [ ] T043 Build project with `./gradlew clean build` and verify zero errors (SC-001)
- [ ] T044 Measure post-implementation build time with `time ./gradlew clean build` (run 3 times, calculate average)
- [ ] T045 Verify build time increase is less than 30 seconds compared to baseline (SC-005)
- [ ] T046 Build release APK with `./gradlew assembleRelease` and verify ProGuard/R8 completes successfully
- [ ] T047 Install release APK on device/emulator and verify app launches without crashes
- [ ] T048 Check logcat during release build for R8 warnings or errors
- [ ] T049 Verify no ClassNotFoundException or NoSuchMethodException at runtime in release build (SC-006)
- [ ] T050 Document baseline and post-implementation build times in specs/001-architecture-foundation/plan.md
- [ ] T051 Verify all 6 success criteria are met (SC-001 through SC-006)

**Completion Criteria**:
- ✅ SC-001: Project compiles successfully with zero errors
- ✅ SC-005: Build time increase < 30 seconds from baseline
- ✅ SC-006: ProGuard release build passes all validations
- All validation metrics documented
- Foundation ready for Spec 2 (Send Thought Feature)

**Parallel Opportunities**: None - validation tasks must be sequential to verify cumulative changes.

---

## Dependencies

### User Story Dependencies (Completion Order)

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational)
    ↓
    ├─→ User Story 1 (Development Infrastructure)
    └─→ User Story 2 (Code Organization)
         ↓
Phase 5 (Validation & Polish)
```

**Notes**:
- User Story 1 and User Story 2 are both P1 priority
- User Story 2 depends on User Story 1 (need Hilt working before creating directories)
- Both stories must complete before Spec 2 (Send Thought Feature) can begin
- Phase 2 (Foundational) is a blocking prerequisite for all user stories

### External Dependencies

**Upstream**: None (this is the foundation)

**Downstream**: All other specs depend on this foundation
- Spec 2 (Send Thought): Requires ViewModels, Repository pattern, API setup
- Spec 3 (Secure Settings): Requires EncryptedSharedPreferences setup
- Spec 4 (Quality Assurance): Requires testing framework setup

---

## Parallel Execution Examples

### Phase 2: Foundational Tasks
**Parallel Group 1** (Independent dependency additions):
```bash
# Can be done simultaneously by different developers or in a single commit
- T006: Add ViewModel/Lifecycle dependencies
- T007: Add Retrofit/OkHttp dependencies
- T008: Add Coroutines dependencies
- T009: Add Hilt dependencies
- T010: Add EncryptedSharedPreferences
- T011: Add Gson dependency
```

**Parallel Group 2** (Independent ProGuard rules):
```bash
# Can be done simultaneously - different sections of proguard-rules.pro
- T013: Retrofit ProGuard rules
- T014: Gson ProGuard rules
- T015: OkHttp ProGuard rules
- T016: Hilt ProGuard rules
- T017: Data model ProGuard rules
```

### Phase 3: User Story 1
**Parallel Group 3** (Independent file modifications):
```bash
# Can be done simultaneously - different files
- T020: Create NapkinApplication.kt
- T022: Update AndroidManifest.xml (INTERNET permission)
- T023: Update AndroidManifest.xml (android:name)
```

### Phase 4: User Story 2
**Parallel Group 4** (Independent directory creation):
```bash
# Can be done simultaneously - all mkdir commands
- T027: Create data/api/
- T028: Create data/model/
- T029: Create data/repository/
- T030: Create ui/screens/
- T031: Create ui/components/
- T032: Create viewmodel/
- T033: Create di/
- T034: Create utils/
```

**Parallel Group 5** (Independent file creation):
```bash
# Can be done simultaneously after directories exist
- T035-T036: Create UiState.kt
- T037-T038: Create ThoughtRequest.kt
```

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**MVP = Complete Architecture Foundation**
- All tasks in this spec must complete (no optional tasks)
- Foundation is atomic - cannot be partially implemented
- Estimated effort: 2-4 hours

**Why this is MVP**:
- Spec 2 (Send Thought) cannot proceed without complete foundation
- Dependencies, directory structure, and base models are all prerequisites
- Partial foundation would violate constitution requirements

### Incremental Delivery Approach

1. **Iteration 1: Phase 1 & 2** (Setup + Foundational)
   - Measure baseline
   - Add all dependencies
   - Configure ProGuard/R8
   - **Checkpoint**: Project compiles with new dependencies

2. **Iteration 2: Phase 3** (User Story 1)
   - Configure Hilt application
   - Add INTERNET permission
   - **Checkpoint**: App launches with Hilt initialized

3. **Iteration 3: Phase 4** (User Story 2)
   - Create directory structure
   - Create base models (UiState, ThoughtRequest)
   - **Checkpoint**: All directories exist, models compile

4. **Iteration 4: Phase 5** (Validation)
   - Build time verification
   - Release build validation
   - **Checkpoint**: All success criteria met

### Rollback Plan

If issues occur during implementation:
1. Restore from backup (T003)
2. Revert to specific phase using git
3. Re-run Gradle sync after reverting

---

## Task Summary

### Total Tasks: 51

**By Phase**:
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundational): 16 tasks
- Phase 3 (User Story 1): 7 tasks
- Phase 4 (User Story 2): 16 tasks
- Phase 5 (Validation): 9 tasks

**By User Story**:
- Setup/Foundational: 19 tasks (no story label)
- User Story 1: 7 tasks ([US1])
- User Story 2: 16 tasks ([US2])
- Validation/Polish: 9 tasks (no story label)

**Parallelizable Tasks**: 22 tasks marked with [P]
- Phase 2: 11 parallelizable tasks (T006-T011, T014-T017)
- Phase 4: 11 parallelizable tasks (T027-T034, T035-T038)

**Sequential Tasks**: 29 tasks (require completion of previous tasks)

---

## Format Validation

✅ All tasks follow checklist format: `- [ ] [TaskID] [P?] [Story?] Description with file path`
✅ Task IDs are sequential (T001-T051)
✅ User story labels present for story-specific tasks ([US1], [US2])
✅ Parallelizable tasks marked with [P]
✅ All tasks include specific file paths
✅ Independent test criteria defined for each user story
✅ Dependencies clearly documented

---

## References

- Constitution: `/memory/constitution.md` v1.1.0
- Feature Spec: `./spec.md`
- Implementation Plan: `./plan.md`
- Data Model: `./data-model.md`
- Research: `./research.md`
- API Contract: `./contracts/napkin-api.md`
- Quickstart: `./quickstart.md`

---

**Document Version**: 1.0
**Status**: Ready for Implementation
**Last Updated**: 2025-11-05
