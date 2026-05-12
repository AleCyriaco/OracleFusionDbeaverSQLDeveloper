@echo off
title Fusion Query JDBC Installer

echo Unblocking files (one-time Windows security check)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path '%~dp0' -Recurse | Unblock-File" 2>nul

echo Starting installer...
start "" "%~dp0fusion-sqldev-installer-1.0.0.exe"
