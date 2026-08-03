package com.ncert7.aitutorandlab.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

@Composable
fun PolicyConsentText(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    fontSizeSp: Float = 11f,
) {
    val context = LocalContext.current
    val termsUrl = stringResource(R.string.terms_of_service_url)
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val prefix = stringResource(R.string.policy_msg_prefix)
    val termsLabel = stringResource(R.string.terms_of_service_label)
    val privacyLabel = stringResource(R.string.privacy_policy_label)
    val suffix = stringResource(R.string.policy_msg_suffix)

    val annotated =
        buildAnnotatedString {
            append(prefix)
            pushStringAnnotation(tag = "url", annotation = termsUrl)
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(termsLabel)
            }
            pop()
            append(suffix)
            pushStringAnnotation(tag = "url", annotation = privacyUrl)
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(privacyLabel)
            }
            pop()
        }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style =
            androidx.compose.ui.text.TextStyle(
                color = TextSecondary,
                fontSize = fontSizeSp.sp,
                lineHeight = 16.sp,
            ),
        onClick = { offset ->
            annotated.getStringAnnotations("url", offset, offset).firstOrNull()?.let { span ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(span.item)))
            }
        },
    )
}
