package com.example.prob1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.prob1.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LectionWebViewActivity : AppCompatActivity() {
    private val db = Firebase.firestore
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lection_webview)

        val webView = findViewById<WebView>(R.id.webView)
        val url = intent.getStringExtra("url") ?: run { finish(); return }
        val lectionId = intent.getStringExtra("lectionId") ?: run { finish(); return }

        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        val lectionId = intent.getStringExtra("lectionId") ?: return super.onBackPressed()

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("lectionId", lectionId)
            putExtra("partId", intent.getStringExtra("partId"))
            putExtra("isManual", intent.getBooleanExtra("isManual", false))
        })
        super.onBackPressed()
    }
}