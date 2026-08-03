package com.ncert7.aitutorandlab.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightClass
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.debug.OnboardingDebugHelper
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.screens.login.components.FooterCard
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.BackgroundSecondary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.ColorError
import com.ncert7.aitutorandlab.ui.theme.ColorHint
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.UserSaveState
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.UserViewModel

@Composable
fun UserDetailEntryScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val dimens = LocalDimensions.current
    val context = LocalContext.current


    // Analytics Tracking
    TrackScreenEvent(screenName = ScreenName.USER_DETAIL_ENTRY)

    // Debug logging to verify user data is available
    val currentUser by userViewModel.user.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        DebugLogger.debugLog("UserDetailEntryScreen", "User ID: ${currentUser.id}")
        DebugLogger.debugLog("UserDetailEntryScreen", "User Email: ${currentUser.email}")
        DebugLogger.debugLog("UserDetailEntryScreen", "User Name: ${currentUser.displayName}")
    }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedClass by remember { mutableIntStateOf(7) } // default Class 7
    var schoolName by remember { mutableStateOf("") }

    var phoneError by remember { mutableStateOf<String?>(null) }
    var schoolError by remember { mutableStateOf<String?>(null) }

    val classOptions = (1..10).map { stringResource(R.string.class_format, it) }

    val userSaveState by userViewModel.userSaveState.collectAsStateWithLifecycle()
    var isSaving by remember { mutableStateOf(false) }

    // Handle user save state changes
    LaunchedEffect(userSaveState) {
        when (val state = userSaveState) {
            is UserSaveState.Success -> {
                isSaving = false
                DebugLogger.debugLog("UserDetailEntryScreen", "User saved successfully")
                OnboardingDebugHelper.prepareOnboardingReplayAfterSignIn(context)
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is UserSaveState.Error -> {
                isSaving = false
                DebugLogger.debugLog("UserDetailEntryScreen", "Error saving user: ${state.exception.message}")
            }
            is UserSaveState.Saving -> {
                isSaving = true
            }
            is UserSaveState.Idle -> {
                isSaving = false
            }
        }
    }

    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .background(BackgroundSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundSecondary)
                .padding(dimens.spaceMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = stringResource(R.string.app_logo_desc),
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(dimens.cornerRadiusMedium)),
                    contentScale = ContentScale.Fit
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimens.spaceMedium),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = dimens.spaceExtraSmall)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundPrimary)
                        .padding(dimens.spaceSmall + dimens.spaceExtraSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.padding(dimens.spaceSmall + dimens.spaceExtraSmall))

                    Text(
                        text = stringResource(R.string.lets_go_to_know_you_message),
                        color = TextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.padding(dimens.spaceSmall + dimens.spaceExtraSmall))

                    /**
                     * TextField to entry Full Name
                     */
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(stringResource(R.string.full_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.full_name_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.person_icon_desc)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = ColorHint,
                            focusedLabelColor = BrandPrimary,
                            focusedLeadingIconColor = BrandPrimary,
                            unfocusedLeadingIconColor = ColorHint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.padding(dimens.spaceSmall))

                    /**
                     * TextField to entry phone number
                     * On change it will update the mutable variable phoneNumber
                     */
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            // Dynamic validation logic
                            phoneError = when {
                                phoneNumber.isBlank() -> "Phone number can not be empty"
                                !phoneNumber.matches(Regex("^[0-9]{10}$")) -> "Phone number must be exactly 10 digits"
                                else -> null
                            }
                        },
                        label = { Text(stringResource(R.string.phone_number_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.phone_number_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = stringResource(R.string.phone_icon_desc)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = phoneError != null,
                        supportingText = {
                            if (phoneError != null) {
                                Text(
                                    text = phoneError!!,
                                    color = ColorError,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = ColorHint,
                            focusedLabelColor = BrandPrimary,
                            focusedLeadingIconColor = BrandPrimary,
                            unfocusedLeadingIconColor = ColorHint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorTextColor = ColorError
                        )
                    )

                    Spacer(modifier = Modifier.padding(dimens.spaceSmall))

                    /**
                     * Dropdown to select a class
                     */
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cornerRadiusRound),
                        border = BorderStroke(dimens.inputBorderWidth, ColorHint),
                        colors = CardDefaults.cardColors(containerColor = White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundPrimary),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightClass,
                                contentDescription = null,
                                tint = ColorHint,
                                modifier = Modifier
                                    .padding(start = dimens.spaceSmall + dimens.spaceExtraSmall, dimens.screenPadding - dimens.spaceExtraSmall)
                                    .alignByBaseline()
                            )
                            DropDownMenu(
                                label = stringResource(R.string.class_selection),
                                options = classOptions,
                                selectedValue = "Class $selectedClass",
                                onValueSelected = { selectedString ->
                                    selectedClass = selectedString.removePrefix("Class ").trim().toInt()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(dimens.spaceSmall))

                    /**
                     * TextField to entry school name
                     * On change it will update the mutable variable schoolName
                     */
                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = {
                            schoolName = it
                            // Dynamic validation logic
                            schoolError = when {
                                schoolName.isBlank() -> "School name can not be empty"
                                schoolName.length < 3 -> "School name must be at least 3 characters"
                                !schoolName.matches(Regex("^[a-zA-Z0-9 .,'-]{3,}$")) -> "School name should only contain alphabet"
                                else -> null
                            }
                        },
                        label = { Text(stringResource(R.string.school_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.school_name_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = stringResource(R.string.school_icon_desc)
                            )
                        },
                        isError = schoolError != null,
                        supportingText = {
                            if (schoolError != null) {
                                Text(
                                    text = schoolError!!,
                                    color = ColorError,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = ColorHint,
                            focusedLabelColor = BrandPrimary,
                            focusedLeadingIconColor = BrandPrimary,
                            unfocusedLeadingIconColor = ColorHint,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorTextColor = ColorError
                        )
                    )

                    Spacer(modifier = Modifier.padding(dimens.screenPadding - dimens.spaceExtraSmall))

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(dimens.spaceSmall),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.buttonHeight)
                            .padding(horizontal = dimens.spaceLarge - dimens.spaceExtraSmall),
                        enabled = !isSaving && phoneError == null && schoolError == null &&
                                phoneNumber.isNotBlank() && schoolName.isNotBlank(),
                        onClick = {
                            DebugLogger.debugLog("UserDetailEntryScreen", "Get Started Button Clicked")
                            FunnelAnalyticsTracker.track(FunnelStep.PROFILE_SUBMIT)

                            // Update user data in ViewModel
                            userViewModel.updateName(fullName)
                            userViewModel.updateSchool(schoolName)
                            userViewModel.updateClass(selectedClass)
                            userViewModel.updatePhoneNumber(phoneNumber)
                            userViewModel.updateUpdatedAt(System.currentTimeMillis())
                            userViewModel.updateCreatedAt(System.currentTimeMillis())

                            // Submit new user - all logic handled in ViewModel
                            userViewModel.submitNewUser(context)
                        },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(dimens.iconLarge),
                                    color = White,
                                    strokeWidth = dimens.inputBorderWidth
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.get_started),
                                    color = White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(dimens.spaceSmall))
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            Card(
                elevation = CardDefaults.cardElevation(dimens.cardElevation + dimens.cardElevation),
                shape = RoundedCornerShape(dimens.spaceMedium)
            ) {
                FooterCard()
            }

            Spacer(modifier = Modifier.height(dimens.spaceMedium))
        }
    }
}