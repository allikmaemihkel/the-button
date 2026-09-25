$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = $PSScriptRoot
$downloads = Join-Path $projectRoot 'downloads'
New-Item -ItemType Directory -Force $downloads | Out-Null
$apk = Join-Path $projectRoot 'app\build\manual\TheButton-debug.apk'
if(!(Test-Path -LiteralPath $apk)) { throw 'Run build-apk.ps1 first.' }
$names = @('.gitignore','build-apk.ps1','build.gradle','gradle.properties','README.md','settings.gradle','test.ps1','package-downloads.ps1','app\build.gradle')
$files = @($names | ForEach-Object { Join-Path $projectRoot $_ }) + @(Get-ChildItem -LiteralPath "$projectRoot\app\src","$projectRoot\tests" -Recurse -File | ForEach-Object { $_.FullName })
$zipPath = Join-Path $downloads 'TheButton-source-v2.0.zip'
$stream = [System.IO.File]::Open($zipPath,[System.IO.FileMode]::Create)
$archive = New-Object System.IO.Compression.ZipArchive($stream,[System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach($file in $files) {
        $entry = $file.Substring($projectRoot.Length + 1).Replace('\','/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$file,$entry) | Out-Null
    }
} finally { $archive.Dispose();$stream.Dispose() }
Copy-Item -LiteralPath $apk -Destination "$downloads\TheButton-v2.0.apk" -Force
# Keep the previously shared local links up to date too.
Copy-Item -LiteralPath $apk -Destination "$downloads\TheButton-debug.apk" -Force
Copy-Item -LiteralPath $zipPath -Destination "$downloads\TheButton-source.zip" -Force
Get-ChildItem -LiteralPath $downloads | Select-Object Name,Length
