#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Kullanım: ./run-member.sh <member_id> <port>"
    echo "Örnek: ./run-member.sh 1 50052"
    exit 1
fi

MEMBER_ID=$1
PORT=$2

echo "Member-$MEMBER_ID başlatılıyor (Port: $PORT)..."
echo ""

mvn exec:java -Dexec.mainClass="com.hatokuse.Member" -Dexec.args="$MEMBER_ID $PORT"
