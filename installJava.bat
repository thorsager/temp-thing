@echo off
echo 
echo Please read the following End User License Agreement (EULA) carefully and answer the prompt that follows it.
set /p acceptContinue=Press "enter" to continue:
type .\cmapijava-sdk\LICENSE.txt | more

:getConfirmation
set /p acceptEULA=Do you agree to the terms of the EULA? [y/n]:
if /I "%acceptEULA%"=="n" goto cancelDeploy
if /I "%acceptEULA%"=="y" goto deployCode
goto getConfirmation

:deployCode
set acceptDir=C:\
set /p acceptDir=Enter the directory name to install the AE Services DMCC Java SDK [C:\]: 
echo Copying files - this may take a few minutes ...
xcopy /I/E/Q %TEMP%\WZSE0.TMP\cmapijava-sdk %acceptDir%\cmapijava-sdk
echo .
echo DMCC Java SDK installed to %acceptDir%
goto end

:cancelDeploy
echo .
echo Aborting DMCC Java SDK installation.
goto end

:end
echo .
set /p acceptEnd=Hit "ENTER" to complete the SDK installation:
rmdir /S/Q %TEMP%WZSE0.TMP

