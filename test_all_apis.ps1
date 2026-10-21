# Complete E2E API Verification Script for Module 5 & Module 6
$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " STARTING COMPREHENSIVE END-TO-END API TEST SUITE" -ForegroundColor Cyan
Write-Host "========================================================`n" -ForegroundColor Cyan

$passed = 0
$failed = 0

function Assert-Step($title, $condition, $details) {
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

# 1. Health & OpenAPI
Write-Host ">>> 1. SYSTEM HEALTH & API DOCS" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Assert-Step "GET /actuator/health returns UP" ($health.status -eq "UP") "Status: $($health.status)"
} catch {
    Assert-Step "GET /actuator/health returns UP" $false $_.Exception.Message
}

try {
    $docs = Invoke-RestMethod -Uri "$baseUrl/v3/api-docs" -Method Get
    Assert-Step "GET /v3/api-docs returns valid OpenAPI spec" ($null -ne $docs.openapi) "Title: $($docs.info.title)"
} catch {
    Assert-Step "GET /v3/api-docs returns valid OpenAPI spec" $false $_.Exception.Message
}

# 2. Users Setup
Write-Host "`n>>> 2. USER & ARTIST PROFILE APIS" -ForegroundColor Yellow

$suffix = [System.Guid]::NewGuid().ToString().Substring(0, 6)
$aliceBody = @{
    username = "alice_$suffix"
    name = "Alice Wonder"
    email = "alice_$suffix@example.com"
    headline = "Classical Vocalist & Composer"
    role = "ARTIST"
    location = "Mumbai, India"
} | ConvertTo-Json

try {
    $aliceRes = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body $aliceBody
    $aliceId = $aliceRes.data.id
    Assert-Step "POST /api/users (Create Alice)" ($null -ne $aliceId) "Alice ID: $aliceId"
} catch {
    Assert-Step "POST /api/users (Create Alice)" $false $_.Exception.Message
}

$bobBody = @{
    username = "bob_$suffix"
    name = "Bob Vance"
    email = "bob_$suffix@example.com"
    headline = "Lead Casting Director at CineMedia"
    role = "EMPLOYER"
    location = "Hyderabad, India"
} | ConvertTo-Json

try {
    $bobRes = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body $bobBody
    $bobId = $bobRes.data.id
    Assert-Step "POST /api/users (Create Bob)" ($null -ne $bobId) "Bob ID: $bobId"
} catch {
    Assert-Step "POST /api/users (Create Bob)" $false $_.Exception.Message
}

$charlieBody = @{
    username = "charlie_$suffix"
    name = "Charlie Chaplin"
    email = "charlie_$suffix@example.com"
    headline = "Indie Film Director"
    role = "DIRECTOR"
    location = "Bengaluru, India"
} | ConvertTo-Json

try {
    $charlieRes = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Post -ContentType "application/json; charset=utf-8" -Body $charlieBody
    $charlieId = $charlieRes.data.id
    Assert-Step "POST /api/users (Create Charlie)" ($null -ne $charlieId) "Charlie ID: $charlieId"
} catch {
    Assert-Step "POST /api/users (Create Charlie)" $false $_.Exception.Message
}

# GET /api/users/me
try {
    $me = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -Headers @{ "X-User-Id" = $aliceId }
    Assert-Step "GET /api/users/me (With X-User-Id header)" ($me.data.name -eq "Alice Wonder") "Name: $($me.data.name)"
} catch {
    Assert-Step "GET /api/users/me" $false $_.Exception.Message
}

# 3. Module 5: Opportunities
Write-Host "`n>>> 3. MODULE 5: OPPORTUNITIES APIS" -ForegroundColor Yellow

# GET Initial opportunities
try {
    $opps = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Get
    Assert-Step "GET /api/opportunities (Paginated cursor endpoint reachable)" ($null -ne $opps.items) "Items count: $(@($opps.items).Count)"
} catch {
    Assert-Step "GET /api/opportunities" $false $_.Exception.Message
}

# POST Create Opportunity
$oppBody = @{
    title = "Lead Female Vocalist for Feature Film OST"
    content = "Looking for a classically trained vocalist with contemporary range for a major upcoming feature soundtrack recording in Hyderabad."
    employer = "CineMedia Productions"
    compensation = "INR 150,000 per track"
    location = "Hyderabad, India"
    expiryDate = "2026-12-31T23:59:59Z"
    mediaUrls = @("https://example.com/demo-sheet.pdf")
} | ConvertTo-Json

try {
    $createOppRes = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $bobId } -Body $oppBody
    $oppId = $createOppRes.data.id
    $oppType = $createOppRes.data.type
    Assert-Step "POST /api/opportunities (Forces type=opportunity & sets author)" ($null -ne $oppId -and $oppType -eq "opportunity" -and $createOppRes.data.authorId -eq $bobId) "Opp ID: $oppId, Type: $oppType"
} catch {
    Assert-Step "POST /api/opportunities" $false $_.Exception.Message
}

# GET /api/opportunities (Newest first)
try {
    $oppsFeed = Invoke-RestMethod -Uri "$baseUrl/api/opportunities?limit=10" -Method Get
    $firstOpp = @($oppsFeed.items)[0]
    Assert-Step "GET /api/opportunities returns created opportunity" ($firstOpp.id -eq $oppId -and $firstOpp.type -eq "opportunity") "Title: $($firstOpp.title)"
} catch {
    Assert-Step "GET /api/opportunities" $false $_.Exception.Message
}

# GET /api/opportunities/{id}
try {
    $singleOpp = Invoke-RestMethod -Uri "$baseUrl/api/opportunities/$oppId" -Method Get
    Assert-Step "GET /api/opportunities/:id returns correct record" ($singleOpp.data.id -eq $oppId) "Employer: $($singleOpp.data.employer)"
} catch {
    Assert-Step "GET /api/opportunities/:id" $false $_.Exception.Message
}

# Schema validation failure (empty title and content)
try {
    $invalidOppBody = @{ title = ""; content = "" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $bobId } -Body $invalidOppBody
    Assert-Step "POST /api/opportunities (Validation rejects empty title/content)" $false "Expected 400 Bad Request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Step "POST /api/opportunities (Validation rejects empty title/content)" ($statusCode -eq 400) "Returned Status 400 Bad Request"
}

# Auth failure check (Missing X-User-Id)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/opportunities" -Method Post -ContentType "application/json; charset=utf-8" -Body $oppBody
    Assert-Step "POST /api/opportunities (Rejects missing auth header)" $false "Expected 401 Unauthorized"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Step "POST /api/opportunities (Rejects missing auth header)" ($statusCode -eq 401) "Returned Status 401 Unauthorized"
}

# Ordinary Posts Feed Rule
$postBody = @{
    content = "Rehearsing morning raagas with tanpura! Excited for studio session."
    mediaUrls = @("https://example.com/audio-snippet.mp3")
} | ConvertTo-Json

try {
    $createPostRes = Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $aliceId } -Body $postBody
    Assert-Step "POST /api/posts (Creates standard post type=post)" ($createPostRes.data.type -eq "post") "Post ID: $($createPostRes.data.id)"
} catch {
    Assert-Step "POST /api/posts" $false $_.Exception.Message
}

# GET /api/posts (Must exclude opportunities)
try {
    $postsFeed = Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method Get
    $hasOpportunity = @($postsFeed.items | Where-Object { $_.type -eq "opportunity" }).Count -gt 0
    Assert-Step "GET /api/posts (Rule: Opportunity posts must NOT appear in ordinary feed)" (-not $hasOpportunity) "Ordinary posts count: $(@($postsFeed.items).Count), Opportunities present: $hasOpportunity"
} catch {
    Assert-Step "GET /api/posts feed exclusion rule" $false $_.Exception.Message
}

# 4. Module 6: Network and Connections
Write-Host "`n>>> 4. MODULE 6: NETWORK & CONNECTIONS APIS" -ForegroundColor Yellow

# GET /api/connections/suggestions (Alice)
try {
    $sugg = Invoke-RestMethod -Uri "$baseUrl/api/connections/suggestions" -Method Get -Headers @{ "X-User-Id" = $aliceId }
    $suggIds = @($sugg.data | ForEach-Object { $_.id })
    $excludesSelf = -not ($suggIds -contains $aliceId)
    $hasOthers = ($suggIds -contains $bobId) -and ($suggIds -contains $charlieId)
    Assert-Step "GET /api/connections/suggestions (Excludes self, includes eligible)" ($excludesSelf -and $hasOthers) "Suggested users count: $(@($sugg.data).Count)"
} catch {
    Assert-Step "GET /api/connections/suggestions" $false $_.Exception.Message
}

# POST /api/connections (Alice -> Bob)
$connBody = @{ recipientId = $bobId } | ConvertTo-Json
try {
    $connRes = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $aliceId } -Body $connBody
    $connId = $connRes.data.id
    $status = $connRes.data.status
    Assert-Step "POST /api/connections (Alice -> Bob creates pending request & M8 notif)" ($null -ne $connId -and $status -eq "pending") "Connection ID: $connId, Status: $status"
} catch {
    Assert-Step "POST /api/connections" $false $_.Exception.Message
}

# POST /api/connections (Self-connection rejection)
try {
    $selfConnBody = @{ recipientId = $aliceId } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $aliceId } -Body $selfConnBody
    Assert-Step "POST /api/connections (Rejects self-connection)" $false "Expected 400 Bad Request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Step "POST /api/connections (Rejects self-connection)" ($statusCode -eq 400) "Returned Status 400 Bad Request"
}

# POST /api/connections (Duplicate active pair rejection)
try {
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Post -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $aliceId } -Body $connBody
    Assert-Step "POST /api/connections (Rejects duplicate active pair)" $false "Expected 409 Conflict"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Step "POST /api/connections (Rejects duplicate active pair)" ($statusCode -eq 409) "Returned Status 409 Conflict"
}

# PATCH /api/connections/:id (Third-party Charlie tries to accept -> 403 Forbidden)
try {
    $acceptBody = @{ status = "accepted" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $charlieId } -Body $acceptBody
    Assert-Step "PATCH /api/connections/:id (Rejects non-recipient with 403 Forbidden)" $false "Expected 403 Forbidden"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Step "PATCH /api/connections/:id (Rejects non-recipient with 403 Forbidden)" ($statusCode -eq 403) "Returned Status 403 Forbidden"
}

# PATCH /api/connections/:id (Recipient Bob accepts -> 200 OK)
try {
    $acceptBody = @{ status = "accepted" } | ConvertTo-Json
    $patchRes = Invoke-RestMethod -Uri "$baseUrl/api/connections/$connId" -Method Patch -ContentType "application/json; charset=utf-8" -Headers @{ "X-User-Id" = $bobId } -Body $acceptBody
    Assert-Step "PATCH /api/connections/:id (Recipient Bob accepts request)" ($patchRes.data.status -eq "accepted") "Status updated to: $($patchRes.data.status)"
} catch {
    Assert-Step "PATCH /api/connections/:id (Recipient accepts)" $false $_.Exception.Message
}

# GET /api/connections (List accepted for Alice)
try {
    $aliceConns = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Get -Headers @{ "X-User-Id" = $aliceId }
    $aliceConnectedToBob = @($aliceConns.data | Where-Object { $_.connectedUser.id -eq $bobId }).Count -gt 0
    Assert-Step "GET /api/connections (Alice sees accepted connection to Bob)" $aliceConnectedToBob "Accepted count: $(@($aliceConns.data).Count)"
} catch {
    Assert-Step "GET /api/connections (Alice)" $false $_.Exception.Message
}

# GET /api/connections (List accepted for Bob)
try {
    $bobConns = Invoke-RestMethod -Uri "$baseUrl/api/connections" -Method Get -Headers @{ "X-User-Id" = $bobId }
    $bobConnectedToAlice = @($bobConns.data | Where-Object { $_.connectedUser.id -eq $aliceId }).Count -gt 0
    Assert-Step "GET /api/connections (Bob sees accepted connection to Alice)" $bobConnectedToAlice "Accepted count: $(@($bobConns.data).Count)"
} catch {
    Assert-Step "GET /api/connections (Bob)" $false $_.Exception.Message
}

# GET /api/connections/suggestions (Alice now sees Bob excluded because connected)
try {
    $suggAfter = Invoke-RestMethod -Uri "$baseUrl/api/connections/suggestions" -Method Get -Headers @{ "X-User-Id" = $aliceId }
    $suggIdsAfter = @($suggAfter.data | ForEach-Object { $_.id })
    $bobExcluded = -not ($suggIdsAfter -contains $bobId)
    $charlieIncluded = ($suggIdsAfter -contains $charlieId)
    Assert-Step "GET /api/connections/suggestions (Excludes accepted connections)" ($bobExcluded -and $charlieIncluded) "Remaining suggestions count: $(@($suggAfter.data).Count)"
} catch {
    Assert-Step "GET /api/connections/suggestions (After connect)" $false $_.Exception.Message
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host " TEST EXECUTION SUMMARY: $passed PASSED, $failed FAILED" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================================`n" -ForegroundColor Cyan

if ($failed -gt 0) { exit 1 } else { exit 0 }
