package com.pratham.urjasolutions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class CallReceiver : BroadcastReceiver() {


    companion object {

        @Volatile
        private var isProcessing = false

    }



    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == "android.intent.action.PHONE_STATE") {


            val state =
                intent.getStringExtra("state")


            if (state == "IDLE") {


                if (isProcessing) return


                CoroutineScope(Dispatchers.IO).launch {


                    delay(4000)


                    processCall(context)


                }

            }

        }

    }





    private fun processCall(
        context: Context
    ) {


        if (isProcessing) return


        isProcessing = true



        try {


            val prefs =
                context.getSharedPreferences(
                    "PrathamSolar",
                    Context.MODE_PRIVATE
                )



            val lastId =
                prefs.getString(
                    "last_id",
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


                if(it.moveToFirst()) {


                    val id =
                        it.getString(0)



                    if(id == lastId) {

                        return

                    }



                    val number =
                        it.getString(1) ?: ""



                    val type =
                        it.getInt(2)



                    val duration =
                        it.getString(3) ?: "0"



                    val dateTime =
                        it.getLong(4)



                    val mobile =
                        cleanNumber(number)



                    val customerName =
                        getName(
                            context,
                            mobile
                        )



                    val callType =
                        when(type) {

                            1 -> "Incoming"

                            2 -> "Outgoing"

                            3 -> "Missed"

                            else -> "Unknown"

                        }



                    val remarks =
                        when(callType) {

                            "Missed" ->
                                "Auto Greeting Pending"

                            "Incoming" ->
                                "Customer Talked"

                            "Outgoing" ->
                                "Follow Up Call"

                            else ->
                                ""

                        }



                    val whatsappNumber =
                        "+91$mobile"



                    val json =
                        JSONObject()



                    json.put(
                        "date",
                        SimpleDateFormat(
                            "dd-MM-yyyy",
                            Locale.getDefault()
                        ).format(Date(dateTime))
                    )



                    json.put(
                        "time",
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                        ).format(Date(dateTime))
                    )



                    json.put(
                        "name",
                        customerName
                    )



                    json.put(
                        "mobile",
                        mobile
                    )



                    json.put(
                        "whatsappNumber",
                        whatsappNumber
                    )



                    json.put(
                        "type",
                        callType
                    )



                    json.put(
                        "duration",
                        duration
                    )



                    json.put(
                        "remarks",
                        remarks
                    )

                    sendToSheet(
                        json.toString()
                    )


                    prefs.edit()
                        .putString(
                            "last_id",
                            id
                        )
                        .apply()


                }

            }


        } finally {


            isProcessing = false


        }

    }





    private fun cleanNumber(
        number: String
    ): String {


        return number
            .replace("+91", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .takeLast(10)

    }





    private fun getName(
        context: Context,
        number: String
    ): String {


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


                if(savedNumber == number) {


                    return it.getString(0)


                }


            }


        }


        return "New Lead"

    }





    private fun sendToSheet(
        data: String
    ) {


        try {


            val url =
                URL(
                    "https://script.google.com/macros/s/AKfycbx7C7EdLEDFCvzFEfOtkGCwA67vg8tNMxIKaDRpLf89dLBKVyRirV0oyIgdY0pS2nyE/exec"
                )



            val con =
                url.openConnection()
                        as HttpURLConnection



            con.requestMethod =
                "POST"



            con.doOutput =
                true



            con.setRequestProperty(
                "Content-Type",
                "application/json"
            )



            con.outputStream.use {


                it.write(
                    data.toByteArray()
                )


            }



            con.responseCode



        }
        catch(e: Exception) {


            e.printStackTrace()


        }

    }


}
