# Live Edge Cases Verification Script for Module 5 & Module 6
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " STARTING COMPREHENSIVE EDGE CASES TEST SUITE" -ForegroundColor Magenta
Write-Host "========================================================`n" -ForegroundColor Magenta

$passed = 0
$failed = 0

function Assert-EdgeCase($title, $condition, $details) {
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

# Setup dedicated test users for edge case testing
$rand = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$u1 = (Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body (@{
    username = "edge1_$rand"
    name = "Edge User One"
    email = "edge1_$rand@test.com"
    role = "ARTIST"
} | ConvertTo-Json)).data.id

$u2 = (Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body (@{
    username = "edge2_$rand"
    name = "Edge User Two"
    email = "edge2_$rand@test.com"
    role = "EMPLOYER"
} | ConvertTo-Json)).data.id

$u3 = (Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body (@{
    username = "edge3_$rand"
    name = "Edge User Three"
    email = "edge3_$rand@test.com"
    role = "DIRECTOR"
} | ConvertTo-Json)).data.id

Write-Host ">>> MODULE 5: OPPORTUNITY EDGE CASES" -ForegroundColor Yellow

# EC 1: Past Expiry Date Rejected
try {
    $pastDate = (Get-Date).AddDays(-3).ToString("yyyy-MM-ddTHH:mm:ssZ")
    $body = @{ title = "Past Job"; content = "Description"; expiryDate = $pastDate } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body $body
    Assert-EdgeCase "Reject opportunity with past expiry date" $false "Expected 400 Bad Request"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject opportunity with past expiry date" ($code -eq 400) "Returned Status 400 Bad Request"
}

# EC 2: Non-existent Author in Header
try {
    $body = @{ title = "Valid Title"; content = "Valid Content" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = "non_existent_author_9999" } -Body $body
    Assert-EdgeCase "Reject non-existent author in X-User-Id" $false "Expected 404 Not Found"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject non-existent author in X-User-Id" ($code -eq 404) "Returned Status 404 Not Found"
}

# EC 3: Invalid Cursor Format
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities?cursor=invalid_cursor_xyz" -Method Get
    Assert-EdgeCase "Reject invalid pagination cursor format" $false "Expected 400 Bad Request"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject invalid pagination cursor format" ($code -eq 400) "Returned Status 400 Bad Request"
}

# EC 4: Querying Non-existent Opportunity ID
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities/non_existent_opp_9999" -Method Get
    Assert-EdgeCase "Querying non-existent opportunity returns 404" $false "Expected 404 Not Found"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Querying non-existent opportunity returns 404" ($code -eq 404) "Returned Status 404 Not Found"
}

# EC 5: Querying Ordinary Post via /api/opportunities/:id
$normPost = (Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ content = "Edge test normal post" } | ConvertTo-Json)).data
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities/$($normPost.id)" -Method Get
    Assert-EdgeCase "Querying ordinary post via /api/opportunities/:id returns 404" $false "Expected 404 Not Found"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Querying ordinary post via /api/opportunities/:id returns 404" ($code -eq 404) "Returned Status 404 Not Found"
}

# EC 6: Cursor Pagination Step-Through
$opp1 = (Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u2 } -Body (@{ title = "Edge Job 1"; content = "Desc 1" } | ConvertTo-Json)).data
Start-Sleep -Milliseconds 50
$opp2 = (Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u2 } -Body (@{ title = "Edge Job 2"; content = "Desc 2" } | ConvertTo-Json)).data

$page1 = Invoke-RestMethod -Uri "$baseUrl/api/opportunities?limit=1" -Method Get
$hasMore = $page1.hasMore
$cursor = $page1.nextCursor
Assert-EdgeCase "Cursor pagination limit=1 hasMore=true" ($hasMore -eq $true -and $null -ne $cursor) "Cursor: $cursor"

$page2 = Invoke-RestMethod -Uri "$baseUrl/api/opportunities?cursor=$cursor&limit=1" -Method Get
Assert-EdgeCase "Cursor pagination page 2 retrieval" ($null -ne $page2.items) "Items on page 2: $(@($page2.items).Count)"


Write-Host "`n>>> MODULE 6: NETWORK & CONNECTIONS EDGE CASES" -ForegroundColor Yellow

# EC 7: Missing / Empty recipientId
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ recipientId = "" } | ConvertTo-Json)
    Assert-EdgeCase "Reject empty recipientId in connection request" $false "Expected 400 Bad Request"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject empty recipientId in connection request" ($code -eq 400) "Returned Status 400 Bad Request"
}

# EC 8: Non-existent recipientId
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ recipientId = "ghost_user_9999" } | ConvertTo-Json)
    Assert-EdgeCase "Reject non-existent recipientId with 404" $false "Expected 404 Not Found"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject non-existent recipientId with 404" ($code -eq 404) "Returned Status 404 Not Found"
}

# EC 9: Reverse Duplicate Connection Request (u1->u2 exists, u2 tries to connect to u1)
$conn = (Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ recipientId = $u2 } | ConvertTo-Json)).data
$connId = $conn.id
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u2 } -Body (@{ recipientId = $u1 } | ConvertTo-Json)
    Assert-EdgeCase "Reject reverse duplicate connection request with 409 Conflict" $false "Expected 409 Conflict"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject reverse duplicate connection request with 409 Conflict" ($code -eq 409) "Returned Status 409 Conflict"
}

# EC 10: Requester cannot accept own request (only recipient can accept)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ status = "accepted" } | ConvertTo-Json)
    Assert-EdgeCase "Reject requester attempting to accept own request with 403 Forbidden" $false "Expected 403 Forbidden"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject requester attempting to accept own request with 403 Forbidden" ($code -eq 403) "Returned Status 403 Forbidden"
}

# EC 11: Third-party cannot view private connection by ID
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Get -Headers @{ "X-User-Id" = $u3 }
    Assert-EdgeCase "Reject third party viewing private connection with 403 Forbidden" $false "Expected 403 Forbidden"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject third party viewing private connection with 403 Forbidden" ($code -eq 403) "Returned Status 403 Forbidden"
}

# EC 12: Cannot revert connection status back to PENDING
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u2 } -Body (@{ status = "pending" } | ConvertTo-Json)
    Assert-EdgeCase "Reject reverting connection status to pending with 400 Bad Request" $false "Expected 400 Bad Request"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Reject reverting connection status to pending with 400 Bad Request" ($code -eq 400) "Returned Status 400 Bad Request"
}

# EC 13: Recipient blocks user -> Blocked user cannot send request and is excluded from suggestions
$blockRes = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u2 } -Body (@{ status = "blocked" } | ConvertTo-Json)
Assert-EdgeCase "Update connection status to BLOCKED" ($blockRes.data.status -eq "blocked") "Status: $($blockRes.data.status)"

try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $u1 } -Body (@{ recipientId = $u2 } | ConvertTo-Json)
    Assert-EdgeCase "Blocked user contacting blocker rejected with 403 Forbidden" $false "Expected 403 Forbidden"
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Assert-EdgeCase "Blocked user contacting blocker rejected with 403 Forbidden" ($code -eq 403) "Returned Status 403 Forbidden"
}

# Blocked user excluded from suggestions
$sugg = Invoke-RestMethod -Uri "$baseUrl/api/connections/suggestions" -Method Get -Headers @{ "X-User-Id" = $u1 }
$suggIds = @($sugg.data | ForEach-Object { $_.id })
$isBlockedUserExcluded = -not ($suggIds -contains $u2)
Assert-EdgeCase "Blocked user is strictly excluded from suggestions" $isBlockedUserExcluded "Excluded: $isBlockedUserExcluded"

Write-Host "`n========================================================" -ForegroundColor Magenta
Write-Host " EDGE CASES SUMMARY: $passed PASSED, $failed FAILED" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================================`n" -ForegroundColor Magenta

if ($failed -gt 0) { exit 1 } else { exit 0 }
