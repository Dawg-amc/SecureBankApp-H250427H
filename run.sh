#!/bin/sh
# Compile and run (Linux/macOS). Requires JDK 17+.
mkdir -p out && javac -d out src/securebank/*.java && java -cp out securebank.Main data
