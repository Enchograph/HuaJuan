$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
Set-Location $repoRoot

function Get-ResourceNames {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Path
  )

  if (-not (Test-Path $Path)) {
    throw "Missing resource file: $Path"
  }

  [xml]$xml = Get-Content $Path -Raw
  $names = New-Object 'System.Collections.Generic.HashSet[string]'

  foreach ($node in $xml.resources.ChildNodes) {
    if ($node.NodeType -ne [System.Xml.XmlNodeType]::Element) {
      continue
    }
    if ($node.HasAttribute("name")) {
      [void]$names.Add($node.name)
    }
  }

  return @($names | Sort-Object)
}

function Compare-ResourceSets {
  param(
    [string]$BasePath,
    [string]$LocalePath,
    [string]$LocaleName
  )

  $baseNames = Get-ResourceNames -Path $BasePath
  $localeNames = Get-ResourceNames -Path $LocalePath

  $missing = $baseNames | Where-Object { $_ -notin $localeNames }
  $extra = $localeNames | Where-Object { $_ -notin $baseNames }

  return [pscustomobject]@{
    Locale = $LocaleName
    Missing = @($missing)
    Extra = @($extra)
  }
}

function Invoke-Rg {
  param(
    [string[]]$Arguments
  )

  $lines = & rg @Arguments 2>$null
  $code = $LASTEXITCODE
  if ($code -gt 1) {
    throw "rg failed with exit code $code for arguments: $($Arguments -join ' ')"
  }
  if ($code -eq 1) {
    return @()
  }
  return @($lines)
}

function Get-AllowlistPatterns {
  param([string]$Path)

  if (-not (Test-Path $Path)) {
    return @()
  }

  return Get-Content $Path | Where-Object {
    $line = $_.Trim()
    $line -and -not $line.StartsWith("#")
  }
}

function Apply-Allowlist {
  param(
    [string[]]$Lines,
    [string[]]$AllowPatterns
  )

  if (-not $AllowPatterns -or $AllowPatterns.Count -eq 0) {
    return @($Lines)
  }

  $kept = New-Object System.Collections.Generic.List[string]
  foreach ($line in $Lines) {
    $allowed = $false
    foreach ($pattern in $AllowPatterns) {
      if ($line -match $pattern) {
        $allowed = $true
        break
      }
    }
    if (-not $allowed) {
      [void]$kept.Add($line)
    }
  }
  return @($kept)
}

$failures = New-Object System.Collections.Generic.List[string]

$baseStrings = Join-Path $repoRoot "app/src/main/res/values/strings.xml"
$localeFiles = @(
  @{ Name = "en"; Path = (Join-Path $repoRoot "app/src/main/res/values-en/strings.xml") },
  @{ Name = "ja"; Path = (Join-Path $repoRoot "app/src/main/res/values-ja/strings.xml") }
)

Write-Host "[i18n] Checking resource parity..." -ForegroundColor Cyan
foreach ($locale in $localeFiles) {
  $result = Compare-ResourceSets -BasePath $baseStrings -LocalePath $locale.Path -LocaleName $locale.Name
  if ($result.Missing.Count -gt 0) {
    [void]$failures.Add("Missing keys in ${($result.Locale)}: $($result.Missing -join ', ')")
  }
  if ($result.Extra.Count -gt 0) {
    [void]$failures.Add("Extra keys in ${($result.Locale)}: $($result.Extra -join ', ')")
  }
}

$allowlist = Get-AllowlistPatterns -Path (Join-Path $repoRoot "tools/i18n_allowlist.txt")

Write-Host "[i18n] Scanning Compose/UI hardcoded strings..." -ForegroundColor Cyan
$uiLiteralArgs = @(
  "-n",
  'Text\s*\(\s*"|contentDescription\s*=\s*"|placeholder\s*=\s*\{\s*Text\s*\(\s*"|label\s*=\s*\{\s*Text\s*\(\s*"|title\s*=\s*\{\s*Text\s*\(\s*"|headlineContent\s*=\s*\{\s*Text\s*\(\s*"',
  "app/src/main/java",
  "--glob",
  "*.kt"
)
$uiLiterals = Invoke-Rg -Arguments $uiLiteralArgs
$uiViolations = $uiLiterals | Where-Object {
  $_ -notmatch 'Text\("[-+]"\)' -and
  $_ -notmatch 'Text\("\{.*\}"\)' -and
  $_ -notmatch 'Text\("1024"\)'
}
if ($uiViolations.Count -gt 0) {
  [void]$failures.Add("Hardcoded UI strings found:`n$($uiViolations -join "`n")")
}

Write-Host "[i18n] Scanning Kotlin string literals with CJK characters..." -ForegroundColor Cyan
$hanLiteralArgs = @(
  "-n",
  '"[^"\r\n]*[一-龥ぁ-んァ-ヶ][^"\r\n]*"',
  "app/src/main/java",
  "--glob",
  "*.kt"
)
$hanLines = Invoke-Rg -Arguments $hanLiteralArgs
$hanViolations = Apply-Allowlist -Lines $hanLines -AllowPatterns $allowlist
if ($hanViolations.Count -gt 0) {
  [void]$failures.Add("CJK Kotlin literals found outside allowlist:`n$($hanViolations -join "`n")")
}

if ($failures.Count -gt 0) {
  Write-Host ""
  Write-Host "[i18n] Failed." -ForegroundColor Red
  foreach ($failure in $failures) {
    Write-Host ""
    Write-Host $failure -ForegroundColor Red
  }
  exit 1
}

Write-Host "[i18n] Resource parity and hardcoded-text checks passed." -ForegroundColor Green
