; wpbd.nsi
;
; NSIS build script for the Bridge Designer.
;

; Set up mutiuser privilege package
!define MULTIUSER_EXECUTIONLEVEL Highest
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_MUI

!include "MultiUser.nsh"
!include "MUI2.nsh"
!include "FileAssociation.nsh"
!include "FileFunc.nsh"

!define JRE_VERSION "1.6"
; Last tested Java Runtime download.
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=62321"
!include "JREDyna.nsh"

; Init functions needed for multi-user package.
Function .onInit
  !insertmacro MULTIUSER_INIT
FunctionEnd

Function un.onInit
  !insertmacro MULTIUSER_UNINIT
FunctionEnd

!define BD "Bridge Designer 20${YEAR} (2nd Edition)"
!define RESOURCE_DIR  "..\src\wpbd\resources"
!define EXE "wpbdv${YEAR}j${BUILD}.exe"

Name "${BD}"
OutFile "../release/setupwpbdv${YEAR}j.exe"

InstallDir "$PROGRAMFILES\${BD}"
InstallDirRegKey HKCU "Software\${BD}" ""
; We're letting MultiUser handle this now.
; Admin execution level is necessary for Vista and Windows 7.
; RequestExecutionLevel admin
BrandingText "Engineering Encounters"

DirText "Choose a folder for the Bridge Designer."

Var StartMenuFolder

; Give a warning if the user tries to stop before complete.
!define MUI_ABORTWARNING

; Provide custom graphics and configuration information.
!define MUI_HEADERIMAGE_BITMAP "${RESOURCE_DIR}\installlogo.bmp"
!define MUI_HEADERIMAGE_UNBITMAP  "${RESOURCE_DIR}\installlogo.bmp"
!define MUI_HEADERIMAGE_RIGHT
!define MUI_WELCOMEFINISHPAGE_BITMAP "${RESOURCE_DIR}\welcomelogo.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP  "${RESOURCE_DIR}\welcomelogo.bmp"

; Welcome page settings.
!define MUI_WELCOMEPAGE_TITLE "${BD}"
!define MUI_WELCOMEPAGE_TEXT "Welcome to the ${BD} installer.$\r$\n$\r$\nThe Bridge Designer is designed to run \
on any computer that supports Java Runtime Version 1.6 or later. This installer will attempt to download and install the \
Runtime from Oracle if necessary.$\r$\n$\r$\n\If you have any other programs running, please close them \
before proceeding with this installation.$\r$\n$\r$\nClick Next to continue."
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${RESOURCE_DIR}\license.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MULTIUSER_PAGE_INSTALLMODE
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
!insertmacro CUSTOM_PAGE_JREINFO
!insertmacro MUI_PAGE_INSTFILES
; Finish page settings.
!define MUI_FINISHPAGE_TITLE "${BD} installation complete"
!define MUI_FINISHPAGE_TEXT "Installation is complete! Thanks for choosing to use our \
software. Check http://bridgecontest.org for information and updates \
about the Bridge Design Contest."
!define MUI_FINISHPAGE_RUN "$INSTDIR\${EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Run the ${BD}."
!define MUI_FINISHPAGE_RUN_PARAMETERS "-legacygraphics"
!define MUI_FINISHPAGE_RUN_NOTCHECKED
!insertmacro MUI_PAGE_FINISH

!define MUI_WELCOMEPAGE_TITLE "${BD} Uninstaller"
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!define MUI_FINISHPAGE_TITLE "${BD} is uninstalled"
!define MUI_FINISHPAGE_TEXT "Uninstallation is complete! Check \
http://bridgecontest.org for information and updates \
about the Bridge Design Contest."
!insertmacro MUI_UNPAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

!verbose 3

Section "Bridge Designer" SectionBD

    SetOutPath $INSTDIR

    File ${RESOURCE_DIR}\*.ico
    File /r /x WPBD.jar /x README.TXT ..\dist\*.* 

    ; Load either 32-bit or 64-bit dlls.  Detectjvm.exe is a small java program that checks
    ; global attributes for the jvm architecture and returns 64, 32, or 0 error code for 64 bit, 32 bit, or unknown.
    ClearErrors
    ExecWait '"$INSTDIR\detectjvm.exe"' $0
    IfErrors DetectExecError
    IntCmp $0 0 DetectError DetectError DoneDetect
    DetectExecError:
        StrCpy $0 "exec error"
    DetectError:
        MessageBox MB_OK "Could not determine JVM architecture ($0). Assuming 32-bit."
        Goto NotX64
    DoneDetect:
    IntCmp $0 64 X64 NotX64 NotX64
    X64:
        File ..\..\libs\jogamp-all-platforms\lib\windows-amd64\*.dll
        Goto DoneX64
    NotX64:
        File ..\..\libs\jogamp-all-platforms\lib\windows-i586\*.dll
    DoneX64:

    ; Don't need the detector any more. Delete it.
    Delete "$INSTDIR\detectjvm.exe"

    ; Create the uninstaller executable.
    WriteUninstaller "$INSTDIR\uninstall.exe"

    ; Write shortcuts in Start menu.
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
        CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
        CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${BD}.lnk" "$INSTDIR\${EXE}" "" "$INSTDIR\appicon.ico"
        CreateShortCut "$SMPROGRAMS\$StartMenuFolder\${BD} for older computers.lnk" "$INSTDIR\${EXE}" "-legacygraphics" "$INSTDIR\appicon.ico"
        CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall ${BD}.lnk" "$INSTDIR\uninstall.exe" 
    !insertmacro MUI_STARTMENU_WRITE_END

    ; Save the Start menu folder path.
    FileOpen $0 "$INSTDIR\uninstall.dat" w
    FileWrite $0 "$SMPROGRAMS\$StartMenuFolder"
    FileClose $0

    ; Write registry with uninstall info for the programs manager.
    StrCpy $0 "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${BD}"
    WriteRegStr HKLM $0 "DisplayName" "${BD} (remove only)"
    WriteRegStr HKLM $0 "UninstallString" "$INSTDIR\uninstall.exe"

    ; Clear old session data, if any.
    ExpandEnvStrings $0 "%APPDATA%"
    Delete "$0\EngineeringEncounters\WPBD\*.*"
SectionEnd

Section "Register File Extension" SectionRegExt
    ${FileAssociation_VERBOSE} 1
    ${RegisterExtension} "$INSTDIR\${EXE}" ".bdc" "WP Bridge Design File"
    ${RefreshShellIcons}
SectionEnd

Section "Java Runtime Check" SectionJavaRt
    ${GetParameters} $R0
    ${GetOptions} $R0 "/J" $R1
    IfErrors 0 +2
    call DownloadAndInstallJREIfNecessary
SectionEnd

; Set up the description blocks for mouseovers of the page selection checkboxes.
LangString DESC_SectionBD ${LANG_ENGLISH} \
   "Install all ${BD} and Help system files."
LangString DESC_SectionRegExt ${LANG_ENGLISH} \
   "Register .bdc file extension so that a double-click will start the Bridge Designer and load the file."
LangString DESC_SectionJavaRt ${LANG_ENGLISH} \
   "Check for Java Runtime 1.6 or newer and download from the Internet if necessary."

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SectionBD} $(DESC_SectionBD)
    !insertmacro MUI_DESCRIPTION_TEXT ${SectionRegExt} $(DESC_SectionRegExt)
    !insertmacro MUI_DESCRIPTION_TEXT ${SectionJavaRt} $(DESC_SectionJavaRt)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"

    ; Unregister the bridge file type.
    ${UnregisterExtension} ".bdc" "WP Bridge Design File"
    ${RefreshShellIcons}

    ; Retrieve the start menu folder stored in a file during installation.
    ClearErrors
    FileOpen $0 "$INSTDIR\uninstall.dat" r
    IfErrors LinkRemovalProblem
    FileRead $0 $1
    FileClose $0

    ; Delete the start menu entries and folder using path in $1.
    Delete "$1\${BD}.lnk"
    Delete "$1\${BD} for older computers.lnk"
    Delete "$1\Uninstall ${BD}.lnk"
    RMDir "$1"
    IfErrors LinkRemovalProblem

    Goto DoneDeleteLinks

    ; Handle any problems with link removal.
    LinkRemovalProblem:
    MessageBox MB_OK "Sorry. Could not delete Start menu items. Try deleting manually."

    DoneDeleteLinks:

    ; Delete uninstall and start menu registry keys.
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${BD}"

    ; Clear session data, if any.
    ExpandEnvStrings $0 "%APPDATA%"
    RMDir /r "$0\EngineeringEncounters"

    ; Delete the installation files. Do this last so we don't clobber uninstall.dat before use.
    RMDir /r /REBOOTOK $INSTDIR

SectionEnd