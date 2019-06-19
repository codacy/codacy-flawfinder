#!/usr/bin/env bash

set -e

TOOL_VERSION="$(cat .flawfinder-version)"
docker build -t "codacy-flawfinder-base:latest" -f Dockerfile.base . --build-arg toolVersion=$TOOL_VERSION