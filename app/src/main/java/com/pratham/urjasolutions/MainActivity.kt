package com.pratham.urjasolutions

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.TextView

class MainActivity : ComponentActivity() {

    private val permissions =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this)
        text.text = """
            Pratham Solar App
            
            Status:
            🟢 Call Logger Ready
            
            Business:
            Pratham Urja Solutions
        """.trimIndent()

        text.textSize = 20f

        setContentView(text)

        permissions.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            )
        )
    }
}
