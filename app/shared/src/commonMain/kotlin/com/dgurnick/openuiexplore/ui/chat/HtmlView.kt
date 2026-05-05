package com.dgurnick.openuiexplore.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders HTML content (TailwindCSS body markup) using a platform WebView. On Android this is
 * backed by android.webkit.WebView.
 */
@Composable expect fun HtmlView(html: String, isStreaming: Boolean, modifier: Modifier = Modifier)

/**
 * Strips OpenUI frontmatter (--- … ---) and returns the body HTML, or null if the content does not
 * look like HTML.
 */
fun extractHtml(content: String): String? {
  val body =
          if (content.trimStart().startsWith("---")) {
            val start = content.indexOf("---")
            val end = content.indexOf("---", start + 3)
            if (end == -1) content else content.substring(end + 3).trimStart()
          } else {
            content
          }
  val trimmed = body.trimStart()
  return if (trimmed.startsWith("<")) trimmed else null
}
