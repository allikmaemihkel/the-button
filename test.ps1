$ErrorActionPreference = 'Stop'
$jdk = if($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Java\jdk-21' }
$classes = Join-Path $PSScriptRoot 'app\build\url-tests'
New-Item -ItemType Directory -Force $classes | Out-Null
& "$jdk\bin\javac.exe" -encoding UTF-8 -d $classes "$PSScriptRoot\app\src\main\java\ee\thebutton\WebAddress.java" "$PSScriptRoot\tests\ee\thebutton\WebAddressTest.java"
if($LASTEXITCODE -ne 0) { throw 'URL test compilation failed' }
& "$jdk\bin\java.exe" -cp $classes ee.thebutton.WebAddressTest
if($LASTEXITCODE -ne 0) { throw 'URL tests failed' }
