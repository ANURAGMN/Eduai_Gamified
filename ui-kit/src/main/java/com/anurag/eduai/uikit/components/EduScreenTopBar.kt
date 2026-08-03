package com.anurag.eduai.uikit.components



import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.sp

import com.anurag.eduai.uikit.theme.EduAiTheme



/** Compact gamified screen header — matches Plan / Leagues tabs. */

@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun EduScreenTopBar(

    title: String,

    showBack: Boolean = false,

    onBack: () -> Unit = {},

    actionLabel: String? = null,

    onActionClick: () -> Unit = {},

) {

    val colors = EduAiTheme.colors

    TopAppBar(

        title = {

            Text(

                text = title,

                fontWeight = FontWeight.SemiBold,

                fontSize = 16.sp,

                color = colors.text,

            )

        },

        navigationIcon = {

            if (showBack) {

                IconButton(onClick = onBack) {

                    Icon(

                        Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription = "Back",

                        tint = colors.text,

                    )

                }

            }

        },

        actions = {

            if (actionLabel != null) {

                TextButton(onClick = onActionClick) {

                    Text(

                        text = actionLabel,

                        color = colors.accent,

                        fontSize = 13.sp,

                        fontWeight = FontWeight.SemiBold,

                    )

                }

            }

        },

        colors =

            TopAppBarDefaults.topAppBarColors(

                containerColor = colors.surface1,

                titleContentColor = colors.text,

                navigationIconContentColor = colors.text,

            ),

    )

}

