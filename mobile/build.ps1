param(
    [string]$Sdk = "$env:LOCALAPPDATA/Android/Sdk",
    [string]$JavaHome = 'C:/Program Files/Android/Android Studio1/jbr',
    [string]$VersionName = '1.6',
    [int]$VersionCode = 11
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome/bin;$env:PATH"
$root = $PSScriptRoot
$tools = Join-Path $Sdk 'build-tools/36.0.0'
$android = Join-Path $Sdk 'platforms/android-36/android.jar'
$build = Join-Path $root 'build'
$dist = Join-Path $root 'dist'
New-Item -ItemType Directory -Force $build,$dist,"$build/classes","$build/dex","$build/legacy","$root/assets" | Out-Null
function Run([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Command failed: $Exe ($LASTEXITCODE)" }
}
# Use the desktop's bundled, curated list as the reproducible seed.
$porn = [System.Collections.Generic.List[string]]::new()
$escort = [System.Collections.Generic.List[string]]::new()
$inEscort = $false
foreach ($line in Get-Content "$root/../desktop/adult_domains.txt") {
    if ($line -match '^# --- Escorts') { $inEscort = $true }
    if ($line.Trim() -and -not $line.StartsWith('#')) {
        if ($inEscort) { $escort.Add($line.Trim()) } else { $porn.Add($line.Trim()) }
    }
}
[IO.File]::WriteAllLines("$root/assets/porn.txt", $porn)
[IO.File]::WriteAllLines("$root/assets/escort.txt", $escort)
$sources = @(Get-ChildItem "$root/src" -Recurse -Filter '*.java' | ForEach-Object FullName)
Run "$JavaHome/bin/javac.exe" (@('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',"$android;$tools/core-lambda-stubs.jar",'-d',"$build/classes") + $sources)
Run "$JavaHome/bin/jar.exe" @('cf',"$build/classes.jar",'-C',"$build/classes",'.')
Run "$tools/d8.bat" @('--lib',$android,'--min-api','23','--output',"$build/dex","$build/classes.jar")
$key = "$env:USERPROFILE/.android/badilika-local.jks"
if (-not (Test-Path $key)) {
    Run "$JavaHome/bin/keytool.exe" @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','badilika','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Badilika Local Build')
}
$legacy = Join-Path $dist 'Badilika-Insights.apk'
if (Test-Path $legacy) {
    New-Item -ItemType Directory -Force "$build/legacy" | Out-Null
    Move-Item -LiteralPath $legacy -Destination "$build/legacy/Badilika-Insights-1.0.apk" -Force
    if(Test-Path "$legacy.idsig"){Move-Item -LiteralPath "$legacy.idsig" -Destination "$build/legacy/Badilika-Insights-1.0.apk.idsig" -Force}
}
foreach ($variant in @('Badilika')) {
    $dir = Join-Path $build $variant
    New-Item -ItemType Directory -Force "$dir/res" | Out-Null
    Copy-Item "$root/res/*" "$dir/res" -Recurse -Force
    $manifest = [IO.File]::ReadAllText("$root/AndroidManifest.xml")
    [IO.File]::WriteAllText("$dir/AndroidManifest.xml", $manifest)
    Run "$tools/aapt2.exe" @('compile','--dir',"$dir/res",'-o',"$dir/resources.zip")
    Run "$tools/aapt2.exe" @('link','-I',$android,'--manifest',"$dir/AndroidManifest.xml",'--version-code',"$VersionCode",'--version-name',$VersionName,'-A',"$root/assets",'-o',"$dir/unsigned.apk","$dir/resources.zip")
    # Add dex with the JDK zip writer; no external package dependency.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::Open("$dir/unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
    try { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build/dex/classes.dex",'classes.dex') | Out-Null } finally { $zip.Dispose() }
    Run "$tools/zipalign.exe" @('-f','4',"$dir/unsigned.apk","$dir/aligned.apk")
    Run "$tools/apksigner.bat" @('sign','--ks',$key,'--ks-key-alias','badilika','--ks-pass','pass:android','--key-pass','pass:android','--v1-signing-enabled','true','--v2-signing-enabled','true','--v3-signing-enabled','true','--v4-signing-enabled','false','--out',"$dist/$variant.apk","$dir/aligned.apk")
    Run "$tools/apksigner.bat" @('verify','--verbose','--min-sdk-version','21',"$dist/$variant.apk")
    if(Test-Path "$dist/$variant.apk.idsig"){Move-Item -LiteralPath "$dist/$variant.apk.idsig" -Destination "$build/legacy/$variant-old.apk.idsig" -Force}
    Write-Host "Built $dist/$variant.apk"
}
Copy-Item -LiteralPath "$dist/Badilika.apk" -Destination "$dist/Badilika-$VersionName.apk" -Force
Compress-Archive -LiteralPath "$dist/Badilika-$VersionName.apk" -DestinationPath "$dist/Badilika-$VersionName.zip" -Force
