package com.dgurnick.openuiexplore.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private class ScrollPassThroughWebView(context: Context) : WebView(context) {
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (event.actionMasked == MotionEvent.ACTION_MOVE) {
      parent?.requestDisallowInterceptTouchEvent(false)
    }
    return super.onTouchEvent(event)
  }
}

private fun buildPage(bodyHtml: String) =
        """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script src="https://cdn.tailwindcss.com"></script>
<style>
  html, body { margin: 0; padding: 8px; background: #ffffff; }
</style>
</head>
<body>
$bodyHtml
</body>
</html>
""".trimIndent()

private fun WebView.pollNativeHeight(density: Float, onDp: (Int) -> Unit) {
  val handler = android.os.Handler(android.os.Looper.getMainLooper())
  fun measure() {
    val px = (contentHeight * scale).toInt()
    if (px > 50) onDp((px / density).toInt().coerceAtLeast(200))
  }
  listOf(100L, 400L, 900L, 2000L, 4000L, 7000L).forEach { delay ->
    handler.postDelayed(::measure, delay)
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun HtmlView(html: String, isStreaming: Boolean, modifier: Modifier) {
  if (isStreaming) {
    Box(modifier = modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
    }
    return
  }

  val heightState = remember(html) { mutableIntStateOf(400) }
  val page = remember(html) { buildPage(html) }

  key(html) {
    AndroidView(
            factory = { context ->
              val density = context.resources.displayMetrics.density
              val onDp: (Int) -> Unit = { dp ->
                if (dp > heightState.intValue) heightState.intValue = dp
              }
              ScrollPassThroughWebView(context).apply {
                webViewClient =
                        object : WebViewClient() {
                          override fun shouldOverrideUrlLoading(
                                  view: WebView?,
                                  request: WebResourceRequest?
                          ) = false
                          override fun onPageFinished(view: WebView?, url: String?) {
                            view?.pollNativeHeight(density, onDp)
                          }
                        }
                webChromeClient =
                        object : WebChromeClient() {
                          override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 80) {
                              val px = ((view?.contentHeight ?: 0) * (view?.scale ?: 1f)).toInt()
                              if (px > 50) onDp((px / density).toInt().coerceAtLeast(200))
                            }
                          }
                        }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(false)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                loadDataWithBaseURL("https://localhost/", page, "text/html", "UTF-8", null)
              }
            },
            update = {},
            modifier = Modifier.fillMaxWidth().height(heightState.intValue.dp)
    )
  }
}
