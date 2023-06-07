package com.example.mykapplication

import android.os.Environment
import android.util.Log
import androidx.test.uiautomator.*

import org.junit.Test

import java.io.File
import java.io.FileOutputStream

import com.github.pemistahl.lingua.api.*
import com.github.pemistahl.lingua.api.Language.*

/**

    Android UI Automator script to scrape jobs from LinkedIn

    * Requires an Android device or emulator
    * Requires installation of LinkedIn (and log yourself in) on above from:
    https://m.apkpure.com/linkedin-jobs-business-news/com.linkedin.android
     * Requires you to have a previous search performed
    * Requires running `java -jar adbserver-desktop.jar` from:
    https://github.com/KasperskyLab/Kaspresso/blob/master/artifacts/adbserver-desktop.jar

    Jobs will be saved to a file in /sdcard/Download/jobs.txt

    The file format is:
    job_title|company|location|number_of_employees|language_of_text|full_job_description

 */
class LinkedInJobScraper : BaseTest() {
    private val packageName = "com.linkedin.android"
    private val id = "$packageName:id/"
    private val d = device.uiDevice
    private var count = 1
    private val jobs = mutableSetOf<String>()
    private val detector: LanguageDetector =
        LanguageDetectorBuilder.fromLanguages(ENGLISH, FRENCH, GERMAN, ITALIAN).build()

    @Test
    fun findLinkedInJobs() {
        val jobsDesc = mutableSetOf<String>()
        val startFromCount = 0

        outerLoop@ while (true) {
            try {
                adbServer.performShell("am force-stop $packageName") // Kill app
                adbServer.performShell("monkey -p $packageName 1") // Launch app

                val tabJobsBy = By.res("${id}tab_jobs")
                d.wait(Until.findObject(tabJobsBy), 5_000).click()

                val jobSearchHistoryBy = By.res("${id}job_search_history_container")
                d.wait(Until.findObject(jobSearchHistoryBy), 5_000).click()

                val jobFilterSelectedBy = By.descContains("Jobs Filter selected")
                d.wait(Until.findObject(jobFilterSelectedBy), 15_000)

                val recycler = d.findObject(By.res("${id}careers_job_list_fragment_recycler_view"))

                for (child in recycler.children.drop(1).dropLast(1)) {
                    val jobDesc = cleanContentDesc(child)

                    if (!jobsDesc.contains(jobDesc) && count > startFromCount) {
                        clickIntoJob(child)
                        jobsDesc.add(jobDesc)
                    }
                }

                performScroll(recycler)

                while (true) {
                    for (child in recycler.children.drop(1).dropLast(1)) {
                        val jobDesc = cleanContentDesc(child)

                        if (!jobsDesc.contains(jobDesc) && count <= startFromCount) {
                            jobsDesc.add(jobDesc)
                            count++
                        }
                        else if (!jobsDesc.contains(jobDesc) && count > startFromCount) {
                            clickIntoJob(child)
                            jobsDesc.add(jobDesc)
                            count++

                            if (jobsDesc.count() % 50 == 0) // restart app due to slowness
                                continue@outerLoop
                        }
                    }

                    // if job search query appears then we've hit the end - end early
                    val seeMoreJobBy = By.res("${id}job_search_query_expansion_parent")
                    if (recycler.children.last().findObject(seeMoreJobBy) != null)
                        break@outerLoop

                    val thirdToLastChild = cleanContentDesc(recycler.children.takeLast(3).first())
                    val secToLastChild = cleanContentDesc(recycler.children.takeLast(2).first())
                    val lastChild = cleanContentDesc(recycler.children.last())

                    val secondLastChild = recycler.children.takeLast(2).first()
                    val startPoint = secondLastChild.visibleCenter
                    val endX = d.displayWidth / 2

                    val topBar = d.findObject(By.res("${id}search_filters_list"))
                    val endY =
                        (topBar.visibleBounds.bottom + secondLastChild.visibleBounds.height() / 1.6).toInt()

                    d.drag(startPoint.x, startPoint.y, endX, endY, 100)
                    d.pressBack()

                    val newThirdToLastChild = cleanContentDesc(recycler.children.takeLast(3).first())
                    val newSecToLastChild = cleanContentDesc(recycler.children.takeLast(2).first())
                    val newLastChild = cleanContentDesc(recycler.children.last())

                    // Last result hit (in theory)
                    if (lastChild == newLastChild
                        && secToLastChild == newSecToLastChild
                        && thirdToLastChild == newThirdToLastChild
                    ) break@outerLoop

                }
            }
            catch (e: java.lang.Exception) {
                Log.d("Lnkd", "EXCEPTION: ${e.message}")
                continue@outerLoop
            }
        }

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "jobs.txt")

        FileOutputStream(file).let {
            it.write(jobs.joinToString(separator = "\n").toByteArray())
            it.close()
        }
    }

    private fun cleanContentDesc(child: UiObject2) : String {
        val contDesc = child.findObject(By.res("${id}careers_job_item_root")).contentDescription

        return contDesc.substringBeforeLast("·")
            .substringBeforeLast(',')
            .replace(Regex("\\d+\\s\\w+\\s+ago.*$"), "")
            .replace(", Your profile matches this job", "")
            .replace(", Message the job poster directly", "")
            .replace(", Actively recruiting", "")
            .replace(", Promoted", "")
            .trim()
    }

    private fun performScroll(recycler: UiObject2) {
        val b = recycler.visibleBounds
        d.swipe(
            b.centerX(),
            (b.centerY() * 1.25).toInt(),
            b.centerX(),
            b.centerY(),
            10
        )
    }

    /**
     *
     * Click a job and record it's details to jobs var
     *
     */
    private fun clickIntoJob(child: UiObject2) {
        child.click()
        var jobTextDelimited = ""
        val jobRecyclerSelector =
            UiSelector().resourceId("${id}entities_recycler_view")
        val jobRecycler = UiScrollable(jobRecyclerSelector)
        val jobTitleSelector =
            UiSelector().resourceId("${id}entities_top_card_title")
        jobTextDelimited += jobRecycler.getChild(jobTitleSelector).text.replace(
            "|",
            "-",
        ) + "|"
        val companyNameSelector =
            UiSelector().resourceId("${id}careers_top_card_subtitle_1")
        jobTextDelimited += try {
            jobRecycler.getChild(companyNameSelector).text.replace(
                "|",
                "-"
            ) + "|"
        } catch (e: UiObjectNotFoundException) {
            "NO_COMPANY_MENTIONED|"
        }
        jobTextDelimited += jobRecycler.getChild(UiSelector().resourceId("${id}careers_top_card_subtitle_2")).text + "|"
        val companySizeSelector =
            UiSelector().resourceId("${id}careers_job_summary_card_item_title")
        jobTextDelimited += jobRecycler.getChildByInstance(
            companySizeSelector,
            1
        ).text + "|"
        val fullJobDescSelector =
            UiSelector().resourceId("${id}careers_paragraph_body_predash")
        val recyclerViewBounds = jobRecycler.bounds
        val startX = recyclerViewBounds.centerX()
        val startY = recyclerViewBounds.centerY()
        val endY = startY - (recyclerViewBounds.height() / 2)
        jobRecycler.dragTo(startX, endY, 50)

        try {
            val fullJobDesc = jobRecycler.getChild(fullJobDescSelector)
            fullJobDesc.click()
            val detectedLanguage: Language = detector.detectLanguageOf(text = fullJobDesc.text)
            jobTextDelimited += "$detectedLanguage|"
            jobTextDelimited += fullJobDesc.text
                .replace("|", "-")
                .replace("\n", "<br />")
        } catch (e: UiObjectNotFoundException) {
            jobTextDelimited += "CANT_READ_JD|CANT_READ_JD|"
        }

        d.pressBack()
        jobs.add(jobTextDelimited)
        Log.d("Lnkd", "$count $jobTextDelimited")
        count++
    }
}