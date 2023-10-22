#!/usr/bin/env bash

ROOT="$(cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd)"/..
java -jar "$ROOT"/out/artifacts/scsi_substitution_jar/scsi-substitution.jar "$@"