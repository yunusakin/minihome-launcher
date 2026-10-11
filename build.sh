#!/bin/bash
# Builds minihome.apk without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner
# Needs JDK 17 (d8 crashed on JDK 21 in our tests) and an Android SDK with build-tools 34 and platforms;android-34.
#   ANDROID_HOME=/path/to/sdk ./build.sh
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$PWD/sdk}; BT=$(ls -d $SDK/build-tools/* | tail -1); AJ=$(ls $SDK/platforms/*/android.jar | tail -1)
rm -rf build && mkdir -p build/classes build/dex
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk build/res.zip --manifest AndroidManifest.xml -I $AJ --min-sdk-version 21 --target-sdk-version 34
javac --release 8 -Xlint:-options -cp $AJ -d build/classes $(find src -name '*.java')
$BT/d8 --release --lib $AJ --min-api 21 --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q ../base.apk classes.dex)
$BT/zipalign -f 4 build/base.apk build/aligned.apk
# Debug key, generated on first build. Sign with your own key if you redistribute the APK.
[ -f build/debug.jks ] || keytool -genkeypair -keystore build/debug.jks -storepass android -keypass android -alias debug -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=minihome" >/dev/null 2>&1
$BT/apksigner sign --ks build/debug.jks --ks-pass pass:android --out minihome.apk build/aligned.apk
ls -l minihome.apk
