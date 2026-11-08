@echo off
title Rebuilding Badilika Executables
cd /d "%~dp0"
echo ========================================================
echo 1/2 Building standalone Badilika.exe...
echo ========================================================
python -m PyInstaller --noconfirm --onefile --windowed --name "Badilika" --icon "assets\badilika.ico" --add-data "adult_domains.txt;." --add-data "assets;assets" main.py
if errorlevel 1 (
    echo Badilika.exe build failed!
    pause
    exit /b 1
)
copy /y "dist\Badilika.exe" "Badilika.exe" >nul

echo.
echo ========================================================
echo 2/2 Building Badilika-Setup.exe Installer/Updater...
echo ========================================================
python -m PyInstaller --noconfirm --onefile --windowed --name "Badilika-Setup" --icon "assets\badilika.ico" --add-data "Badilika.exe;." --add-data "adult_domains.txt;." --add-data "assets;assets" installer.py
if errorlevel 1 (
    echo Badilika-Setup.exe build failed!
    pause
    exit /b 1
)
copy /y "dist\Badilika-Setup.exe" "Badilika-Setup.exe" >nul

echo.
echo ========================================================
echo SUCCESS!
echo App:       %~dp0Badilika.exe
echo Installer: %~dp0Badilika-Setup.exe
echo ========================================================
