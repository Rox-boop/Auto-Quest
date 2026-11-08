#!/usr/bin/env bash
# Usage: ./build.sh [test]   Needs a JDK 21+ (javac, jar) and curl.
#
# Compiles two source roots (this script does NOT touch src/fabric - that is the Fabric
# loader bridge, built separately by tools/build-fabric-bridge.sh after this script, see
# that script's header for why):
#   src/main/java          - the version-independent client (named packages, eg.*)
#   src/main/java_unnamed  - the vanilla-launcher bridge. It references the real obfuscated
#                            game classes directly, so it must live in Java's unnamed
#                            package too (a named package cannot import an unnamed-package
#                            type). It compiles against Mojang's official, unmodified
#                            client.jar, downloaded straight from Mojang below and cached
#                            in libs/ - never committed to the repo (not ours to
#                            redistribute). The jar itself is never bundled into the
#                            output: only our own compiled bridge classes are, and they
#                            resolve against the real game classes at runtime once
#                            injected into Minecraft.
set -euo pipefail
cd "$(dirname "$0")"

# javac/java always want ';' between classpath entries on Windows, even inside Git Bash
# (it's the real Windows JVM binary parsing argv, not bash) - ':' everywhere else.
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) CP_SEP=";" ;;
  *) CP_SEP=":" ;;
esac

MC_VERSION="1.21.11"
MANIFEST_URL="https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
CLIENT_JAR="libs/client-${MC_VERSION}.jar"
# From Mojang's official version manifest for 1.21.11 - verified against the downloaded jar.
CLIENT_JAR_SHA1="ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd"
CLIENT_JAR_URL="https://piston-data.mojang.com/v1/objects/${CLIENT_JAR_SHA1}/client.jar"

mkdir -p libs libs/deps
if [ ! -f "$CLIENT_JAR" ] || [ "$(sha1sum "$CLIENT_JAR" | cut -d' ' -f1)" != "$CLIENT_JAR_SHA1" ]; then
  echo "fetching Mojang's official ${MC_VERSION} client.jar (compile-time only, not redistributed)..."
  curl -sS -m 300 -o "$CLIENT_JAR" "$CLIENT_JAR_URL"
  got="$(sha1sum "$CLIENT_JAR" | cut -d' ' -f1)"
  if [ "$got" != "$CLIENT_JAR_SHA1" ]; then
    echo "client.jar sha1 mismatch: got $got, expected $CLIENT_JAR_SHA1" >&2
    echo "Mojang may have reissued this version - check $MANIFEST_URL and update CLIENT_JAR_SHA1/URL." >&2
    exit 1
  fi
fi

# The client's own library dependencies (fastutil, datafixers, ...), needed only so javac can
# fully type-check code that references client.jar - never bundled into our output jar either.
# libs-manifest-1.21.11.csv (url,sha1 per line) comes straight from Mojang's version manifest.
DEPS_CP=""
while IFS=, read -r url sha1; do
  url="${url%$'\r'}"; sha1="${sha1%$'\r'}" # strip CR if this file was checked out with CRLF (e.g. on Windows)
  [ -z "$url" ] && continue
  f="libs/deps/$(basename "$url")"
  if [ ! -f "$f" ] || [ "$(sha1sum "$f" | cut -d' ' -f1)" != "$sha1" ]; then
    curl -sS -m 60 -o "$f" "$url"
    got="$(sha1sum "$f" | cut -d' ' -f1)"
    [ "$got" = "$sha1" ] || { echo "dependency sha1 mismatch for $url: got $got, expected $sha1" >&2; exit 1; }
  fi
  DEPS_CP="$DEPS_CP${CP_SEP}$f"
done < libs-manifest-1.21.11.csv

rm -rf out && mkdir -p out/classes out/test

javac --release 21 -Xlint:all -cp "${DEPS_CP#$CP_SEP}" -d out/classes $(find src/main/java -name '*.java')
javac --release 21 -Xlint:all -cp "out/classes${CP_SEP}${CLIENT_JAR}${DEPS_CP}" -d out/classes $(find src/main/java_unnamed -name '*.java')

printf 'Main-Class: eg.inject.Injector\nAgent-Class: eg.agent.Agent\nPremain-Class: eg.agent.Agent\nCan-Retransform-Classes: false\nCan-Redefine-Classes: false\n' > out/manifest.mf
jar cfm out/elytra-ghost.jar out/manifest.mf -C out/classes .
echo "built out/elytra-ghost.jar"

if [ "${1:-}" = "test" ]; then
  javac --release 21 -cp out/classes -d out/test $(find src/test/java -name '*.java')
  java -Djava.awt.headless=true -cp "out/classes${CP_SEP}out/test" eg.SelfTest
fi
