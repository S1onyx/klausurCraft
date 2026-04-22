#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

WITH_COVERAGE="${1:-}"

echo "[1/3] Running tests..."
mvn -q test

echo "[2/3] Generating Javadoc HTML..."
mvn -q -Dmaven.repo.local=/tmp/m2 -Dmaven.compiler.release=21 javadoc:javadoc

echo "Javadoc index: $ROOT_DIR/target/reports/apidocs/index.html"

if [[ "$WITH_COVERAGE" == "--coverage" ]]; then
  echo "[3/3] Generating JaCoCo report..."
  mvn -q -Dmaven.repo.local=/tmp/m2 -Dmaven.compiler.release=21 clean \
    org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent \
    test \
    org.jacoco:jacoco-maven-plugin:0.8.12:report
  echo "Coverage index: $ROOT_DIR/target/site/jacoco/index.html"
else
  echo "[3/3] Skipped coverage (use --coverage to include it)."
fi

echo "Live-demo checks finished."
