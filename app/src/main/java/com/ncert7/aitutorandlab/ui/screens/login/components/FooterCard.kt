package com.ncert7.aitutorandlab.ui.screens.login.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import com.ncert7.aitutorandlab.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary

@Composable
fun FooterCard() {
    val dimens = LocalDimensions.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundPrimary)
            .padding(dimens.spaceMedium),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FeatureItem(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.complete_ncert),
            subtitle = stringResource(R.string.language_number),
            modifier = Modifier.weight(1f)
        )

        FeatureItem(
            icon = Icons.Default.AutoAwesome,
            title = stringResource(R.string.ai_title),
            subtitle = stringResource(R.string.smart_learning),
            modifier = Modifier.weight(1f)
        )

        FeatureItem(
            icon = Icons.Default.Science,
            title = stringResource(R.string.login_footer_labs_title),
            subtitle = stringResource(R.string.login_footer_labs_subtitle),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimensions.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = dimens.spaceExtraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(dimens.iconLarge)
        )
        Spacer(modifier = Modifier.height(dimens.spaceSmall))
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}