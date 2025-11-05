# Feature Specification: Quality Assurance

**Feature Branch**: `4-quality-assurance`
**Created**: 2025-11-05
**Status**: Draft
**Depends On**: Spec 1, Spec 2, Spec 3 (All previous specs must be implemented)

## User Scenarios & Testing

### User Story 1 - Reliable Application Behavior (Priority: P1)

As a developer, I need comprehensive test coverage to ensure the app behaves correctly so that users can trust it with their thoughts.

**Why this priority**: Testing prevents regressions and ensures constitution compliance (Principle V). Critical for app reliability.

**Independent Test**: Can be fully tested by running test suite and verifying 80%+ code coverage on critical paths (ViewModels, Repositories).

**Acceptance Scenarios**:

1. **Given** test suite is complete, **When** tests are executed, **Then** all tests pass with green status
2. **Given** code coverage is measured, **When** analyzing ViewModels and Repositories, **Then** coverage is at least 80%
3. **Given** UI tests are implemented, **When** executing on emulator, **Then** all critical user flows complete successfully

---

### User Story 2 - Regression Prevention (Priority: P1)

As a developer, I need automated tests to catch bugs before release so that users don't experience failures or data loss.

**Why this priority**: Constitution requires testing to prevent data loss and ensure reliability. Essential for user trust.

**Independent Test**: Can be tested by intentionally breaking code and verifying corresponding tests fail, then fixing code and verifying tests pass.

**Acceptance Scenarios**:

1. **Given** API client returns error, **When** repository test executes, **Then** test verifies error is properly mapped
2. **Given** invalid credentials, **When** ViewModel test executes, **Then** test verifies error state is set
3. **Given** network failure during send, **When** integration test executes, **Then** test verifies user sees appropriate error message

---

### User Story 3 - Continuous Integration Readiness (Priority: P2)

As a developer, I need tests that run reliably in CI/CD so that pull requests are automatically validated before merge.

**Why this priority**: Automation prevents manual testing overhead. Important for development velocity.

**Independent Test**: Can be tested by running tests in CI environment (GitHub Actions) and verifying consistent results.

**Acceptance Scenarios**:

1. **Given** tests run in CI, **When** build completes, **Then** all tests pass without flakiness
2. **Given** PR is created, **When** CI runs, **Then** test results are reported in PR status checks
3. **Given** tests fail in CI, **When** reviewing results, **Then** failure reasons are clear and actionable

---

### Edge Cases

- What happens if tests are run on different Android API levels?
- What happens if emulator is slow or unresponsive during UI tests?
- What happens if network mocking fails in repository tests?
- How are flaky tests identified and addressed?

## Requirements

### Functional Requirements

- **FR-001**: System MUST include unit tests for all ViewModels with 80%+ code coverage
- **FR-002**: System MUST include unit tests for all Repositories with 80%+ code coverage
- **FR-003**: System MUST include UI tests for critical user flows (send thought, save settings)
- **FR-004**: System MUST include integration test for end-to-end thought submission
- **FR-005**: System MUST mock network calls in repository tests (no real API calls in tests)
- **FR-006**: System MUST verify error handling paths in ViewModel tests
- **FR-007**: System MUST verify UI state changes in Compose UI tests
- **FR-008**: All tests MUST pass before code can be merged (enforced in CI)
- **FR-009**: System MUST generate code coverage report showing tested vs untested code
- **FR-010**: System MUST include test for EncryptedSharedPreferences storage/retrieval

### Key Entities

- **MainViewModelTest**: Unit tests for MainViewModel
- **SettingsViewModelTest**: Unit tests for SettingsViewModel
- **ThoughtRepositoryTest**: Unit tests for ThoughtRepository with mocked API
- **EncryptedCredentialsDataStoreTest**: Unit tests for credentials storage
- **MainScreenTest**: Compose UI tests for main screen
- **SettingsScreenTest**: Compose UI tests for settings screen
- **SendThoughtIntegrationTest**: End-to-end integration test

## Success Criteria

### Measurable Outcomes

- **SC-001**: 80%+ code coverage on ViewModels and Repositories
- **SC-002**: 100% of critical user flows have UI tests
- **SC-003**: All tests pass in under 3 minutes on standard CI runner
- **SC-004**: Zero flaky tests (tests pass consistently 100% of the time)
- **SC-005**: All error handling paths have dedicated test cases
- **SC-006**: Test suite catches 100% of intentionally introduced bugs in demo scenarios

## Technical Specifications

### Test Dependencies

**Add to `build.gradle.kts`**:

```kotlin
dependencies {
    // Existing dependencies...

    // Testing - JUnit & Kotlin
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Testing - Mockito / MockK
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")

    // Testing - Turbine (for Flow testing)
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Testing - Hilt
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptTest("com.google.dagger:hilt-compiler:2.48")

    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Compose Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Hilt Android Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48")
}
```

### ViewModel Unit Tests

**File**: `test/java/com/taquangkhoi/napkincollect/viewmodel/MainViewModelTest.kt`

```kotlin
package com.taquangkhoi.napkincollect.viewmodel

import app.cash.turbine.test
import com.taquangkhoi.napkincollect.data.model.Credentials
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.data.repository.CredentialsDataStore
import com.taquangkhoi.napkincollect.data.repository.ThoughtRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ThoughtRepository
    private lateinit var credentialsDataStore: CredentialsDataStore
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        credentialsDataStore = mockk()
        viewModel = MainViewModel(repository, credentialsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateThought updates state correctly`() = runTest {
        viewModel.uiState.test {
            assertEquals("", awaitItem().thought)

            viewModel.updateThought("Test thought")
            assertEquals("Test thought", awaitItem().thought)
        }
    }

    @Test
    fun `sendThought with empty thought shows error`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.sendThought()
            val state = awaitItem()

            assertTrue(state.sendState is UiState.Error)
            assertEquals("Please enter a thought", (state.sendState as UiState.Error).message)
        }
    }

    @Test
    fun `sendThought with missing credentials shows error`() = runTest {
        coEvery { credentialsDataStore.getCredentials() } returns null

        viewModel.updateThought("Test thought")

        viewModel.uiState.test {
            awaitItem() // Initial state with thought

            viewModel.sendThought()
            skipItems(1) // Loading state
            val state = awaitItem()

            assertTrue(state.sendState is UiState.Error)
            assertEquals(
                "Please configure credentials in settings",
                (state.sendState as UiState.Error).message
            )
        }
    }

    @Test
    fun `sendThought successful clears fields and shows success`() = runTest {
        val credentials = Credentials("test@example.com", "token123")
        coEvery { credentialsDataStore.getCredentials() } returns credentials
        coEvery {
            repository.sendThought(any(), any(), any(), any())
        } returns Result.success(Unit)

        viewModel.updateThought("Test thought")
        viewModel.updateSource("Test source")

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.sendThought()
            skipItems(1) // Loading state
            val state = awaitItem()

            assertEquals("", state.thought)
            assertEquals("", state.source)
            assertTrue(state.sendState is UiState.Success)
        }

        coVerify {
            repository.sendThought("test@example.com", "token123", "Test thought", "Test source")
        }
    }

    @Test
    fun `sendThought failure preserves fields and shows error`() = runTest {
        val credentials = Credentials("test@example.com", "token123")
        coEvery { credentialsDataStore.getCredentials() } returns credentials
        coEvery {
            repository.sendThought(any(), any(), any(), any())
        } returns Result.failure(Exception("Network error"))

        viewModel.updateThought("Test thought")

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.sendThought()
            skipItems(1) // Loading state
            val state = awaitItem()

            assertEquals("Test thought", state.thought)
            assertTrue(state.sendState is UiState.Error)
            assertEquals("Network error", (state.sendState as UiState.Error).message)
        }
    }
}
```

**File**: `test/java/com/taquangkhoi/napkincollect/viewmodel/SettingsViewModelTest.kt`

```kotlin
package com.taquangkhoi.napkincollect.viewmodel

import app.cash.turbine.test
import com.taquangkhoi.napkincollect.data.model.Credentials
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.data.repository.CredentialsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var credentialsDataStore: CredentialsDataStore
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        credentialsDataStore = mockk()
        coEvery { credentialsDataStore.getCredentials() } returns null
        viewModel = SettingsViewModel(credentialsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads existing credentials`() = runTest {
        val credentials = Credentials("test@example.com", "token123")
        coEvery { credentialsDataStore.getCredentials() } returns credentials

        val viewModel = SettingsViewModel(credentialsDataStore)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("test@example.com", state.email)
            assertEquals("token123", state.token)
        }
    }

    @Test
    fun `toggleTokenVisibility changes visibility`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().tokenVisible)

            viewModel.toggleTokenVisibility()
            assertTrue(awaitItem().tokenVisible)

            viewModel.toggleTokenVisibility()
            assertFalse(awaitItem().tokenVisible)
        }
    }

    @Test
    fun `saveCredentials with empty email shows error`() = runTest {
        viewModel.updateEmail("")
        viewModel.updateToken("token123")

        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.saveCredentials()
            val state = awaitItem()

            assertTrue(state.saveState is UiState.Error)
            assertEquals("Email is required", (state.saveState as UiState.Error).message)
        }
    }

    @Test
    fun `saveCredentials with invalid email shows error`() = runTest {
        viewModel.updateEmail("invalid-email")
        viewModel.updateToken("token123")

        viewModel.uiState.test {
            awaitItem()

            viewModel.saveCredentials()
            val state = awaitItem()

            assertTrue(state.saveState is UiState.Error)
            assertEquals("Invalid email format", (state.saveState as UiState.Error).message)
        }
    }

    @Test
    fun `saveCredentials successful saves and shows success`() = runTest {
        coEvery {
            credentialsDataStore.saveCredentials(any())
        } returns Result.success(Unit)

        viewModel.updateEmail("test@example.com")
        viewModel.updateToken("token123")

        viewModel.uiState.test {
            awaitItem()

            viewModel.saveCredentials()
            skipItems(1) // Loading state
            val state = awaitItem()

            assertTrue(state.saveState is UiState.Success)
        }

        coVerify {
            credentialsDataStore.saveCredentials(
                Credentials("test@example.com", "token123")
            )
        }
    }
}
```

### Repository Unit Tests

**File**: `test/java/com/taquangkhoi/napkincollect/data/repository/ThoughtRepositoryTest.kt`

```kotlin
package com.taquangkhoi.napkincollect.data.repository

import com.taquangkhoi.napkincollect.data.api.NapkinApi
import com.taquangkhoi.napkincollect.data.model.ThoughtRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class ThoughtRepositoryTest {

    private lateinit var api: NapkinApi
    private lateinit var repository: ThoughtRepository

    @Before
    fun setup() {
        api = mockk()
        repository = ThoughtRepository(api)
    }

    @Test
    fun `sendThought successful returns success`() = runTest {
        coEvery {
            api.createThought(any())
        } returns Response.success(Unit)

        val result = repository.sendThought(
            "test@example.com",
            "token123",
            "Test thought",
            "Test source"
        )

        assertTrue(result.isSuccess)
        coVerify {
            api.createThought(
                ThoughtRequest(
                    "test@example.com",
                    "token123",
                    "Test thought",
                    "Test source"
                )
            )
        }
    }

    @Test
    fun `sendThought with 401 returns auth error`() = runTest {
        coEvery {
            api.createThought(any())
        } returns Response.error(401, "".toResponseBody())

        val result = repository.sendThought(
            "test@example.com",
            "token123",
            "Test thought",
            ""
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid credentials") == true)
    }

    @Test
    fun `sendThought with 500 returns server error`() = runTest {
        coEvery {
            api.createThought(any())
        } returns Response.error(500, "".toResponseBody())

        val result = repository.sendThought(
            "test@example.com",
            "token123",
            "Test thought",
            ""
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Server error") == true)
    }

    @Test
    fun `sendThought with network exception returns network error`() = runTest {
        coEvery {
            api.createThought(any())
        } throws Exception("Unable to resolve host")

        val result = repository.sendThought(
            "test@example.com",
            "token123",
            "Test thought",
            ""
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("No internet connection") == true)
    }
}
```

### Compose UI Tests

**File**: `androidTest/java/com/taquangkhoi/napkincollect/ui/screens/MainScreenTest.kt`

```kotlin
package com.taquangkhoi.napkincollect.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.taquangkhoi.napkincollect.data.model.Credentials
import com.taquangkhoi.napkincollect.data.model.UiState
import com.taquangkhoi.napkincollect.data.repository.CredentialsDataStore
import com.taquangkhoi.napkincollect.data.repository.ThoughtRepository
import com.taquangkhoi.napkincollect.viewmodel.MainViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var repository: ThoughtRepository
    private lateinit var credentialsDataStore: CredentialsDataStore

    @Before
    fun setup() {
        hiltRule.inject()
        repository = mockk(relaxed = true)
        credentialsDataStore = mockk(relaxed = true)
    }

    @Test
    fun mainScreen_displaysAllUIElements() {
        composeTestRule.setContent {
            MainScreen()
        }

        composeTestRule.onNodeWithText("Enter your thought").assertIsDisplayed()
        composeTestRule.onNodeWithText("Source of Thought").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun mainScreen_sendButton_validatesEmptyThought() {
        val viewModel = MainViewModel(repository, credentialsDataStore)

        composeTestRule.setContent {
            MainScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Send").performClick()

        composeTestRule.onNodeWithText("Please enter a thought").assertIsDisplayed()
    }

    @Test
    fun mainScreen_successfullySendsThought() {
        coEvery { credentialsDataStore.getCredentials() } returns Credentials("test@example.com", "token123")
        coEvery { repository.sendThought(any(), any(), any(), any()) } returns Result.success(Unit)

        val viewModel = MainViewModel(repository, credentialsDataStore)

        composeTestRule.setContent {
            MainScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Enter your thought").performTextInput("Test thought")
        composeTestRule.onNodeWithText("Send").performClick()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Thought sent successfully!")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
```

### Integration Test

**File**: `androidTest/java/com/taquangkhoi/napkincollect/SendThoughtIntegrationTest.kt`

```kotlin
package com.taquangkhoi.napkincollect

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SendThoughtIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun endToEndThoughtSubmission_withCredentials() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Enter credentials
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("API Token").performTextInput("test_token")

        // Save credentials
        composeTestRule.onNodeWithText("Save Credentials").performClick()

        // Wait for success
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Credentials saved successfully!")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Enter thought
        composeTestRule.onNodeWithText("Enter your thought").performTextInput("Integration test thought")

        // Send thought
        composeTestRule.onNodeWithText("Send").performClick()

        // Verify success or error (depending on whether API is mocked or real)
        // In real integration test with mocked API, expect success
        composeTestRule.waitForIdle()
    }
}
```

### Code Coverage Configuration

**Add to `build.gradle.kts`**:

```kotlin
android {
    // ... existing config

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
```

**Generate coverage report**:
```bash
./gradlew testDebugUnitTestCoverage
```

Report will be generated at: `app/build/reports/coverage/test/debug/index.html`

## Assumptions

- Emulator or physical device available for UI tests
- CI environment has Android emulator configured
- Test execution time of 3 minutes is acceptable
- Mock API responses are sufficient (no testing against real Napkin.one API)
- Test code does not need same coverage as production code

## Out of Scope

- Performance testing (load testing, stress testing)
- Security testing (penetration testing, security audits)
- Accessibility testing (TalkBack, large text support)
- Screenshot tests (visual regression testing)
- Manual testing documentation (exploratory testing guides)
- Test data generators or fixtures library

## Dependencies

- **Spec 1**: Architecture Foundation (required for testable architecture)
- **Spec 2**: Send Thought Feature (required for testing main functionality)
- **Spec 3**: Secure Settings (required for testing settings functionality)

## References

- Napkandroid Constitution v1.1.0 (memory/constitution.md)
- Android Testing documentation: https://developer.android.com/training/testing
- Compose Testing documentation: https://developer.android.com/jetpack/compose/testing
- MockK documentation: https://mockk.io/
- Turbine Flow testing: https://github.com/cashapp/turbine
