#!/usr/bin/env bash
set -eo pipefail

mvn -Dmaven.test.skip=true clean install