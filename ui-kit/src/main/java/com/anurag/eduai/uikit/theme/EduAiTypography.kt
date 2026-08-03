package com.anurag.eduai.uikit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EduAiTypography =
    Typography(
        titleLarge =
            TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
            ),
        titleMedium =
            TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp,
            ),
        bodySmall =
            TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
            ),
        labelMedium =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp,
            ),
        labelSmall =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 14.sp,
            ),
    )
