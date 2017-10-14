
REM Created by App Builder v2017.91
REM More information at https://www.davidesperalta.com/

@ECHO OFF
CLS

REM Set this directory as the current one
CD %~dp0

REM Adding your Batch start commands to be executed

COPY ..\Fcm\google-services.json .\google-services.json

CALL cordova plugin add phonegap-plugin-push@^2.0.0-rc4

REM Add the Android platform for our app
CALL cordova platform add android

REM Add the Whitelist plugin for our app
CALL cordova plugin add cordova-plugin-whitelist

REM Build our app for the Android platform
CALL cordova run android

REM Pause the script execution, so we can view the script results
PAUSE
