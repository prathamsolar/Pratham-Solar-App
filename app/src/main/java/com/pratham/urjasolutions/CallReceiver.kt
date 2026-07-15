package com.pratham.urjasolutions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.CallLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallReceiver : BroadcastReceiver() {

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

                val number =
                    it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    )

                val duration =
                    it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    )

                val type =
                    it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    )


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
                json.put("name", "Unknown")
                json.put("mobile", number)
                json.put("type", type)
                json.put("duration", duration)


                sendToSheet(json.toString())
            }
        }
    }


    private fun sendToSheet(data:String){

        val url =
            URL("https://script.google.com/macros/s/AKfycbx7C7EdLEDFCvzFEfOtkGCwA67vg8tNMxIKaDRpLf89dLBKVyRirV0oyIgdY0pS2nyE/exec")


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
