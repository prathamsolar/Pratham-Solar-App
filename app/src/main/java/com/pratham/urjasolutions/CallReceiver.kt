package com.pratham.urjasolutions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastCallId = ""
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "android.intent.action.PHONE_STATE") {

            val state = intent.getStringExtra("state")

            if (state == "IDLE") {

                CoroutineScope(Dispatchers.IO).launch {
                    sendLastCall(context)
                }
            }
        }
    }


    private fun sendLastCall(context: Context) {

        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {

            if (it.moveToFirst()) {

                val id = it.getString(
                    it.getColumnIndexOrThrow(CallLog.Calls._ID)
                )

                // Duplicate protection
                if (id == lastCallId) return
                lastCallId = id


                val number = it.getString(
                    it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                )


                val duration = it.getString(
                    it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                )


                val typeRaw = it.getInt(
                    it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                )


                val name = getContactName(
                    context,
                    number
                )


                val callType = when(typeRaw) {

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
                    ).format(Date())


                val time =
                    SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    ).format(Date())


                val json = JSONObject()

                json.put("date", date)
                json.put("time", time)
                json.put("name", name)
                json.put("mobile", cleanNumber(number))
                json.put("type", callType)
                json.put("duration", duration)


                sendToSheet(json.toString())

            }
        }
    }


    private fun cleanNumber(number:String):String {

        return number
            .replace("+91","")
            .replace(" ","")
            .replace("-","")
    }


    private fun getContactName(
        context: Context,
        phone:String
    ):String {


        val resolver = context.contentResolver


        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI


        val cursor: Cursor? =
            resolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )


        cursor?.use {


            while(it.moveToNext()){


                val savedNumber =
                    it.getString(1)


                if(
                    cleanNumber(savedNumber)
                        .takeLast(10)
                    ==
                    cleanNumber(phone)
                        .takeLast(10)
                ){

                    return it.getString(0)

                }

            }
        }


        return "New Customer"
    }



    private fun sendToSheet(data:String){


        val url =
            URL(
                "https://script.google.com/macros/s/AKfycbx7C7EdLEDFCvzFEfOtkGCwA67vg8tNMxIKaDRpLf89dLBKVyRirV0oyIgdY0pS2nyE/exec"
            )


        val connection =
            url.openConnection() as HttpURLConnection


        connection.requestMethod="POST"

        connection.doOutput=true

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )


        connection.outputStream.use {

            it.write(data.toByteArray())

        }


        connection.responseCode

    }
}
