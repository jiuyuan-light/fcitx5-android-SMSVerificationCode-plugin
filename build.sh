export ANDROID_HOME='/c/Users/lxPCu2/AppData/Local/Android/Sdk'
export JAVA_HOME='/c/Program Files/Android/Android Studio/jbr'
export PLUGIN_VERSION='0.1.2'
export SIGNING_STORE_FILE='D:/code/worker/tmp/Fcitx5-Android-SMS-Plugin.p12'
export SIGNING_STORE_PASSWORD='FcitxX-X-SMS-X2'
export SIGNING_KEY_ALIAS='fcitx5-android-sms-plugin'
export SIGNING_KEY_PASSWORD='FcitxX-X-SMS-X2'

./gradlew --stop
./gradlew clean assembleRelease -x lint
