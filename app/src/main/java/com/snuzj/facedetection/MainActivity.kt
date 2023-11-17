package com.snuzj.facedetection

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.barcode.common.Barcode
import com.snuzj.facedetection.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermission = Manifest.permission.CAMERA

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted->
        if (isGranted){
            startScanner()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.QRscanner.setOnClickListener {
            requestCameraAndStartScanner()
        }

        binding.qrContentTv.setOnLongClickListener {
            copyTextToClipboard(binding.qrContentTv.text.toString())
            Toast.makeText(this,"Copied Text",Toast.LENGTH_SHORT).show()
            true // Indicate that we've handled the long-click
        }

    }

    // Function to copy text to the clipboard
    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
        // Optionally, you can show a toast or a message indicating the text has been copied
    }

    private fun requestCameraAndStartScanner(){
        if (isPermissionGranted(cameraPermission)){
            startScanner()
        } else{
            requestCameraPermission()
        }
    }

    private fun startScanner() {
        ScannerActivity.startScanner(this) { barcodes ->
            barcodes.forEach { barcode ->
                val rawValue = barcode.rawValue

                when (barcode.valueType) {
                    Barcode.TYPE_URL -> {
                        binding.qrTypeTv.text = "URL"

                        binding.qrContentTv.text = "$rawValue"
                    }
                    Barcode.TYPE_CONTACT_INFO -> {

                        binding.qrTypeTv.text = "Contact Info"

                        val typeContactInfo = barcode.contactInfo
                        val name = "${typeContactInfo?.name?.formattedName}"
                        val organization = "${typeContactInfo?.organization}"
                        val title = "${typeContactInfo?.title}"

                        var phones = ""
                        typeContactInfo?.phones?.forEach { phone ->
                            phones += "\n${phone.number}"
                        }

                        var emails = ""
                        typeContactInfo?.emails?.forEach { email ->
                            emails += "\n${email.address}"
                        }

                        binding.qrContentTv.text = "TYPE_CONTACT_INFO \nname: $name \norganization: $organization \ntitle: $title \nphones: $phones \nemails: $emails \n\n$rawValue"
                    }
                    Barcode.TYPE_EMAIL->{
                        val typeEmail = barcode.email
                        val address = "${typeEmail?.address}"
                        val body = "${typeEmail?.body}"
                        val subject = "${typeEmail?.subject}"

                        binding.qrTypeTv.text = "email"
                        binding.qrContentTv.text = "\naddress: $address \nbody: $body \nsubject: $subject \n\n$rawValue"
                    }
                    Barcode.TYPE_WIFI->{
                        val typeWifi = barcode.wifi
                        val ssid = "${typeWifi?.ssid}"
                        val password = "${typeWifi?.password}"
                        var encryptionType = "${typeWifi?.encryptionType}"

                        when (encryptionType) {
                            "1" -> encryptionType = "OPEN"
                            "2" -> encryptionType = "WPA"
                            "3" -> encryptionType = "WEP"
                        }
                        binding.qrTypeTv.text = "Wifi"
                        binding.qrContentTv.text = "\nssid: $ssid \npassword: $password \nencryptionType: $encryptionType \n \n$rawValue"
                    }
                    else -> {
                        binding.qrTypeTv.text = "Other Type"
                        binding.qrContentTv.text = barcode.rawValue.toString()
                    }

                }
            }
        }
    }


    private fun requestCameraPermission() {
        when{
            shouldShowRequestPermissionRationale(cameraPermission) ->{
                cameraPermissionRequest {
                    openPermissionSetting()
                }
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
}