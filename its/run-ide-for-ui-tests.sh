#!/bin/bash
set -euo pipefail

metacity --sm-disable --replace &
sleep 10 # give metacity some time to start
./gradlew --stacktrace --info "$@" runIdeForUiTests &
