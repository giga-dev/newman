set TEST_NAME=%1
rem set CP=%2
set CURRENT_DIR=%~dp0
java -cp %CURRENT_DIR%..\tests\QA\JSpacesTestSuite.jar;%CURRENT_DIR%..\tests\QA\lib\*.jar org.junit.runner.JUnitCore %TEST_NAME%