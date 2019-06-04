#!/usr/bin/env bash
set -eo pipefail

mvn -f agent/pom.xml clean install
mvn -Dmaven.test.skip=true clean install