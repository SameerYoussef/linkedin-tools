## LinkedInMessenger.kt > Android UI Automator script to send messages to your LinkedIn connections

* Requires an Android device or emulator
* Requires installation of LinkedIn (and log yourself in) on above from:
https://m.apkpure.com/linkedin-jobs-business-news/com.linkedin.android
* Requires running `java -jar adbserver-desktop.jar` from:
https://github.com/KasperskyLab/Kaspresso/blob/master/artifacts/adbserver-desktop.jar

Place a file in `/storage/emulated/0/Android/data/com.example.mykapplication/files/Download/data2.txt`

You must also make the file writeable:
`adb push data2.txt /storage/emulated/0/Android/data/com.example.mykapplication/files/Download/data2.txt`

`adb shell chmod a+rw /storage/emulated/0/Android/data/com.example.mykapplication/files/Download/data2.txt`

File format:
`name|message`

`name` should be the exact full name of the contact

After sending the message the file will be updated to:
`name|message|SENT`

..so the script can be stopped and restarted without sending any messages as duplicates.