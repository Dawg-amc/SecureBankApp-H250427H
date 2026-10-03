@echo off
REM Compile and run (Windows). Requires JDK 17+.
if not exist out mkdir out
javac -d out src\securebank\*.java && java -cp out securebank.Main data
