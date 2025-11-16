package com.example.prob1.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import com.example.prob1.R

class BrowseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_browse, container, false)
        val webView = root.findViewById<WebView>(R.id.translateWebView)
        webView.settings.javaScriptEnabled = true


        webView.loadUrl("https://translate.yandex.ru/")
        return root
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}