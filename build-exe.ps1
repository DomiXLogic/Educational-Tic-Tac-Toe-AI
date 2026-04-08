$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$targetDir = Join-Path $projectRoot "target"
$inputDir = Join-Path $targetDir "jpackage-input"
$installerDir = Join-Path $targetDir "installer"
$mainJar = "ai-ti-tac-toe-1.0-SNAPSHOT.jar"

Write-Host "Building project and copying runtime dependencies..."
mvn -q clean package dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"

if (Test-Path $installerDir) {
    Remove-Item -Recurse -Force $installerDir
}

New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item -Force (Join-Path $targetDir $mainJar) $inputDir

Write-Host "Creating Windows installer with jpackage..."
jpackage `
  --type exe `
  --name "Tic-Tac-Toe AI" `
  --input $inputDir `
  --main-jar $mainJar `
  --main-class com.ai.tictactoe.AiTiTacToe `
  --dest $installerDir `
  --vendor "AI Tic-Tac-Toe" `
  --app-version 1.0.0 `
  --win-dir-chooser `
  --win-shortcut `
  --win-menu

Write-Host ""
Write-Host "Installer created in: $installerDir"
