package com.example.mykapplication

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Test
import java.io.File

/**

    Android UI Automator script to send messages to your LinkedIn connections

    See ./LinkedInMessenger.md for more information

 */

class LinkedInMessenger : BaseTest() {
    private val packageName = "com.linkedin.android"
    private val id = "$packageName:id/"
    private val d = device.uiDevice
    private val recyclerBy = By.res("${id}search_results_list")

    @Test
    fun messageAuContacts() = run {
        var count = 1
        val listOfPeople = mutableSetOf<String>()
//        var char = 'a'

        outerLoop@ while(true) {
            try {
                Log.d("Lnkd", "Starting at the top --------")
                adbServer.performShell("am force-stop $packageName") // kill app
                adbServer.performShell("monkey -p $packageName 1") // start app

//            - My Network
                val myNetworkBy = By.res("${id}tab_relationships")
                d.wait(Until.findObject(myNetworkBy), 15_000).click()

//            - Manage my network
                val manageMyNetworkBy =
                    By.res("${id}mynetwork_my_communitities_entry_point_container")
                d.wait(Until.findObject(manageMyNetworkBy), 15_000).click()

//            - Connections
                val connectionsBy = By.text("Connections")
                d.wait(Until.findObject(connectionsBy), 15_000).click()

//            - Search
                val connectionSearchBy =
                    By.res("${id}entity_list_search_filter_result_header_search_button")
                d.wait(Until.findObject(connectionSearchBy), 15_000).click()

//            - Locations
                val locationsBy = By.descContains("Filter by Locations")
                d.wait(Until.findObject(locationsBy), 15_000).click()

//            - Australia
                val bottomSheetLocationsBy =
                    By.res("${id}search_filters_bottom_sheet_chip_container")
                val australiaBy = By.text("Australia")
                d.wait(Until.findObject(bottomSheetLocationsBy), 15_000)
                    .findObject(australiaBy).click()

//            - Show results
                val showResultsBy = By.res("${id}search_filters_bottom_sheet_show_result_button")
                d.findObject(showResultsBy).click()

//            - Search (Optional filter)
//            d.wait(Until.findObject(By.res("${id}search_bar_text_view")), 15_000).click()
//            d.wait(Until.findObject(By.res("${id}search_bar_edit_text")), 15_000).text = "qa $char"
//            d.pressEnter()

//            - Sort by name (Optional)
//            sortByName()

                loop@ while (true) {
                    val viewGroupsParentBy = By.res("${id}search_cluster_expandable_list_view")
                    val children = d.findObjects(viewGroupsParentBy).flatMap { it.children }
                    for (childIndex in children.indices) {
                        val currentChild = children[childIndex]
                        val nameBy = By.res("${id}search_entity_result_title")
                        val personText = currentChild.findObject(nameBy)?.text?.trim()

                        val messageBtn = currentChild.findObject(By.descContains("Message"))
                        if (personText?.isNotBlank() == true && messageBtn != null && !listOfPeople.contains(personText)) {
                            listOfPeople.add(personText)
                            val context = InstrumentationRegistry.getInstrumentation().targetContext

                            // only click if person is in .txt and not already SENT
                            val messageText = shouldSend(context, personText)

                            // click Message (if present)
                            if (messageText != null) {
                                children[childIndex].findObject(By.descContains("Message"))
                                    ?.click()

                                // Paste message
                                val textFieldBy = By.res("${id}messaging_keyboard_text_input_container")
                                d.wait(Until.findObject(textFieldBy), 15_000).text = messageText

                                // Send
                                val sendButtonBy = By.res("${id}keyboard_send_button")
                                d.wait(Until.findObject(sendButtonBy), 15_000).click()

                                // Update .txt to add ,SENT
                                Log.d("Lnkd", "Calling for: |$personText|")
                                updateFile(context, personText)

                                val senderNameBy = By.textContains("Lnkd Youssef")
                                d.wait(Until.findObject(senderNameBy), 15_000)

                                repeat(2) {
                                    d.pressBack()
                                }

                                count++

                                if (count % 10 == 0)
                                    continue@outerLoop

                                // Alternative to above to test a smaller number of contacts
//                                if (count > 2)
//                                    break@outerLoop

                                continue@loop
                            }
                        } else {
                            // if we didn't msg, then consider scrolling (otherwise we'd be at the top)

                            // check if bottom hit (inner border sufficient)
                            val outerBoundaryBy = By.res("${id}search_cluster_expandable_list_view")
                            if (currentChild.visibleBounds.bottom == d.findObject(outerBoundaryBy).visibleBounds.bottom) {
                                // drag 2nd to last child to top
                                val secToLastChild = children.dropLast(1).last()
                                val startAndEndX = secToLastChild.visibleBounds.centerX()
                                val startY = secToLastChild.visibleBounds.centerY()
                                val topBar = d.findObject(By.res("${id}search_results_filters_list"))
                                val endY = topBar.visibleBounds.bottom + 150 // half cell height

                                val lastChild = d.findObjects(viewGroupsParentBy).flatMap { it.children }.last()

                                d.drag(startAndEndX, startY, startAndEndX, endY, 100)

                                // Start - hit end check
                                val newLastChild = d.findObjects(viewGroupsParentBy).flatMap { it.children }.last()

                                if (lastChild == newLastChild) {
                                    // Optional addition if searching to add a single char to the search
//                                    if (char == 'z')
                                        break@outerLoop
//                                    else {
//                                        char++
//                                        continue@outerLoop
//                                    }
                                }
                                // End - hit end check

                                continue@loop
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("Lnkd", "EXCEPTION!!!!!! ${e.message}")
                continue@outerLoop
            }
        }
    }

    private fun sortByName() {
        // Sort
        val sortBy = By.res("${id}entity_list_search_filter_result_header_filters_button")
        d.wait(Until.findObject(sortBy), 15_000).click()

        // First name
        val firstNameBy = By.text("Last name")
        d.wait(Until.findObject(firstNameBy), 15_000).click()

        val showResultsBy = By.res("${id}search_filters_bottom_sheet_show_result_button")
        d.wait(Until.findObject(showResultsBy), 15_000).click()

        // Wait for results count
        d.wait(Until.findObject(recyclerBy), 15_000)
    }

    /*
        Update data2.txt with "|SENT" for line where name == string[1]
     */
    private fun updateFile(appContext: Context, name: String) {
        // Get the public downloads directory
        val filePath = "${appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/data2.txt"

        // Read the file
        val lines = mutableListOf<List<String>>()

        try {
            val file = File(filePath)
            file.forEachLine { line ->
                // Split each line by comma and add it to the list
                lines.add(line.split("|").map { it.trim() })
            }
        } catch (e: Exception) {
            Log.e("File Reading Error", e.toString())
        }

        // Write the file
        try {
            val fileWriter = File(filePath).bufferedWriter()
            fileWriter.use { out ->
                lines.forEach { line ->
                    if (line.isNotEmpty() && line[0] == name) {
                        // Append ",SENT" to the end of the line if line[1] is equal to 'name'
                        out.write(line.joinToString("|") + "|SENT\n")
                    } else {
                        // If not, just write the line without changes
                        out.write(line.joinToString("|") + "\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("File Writing Error", e.toString())
        }
    }

    private fun shouldSend(appContext: Context, name: String): String? {
        val filePath = "${appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/data2.txt"
        val file = File(filePath)
        if (!file.exists()) {
            println("File does not exist")
            return null
        }

        file.useLines { lines ->
            lines.forEach {
                val row = it.split("|")
                if (row.size > 2 && row[0].trim() == name && row[2].trim() == "SENT") {
                    return null
                }
                else if (row.size > 1 && row[0].trim() == name) {
                    return row[1].trim()
                }
            }
        }

        return null
    }
}