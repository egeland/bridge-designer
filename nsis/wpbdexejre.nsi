; wpbdexejre.nsi
;
; NSIS build script for the Bridge Designer.
;

!define BD "Bridge Designer 20${YEAR} (2nd Edition)"
!define EXE "bdv${YEAR}j${BUILD}.exe"
!define RESOURCE_DIR  "..\src\bridgedesigner\resources"

; Set up multiuser privilege package
!define MULTIUSER_INSTALLMODE_INSTDIR "${BD}"
!define MULTIUSER_EXECUTIONLEVEL Highest
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_MUI

!define CANT_WRITE_INSTDIR_MSG "The current Windows User does not have \
permission to install programs at $\r$\n$\r$\n\
$INSTDIR$\r$\n$\r$\n\
Please cancel this installation and try again, either logging in as a more \
powerful user (such as Administrator) or changing the installation folder to \
one where the Windows User has write permission."

!define INST_DIR_EXISTS_MSG "The folder $\r$\n$\r$\n\
$INSTDIR$\r$\n$\r$\n\
that you selected for installation already exists. Please cancel this \
installation and try again, naming a folder that doesn't."

!define INST_DIR_IS_OLD_VERSION "The folder $\r$\n$\r$\n\
$INSTDIR$\r$\n$\r$\n\
that you selected for installation appears to be a previous Bridge Designer \
version. Please cancel this installation and try again after uninstalling \
the old version or naming a folder that doesn't already exist."

!define OK_TO_CLOBBER_INST_DIR_MSG "The Bride Designer folder and all its \
contents are about to be deleted forever.$\r$\n$\r$\n\
$INSTDIR$\r$\n$\r$\n\
Are you sure you want to proceed?"

!define INST_DIR_NOT_DELETED "The folder $\r$\n$\r$\n\
$INSTDIR$\r$\n$\r$\n\
was not changed."

!include "MultiUser.nsh"
!include "MUI2.nsh"
!include "FileAssociation.nsh"
!include "FileFunc.nsh"

; Init functions needed for multi-user package.
Function .onInit
  !insertmacro MULTIUSER_INIT
FunctionEnd

Function un.onInit
  !insertmacro MULTIUSER_UNINIT
FunctionEnd

Name "${BD}"
OutFile "../release/setupbdv${YEAR}j.exe"

; Let MultiUser handle this: InstallDir "$PROGRAMFILES\${BD}"
InstallDirRegKey HKCU "Software\${BD}" ""
BrandingText "Engineering Encounters"

DirText "Choose a folder for the Bridge Designer."

Var StartMenuFolder

; Give a warning if the user tries to stop before complete.
!define MUI_ABORTWARNING

; Set the installer icon.
!define MUI_ICON "${RESOURCE_DIR}\appiconnew.ico"
!define MUI_UNICON "${RESOURCE_DIR}\appiconnew.ico"

; Provide custom graphics and configuration information.
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${RESOURCE_DIR}\installlogo.bmp"
!define MUI_HEADERIMAGE_UNBITMAP  "${RESOURCE_DIR}\installlogo.bmp"
!define MUI_HEADERIMAGE_RIGHT
!define MUI_WELCOMEFINISHPAGE_BITMAP "${RESOURCE_DIR}\welcomelogo.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP  "${RESOURCE_DIR}\welcomelogo.bmp"

; Welcome page settings.
!define MUI_WELCOMEPAGE_TITLE "${BD}"
!define MUI_WELCOMEPAGE_TEXT "Welcome to the ${BD} installer.$\r$\n$\r$\nThe Bridge Designer is designed to run \
on any computer capable of running Java. This installer includes a copy of the Java runtime.$\r$\n$\r$\n\
If you have any other programs running, please close them before proceeding with this \
installation.$\r$\n$\r$\nClick Next to continue."
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "${RESOURCE_DIR}\license.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MULTIUSER_PAGE_INSTALLMODE
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
!insertmacro MUI_PAGE_INSTFILES
; Finish page settings.
!define MUI_FINISHPAGE_TITLE "${BD} installation complete"
!define MUI_FINISHPAGE_TEXT "Installation is complete! Thanks for choosing to \
use our software. Check http://bridgecontest.org for information and \
updates about the Bridge Design Contest."
!define MUI_FINISHPAGE_RUN "$INSTDIR\${EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Run the ${BD}."
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

    ; Ensure the installation directory doesn't exist because we clobber
    ; it recursively when we uninstall. Allow re-installation, though.
    IfFileExists "$INSTDIR\${EXE}" CreateAndSet
    IfFileExists $INSTDIR InstDirExistsError

    ; Create (if necessary) and set the installation directory.
  CreateAndSet:
    ClearErrors
    SetOutPath $INSTDIR
    IfErrors CantWriteInstallDirectory

    ; Check writability of installation directory.
    FileOpen $0 "$INSTDIR\writecheck.txt" w
    FileWrite $0 "write check"
    IfErrors CantWriteInstallDirectory
    FileClose $0
    Delete "$INSTDIR\writecheck.txt"

    ; Copy all the system files to the install directory.
    File ${RESOURCE_DIR}\*.ico
    File /r /x WPBD.jar /x README.TXT /x detectjvm.exe ..\dist\*.* 
    File /r ..\jre\*.*
    ; Since we are using a 32-bit jre, always get the 32-bit dll.
    File ..\..\lib\jogamp-all-platforms\lib\windows-i586\*.dll

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
    goto Done

  CantWriteInstallDirectory:
    MessageBox MB_OK|MB_ICONEXCLAMATION "${CANT_WRITE_INSTDIR_MSG}" 
    Abort

  InstDirExistsError:
    IfFileExists "$INSTDIR\bdv??j*.exe" ElseShowUninstallMsg
      MessageBox MB_OK|MB_ICONEXCLAMATION "${INST_DIR_EXISTS_MSG}"
      Abort
    ElseShowUninstallMsg:
      MessageBox MB_OK|MB_ICONEXCLAMATION "${INST_DIR_IS_OLD_VERSION}"
      Abort

    Done:
SectionEnd

Section "Register File Extension" SectionRegExt
    ${FileAssociation_VERBOSE} 1
    ${RegisterExtension} "$INSTDIR\${EXE}" ".bdc" "WP Bridge Design File"
    ${RefreshShellIcons}
SectionEnd

; Set up the description blocks for mouseovers of the page selection checkboxes.
LangString DESC_SectionBD ${LANG_ENGLISH} \
   "Install all ${BD} and Help system files."
LangString DESC_SectionRegExt ${LANG_ENGLISH} \
   "Register .bdc file extension so that a double-click will start the Bridge Designer and load the file."

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SectionBD} $(DESC_SectionBD)
    !insertmacro MUI_DESCRIPTION_TEXT ${SectionRegExt} $(DESC_SectionRegExt)
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

  ; Unless in silent mode, make double sure the user wants to clobber the dir.
  MessageBox MB_YESNO|MB_ICONEXCLAMATION "${OK_TO_CLOBBER_INST_DIR_MSG}" /SD IDYES IDNO ShowNotDeletedMsg 

    ; Delete the installation files. Do this last so we don't clobber uninstall.dat before use.
    RMDir /r /REBOOTOK $INSTDIR
    Goto UninstallComplete

  ShowNotDeletedMsg:
    MessageBox MB_OK "${INST_DIR_NOT_DELETED}"

  UninstallComplete:

SectionEnd