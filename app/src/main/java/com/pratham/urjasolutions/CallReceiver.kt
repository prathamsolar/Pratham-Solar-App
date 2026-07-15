package com.pratham.urjasolutions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {


    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == "android.intent.action.PHONE_STATE") {

            val state =
                intent.getStringExtra("state")


            if (state == "IDLE") {

                CoroutineScope(Dispatchers.IO).launch {

                    // Wait for Android CallLog update
                    delay(5000)

                    sendLatestCall(context)

                }
            }
        }
    }



    private fun sendLatestCall(
        context: Context
    ) {


        val prefs =
            context.getSharedPreferences(
                "PrathamSolar",
                Context.MODE_PRIVATE
            )


        val lastCallId =
            prefs.getString(
                "last_call_id",
                ""
            )



        val cursor =
            context.contentResolver.query(

                CallLog.Calls.CONTENT_URI,

                arrayOf(

                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE

                ),

                null,
                null,

                "${CallLog.Calls.DATE} DESC"

            )



        cursor?.use {


            if (it.moveToFirst()) {


                val id =
                    it.getString(0)



                // Stop duplicate entry
                if (id == lastCallId) {

                    return

                }



                val number =
                    it.getString(1)
                        ?: ""



                val typeValue =
                    it.getInt(2)



                val duration =
                    it.getString(3)
                        ?: "0"



                val callTime =
                    it.getLong(4)



                val name =
                    getContactName(
                        context,
                        number
                    )



                val callType =
                    when(typeValue) {

                        1 -> "Incoming"

                        2 -> "Outgoing"

                        3 -> "Missed"

                        5 -> "Rejected"

                        else -> "Unknown"

                    }



                val date =
                    SimpleDateFormat(
                        "dd-MM-yyyy",
                        Locale.getDefault()
                    )
                        .format(
                            Date(callTime)
                        )



                val time =
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    )
                        .format(
                            Date(callTime)
                        )



                val json =
                    JSONObject()



                json.put(
                    "date",
                    date
                )


                json.put(
                    "time",
                    time
                )


                json.put(
                    "name",
                    name
                )


                json.put(
                    "mobile",
                    cleanNumber(number)
                )


                json.put(
                    "type",
                    callType
                )


                json.put(
                    "duration",
                    duration
                )



                sendToSheet(
                    json.toString()
                )



                // Save processed call id
                prefs.edit()
                    .putString(
                        "last_call_id",
                        id
                    )
                    .apply()

            }

        }

    }





    private fun cleanNumber(
        number:String
    ):String {

        return number
            .replace("+91","")
            .replace(" ","")
            .replace("-","")
            .takeLast(10)

    }





    private fun getContactName(
        context: Context,
        phone:String
    ):String {


        val cursor =
            context.contentResolver.query(

                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,

                arrayOf(

                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER

                ),

                null,
                null,
                null

            )



        cursor?.use {


            while(it.moveToNext()) {


                val savedNumber =
                    cleanNumber(
                        it.getString(1)
                    )


                if (
                    savedNumber ==
                    cleanNumber(phone)
                ) {

                    return it.getString(0)

                }

            }

        }


        return "New Customer"

    }





    private fun sendToSheet(
        data:String
    ) {


        val url =
            URL(

                "https://script.google.com/macros/s/AKfycbx7C7EdLEDFCvzFEfOtkGCwA67vg8tNMxIKaDRpLf89dLBKVyRirV0oyIgdY0pS2nyE/exec"

            )



        val connection =
            url.openConnection()
                    as HttpURLConnection



        connection.requestMethod =
            "POST"



        connection.doOutput =
            true



        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )



        connection.outputStream.use {

            it.write(
                data.toByteArray()
            )

        }


        connection.responseCode

    }

}
