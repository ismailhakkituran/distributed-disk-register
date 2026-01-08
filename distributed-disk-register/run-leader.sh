#!/bin/bash

GRPC_PORT=${1:-50051}
CLIENT_PORT=${2:-8080}

echo "Lider başlatılıyor..."
echo "  gRPC Port: $GRPC_PORT"
echo "  İstemci Port: $CLIENT_PORT"
echo ""

mvn exec:java -Dexec.mainClass="com.hatokuse.Leader" -Dexec.args="$GRPC_PORT $CLIENT_PORT"
