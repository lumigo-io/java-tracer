#!/usr/bin/env bash
set -eo pipefail

mvn -f agent/pom.xml clean package
mvn -Dmaven.test.skip=true clean install