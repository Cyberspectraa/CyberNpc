param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("8.8", "8.1.1")]
    [string] $GradleVersion,

    [Parameter(Mandatory = $true)]
    [ValidateSet("CyberNpc", "CyberRaces")]
    [string] $ModName,

    [Parameter(Mandatory = $true)]
    [string] $JarPattern
)

# This script is client-side local tooling only. No GitHub Actions or cloud CI.
# Uses only official Gradle distributions and validates the official SHA-256.
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$transcriptStarted = $false
$exitCode = 0

try {
    Start-Transcript -Path (Join-Path $root "build-local.log") -Force | Out-Null
    $transcriptStarted = $true

    Write-Host "=== $ModName local Forge build ==="
    Write-Host "Using Gradle $GradleVersion; requires JDK 17."
    $javac = Get-Command "javac" -ErrorAction SilentlyContinue
    $java = Get-Command "java" -ErrorAction SilentlyContinue
    if (-not $javac -or -not $java) {
        throw "Java JDK 17 is missing from PATH. Install Eclipse Temurin JDK 17, then reopen PowerShell."
    }

    $javacVersion = (& javac -version | Out-String).Trim()
    Write-Host $javacVersion
    if ($javacVersion -notmatch "^javac 17(\.|$)") {
        throw "The active Java compiler is not JDK 17. Check PATH and JAVA_HOME."
    }

    if (-not $env:LOCALAPPDATA) {
        throw "LOCALAPPDATA is missing. This launcher is intended for Windows."
    }

    $cache = Join-Path $env:LOCALAPPDATA "CyberMods\Gradle"
    New-Item -ItemType Directory -Path $cache -Force | Out-Null
    $gradleExe = Join-Path $cache ("gradle-" + $GradleVersion + "\bin\gradle.bat")

    if (-not (Test-Path -LiteralPath $gradleExe)) {
        $archiveName = "gradle-$GradleVersion-bin.zip"
        $url = "https://services.gradle.org/distributions/$archiveName"
        $archive = Join-Path $cache $archiveName
        $checksum = Join-Path $cache ($archiveName + ".sha256")

        Write-Host "Obtaining official Gradle $GradleVersion download and checksum..."
        Invoke-WebRequest -Uri ($url + ".sha256") -OutFile $checksum -UseBasicParsing
        $checksumText = Get-Content -LiteralPath $checksum -Raw
        $matched = [regex]::Match($checksumText, "[a-fA-F0-9]{64}")
        if (-not $matched.Success) {
            throw "Could not read the official Gradle SHA-256 checksum."
        }

        if (-not (Test-Path -LiteralPath $archive)) {
            Write-Host "Downloading $archiveName (first build only)..."
            Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing
        }

        $actual = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
        if ($actual -ne $matched.Value.ToUpperInvariant()) {
            Remove-Item -LiteralPath $archive -Force -ErrorAction SilentlyContinue
            throw "Gradle archive SHA-256 mismatch. Download was rejected."
        }

        Write-Host "Gradle checksum verified. Extracting..."
        Expand-Archive -LiteralPath $archive -DestinationPath $cache -Force
        if (-not (Test-Path -LiteralPath $gradleExe)) {
            throw "Gradle extraction completed without the expected gradle.bat."
        }
    }

    Write-Host "Starting Gradle build. The first Forge dependency download needs internet."
    Push-Location $root
    try {
        & $gradleExe clean build --no-daemon --stacktrace
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle exited with code $LASTEXITCODE. Check the build error above."
        }
    } finally {
        Pop-Location
    }

    $libs = Join-Path $root "build\libs"
    $jarFiles = @(Get-ChildItem -Path $libs -Filter $JarPattern -File |
        Where-Object { $_.Name -notmatch "-(sources|javadoc|dev|plain)\.jar$" })

    if ($jarFiles.Count -ne 1) {
        throw "Expected exactly one compiled mod JAR in build\libs. Found $($jarFiles.Count)."
    }

    $output = Join-Path $root ($ModName + ".jar")
    Copy-Item -LiteralPath $jarFiles[0].FullName -Destination $output -Force
    Write-Host "SUCCESS: $output"
    Write-Host "Keep the original versioned JAR in build\libs. Test before replacing stable pack files."
} catch {
    Write-Host "BUILD/SETUP FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Please share build-local.log if you need help."
    $exitCode = 1
} finally {
    if ($transcriptStarted) {
        Stop-Transcript | Out-Null
    }
}

exit $exitCode
