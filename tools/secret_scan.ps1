$ErrorActionPreference = "Stop"

$patterns = @(
  'sk-[A-Za-z0-9]{20,}',
  'AKIA[0-9A-Z]{16}',
  'ghp_[A-Za-z0-9]{36}',
  'BEGIN (RSA|OPENSSH|EC|DSA) PRIVATE KEY',
  'api[_-]?key\s*[:=]\s*[''"][^''"]{8,}',
  'token\s*[:=]\s*[''"][^''"]{8,}',
  'password\s*[:=]\s*[''"][^''"]{8,}'
)

$exclude = @(
  "!docs/assets/**",
  "!docs/参考/**",
  "!.git/**",
  "!build/**",
  "!app/build/**"
)

$failed = $false
foreach ($pattern in $patterns) {
  $args = @("-n", "--hidden", "-S", "--glob", "*", "--glob", "!.git/**", "--glob", "!build/**", "--glob", "!app/build/**", "--glob", "!docs/assets/**", "--glob", "!docs/参考/**", $pattern, ".")
  $output = & rg @args
  if ($LASTEXITCODE -eq 0) {
    Write-Host "[secret-scan] Potential secret pattern matched: $pattern" -ForegroundColor Red
    Write-Host $output
    $failed = $true
  }
}

if ($failed) {
  Write-Error "Potential secrets detected. Please remove or rotate credentials before commit."
}

Write-Host "[secret-scan] No obvious secrets detected." -ForegroundColor Green
