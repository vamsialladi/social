# Comprehensive Live End-to-End Verification of All APIs & Edge Cases
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

$passed = 0
$failed = 0

function Assert-Test($title, $condition, $details) {
    if ($condition) {
        Write-Host "  [PASS] $title" -ForegroundColor Green
        if ($details) { Write-Host "         $details" -ForegroundColor DarkGray }
        $script:passed++
    } else {
        Write-Host "  [FAIL] $title" -ForegroundColor Red
        if ($details) { Write-Host "         $details" -ForegroundColor Red }
        $script:failed++
    }
}

function Get-AuthHeaders($token) {
    return @{
        "Cookie" = "session_token=$token"
        "X-Session-Id" = $token
    }
}

Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " RUNNING COMPREHENSIVE LIVE API & EDGE CASE TEST SUITE" -ForegroundColor Magenta
Write-Host "========================================================`n" -ForegroundColor Magenta

# -------------------------------------------------------------
# 0. ACTUATOR HEALTH CHECK
# -------------------------------------------------------------
Write-Host ">>> 0. SYSTEM HEALTH CHECK" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Assert-Test "Actuator Health Check UP" ($health.status -eq "UP") "Status: $($health.status)"
} catch {
    Assert-Test "Actuator Health Check UP" $false "Server unreachable"
}

# -------------------------------------------------------------
# 1. MODULE 1: AUTHENTICATION APIS & EDGE CASES
# -------------------------------------------------------------
Write-Host "`n>>> 1. MODULE 1: AUTHENTICATION APIS & EDGE CASES" -ForegroundColor Yellow

$rand = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$aliceEmail = "alice_$rand@example.com"
$alicePassword = "SecretPassword123!"

# 1.1 Password Pattern Validation Edge Cases (Uppercase, Lowercase, Number, Special Char, No Spaces)
$badPasswords = @(
    @{ pw = "password123!"; desc = "Missing uppercase" },
    @{ pw = "PASSWORD123!"; desc = "Missing lowercase" },
    @{ pw = "Password!!!!"; desc = "Missing number" },
    @{ pw = "Password1234"; desc = "Missing special character" },
    @{ pw = "Pass word123!"; desc = "Contains spaces" },
    @{ pw = "P1!"; desc = "Too short (< 6 chars)" }
)

foreach ($item in $badPasswords) {
    try {
        $body = @{ email = "pwtest_$rand@example.com"; password = $item.pw; displayName = "PW Test" } | ConvertTo-Json
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $body
        Assert-Test "Reject password with $($item.desc)" $false "Expected 400 Bad Request"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Assert-Test "Reject password with $($item.desc)" ($code -eq 400) "Returned Status 400"
    }
}

# 1.2 Malformed Email Validation Edge Cases
$badEmails = @("not-an-email", "@missinguser.com", "missingdomain@.com", "two@@domain.com")
foreach ($badEmail in $badEmails) {
    try {
        $body = @{ email = $badEmail; password = $alicePassword; displayName = "Bad Email" } | ConvertTo-Json
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $body
        Assert-Test "Reject malformed email '$badEmail'" $false "Expected 400 Bad Request"
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        Assert-Test "Reject malformed email '$badEmail'" ($code -eq 400) "Returned Status 400"
    }
}

# 1.3 Register Alice successfully
$aliceRegBody = @{
    email = "   $aliceEmail   "
    password = $alicePassword
    displayName = "Alice Wonderland"
} | ConvertTo-Json

$aliceWebResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $aliceRegBody -Headers @{ "X-Forwarded-For" = "192.168.1.101" } -UseBasicParsing
$aliceData = ($aliceWebResp.Content | ConvertFrom-Json).data
$aliceUserId = $aliceData.user.id
$aliceProfileId = $aliceData.profile.id
$aliceCookieHeader = $aliceWebResp.Headers["Set-Cookie"]

Assert-Test "Register Alice successfully (201 Created)" ($aliceWebResp.StatusCode -eq 201) "UserId: $aliceUserId"
Assert-Test "Session cookie set with HttpOnly" ($aliceCookieHeader -match "session_token=[^;]+" -and $aliceCookieHeader -match "HttpOnly") "Cookie: $aliceCookieHeader"
Assert-Test "Response body does NOT contain JWT/token" ($null -eq $aliceData.token -and $null -eq $aliceData.tokenType) "Clean AuthResponse"
Assert-Test "Password hash is NOT exposed in response" (-not ($aliceWebResp.Content.Contains("passwordHash"))) "No leak of passwordHash"
Assert-Test "Email is normalized and trimmed" ($aliceData.user.email -eq $aliceEmail.ToLower().Trim()) "Email: $($aliceData.user.email)"
Assert-Test "Auto-created empty profile for Alice" ($aliceProfileId -ne $null -and $aliceData.profile.displayName -eq "Alice Wonderland") "ProfileId: $aliceProfileId"

# Extract cookie value
$aliceSessionToken = ""
if ($aliceCookieHeader -match "session_token=([^;]+)") {
    $aliceSessionToken = $Matches[1]
}

# 1.4 Duplicate Registration Edge Case (409 Conflict)
try {
    $dupBody = @{ email = $aliceEmail.ToUpper(); password = $alicePassword } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $dupBody
    Assert-Test "Reject duplicate email registration with 409 Conflict" $false "Expected 409 Conflict"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject duplicate email registration with 409 Conflict" ($code -eq 409) "Returned Status 409"
}

# 1.5 Register Bob
$bobEmail = "bob_$rand@example.com"
$bobPassword = "BobPassword123!"
$bobRegBody = @{ email = $bobEmail; password = $bobPassword; displayName = "Bob Builder" } | ConvertTo-Json
$bobWebResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body $bobRegBody -Headers @{ "X-Forwarded-For" = "192.168.1.102" } -UseBasicParsing
$bobData = ($bobWebResp.Content | ConvertFrom-Json).data
$bobUserId = $bobData.user.id
$bobSessionToken = ""
if ($bobWebResp.Headers["Set-Cookie"] -match "session_token=([^;]+)") {
    $bobSessionToken = $Matches[1]
}
Assert-Test "Register Bob successfully (201 Created)" ($bobWebResp.StatusCode -eq 201) "BobUserId: $bobUserId"

# 1.6 Login with Case-Insensitive Email & Valid Password
$loginBody = @{ email = $aliceEmail.ToUpper(); password = $alicePassword } | ConvertTo-Json
$loginWebResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody -Headers @{ "X-Forwarded-For" = "192.168.1.103" } -UseBasicParsing
$loginData = ($loginWebResp.Content | ConvertFrom-Json).data
Assert-Test "Login Alice with case-insensitive email (200 OK)" ($loginWebResp.StatusCode -eq 200) "Email: $($loginData.user.email)"
Assert-Test "Login Set-Cookie contains session_token" ($loginWebResp.Headers["Set-Cookie"] -match "session_token=") "Set-Cookie present"

# Update Alice session token to the newly logged in token
if ($loginWebResp.Headers["Set-Cookie"] -match "session_token=([^;]+)") {
    $aliceSessionToken = $Matches[1]
}

# 1.7 Login Wrong Password Edge Case (Generic 401)
try {
    $badLogin = @{ email = $aliceEmail; password = "WrongPassword999!" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $badLogin
    Assert-Test "Reject login with wrong password" $false "Expected 401"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject login with wrong password (401)" ($code -eq 401) "Generic message returned"
}

# 1.8 Login Non-existent Email Edge Case (Generic 401)
try {
    $badUserLogin = @{ email = "non_existent_$rand@example.com"; password = "Password123!" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $badUserLogin
    Assert-Test "Reject login with non-existent email" $false "Expected 401"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject login with non-existent email (401)" ($code -eq 401) "Generic message returned"
}

# 1.9 GET /api/auth/me via Cookie Header
$meResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers (Get-AuthHeaders $aliceSessionToken)
Assert-Test "GET /api/auth/me via session cookie (200 OK)" ($meResp.data.user.id -eq $aliceUserId -and $meResp.data.profile.displayName -eq "Alice Wonderland") "Caller identified"

# 1.10 GET /api/auth/me via X-Session-Id Header
$meHeaderResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers @{ "X-Session-Id" = $aliceSessionToken }
Assert-Test "GET /api/auth/me via X-Session-Id header (200 OK)" ($meHeaderResp.data.user.id -eq $aliceUserId) "Header identified"

# 1.11 GET /api/auth/me Unauthenticated Edge Case (401)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get
    Assert-Test "Reject unauthenticated /api/auth/me with 401" $false "Expected 401"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject unauthenticated /api/auth/me with 401" ($code -eq 401) "Returned 401 NOT_AUTHENTICATED"
}

# 1.12 GET /api/auth/me with Invalid / Bogus Token (401)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers @{ "Cookie" = "session_token=bogus_token_xyz_9999" }
    Assert-Test "Reject invalid session cookie with 401" $false "Expected 401"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject invalid session cookie with 401" ($code -eq 401) "Returned 401 NOT_AUTHENTICATED"
}

# 1.13 Rate Limiting Edge Case (429 Too Many Requests)
$rateIp = "198.51.100.77"
$rateLimited = $false
for ($i = 1; $i -le 8; $i++) {
    try {
        $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body (@{ email = "test@limit.com"; password = "Wrong" } | ConvertTo-Json) -Headers @{ "X-Forwarded-For" = $rateIp }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 429) {
            $rateLimited = $true
            break
        }
    }
}
Assert-Test "Login triggers rate limiting (429 Too Many Requests)" $rateLimited "Rate limiter working on IP $rateIp"

# -------------------------------------------------------------
# 2. MODULE 2: USERS & PROFILES
# -------------------------------------------------------------
Write-Host "`n>>> 2. MODULE 2: USERS & PROFILES APIS" -ForegroundColor Yellow

# 2.1 Get Profile by userId
$getProfile = Invoke-RestMethod -Uri "$baseUrl/api/profiles/$aliceUserId" -Method Get
Assert-Test "GET /api/profiles/:userId" ($getProfile.data.userId -eq $aliceUserId) "DisplayName: $($getProfile.data.displayName)"

# 2.2 Update Profile (PATCH /api/profiles/me)
$profUpdate = @{
    name = "Alice Wonderland"
    experience = "5 years of visual art and design"
    address = "San Francisco, CA"
    talents = @("Digital Art", "Creative Direction", "Illustration")
} | ConvertTo-Json

$updatedProfile = Invoke-RestMethod -Uri "$baseUrl/api/profiles/me" -Method Patch -ContentType "application/json" -Body $profUpdate -Headers (Get-AuthHeaders $aliceSessionToken)
Assert-Test "PATCH /api/profiles/me updates profile" ($updatedProfile.data.experience -eq "5 years of visual art and design") "Experience updated"
Assert-Test "Profile talents updated correctly" ($updatedProfile.data.talents -contains "Digital Art") "Talents count: $(@($updatedProfile.data.talents).Count)"

# 2.3 Non-existent profile query (404)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/profiles/ghost_user_9999" -Method Get
    Assert-Test "Non-existent profile returns 404" $false "Expected 404"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Non-existent profile returns 404" ($code -eq 404) "Returned 404 Not Found"
}

# -------------------------------------------------------------
# 3. MODULE 3: POSTS & FEED
# -------------------------------------------------------------
Write-Host "`n>>> 3. MODULE 3: POSTS & FEED APIS" -ForegroundColor Yellow

# 3.1 Create Post by Alice
$postBody = @{
    type = "POST"
    content = "Hello World! This is Alice's first live post in the platform."
    visibility = "PUBLIC"
} | ConvertTo-Json

$createdPost = Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method Post -ContentType "application/json" -Body $postBody -Headers (Get-AuthHeaders $aliceSessionToken)
$postId = $createdPost.data.id
Assert-Test "POST /api/posts creates new post (201 Created)" ($createdPost.data.content -eq "Hello World! This is Alice's first live post in the platform.") "PostId: $postId"

# 3.2 Get Post by ID
$gotPost = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId" -Method Get -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "GET /api/posts/:id retrieves post" ($gotPost.data.id -eq $postId -and $gotPost.data.authorId -eq $aliceUserId) "Author: $($gotPost.data.authorId)"

# 3.3 Get Feed
$feed = Invoke-RestMethod -Uri "$baseUrl/api/posts?limit=10" -Method Get -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "GET /api/posts feed returns posts list" ($feed.data -ne $null -and @($feed.data).Count -ge 1) "Feed items count: $(@($feed.data).Count)"

# 3.4 Query Non-existent Post (404)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/posts/non_existent_post_9999" -Method Get
    Assert-Test "Querying non-existent post returns 404" $false "Expected 404"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Querying non-existent post returns 404" ($code -eq 404) "Returned 404 Not Found"
}

# -------------------------------------------------------------
# 4. MODULE 5: OPPORTUNITIES
# -------------------------------------------------------------
Write-Host "`n>>> 4. MODULE 5: OPPORTUNITY APIS & EDGE CASES" -ForegroundColor Yellow

# 4.1 Create Opportunity with future expiry
$futureExpiry = (Get-Date).AddDays(14).ToString("yyyy-MM-ddTHH:mm:ssZ")
$oppBody = @{
    title = "Lead Animator Wanted"
    content = "Looking for an experienced animator for an upcoming indie game project."
    employer = "Indie Game Studio"
    compensation = "$80,000 - $100,000"
    location = "Remote"
    expiryDate = $futureExpiry
} | ConvertTo-Json

$createdOpp = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json" -Body $oppBody -Headers (Get-AuthHeaders $aliceSessionToken)
$oppId = $createdOpp.data.id
Assert-Test "POST /api/opportunities creates opportunity" ($createdOpp.data.title -eq "Lead Animator Wanted") "OppId: $oppId"

# 4.2 Edge Case: Past Expiry Date Rejected (400)
try {
    $pastExpiry = (Get-Date).AddDays(-5).ToString("yyyy-MM-ddTHH:mm:ssZ")
    $badOpp = @{ title = "Expired Gig"; content = "Description"; expiryDate = $pastExpiry } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json" -Body $badOpp -Headers (Get-AuthHeaders $aliceSessionToken)
    Assert-Test "Reject opportunity with past expiry date (400)" $false "Expected 400"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reject opportunity with past expiry date (400)" ($code -eq 400) "Returned Status 400"
}

# 4.3 Cursor Pagination on Opportunities
$oppPage = Invoke-RestMethod -Uri "$baseUrl/api/opportunities?limit=5" -Method Get
Assert-Test "GET /api/opportunities returns paginated opportunities" ($oppPage.items -ne $null) "Items count: $(@($oppPage.items).Count)"

# -------------------------------------------------------------
# 5. MODULE 6: NETWORK & CONNECTIONS
# -------------------------------------------------------------
Write-Host "`n>>> 5. MODULE 6: NETWORK & CONNECTIONS APIS & EDGE CASES" -ForegroundColor Yellow

# 5.1 Alice sends connection request to Bob
$connBody = @{ recipientId = $bobUserId } | ConvertTo-Json
$connResp = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json" -Body $connBody -Headers (Get-AuthHeaders $aliceSessionToken)
$connId = $connResp.data.id
Assert-Test "Alice sends connection request to Bob (201 Created)" ($connResp.data.status -eq "pending") "ConnId: $connId"

# 5.2 Edge Case: Reverse Duplicate Request (Bob -> Alice) -> 409 Conflict
try {
    $revBody = @{ recipientId = $aliceUserId } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json" -Body $revBody -Headers (Get-AuthHeaders $bobSessionToken)
    Assert-Test "Reverse duplicate connection request rejected with 409" $false "Expected 409"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reverse duplicate connection request rejected with 409" ($code -eq 409) "Returned Status 409 Conflict"
}

# 5.3 Edge Case: Requester (Alice) cannot accept own request -> 403 Forbidden
try {
    $acceptBody = @{ status = "accepted" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json" -Body $acceptBody -Headers (Get-AuthHeaders $aliceSessionToken)
    Assert-Test "Requester cannot accept own request (403 Forbidden)" $false "Expected 403"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Requester cannot accept own request (403 Forbidden)" ($code -eq 403) "Returned Status 403"
}

# 5.4 Recipient (Bob) accepts connection request -> 200 OK
$bobAccept = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json" -Body (@{ status = "accepted" } | ConvertTo-Json) -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "Bob accepts connection request (200 OK)" ($bobAccept.data.status -eq "accepted") "Status: accepted"

# 5.5 Edge Case: Reverting status to PENDING rejected -> 400 Bad Request
try {
    $revertBody = @{ status = "pending" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json" -Body $revertBody -Headers (Get-AuthHeaders $bobSessionToken)
    Assert-Test "Reverting connection status to pending rejected (400)" $false "Expected 400"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Reverting connection status to pending rejected (400)" ($code -eq 400) "Returned Status 400"
}

# 5.6 Connection Suggestions
$sugg = Invoke-RestMethod -Uri "$baseUrl/api/connections/suggestions" -Method Get -Headers (Get-AuthHeaders $aliceSessionToken)
Assert-Test "GET /api/connections/suggestions returns list" ($null -ne $sugg.data) "Suggestions list present"

# -------------------------------------------------------------
# 6. MODULE 7: CONVERSATIONS & MESSAGING
# -------------------------------------------------------------
Write-Host "`n>>> 6. MODULE 7: CONVERSATIONS & MESSAGING APIS" -ForegroundColor Yellow

# 6.1 Create 1-on-1 Conversation (Alice -> Bob)
$convBody = @{ participantId = $bobUserId } | ConvertTo-Json
$convResp = Invoke-RestMethod -Uri "$baseUrl/api/conversations" -Method Post -ContentType "application/json" -Body $convBody -Headers (Get-AuthHeaders $aliceSessionToken)
$convId = $convResp.data.id
Assert-Test "POST /api/conversations creates 1-on-1 conversation" ($convResp.data.participantIds -contains $bobUserId) "ConvId: $convId"

# 6.2 Send Message from Alice to Bob
$msgBody = @{ body = "Hi Bob! Let's collaborate on the new project." } | ConvertTo-Json
$msgResp = Invoke-RestMethod -Uri "$baseUrl/api/conversations/$convId/messages" -Method Post -ContentType "application/json" -Body $msgBody -Headers (Get-AuthHeaders $aliceSessionToken)
$msgId = $msgResp.data.id
Assert-Test "POST /api/conversations/:id/messages sends message" ($msgResp.data.body -eq "Hi Bob! Let's collaborate on the new project.") "MsgId: $msgId"

# 6.3 Bob reads messages in conversation
$bobMsgs = Invoke-RestMethod -Uri "$baseUrl/api/conversations/$convId/messages" -Method Get -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "Bob retrieves conversation messages (200 OK)" (@($bobMsgs.data).Count -ge 1) "Messages count: $(@($bobMsgs.data).Count)"

# 6.4 Bob marks conversation as read
$markRead = Invoke-RestMethod -Uri "$baseUrl/api/conversations/$convId/read" -Method Patch -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "Bob marks conversation messages as read" ($markRead.data.conversationId -eq $convId) "MarkedRead: $($markRead.data.markedReadCount)"

# 6.5 Edge Case: Non-participant querying conversation -> 403 Forbidden
$charlieRand = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$charlieReg = Invoke-WebRequest -Uri "$baseUrl/api/auth/register" -Method Post -ContentType "application/json" -Body (@{ email = "charlie_$charlieRand@example.com"; password = "CharliePass123!"; displayName = "Charlie Third" } | ConvertTo-Json) -Headers @{ "X-Forwarded-For" = "192.168.1.109" } -UseBasicParsing
$charlieCookie = ""
if ($charlieReg.Headers["Set-Cookie"] -match "session_token=([^;]+)") {
    $charlieCookie = $Matches[1]
}

try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/conversations/$convId" -Method Get -Headers (Get-AuthHeaders $charlieCookie)
    Assert-Test "Non-participant access to conversation rejected with 403" $false "Expected 403"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Non-participant access to conversation rejected with 403" ($code -eq 403) "Returned Status 403 Forbidden"
}

# -------------------------------------------------------------
# 7. MODULE 8: NOTIFICATIONS
# -------------------------------------------------------------
Write-Host "`n>>> 7. MODULE 8: NOTIFICATIONS APIS" -ForegroundColor Yellow

Start-Sleep -Seconds 1 # Wait for async event notification propagation

# 7.1 Bob fetches notifications (Should contain message or connection notification)
$bobNotifs = Invoke-RestMethod -Uri "$baseUrl/api/notifications" -Method Get -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "GET /api/notifications retrieves notifications for Bob" ($bobNotifs.data -ne $null) "Notifs count: $(@($bobNotifs.data).Count)"

if (@($bobNotifs.data).Count -ge 1) {
    $firstNotifId = $bobNotifs.data[0].id
    # 7.2 Mark single notification as read
    $markSingle = Invoke-RestMethod -Uri "$baseUrl/api/notifications/$firstNotifId/read" -Method Patch -Headers (Get-AuthHeaders $bobSessionToken)
    Assert-Test "PATCH /api/notifications/:id/read marks single notification read" ($markSingle.data.id -eq $firstNotifId -and $markSingle.data.readAt -ne $null) "ReadAt: $($markSingle.data.readAt)"
}

# 7.3 Mark all notifications as read
$markAll = Invoke-RestMethod -Uri "$baseUrl/api/notifications/read" -Method Patch -ContentType "application/json" -Body "{}" -Headers (Get-AuthHeaders $bobSessionToken)
Assert-Test "PATCH /api/notifications/read marks all notifications read" ($markAll.data.markedReadCount -ne $null) "MarkedReadCount: $($markAll.data.markedReadCount)"

# -------------------------------------------------------------
# 8. LOGOUT & POST-LOGOUT SESSION INVALIDATION
# -------------------------------------------------------------
Write-Host "`n>>> 8. LOGOUT & SESSION INVALIDATION" -ForegroundColor Yellow

# 8.1 Logout Alice
$logoutResp = Invoke-WebRequest -Uri "$baseUrl/api/auth/logout" -Method Post -Headers (Get-AuthHeaders $aliceSessionToken) -UseBasicParsing
Assert-Test "POST /api/auth/logout succeeds (200 OK)" ($logoutResp.StatusCode -eq 200) "Message: $(($logoutResp.Content | ConvertFrom-Json).data.message)"
Assert-Test "Logout response clears session cookie (Max-Age=0)" ($logoutResp.Headers["Set-Cookie"] -match "Max-Age=0") "Set-Cookie cleared"

# 8.2 Subsequent request with invalidated Alice session fails (401)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers (Get-AuthHeaders $aliceSessionToken)
    Assert-Test "Subsequent /api/auth/me after logout rejected with 401" $false "Expected 401"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-Test "Subsequent /api/auth/me after logout rejected with 401" ($code -eq 401) "Session token invalidated in database"
}

# -------------------------------------------------------------
# SUMMARY
# -------------------------------------------------------------
Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " COMPLETE LIVE API TEST RESULTS" -ForegroundColor Magenta
Write-Host " TOTAL PASSED: $passed" -ForegroundColor Green
Write-Host " TOTAL FAILED: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================================`n" -ForegroundColor Magenta

if ($failed -gt 0) { exit 1 } else { exit 0 }
