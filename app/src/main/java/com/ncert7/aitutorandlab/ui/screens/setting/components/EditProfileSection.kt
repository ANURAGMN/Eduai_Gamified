package com.ncert7.aitutorandlab.ui.screens.setting.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.entities.StudentEntity
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.ui.components.DropDownMenu
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.ButtonBorderLight
import com.ncert7.aitutorandlab.ui.theme.ButtonContentDark
import com.ncert7.aitutorandlab.ui.theme.ColorError
import com.ncert7.aitutorandlab.ui.theme.ColorHint
import com.ncert7.aitutorandlab.ui.theme.ColorWarning
import com.ncert7.aitutorandlab.ui.theme.EditProfileBackground
import com.ncert7.aitutorandlab.ui.theme.HeaderGradientEnd
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.ProfileAvatarGradientEnd
import com.ncert7.aitutorandlab.ui.theme.ProfileAvatarGradientStart
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.SettingViewModel
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.UpdateProfileState
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EditProfileScreen(
    userId: String,
    student: StudentEntity?,
    userViewModel: SettingViewModel,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val scrollState = rememberScrollState()

    var userName by remember {
        mutableStateOf(
            student?.studentName.orEmpty()
        )
    }
    var classValue by remember {
        mutableIntStateOf(
            student?.classLevel
                ?: 7
        )
    }
    var school by remember {
        mutableStateOf(
            student?.studentSchool.orEmpty()
        )
    }
    // TODO: it should be Integer but due to refactoring purpose it is left this way
    // Country code is not taken for future we can add country code
    var phoneNumber by remember {
        mutableStateOf(
            student?.phoneNumber.orEmpty()
        )
    }
    var profilePhotoUri by remember {
        mutableStateOf(student?.profilePhotoUrl)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.toString()?.let { selected ->
            profilePhotoUri = selected
            userViewModel.updateProfilePhoto(selected)
        }
    }

    LaunchedEffect(student) {
        student?.let {
            userName = it.studentName
            classValue = it.classLevel
            school = it.studentSchool
            phoneNumber = it.phoneNumber
            profilePhotoUri = it.profilePhotoUrl
        }
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var schoolError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val classOptions = (1..10).map { "Class $it" }
    val updateState by userViewModel.updateState.collectAsState()

    LaunchedEffect(updateState) {
        when (updateState) {
            UpdateProfileState.Success -> {
                DebugLogger.debugLog("EditProfilePopUp", "Update success")
                saveError = null
                onClose()
                userViewModel.resetState()
            }
            is UpdateProfileState.Error -> {
                saveError = (updateState as UpdateProfileState.Error).message
                DebugLogger.errorLog("EditProfilePopUp", saveError ?: "Update failed")
                userViewModel.resetState()
            }
            else -> Unit
        }
    }
    Column(
        modifier =
            modifier.fillMaxWidth()
                .background(EditProfileBackground)
                .verticalScroll(scrollState)
                .padding(dimensions.spaceMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(dimensions.spaceMedium))
        // Profile Photo Section
        Box(
            modifier = Modifier
                .size(dimensions.boxSizeMedium)
                .clip(CircleShape)
                .background(
                    brush = Brush
                        .linearGradient(
                            colors =
                                listOf(
                                    ProfileAvatarGradientStart,
                                    ProfileAvatarGradientEnd
                                )
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoUri != null) {
                GlideImage(
                    model = profilePhotoUri,
                    contentDescription = stringResource(R.string.profile_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.profile_photo),
                    modifier = Modifier.size(dimensions.avatarSizeLarge),
                    tint = AccentBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensions.spaceMedium))
        OutlinedButton(
            onClick = { photoPickerLauncher.launch("image/*") },
            modifier = Modifier.height(dimensions.buttonHeightSmall),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = ButtonContentDark
                ),
            border = BorderStroke(dimensions.inputBorderWidth, ButtonBorderLight),
            shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
        ) {
            Text(
                text = stringResource(R.string.change_photo),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spaceLarge))
        // name field
        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                nameError = when {
                    // TODO: remove hard coded error string from validation
                    userName.isBlank() -> "Name can not be empty"
                    userName.length < 5 ->
                        "Full Name must be at least 5 characters"
                    !userName.matches(
                        Regex("^[a-zA-Z0-9 .,'-]{3,}$")
                    ) -> "Name should only contain alphabet"
                    else -> null
                } },
            isError = nameError != null,
            supportingText = {
                if (nameError != null) {
                    Text(
                        text = nameError!!,
                        color = ColorError,
                        style = MaterialTheme.typography.bodySmall
                    )
                } },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.enter_your_name)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = ColorHint,
                focusedLabelColor = BrandPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(dimensions.spaceMedium))
        //      Class Field
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = OutlinedTextFieldDefaults.shape,
            border = BorderStroke(
                width = dimensions.inputBorderWidth,
                color = ColorHint
            ),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(BackgroundPrimary),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DropDownMenu(
                    label = stringResource(R.string.class_selection),
                    options = classOptions,
                    selectedValue = "Class $classValue",
                    onValueSelected = { selectedString ->
                        classValue = selectedString
                            .removePrefix("Class ")
                            .trim()
                            .toInt()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(dimensions.spaceMedium))
        // School Field
        OutlinedTextField(
            value = school,
            onValueChange = {
                school = it
                // Dynamic validation logic
                schoolError = when {
                    school.isBlank() -> "School name can not be empty"
                    school.length < 3 ->
                        "School name must be at least 3 characters"
                    !school.matches(Regex("^[a-zA-Z0-9 .,'-]{3,}$")) ->
                        "School name should only contain alphabet"
                    else -> null
                } },
            isError = schoolError != null,
            supportingText = {
                if (schoolError != null) {
                    Text(
                        text = schoolError!!,
                        color = ColorError,
                        style = MaterialTheme.typography.bodySmall
                    )
                } },
            label = { Text(stringResource(R.string.school)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.enter_school_name)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = ColorHint,
                focusedLabelColor = BrandPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(dimensions.spaceMedium))

        // Phone Number Field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                // Dynamic validation logic
                phoneError = when {
                    phoneNumber.isBlank() ->
                        "Phone number cannot be empty"
                    phoneNumber.matches(Regex("^[0-5]")) ->
                        "Phone number should start from 6 to 9"
                    !phoneNumber.matches(
                        Regex("^[6-9]\\d{9}$")
                    ) -> "Enter a valid 10-digit number"
                    else -> null
                }},
            isError = phoneError != null,
            supportingText = {
                if (phoneError != null) {
                    Text(
                        text = phoneError!!,
                        color = ColorError,
                        style = MaterialTheme.typography.bodySmall
                    )
                } },
            label = { Text(stringResource(R.string.phone_number)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.enter_phone_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = ColorHint,
                focusedLabelColor = BrandPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(dimensions.spaceLarge))

        if (saveError != null) {
            Text(
                text = saveError!!,
                color = ColorError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(dimensions.spaceSmall))
        }

        Button(
            onClick = {
                saveError = null
                if (nameError != null || phoneError != null || schoolError != null) return@Button
                userViewModel.updateProfile(
                    updatedName = userName,
                    updatedPhone = phoneNumber,
                    updatedClass = classValue,
                    updatedSchool = school
                )
            },
            enabled = updateState !is UpdateProfileState.Loading,
            modifier = Modifier.fillMaxWidth().height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = HeaderGradientEnd,
                disabledContainerColor = ColorWarning
            ),
            shape = RoundedCornerShape(dimensions.cornerRadiusMedium)
        ) {
            Text(
                text = if (updateState is UpdateProfileState.Loading) "Saving..."
                else "Save",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = White
            )
        }
        Spacer(modifier = Modifier.height(dimensions.spaceMedium))
    }
}
