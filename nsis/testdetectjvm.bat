@echo off
..\dist\detectjvm
if errorlevel 64 (
  echo 64-bit
  goto done
) 
if errorlevel 32 (
  echo 32-bit
  goto done
) 
if errorlevel 0 (
  echo Undetermined
  goto done
) 
:done
