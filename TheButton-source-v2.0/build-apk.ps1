param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$PlatformPath = "$PSScriptRoot\.tools\android-35",
    [string]$BuildToolsPath = "$PSScriptRoot\.tools\android-15"
)
$ErrorActionPreference = 'Stop'
if (!$JdkPath) { $JdkPath = 'C:\Program Files\Java\jdk-21' }
function Run-Native([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed with exit code $LASTEXITCODE" }
}
$java = Join-Path $JdkPath 'bin\java.exe'
$javac = Join-Path $JdkPath 'bin\javac.exe'
$jar = Join-Path $JdkPath 'bin\jar.exe'
$keytool = Join-Path $JdkPath 'bin\keytool.exe'
$android = Join-Path $PlatformPath 'android.jar'
$aapt = Join-Path $BuildToolsPath 'aapt2.exe'
foreach ($file in @($java,$javac,$jar,$keytool,$android,$aapt)) {
    if (!(Test-Path -LiteralPath $file)) { throw "Missing build tool: $file. See README.md." }
}
Push-Location -LiteralPath $PSScriptRoot
try {
# AAPT2 on Windows cannot reliably open absolute paths containing non-ASCII characters.
$android = (Resolve-Path -LiteralPath $android -Relative)
$output = '.\app\build\manual'
$runDir = Join-Path $output ([Guid]::NewGuid().ToString('N'))
$generated = Join-Path $runDir 'generated'
$classes = Join-Path $runDir 'classes'
$dex = Join-Path $runDir 'dex'
New-Item -ItemType Directory -Force $generated,$classes,$dex | Out-Null
$manifest = Join-Path $runDir 'AndroidManifest.xml'
[xml]$xml = Get-Content -Encoding UTF8 -LiteralPath "$PSScriptRoot\app\src\main\AndroidManifest.xml"
$ns = 'http://schemas.android.com/apk/res/android'
$xml.manifest.SetAttribute('package','ee.thebutton')
$xml.manifest.SetAttribute('versionCode',$ns,'2') | Out-Null
$xml.manifest.SetAttribute('versionName',$ns,'2.0') | Out-Null
$xml.manifest.application.SetAttribute('debuggable',$ns,'true') | Out-Null
$sdk = $xml.CreateElement('uses-sdk')
$sdk.SetAttribute('minSdkVersion',$ns,'26') | Out-Null
$sdk.SetAttribute('targetSdkVersion',$ns,'35') | Out-Null
$xml.manifest.AppendChild($sdk) | Out-Null
$xml.Save((Join-Path $PSScriptRoot $manifest))
$resources = Join-Path $runDir 'resources.zip'
$unsigned = Join-Path $runDir 'unsigned.apk'
Run-Native $aapt @('compile','--dir','.\app\src\main\res','-o',$resources)
Run-Native $aapt @('link','-o',$unsigned,'-I',$android,'--manifest',$manifest,'--java',$generated,'--auto-add-overlay',$resources)
$sources = @(Get-ChildItem -LiteralPath "$PSScriptRoot\app\src\main\java",$generated -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
Run-Native $javac (@('-encoding','UTF-8','-source','17','-target','17','-classpath',$android,'-d',$classes) + $sources)
$classJar = Join-Path $runDir 'classes.jar'
Run-Native $jar @('cf',$classJar,'-C',$classes,'.')
Run-Native $java @('-cp',"$BuildToolsPath\lib\d8.jar",'com.android.tools.r8.D8','--lib',$android,'--min-api','26','--output',$dex,$classJar)
Run-Native $jar @('uf',$unsigned,'-C',$dex,'classes.dex')
$aligned = Join-Path $runDir 'aligned.apk'
Run-Native "$BuildToolsPath\zipalign.exe" @('-f','4',$unsigned,$aligned)
$key = Join-Path $PSScriptRoot '.tools\debug.keystore'
if (!(Test-Path -LiteralPath $key)) {
    New-Item -ItemType Directory -Force (Split-Path $key) | Out-Null
    Run-Native $keytool @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Android Debug,O=Android,C=EE','-keyalg','RSA','-keysize','2048','-validity','10000')
}
$apk = Join-Path $output 'TheButton-debug.apk'
Run-Native $java @('-jar',"$BuildToolsPath\lib\apksigner.jar",'sign','--ks',$key,'--ks-pass','pass:android','--key-pass','pass:android','--out',$apk,$aligned)
Run-Native $java @('-jar',"$BuildToolsPath\lib\apksigner.jar",'verify','--verbose',$apk)
Run-Native $aapt @('dump','badging',$apk)
Write-Output "APK ready: $apk"
} finally { Pop-Location }
