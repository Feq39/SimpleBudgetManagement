#!/usr/bin/env bash

set -e

if [ -z "$1" ]; then
  echo "Usage: ./run.sh <port>"
  exit 1
fi

PORT="$1"

./mvnw clean package

export APP_PORT="$PORT"

docker compose up --build