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

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == "android.intent.action.PHONE_STATE") {

            val state = intent.getStringExtra("state")

            if (state == "IDLE") {

                CoroutineScope(Dispatchers.IO).launch {

                    delay(7000)

                    readLastCall(context)

                }
            }
        }
    }


    private fun readLastCall(context: Context) {

        val prefs =
            context.getSharedPreferences(
                "PrathamSolar",
                Context.MODE_PRIVATE
            )


        val lastTime =
            prefs.getLong(
                "last_time",
                0
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


                val number =
                    it.getString(1)
                        ?: ""


                val type =
                    it.getInt(2)


                val duration =
                    it.getString(3)
                        ?: "0"


                val callDate =
                    it.getLong(4)



                if(callDate <= lastTime)
                    return



                val name =
                    getName(
                        context,
                        number
                    )


                val callType =
                    when(type){

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
                        .format(Date(callDate))


                val time =
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    )
                        .format(Date(callDate))



                val json =
                    JSONObject()


                json.put("date",date)

                json.put("time",time)

                json.put("name",name)

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



                send(json.toString())



                prefs.edit()
                    .putLong(
                        "last_time",
                        callDate
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



    private fun getName(
        context: Context,
        number:String
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

                if(
                    cleanNumber(it.getString(1))
                    ==
                    cleanNumber(number)
                ){

                    return it.getString(0)

                }
            }
        }


        return "New Customer"
    }



    private fun send(data:String) {


        val url =
            URL(
                "https://script.google.com/macros/s/AKfycbx7C7EdLEDFCvzFEfOtkGCwA67vg8tNMxIKaDRpLf89dLBKVyRirV0oyIgdY0pS2nyE/exec"
            )


        val con =
            url.openConnection()
                    as HttpURLConnection


        con.requestMethod="POST"

        con.doOutput=true

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
}
