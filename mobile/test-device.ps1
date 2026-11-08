param([string]$Sdk="$env:LOCALAPPDATA/Android/Sdk",[string]$JavaHome='C:/Program Files/Android/Android Studio1/jbr',[switch]$BuildOnly)
$ErrorActionPreference='Stop'
$env:JAVA_HOME=$JavaHome
$env:PATH="$JavaHome/bin;$env:PATH"
$root=$PSScriptRoot
$build="$root/build/integration"
$tools="$Sdk/build-tools/36.0.0"
$android="$Sdk/platforms/android-36/android.jar"
$adb="$Sdk/platform-tools/adb.exe"
New-Item -ItemType Directory -Force "$build/classes","$build/dex" | Out-Null
function Run([string]$Exe,[string[]]$Arguments){& $Exe @Arguments;if($LASTEXITCODE -ne 0){throw "Failed: $Exe"}}
Run "$JavaHome/bin/javac.exe" @('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',"$android;$tools/core-lambda-stubs.jar",'-classpath',"$root/build/classes.jar",'-d',"$build/classes","$root/tests/IntegrationRunner.java")
Run "$JavaHome/bin/jar.exe" @('cf',"$build/tests.jar",'-C',"$build/classes",'.')
Run "$tools/d8.bat" @('--lib',$android,'--classpath',"$root/build/classes.jar",'--min-api','26','--output',"$build/dex","$build/tests.jar")
'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.badilika.tests"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:label="Badilika tests"/><instrumentation android:name="com.badilika.tests.IntegrationRunner" android:targetPackage="com.badilika.mobile"/></manifest>' | Set-Content "$build/AndroidManifest.xml"
Run "$tools/aapt2.exe" @('link','-I',$android,'--manifest',"$build/AndroidManifest.xml",'-o',"$build/unsigned.apk")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build/dex/classes.dex",'classes.dex')|Out-Null}finally{$zip.Dispose()}
Run "$tools/zipalign.exe" @('-f','4',"$build/unsigned.apk","$build/aligned.apk")
Run "$tools/apksigner.bat" @('sign','--ks',"$env:USERPROFILE/.android/badilika-local.jks",'--ks-key-alias','badilika','--ks-pass','pass:android','--out',"$build/tests.apk","$build/aligned.apk")
if(-not $BuildOnly){Run $adb @('install','-r',"$root/dist/Badilika.apk");Run $adb @('install','-r',"$build/tests.apk")}
$fixture="$build/fixture"
New-Item -ItemType Directory -Force "$fixture/classes","$fixture/dex" | Out-Null
Run "$JavaHome/bin/javac.exe" @('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',$android,'-d',"$fixture/classes","$root/tests/TimerFixture.java")
Run "$JavaHome/bin/jar.exe" @('cf',"$fixture/classes.jar",'-C',"$fixture/classes",'.')
Run "$tools/d8.bat" @('--lib',$android,'--min-api','26','--output',"$fixture/dex","$fixture/classes.jar")
'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.badilika.timerfixture"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:label="Timer test app"><activity android:name="com.badilika.timerfixture.TimerFixture" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>' | Set-Content "$fixture/AndroidManifest.xml"
Run "$tools/aapt2.exe" @('link','-I',$android,'--manifest',"$fixture/AndroidManifest.xml",'-o',"$fixture/unsigned.apk")
$zip=[IO.Compression.ZipFile]::Open("$fixture/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$fixture/dex/classes.dex",'classes.dex')|Out-Null}finally{$zip.Dispose()}
Run "$tools/zipalign.exe" @('-f','4',"$fixture/unsigned.apk","$fixture/aligned.apk")
Run "$tools/apksigner.bat" @('sign','--ks',"$env:USERPROFILE/.android/badilika-local.jks",'--ks-key-alias','badilika','--ks-pass','pass:android','--out',"$fixture/fixture.apk","$fixture/aligned.apk")
if($BuildOnly){Write-Host 'Built isolated instrumentation and fixture APKs';return}
Run $adb @('install','-r',"$fixture/fixture.apk")
Run $adb @('shell','appops','set','com.badilika.mobile','GET_USAGE_STATS','allow')
Run $adb @('shell','appops','set','com.badilika.mobile','ACTIVATE_VPN','allow')
if([int]((& $adb shell getprop ro.build.version.sdk)-join '') -ge 33){Run $adb @('shell','pm','grant','com.badilika.mobile','android.permission.POST_NOTIFICATIONS')}
# The system service is enabled only in this isolated-device test, never by production code.
Run $adb @('shell','settings','put','secure','enabled_accessibility_services','com.badilika.mobile/com.badilika.mobile.AppLimitService')
Run $adb @('shell','settings','put','secure','accessibility_enabled','1')
$report = & $adb shell am instrument -w com.badilika.tests/com.badilika.tests.IntegrationRunner
$report | ForEach-Object { Write-Host $_ }
if($LASTEXITCODE -ne 0 -or ($report -join "`n") -notmatch 'PASS: \d+ Android integration checks' -or ($report -join "`n") -match 'FAIL'){throw 'Android integration checks failed'}
