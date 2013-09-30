@ECHO OFF
REM Windows batch file for running ELKI with Lucene add-on.
REM You will need the following files:
REM elki.jar, elki-lucene.jar, Lucene3 jars (lucene3-core, lucene3-queries)
REM
REM You can put them into the same folder, or into a "lib" directory

REM Build classpath
SETLOCAL enabledelayedexpansion
SET CP=
FOR %%F IN ("*.jar" "lib\*.jar") DO (
  SET CP=!CP!;%%F%
)

REM Locate Java. Honor JAVA_HOME variable
IF DEFINED JAVA_HOME (
  SET JAVA="%JAVA_HOME%\bin\java.exe"
)
REM Try in windows path
IF "%JAVA%" == "" (
  FOR /f %%j IN ("java.exe") DO (
    SET JAVA=%%~dpn$PATH:j
  )
)
REM Guess typical installation directories for java 7.
IF "%JAVA%" == "" (
  FOR /d %%i IN ("%ProgramFiles%\Java\jdk7*" "%ProgramFiles%\Java\jre7*" "%ProgramFiles%(x86)\Java\jdk7*" "%ProgramFiles(x86)%\Java\jre7*") DO (
    SET JAVA=%%i\bin\java.exe
  )
)
REM Fail if still no java found
IF "%JAVA%" == "" (
  ECHO CANNOT LOCATE JAVA. Try setting JAVA_HOME, or modify the
  ECHO Launcher script with your path.
  PAUSE
  EXIT
)

REM Launch ELKI:
"%JAVA%" -Xmx1G ^
 -cp "%CP%" ^
 de.lmu.ifi.dbs.elki.application.ELKILauncher
