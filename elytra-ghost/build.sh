#!/usr/bin/env bash
# Usage: ./build.sh [test]   Needs a JDK 21+ (javac, jar).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf out && mkdir -p out/classes out/test
javac --release 21 -Xlint:all -d out/classes $(find src/main/java -name '*.java')
printf 'Main-Class: eg.inject.Injector\nAgent-Class: eg.agent.Agent\nPremain-Class: eg.agent.Agent\nCan-Retransform-Classes: false\nCan-Redefine-Classes: false\n' > out/manifest.mf
jar cfm out/elytra-ghost.jar out/manifest.mf -C out/classes .
echo "built out/elytra-ghost.jar"
if [ "${1:-}" = "test" ]; then
  javac --release 21 -cp out/classes -d out/test $(find src/test/java -name '*.java')
  java -Djava.awt.headless=true -cp out/classes:out/test eg.SelfTest
fi
