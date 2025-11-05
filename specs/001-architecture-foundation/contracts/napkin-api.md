# API Contract: Napkin.one Thought Creation

**Service**: Napkin.one API
**Endpoint**: `POST /api/createThought`
**Base URL**: `https://app.napkin.one`
**Version**: Unknown (no API versioning documented)
**Status**: PARTIAL - Request format confirmed, response format TBD

## Overview

The Napkin.one API provides an endpoint for creating thoughts programmatically. This contract documents the request format (confirmed from specification) and expected response behavior (to be verified during implementation).

## Authentication

**Method**: Token-based authentication (included in request body)

**Credentials**:
- Email address (user's Napkin.one account email)
- API token (obtained from Napkin.one account settings)

**Security**:
- Credentials MUST be stored in EncryptedSharedPreferences
- Credentials MUST be transmitted over HTTPS only
- Credentials MUST NOT be logged in any build variant

## Endpoint: Create Thought

### Request

**Method**: `POST`

**URL**: `https://app.napkin.one/api/createThought`

**Headers**:
```
Content-Type: application/json
```

**Request Body**:

```json
{
  "email": "string",
  "token": "string",
  "thought": "string",
  "sourceUrl": "string"
}
```

**Field Specifications**:

| Field | Type | Required | Description | Constraints |
|-------|------|----------|-------------|-------------|
| email | string | Yes | User's Napkin.one account email | Valid email format |
| token | string | Yes | API authentication token | Non-empty string |
| thought | string | Yes | Thought content to capture | Non-empty, max length unknown |
| sourceUrl | string | Yes | Source URL for thought context | Valid URL or empty string |

**Example Request**:
```json
{
  "email": "user@example.com",
  "token": "abc123def456token",
  "thought": "Great idea for improving productivity workflows",
  "sourceUrl": "https://example.com/article-about-productivity"
}
```

**Validation** (Client-Side):
- `email`: Validated as email format before request
- `token`: Validated as non-empty before request
- `thought`: Trimmed of leading/trailing whitespace, validated as non-empty
- `sourceUrl`: Optional - empty string if not provided

---

### Response

**Status**: ⚠️ UNKNOWN - To be determined during Spec 2 implementation

**Reason**: API documentation at https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts returned 403 Forbidden during research phase.

**Expected Success Response** (Estimated):

**Status Code**: `200 OK` or `201 Created`

**Response Body** (Hypothetical):
```json
{
  "success": true,
  "thoughtId": "string",
  "createdAt": "ISO 8601 timestamp",
  "message": "Thought created successfully"
}
```

**Expected Error Responses** (Estimated):

#### 400 Bad Request
Invalid request format or missing required fields.

```json
{
  "success": false,
  "error": "Bad Request",
  "message": "Invalid email format"
}
```

#### 401 Unauthorized
Invalid credentials (email/token mismatch).

```json
{
  "success": false,
  "error": "Unauthorized",
  "message": "Invalid API token"
}
```

#### 403 Forbidden
Valid credentials but no access (e.g., account suspended).

```json
{
  "success": false,
  "error": "Forbidden",
  "message": "Account access denied"
}
```

#### 429 Too Many Requests
Rate limit exceeded.

```json
{
  "success": false,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later."
}
```

#### 500 Internal Server Error
Server-side error.

```json
{
  "success": false,
  "error": "Internal Server Error",
  "message": "An error occurred processing your request"
}
```

---

## Implementation Strategy

### Phase 1: Architecture Foundation (Current)
- ✅ Document request format (confirmed from spec)
- ⚠️ Defer response model creation (response structure unknown)
- ✅ Prepare Retrofit dependencies

### Phase 2: Send Thought Feature (Spec 2)
- Implement Retrofit API interface with flexible response handling
- Test with real Napkin.one account credentials
- Capture actual response structure through logging/debugging
- Update this contract with confirmed response format
- Create ThoughtResponse data model based on actual response

### Testing Approach (Spec 2)

```kotlin
// Initial implementation with flexible response capture
interface NapkinApiService {
    @POST("api/createThought")
    suspend fun createThought(
        @Body request: ThoughtRequest
    ): Response<ResponseBody>  // Capture raw response
}

// In Repository
suspend fun sendThought(request: ThoughtRequest): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.createThought(request)
            if (response.isSuccessful) {
                // Log response for analysis (debug builds only)
                val rawResponse = response.body()?.string()
                Log.d("NapkinAPI", "Success response: $rawResponse")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.d("NapkinAPI", "Error response: $errorBody")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

After capturing actual responses:
1. Analyze response structure
2. Create ThoughtResponse data class
3. Update API interface to use typed response
4. Update this contract documentation with confirmed format

---

## Error Handling Strategy

### Client-Side Validation (Before Request)
```kotlin
// In ViewModel
fun validateAndSendThought(thought: String, sourceUrl: String) {
    val trimmedThought = thought.trim()

    // Validation
    if (trimmedThought.isEmpty()) {
        _uiState.value = UiState.Error("Thought cannot be empty")
        return
    }

    if (email.isEmpty() || token.isEmpty()) {
        _uiState.value = UiState.Error("Please configure your Napkin.one credentials")
        return
    }

    // Proceed with API call
    sendThought(trimmedThought, sourceUrl)
}
```

### Network Error Handling (After Request)
```kotlin
// In Repository
suspend fun sendThought(request: ThoughtRequest): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.createThought(request)

            when {
                response.isSuccessful -> {
                    Result.success(Unit)
                }
                response.code() == 400 -> {
                    Result.failure(Exception("Invalid request format"))
                }
                response.code() == 401 -> {
                    Result.failure(Exception("Invalid credentials"))
                }
                response.code() == 429 -> {
                    Result.failure(Exception("Rate limit exceeded"))
                }
                response.code() >= 500 -> {
                    Result.failure(Exception("Server error - please try again later"))
                }
                else -> {
                    Result.failure(Exception("HTTP ${response.code()}"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error - check your connection"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
}
```

### UI Error Display (In Composable)
```kotlin
when (val state = uiState.value) {
    is UiState.Error -> {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Error") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }
    // ... other states
}
```

---

## Rate Limiting

**Status**: Unknown

**Best Practices**:
- Implement client-side throttling (e.g., max 1 request per second)
- Handle 429 responses gracefully with user-friendly messages
- Consider exponential backoff for retries (if rate limits are encountered)

**Implementation** (if needed in future):
```kotlin
// In Repository
private var lastRequestTime: Long = 0
private val minRequestInterval = 1000L // 1 second

suspend fun sendThought(request: ThoughtRequest): Result<Unit> {
    val now = System.currentTimeMillis()
    val timeSinceLastRequest = now - lastRequestTime

    if (timeSinceLastRequest < minRequestInterval) {
        delay(minRequestInterval - timeSinceLastRequest)
    }

    lastRequestTime = System.currentTimeMillis()

    // ... proceed with API call
}
```

---

## Testing Strategy

### Contract Testing (Spec 4)

```kotlin
@Test
fun `verify thought request serialization`() {
    val request = ThoughtRequest(
        email = "test@example.com",
        token = "test-token",
        thought = "Test thought",
        sourceUrl = "https://example.com"
    )

    val json = gson.toJson(request)
    val expected = """
        {
          "email": "test@example.com",
          "token": "test-token",
          "thought": "Test thought",
          "sourceUrl": "https://example.com"
        }
    """.trimIndent()

    JSONAssert.assertEquals(expected, json, JSONCompareMode.STRICT)
}
```

### Integration Testing with Mock Server

```kotlin
@Test
fun `verify API call with mock server`() = runTest {
    val mockWebServer = MockWebServer()
    mockWebServer.start()

    // Enqueue successful response
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"success": true}""")
    )

    val request = ThoughtRequest(
        email = "test@example.com",
        token = "test-token",
        thought = "Test thought",
        sourceUrl = ""
    )

    val result = repository.sendThought(request)

    assertTrue(result.isSuccess)
    mockWebServer.shutdown()
}
```

### Manual Testing Checklist (Spec 2)

- [ ] Send thought with valid credentials → Success
- [ ] Send thought with invalid token → 401 error
- [ ] Send thought with empty thought → 400 error (or client validation)
- [ ] Send thought with no internet → Network error
- [ ] Send thought with valid sourceUrl → Success
- [ ] Send thought with empty sourceUrl → Success
- [ ] Verify thought appears in Napkin.one account

---

## Retrofit Configuration

### API Interface (Spec 2)

```kotlin
package com.taquangkhoi.napkincollect.data.api

import com.taquangkhoi.napkincollect.data.model.ThoughtRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface NapkinApiService {
    @POST("api/createThought")
    suspend fun createThought(
        @Body request: ThoughtRequest
    ): Response<ResponseBody>
}
```

### Retrofit Instance (Spec 2)

```kotlin
package com.taquangkhoi.napkincollect.di

import com.taquangkhoi.napkincollect.data.api.NapkinApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
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
    fun provideNapkinApiService(retrofit: Retrofit): NapkinApiService {
        return retrofit.create(NapkinApiService::class.java)
    }
}
```

---

## Security Considerations

### HTTPS Enforcement
- Retrofit/OkHttp enforce HTTPS by default
- No HTTP fallback (prevents downgrade attacks)
- Certificate pinning not implemented (may be added in future for enhanced security)

### Credential Protection
- Credentials stored in EncryptedSharedPreferences (Spec 3)
- Never logged in any build variant (debug or release)
- Transmitted in HTTPS request body only (not in URL or headers)
- No credential caching in memory beyond request lifecycle

### Request/Response Logging
```kotlin
// Debug builds only
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor.Level.BODY
    } else {
        HttpLoggingInterceptor.Level.NONE
    }
}
```

**⚠️ WARNING**: Even in debug builds, be cautious about logging sensitive data. Consider redacting token from logs.

---

## Future Enhancements

### Offline Support (Post-MVP)
- Queue thoughts locally when network unavailable
- Sync when connection restored
- Show sync status in UI

### Retry Logic
- Implement exponential backoff for transient failures
- Distinguish between retryable (500, 429) and non-retryable (400, 401) errors

### Response Caching
- Cache successful responses (if API supports ETags)
- Reduce network calls for repeated operations

### Analytics
- Track API call success/failure rates
- Monitor average response times
- Identify common error scenarios

---

## Status Summary

| Aspect | Status | Notes |
|--------|--------|-------|
| Request Format | ✅ Confirmed | From specification |
| Request Validation | ✅ Defined | Client-side validation rules documented |
| Response Format | ⚠️ Unknown | To be determined in Spec 2 |
| Error Codes | ⚠️ Estimated | Based on REST conventions |
| Rate Limits | ❓ Unknown | No documentation available |
| Authentication | ✅ Confirmed | Token + email in request body |
| HTTPS | ✅ Enforced | Retrofit default behavior |

---

## References

- Napkin.one API Documentation: https://intercom.help/napkin-support/en/articles/6419774-api-creating-thoughts (403 - access restricted)
- Retrofit Documentation: https://square.github.io/retrofit/
- OkHttp Documentation: https://square.github.io/okhttp/
- Constitution: `/memory/constitution.md` v1.1.0 (API Integration, Security)
- Feature Spec: `../spec.md` (FR-001, FR-004, FR-005)
- Data Model: `../data-model.md` (ThoughtRequest entity)
- Research: `../research.md` (Section 4: API Contract Verification)

---

**Document Version**: 1.0
**Last Updated**: 2025-11-05
**Status**: Draft - Pending API testing verification in Spec 2
