Protobuf Notes

//TODO 2017-01-17 sbuck Make this compile "magic" with Gradle

2017-01-17  Here is an example of what I used to succesfully compile the EEGProtos.proto file.  It failed when using ~ ahead of the paths for home so I switched to absolute paths

Stephens-MacBook:java stephenbuck$ protoc -I=/Users/stephenbuck/AndroidStudioProjects/SleepAnalyzer/src/Android/TestLibMuseAndroid/app/src/main/java/org/tssg/sa --proto_path=/Users/stephenbuck/AndroidStudioProjects/SleepAnalyzer/src/Android/TestLibMuseAndroid/app/src/main/java/org/tssg/sa --java_out=/Users/stephenbuck//AndroidStudioProjects/SleepAnalyzer/src/Android/TestLibMuseAndroid/app/src/main/java /Users/stephenbuck/AndroidStudioProjects/SleepAnalyzer/src/Android/TestLibMuseAndroid/app/src/main/java/org/tssg/sa/EEGProtos.proto