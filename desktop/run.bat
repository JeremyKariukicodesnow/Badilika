@echo off
title Badilika
cd /d "%~dp0"
if exist "Badilika.exe" (
    start "" "Badilika.exe"
) else (
    python main.py
)
