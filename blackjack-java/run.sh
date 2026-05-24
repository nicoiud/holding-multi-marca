#!/bin/bash
set -e

if [ ! -d "out" ]; then
    echo "Compilando primero..."
    bash compile.sh
fi

java -cp out blackjack.Main
