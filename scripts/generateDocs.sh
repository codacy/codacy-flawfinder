#!/usr/bin/env bash

set -e

current_directory="$( cd "$( dirname "$0" )" && pwd )"

OUTPUT_FILE="$(mktemp)"

${current_directory}/publish-base.sh
docker run -i codacy/codacy-flawfinder-base:latest -QD --listrules > $OUTPUT_FILE
sbt "run-main codacy.flawfinder.DocGenerator $OUTPUT_FILE"
