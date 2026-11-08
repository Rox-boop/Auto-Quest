#!/usr/bin/env bash
# Builds and ADDS the Fabric 1.21.11 bridge (src/fabric/...) to an already-built
# out/elytra-ghost.jar. Run ./build.sh first.
#
# HONESTY NOTE, read this before trusting the output: the `java -jar tiny-remapper... `
# command below was written from tiny-remapper's publicly documented CLI usage
# (Main args: <input.jar> <output.jar> <mappings> <fromNamespace> <toNamespace>
# [classpath entries...]) but was NOT run in the session that wrote this file - running a
# downloaded third-party tool was blocked there. Everything FabricBridge.java calls was
# individually confirmed to exist with javap against the real obfuscated 1.21.11
# client.jar (see src/fabric/eg/bridge/fabric121_11/Mapping.java); what is genuinely
# unverified is only this specific command line and the resulting intermediary class names
# it should produce. If step 3 below fails, the error message itself will say exactly what
# is wrong (bad CLI args, wrong tiny version, ...) - send it over and that gets fixed for
# real, instead of guessing again.
set -euo pipefail
cd "$(dirname "$0")/.."

MC_VERSION="1.21.11"
CLIENT_JAR="libs/client-${MC_VERSION}.jar"
[ -f "$CLIENT_JAR" ] || { echo "run ./build.sh first (it fetches $CLIENT_JAR)" >&2; exit 1; }
[ -f out/classes/eg/bridge/BridgeLoader.class ] || { echo "run ./build.sh first" >&2; exit 1; }

mkdir -p libs/fabric

INTERMEDIARY_JAR="libs/fabric/intermediary-${MC_VERSION}.jar"
if [ ! -f "$INTERMEDIARY_JAR" ]; then
  echo "fetching Fabric's intermediary mappings for ${MC_VERSION}..."
  curl -sS -m 60 -o "$INTERMEDIARY_JAR" \
    "https://maven.fabricmc.net/net/fabricmc/intermediary/${MC_VERSION}/intermediary-${MC_VERSION}.jar"
fi

REMAPPER_JAR="libs/fabric/tiny-remapper.jar"
if [ ! -f "$REMAPPER_JAR" ]; then
  echo "fetching tiny-remapper..."
  curl -sS -m 60 -o "$REMAPPER_JAR" \
    "https://maven.fabricmc.net/net/fabricmc/tiny-remapper/0.15.0/tiny-remapper-0.15.0-fat.jar"
fi

rm -rf libs/fabric/extracted-mappings && mkdir -p libs/fabric/extracted-mappings
( cd libs/fabric/extracted-mappings && jar xf "../../../$INTERMEDIARY_JAR" mappings/mappings.tiny )
MAPPINGS_TINY="libs/fabric/extracted-mappings/mappings/mappings.tiny"

echo "step 3: remapping client.jar from official to intermediary names (unverified command, see header comment)..."
REMAPPED_JAR="libs/fabric/client-${MC_VERSION}-intermediary.jar"
DEPS_CP=""
for f in libs/deps/*.jar; do DEPS_CP="$DEPS_CP:$f"; done
java -jar "$REMAPPER_JAR" "$CLIENT_JAR" "$REMAPPED_JAR" "$MAPPINGS_TINY" official intermediary "${DEPS_CP#:}"

echo "step 4: compiling src/fabric against the remapped jar..."
rm -rf out/fabric-classes && mkdir -p out/fabric-classes
javac --release 21 -Xlint:all -cp "out/classes:$REMAPPED_JAR$DEPS_CP" -d out/fabric-classes \
  $(find src/fabric -name '*.java')

echo "step 5: adding the Fabric bridge classes to out/elytra-ghost.jar..."
jar uf out/elytra-ghost.jar -C out/fabric-classes .

echo "done. out/elytra-ghost.jar now carries both the vanilla and Fabric bridges;"
echo "BridgeLoader tries vanilla first, then Fabric, and picks whichever one's game"
echo "classes are actually present in the JVM it gets injected into."
