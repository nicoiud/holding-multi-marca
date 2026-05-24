#!/bin/bash
set -e

SRC=src
OUT=out

mkdir -p "$OUT"

find "$SRC" -name "*.java" > sources.txt

javac -d "$OUT" @sources.txt

rm sources.txt

echo "Compilación exitosa. Archivos en: $OUT/"
