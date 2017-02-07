# SleepAnalyzer

## Get the source
Clone the repository and in Android Studio "Open an existing Android Studio project" and
browse to the location of SleepAnalyzer/src/Android/SleepAnalyzer. Open the project.

We are using gradle wrapper to build. If you are not running Android Gradle Plugin 2.2.3 and
Gradle Version 2.14.1 you may be prompted to upgrade.

## How to build
In order to build SleepAnalyzer you need to install the SDK from Interaxon.
We do not distribute the SDK for licensing reasons. 

The initial version of the code is based on the 6.0.0 SDK.

Go to  http://developer.choosemuse.com/android and follow directions to download
and install the 6.0.0 Android SDK.

The file should be called libmuse-android-6.0.0-windows-installer.exe

Once the SDK is installed you will need two library files:
1. libmuse-android.jar
2. libmuse\_android.so

Copy libmuse-android.jar to the SleepAnalyzer/app/libs folder

Copy libmuse\_android.so to the SleepAnalyzer/app/src/main/jniLibs/armeabi-v7a/ folder.

At this point you should be able to build the application.

Follow the directions at http://developer.choosemuse.com/android/getting-started-with-libmuse-android on how to pair the headset to you device.

