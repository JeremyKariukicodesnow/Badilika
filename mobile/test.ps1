param([string]$JavaHome='C:/Program Files/Android/Android Studio1/jbr')
$ErrorActionPreference='Stop'
$out=Join-Path $PSScriptRoot 'build/tests'
New-Item -ItemType Directory -Force $out | Out-Null
& "$JavaHome/bin/javac.exe" -d $out "$PSScriptRoot/src/com/badilika/mobile/TimePolicy.java" "$PSScriptRoot/src/com/badilika/mobile/DnsPacket.java" "$PSScriptRoot/src/com/badilika/mobile/AppBudget.java" "$PSScriptRoot/src/com/badilika/mobile/ForegroundClock.java" "$PSScriptRoot/tests/PolicyTests.java"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& "$JavaHome/bin/java.exe" -cp $out PolicyTests
if($LASTEXITCODE -ne 0){throw 'Tests failed'}
