## LinkedInJobScraper.ky > Android UI Automator script to scrape jobs from LinkedIn

* Requires an Android device or emulator
* Requires installation of LinkedIn (and log yourself in) on above from:
https://m.apkpure.com/linkedin-jobs-business-news/com.linkedin.android
* Requires you to have done a previous search (so script can click on it)
* Requires running `java -jar adbserver-desktop.jar` from:
https://github.com/KasperskyLab/Kaspresso/blob/master/artifacts/adbserver-desktop.jar

Jobs will be saved to a file in `/sdcard/Download/jobs.txt`

The file format is:
`job_title|company|location|number_of_employees|language_of_text|full_job_description`

Kudos to [pemistahl](https://github.com/pemistahl/lingua) for Lingua that does the langauge detection.