<#
 Dapur Sappa - publish: upload semua perubahan ke GitHub.
 Web (GitHub Pages) langsung update, lalu GitHub Actions membuat APK dan aplikasi menampilkan "update tersedia".
 Jalankan dengan double-click publish.bat SETELAH versi di Config.kt dan app\build.gradle.kts dinaikkan.
#>
$ErrorActionPreference = "Continue"
Set-Location $PSScriptRoot
$repoUrl = "https://github.com/dennistan7-tan/dapur-sappa.git"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Write-Host "Git belum terpasang. Install dari https://git-scm.com lalu jalankan lagi." -ForegroundColor Red; exit 1 }
if (Get-Command gh -ErrorAction SilentlyContinue) { gh auth setup-git 2>&1 | Out-Null }

$gradle = Get-Content "app\build.gradle.kts" -Raw
$name = [regex]::Match($gradle, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $name) { Write-Host "versionName tidak terbaca" -ForegroundColor Red; exit 1 }
$tag = "v$name"
Write-Host "Publish Dapur Sappa $tag ..."

if (-not (Test-Path ".git")) {
    git init -b main | Out-Null
    git remote add origin $repoUrl
}
if (-not (git config user.name))  { git config user.name  "Dennis" }
if (-not (git config user.email)) { git config user.email "dev.dericktan@gmail.com" }

git fetch origin main
if ($LASTEXITCODE -ne 0) { Write-Host "Gagal fetch dari GitHub (cek login)." -ForegroundColor Red; exit 1 }
git reset origin/main | Out-Null      # samakan riwayat, file lokal tetap
# workflow Actions (selalu ditulis ulang supaya tidak pernah hilang dari repo)
New-Item -ItemType Directory -Force ".github\workflows" | Out-Null
$yml = @'
name: Build APK dan rilis
on:
  push:
    tags: ['v*']
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - uses: gradle/actions/setup-gradle@v4
        with: { gradle-version: '8.7' }
      - name: Build APK
        run: gradle :app:assembleDebug --no-daemon
      - name: Ganti nama APK
        run: cp app/build/outputs/apk/debug/app-debug.apk "DapurSappa-${GITHUB_REF_NAME}.apk"
      - uses: softprops/action-gh-release@v2
        with:
          files: DapurSappa-${{ github.ref_name }}.apk
          body_path: release-notes.txt
          name: Dapur Sappa ${{ github.ref_name }}
'@
[System.IO.File]::WriteAllText((Join-Path $PSScriptRoot ".github\workflows\release.yml"), ($yml -replace "`r`n","`n"))
git add -A
git commit -m "Dapur Sappa $tag"
git push origin main
if ($LASTEXITCODE -ne 0) { Write-Host "Push gagal (cek login GitHub)." -ForegroundColor Red; exit 1 }

git tag -f $tag
git push origin $tag --force
if ($LASTEXITCODE -ne 0) { Write-Host "Push tag gagal." -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "SELESAI. Web langsung update. APK dibuat GitHub +/- 4 menit:"
Write-Host "https://github.com/dennistan7-tan/dapur-sappa/actions"
