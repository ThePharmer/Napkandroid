<!--
Sync Impact Report:
- Version change: 1.0.0 → 1.1.0
- Amendment: Added Kotlin-specific development standards
- Modified sections: Development Standards (expanded with Kotlin best practices)
- Added guidance:
  ✅ Kotlin Coroutines patterns and requirements
  ✅ StateFlow vs LiveData preferences
  ✅ Compose state management best practices
  ✅ Error handling patterns (sealed classes, Result type)
- Templates requiring updates:
  ⚠ /.specs/spec-template.md - Review for alignment with Kotlin patterns
  ⚠ /.specs/plan-template.md - Review for alignment with async patterns
  ⚠ /.specs/tasks-template.md - Review for alignment with testing requirements
- Follow-up TODOs: None
-->

# Napkandroid Constitution

## Core Principles

### I. Modern Android First

All new features MUST be developed using modern Android development practices:
- Kotlin as the primary programming language
- Jetpack Compose for UI development
- Material Design 3 for consistent user experience
- AndroidX libraries for compatibility and modern APIs

**Rationale**: The project maintains dual implementations (Java legacy, Kotlin modern) to demonstrate evolution. New development focuses exclusively on the Kotlin/Compose codebase in `NapkinCollect/` to establish best practices and maintainability.

### II. User Privacy & Security

User credentials and sensitive data MUST be protected:
- API tokens and email addresses MUST be stored securely using EncryptedSharedPreferences or Android Keystore
- Network communication MUST use HTTPS exclusively
- No sensitive data MUST be logged or exposed in debug outputs
- Users MUST have clear control over their authentication credentials

**Rationale**: The application handles Napkin.one API credentials that grant access to user thought collections. Protecting these credentials is paramount to user trust and data security.

### III. Offline-First with Graceful Degradation

The application MUST handle network conditions gracefully:
- Network failures MUST provide clear user feedback
- Thoughts MUST be queued locally when network is unavailable (future enhancement)
- Users MUST know the status of their submissions (pending, sent, failed)
- Retry mechanisms MUST be implemented for transient failures

**Rationale**: Mobile applications operate in varied network conditions. Users should never lose their thoughts due to connectivity issues.

### IV. Simplicity & Focus

Feature development MUST maintain application simplicity:
- Core functionality is sending thoughts to Napkin.one - features MUST support this goal
- UI MUST remain intuitive with minimal learning curve
- Configuration MUST be straightforward (email + token only)
- Avoid feature creep - justify each addition against user value

**Rationale**: Napkandroid is a focused productivity tool. Complexity dilutes the core value proposition of quick thought capture.

### V. Testing & Quality Assurance

Code quality MUST be maintained through systematic testing:
- Unit tests MUST cover business logic (API communication, data validation)
- UI tests MUST verify critical user flows (send thought, configure settings)
- Integration tests MUST validate API contract with Napkin.one
- Manual testing checklist MUST be completed before release

**Rationale**: The application directly interacts with user data and external APIs. Testing prevents data loss and ensures reliability.

## Development Standards

### Architecture Pattern

- **MVVM (Model-View-ViewModel)** MUST be adopted for the Kotlin/Compose implementation
- ViewModels MUST handle business logic and state management
- Composables MUST remain purely presentational
- Repository pattern MUST abstract data sources (API, local storage)
- Dependency injection SHOULD be used for testability (Hilt or Koin)

### Code Organization

```
NapkinCollect/app/src/main/java/com/taquangkhoi/napkincollect/
├── data/           # Data layer (repositories, API clients, models)
├── ui/             # UI layer (composables, screens, theme)
├── viewmodel/      # ViewModels for state management
├── di/             # Dependency injection modules
└── utils/          # Shared utilities and extensions
```

### API Integration

- HTTP client MUST use OkHttp or Retrofit for type-safe API calls
- API models MUST use data classes with kotlinx.serialization or Gson
- Network errors MUST be mapped to user-friendly messages
- API endpoint: `https://app.napkin.one/api/createThought`
- Request format MUST match documented JSON schema:
  ```json
  {
    "email": "string",
    "token": "string",
    "thought": "string",
    "sourceUrl": "string"
  }
  ```

### State Management

- UI state MUST be represented as immutable data classes
- State updates MUST flow unidirectionally (user action → ViewModel → UI)
- Loading, success, and error states MUST be explicitly modeled
- Configuration changes MUST preserve user input (thought text, source URL)

### Kotlin Coroutines

Asynchronous operations MUST use Kotlin Coroutines:
- Network calls MUST be executed as `suspend` functions
- ViewModels MUST use `viewModelScope` for coroutine lifecycle management
- Repository functions MUST be `suspend` functions for async operations
- Structured concurrency MUST be maintained (no `GlobalScope`)
- Coroutine dispatchers MUST be explicitly specified:
  - `Dispatchers.IO` for network/database operations
  - `Dispatchers.Main` for UI updates (default in `viewModelScope`)
  - `Dispatchers.Default` for CPU-intensive work

**Example**:
```kotlin
class MainViewModel(private val repository: ThoughtRepository) : ViewModel() {
    fun sendThought(thought: String) {
        viewModelScope.launch {
            // Automatically runs on Dispatchers.Main
            val result = repository.sendThought(thought) // suspend function
            // Handle result
        }
    }
}
```

### Reactive State with StateFlow

State management MUST use `StateFlow` over `LiveData`:
- ViewModels MUST expose state as `StateFlow<T>` or `StateFlow<UiState>`
- Use `MutableStateFlow` internally, expose as `StateFlow` (immutability)
- UI MUST collect state using `collectAsState()` in Composables
- Prefer `StateFlow` for its Kotlin-first design and better Compose integration

**Example**:
```kotlin
class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    fun updateThought(thought: String) {
        _uiState.update { it.copy(thought = thought) }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // Use uiState to render UI
}
```

### Compose State Management

Composable state management MUST follow these patterns:
- Use `rememberSaveable` for state that should survive process death
- Use `remember` for state that only needs to survive recomposition
- State SHOULD be hoisted to ViewModels for business logic
- Local UI state (e.g., text field focus) MAY remain in Composables
- Avoid `mutableStateOf` in Composables for business data

**Configuration Survival**:
```kotlin
// ❌ Wrong: Lost on config change
var thought by remember { mutableStateOf("") }

// ✅ Correct: Survives config change (but prefer ViewModel)
var thought by rememberSaveable { mutableStateOf("") }

// ✅ Best: State in ViewModel (survives process death if using SavedStateHandle)
val thought by viewModel.uiState.collectAsState()
```

### Error Handling

Error handling MUST use type-safe patterns:
- Use sealed classes or `Result<T>` for operation outcomes
- Network errors MUST be caught and mapped to user-friendly messages
- UI states MUST include explicit error states (not just null checks)
- Use exhaustive `when` expressions for handling all error cases

**Example with Sealed Class**:
```kotlin
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ViewModel
private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

// UI must handle all cases
when (val state = uiState.value) {
    is UiState.Idle -> { /* Show idle state */ }
    is UiState.Loading -> { /* Show loading spinner */ }
    is UiState.Success -> { /* Show success message */ }
    is UiState.Error -> { /* Show error: state.message */ }
}
```

**Example with Result Type**:
```kotlin
suspend fun sendThought(request: ThoughtRequest): Result<Unit> {
    return try {
        api.createThought(request)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Kotlin Coding Standards

All Kotlin code MUST follow these conventions:
- Use data classes for immutable data models
- Prefer `val` over `var` (immutability by default)
- Use nullable types (`T?`) explicitly, avoid `!!` operator
- Use scope functions appropriately (`let`, `apply`, `run`, `with`, `also`)
- Use extension functions for utility operations
- Keep functions small and single-purpose
- Use meaningful parameter names (avoid single letters except for lambdas)

## Quality Gates

### Definition of Done

A feature is considered complete when:

1. **Functionality**: All acceptance criteria met and manually verified
2. **Code Quality**:
   - Follows Kotlin coding conventions
   - No compiler warnings
   - Code reviewed by peer or AI assistant
3. **Testing**:
   - Unit tests written and passing
   - Critical paths covered by UI tests
   - Manual testing checklist completed
4. **Documentation**:
   - Public APIs documented with KDoc
   - README updated if user-facing changes
   - Spec and implementation plan marked complete
5. **Security**:
   - No credentials in code or logs
   - Sensitive data properly encrypted
   - Network security validated

### Release Checklist

Before any release to users:

- [ ] All automated tests passing
- [ ] Manual smoke test on physical device
- [ ] Credentials storage verified as secure
- [ ] API integration tested with real Napkin.one account
- [ ] ProGuard rules verified (release builds)
- [ ] Version code and version name incremented
- [ ] Changelog updated with user-facing changes
- [ ] No debug logging in production build

## Development Workflow

### Feature Development Process

1. **Specification** (`/specify`): Define feature requirements, user stories, success criteria
2. **Clarification** (`/clarify`): Resolve ambiguities and validate assumptions
3. **Planning** (`/plan`): Design implementation approach, identify files to modify
4. **Implementation** (`/implement`): Write code following architectural patterns
5. **Validation** (`/checklist`): Verify against quality gates and requirements
6. **Review**: Code review and manual testing
7. **Integration**: Merge to main branch after approval

### Branch Strategy

- **Main branch**: Stable, release-ready code
- **Feature branches**: Format `<number>-<short-name>` (e.g., `1-network-integration`)
- **Development**: Work in feature branches created via spec-kit
- **Integration**: Merge via pull requests with review

### Commit Standards

- Use conventional commit format: `type(scope): description`
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- Keep commits focused and atomic
- Reference spec/task numbers when applicable

## Governance

### Constitutional Authority

This constitution supersedes all other development practices and guidelines. When conflicts arise between this document and other guidance, this constitution takes precedence.

### Amendment Process

Constitution amendments require:

1. **Proposal**: Document the proposed change with rationale and impact analysis
2. **Review**: Evaluate consistency with project goals and existing principles
3. **Version Update**: Increment version according to semantic versioning:
   - **MAJOR**: Backward-incompatible principle changes or removals
   - **MINOR**: New principles or material expansions
   - **PATCH**: Clarifications, wording improvements, non-semantic changes
4. **Update**: Modify constitution with sync impact report
5. **Propagation**: Update dependent templates and documentation
6. **Commit**: Record amendment with clear commit message

### Compliance Verification

All pull requests MUST:

- Verify compliance with applicable principles (security, testing, architecture)
- Justify any complexity added to the codebase
- Follow the development workflow defined above
- Meet quality gate requirements

### Guidance Reference

For runtime development guidance and practical examples, refer to:
- **README.md**: Project overview and setup instructions
- **/.specs/**: Feature specifications and implementation plans
- **/.claude/commands/**: Available workflow commands and their usage

**Version**: 1.1.0 | **Ratified**: 2025-11-05 | **Last Amended**: 2025-11-05
