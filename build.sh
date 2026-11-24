#!/bin/bash
set -e
BUNGEE_JAR="${1:?Aufruf: ./build.sh /pfad/zu/BungeeCord.jar}"
JAVAC="${JAVAC:-javac}"
JAR="${JAR:-jar}"
cd "$(dirname "$0")"
rm -rf build && mkdir -p build/libs build/classes
unzip -q -o "$BUNGEE_JAR" 'META-INF/libraries/*.jar' -d build/extract
mv build/extract/META-INF/libraries/*.jar build/libs/
CP=$(ls build/libs/*.jar | tr '\n' ':')
find src -name '*.java' > build/sources.txt
$JAVAC -nowarn -encoding UTF-8 -cp "$CP" -d build/classes @build/sources.txt
cp resources/* build/classes/
$JAR --create --file PreNettyPasstrough.jar -C build/classes .
echo "Fertig: PreNettyPasstrough.jar  (in den plugins-Ordner von BungeeCord kopieren)"
