package ai.medray.staff.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.R
import ai.medray.staff.core.config.BrandConfig
import ai.medray.staff.data.repository.PlacesAutocompleteRepository
import ai.medray.staff.ui.common.AddressAutocompleteField
import ai.medray.staff.ui.common.WavyBackground
import ai.medray.staff.ui.theme.*

private val HeroGradientStart = Color(0xFFEFF6FF)
private val HeroGradientMiddle = Color(0xFFEEF2FF)
private val HeroGradientEnd = Color(0xFFDBEAFE)
private val MedRayPrimaryBlue = Color(0xFF2563EB)
private val MedRayTextDark = Color(0xFF0F172A)
private val CardBorderColor = Color(0xFFE2E8F0)

data class ClinicSignupFormState(
    val clinicName: String = "",
    val clinicAddress: String = "",
    val clinicPhone: String = "",
    val adminFullName: String = "",
    val adminEmail: String = "",
    val adminPhone: String = "",
    val password: String = "",
    val confirmPassword: String = ""
) {
    val passwordsMatch: Boolean get() = password.isNotEmpty() && password == confirmPassword
    val isStep1Valid: Boolean get() = clinicName.trim().isNotBlank()
    val isStep2Valid: Boolean
        get() = adminFullName.trim().isNotBlank() &&
            adminEmail.trim().isNotBlank() &&
            adminPhone.trim().isNotBlank() &&
            password.length >= 8 &&
            passwordsMatch
    val isValid: Boolean get() = isStep1Valid && isStep2Valid
}

/**
 * "Sign Up Your Clinic" — 2-Step onboarding flow mirroring web's /signup
 * backend (publicClinicSignup.ts, same two-gate flow: verify email, then
 * Super Admin approval).
 * - Step 1: Clinic profile (Name, Address, Phone)
 * - Step 2: Admin credentials (Name, Email, Mobile, Password, Confirm)
 */
@Composable
fun ClinicSignupScreen(
    formState: ClinicSignupFormState,
    onFormChange: (ClinicSignupFormState) -> Unit,
    onSubmit: () -> Unit,
    isSubmitting: Boolean,
    submitted: Boolean,
    confirmationMessage: String?,
    error: String?,
    onBackToLogin: () -> Unit,
    placesRepository: PlacesAutocompleteRepository,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Intercept Android hardware back button: go from Step 2 back to Step 1 without leaving form
    BackHandler(enabled = currentStep == 2 && !submitted) {
        currentStep = 1
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientMiddle, HeroGradientEnd)))
    ) {
        WavyBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_medray_logo),
                        contentDescription = "MedRay",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    BrandConfig.APP_NAME,
                    fontFamily = HeadingFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedRayTextDark
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                if (submitted) {
                    // Confirmation panel — mirrors web/src/app/signup/page.tsx's `sent` state.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.MarkEmailRead, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Check your email",
                            fontFamily = HeadingFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayTextDark
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            confirmationMessage
                                ?: "We sent a verification link to ${formState.adminEmail}. Once verified, we'll review your clinic and email you when it's ready — usually within one business day.",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        TextButton(onClick = onBackToLogin) {
                            Text("Back to sign in", fontFamily = HeadingFontFamily, color = MedRayPrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    return@Card
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with back navigation & step tracker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                if (currentStep == 2) {
                                    currentStep = 1
                                } else {
                                    onBackToLogin()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (currentStep == 2) "Back to Step 1" else "Back to Sign In",
                                tint = Color(0xFF64748B)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentStep == 1) "Clinic Details" else "Admin Credentials",
                                fontFamily = HeadingFontFamily,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedRayTextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Step $currentStep of 2",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MedRayPrimaryBlue
                            )
                        }
                    }

                    Text(
                        text = if (currentStep == 1)
                            "Enter your clinic details to set up your practice workspace."
                        else
                            "Create the primary administrator account for this clinic.",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 38.dp, top = 2.dp, bottom = 12.dp)
                    )

                    SignupStepIndicator(
                        currentStep = currentStep,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // Step Content with Horizontal Slide Animation
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width / 3 } + fadeOut()
                                )
                            }.using(SizeTransform(clip = false))
                        },
                        label = "ClinicSignupStepTransition"
                    ) { step ->
                        if (step == 1) {
                            Column {
                                SectionLabel("Clinic Information")
                                LabeledField(
                                    label = "Clinic Name *",
                                    value = formState.clinicName,
                                    onValueChange = { onFormChange(formState.copy(clinicName = it)) },
                                    placeholder = "e.g. Dr Sharma Clinic"
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Address (optional)",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayTextDark
                                )
                                Spacer(Modifier.height(6.dp))
                                AddressAutocompleteField(
                                    value = formState.clinicAddress,
                                    onValueChange = { onFormChange(formState.copy(clinicAddress = it)) },
                                    repository = placesRepository,
                                    placeholder = "Start typing an address…",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))
                                LabeledField(
                                    label = "Clinic Phone (optional)",
                                    value = formState.clinicPhone,
                                    onValueChange = { onFormChange(formState.copy(clinicPhone = it)) },
                                    placeholder = "Reception phone number",
                                    keyboardType = KeyboardType.Phone
                                )

                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = { currentStep = 2 },
                                    enabled = formState.isStep1Valid,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MedRayPrimaryBlue,
                                        disabledContainerColor = Color(0xFF93C5FD)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        "Next: Admin Details",
                                        fontFamily = HeadingFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Column {
                                SectionLabel("Clinic Admin Account")
                                LabeledField(
                                    label = "Your Full Name *",
                                    value = formState.adminFullName,
                                    onValueChange = { onFormChange(formState.copy(adminFullName = it)) },
                                    placeholder = "Full name"
                                )
                                Spacer(Modifier.height(10.dp))
                                LabeledField(
                                    label = "Email Address *",
                                    value = formState.adminEmail,
                                    onValueChange = { onFormChange(formState.copy(adminEmail = it)) },
                                    placeholder = "you@clinic.com",
                                    keyboardType = KeyboardType.Email
                                )
                                Spacer(Modifier.height(10.dp))
                                LabeledField(
                                    label = "Phone *",
                                    value = formState.adminPhone,
                                    onValueChange = { onFormChange(formState.copy(adminPhone = it)) },
                                    placeholder = "Your mobile number",
                                    keyboardType = KeyboardType.Phone
                                )
                                Spacer(Modifier.height(10.dp))

                                Text("Password *", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.password,
                                    onValueChange = { onFormChange(formState.copy(password = it)) },
                                    placeholder = { Text("At least 8 characters", fontFamily = InterFontFamily, fontSize = 14.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedRayPrimaryBlue, unfocusedBorderColor = Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))

                                Text("Confirm Password *", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.confirmPassword,
                                    onValueChange = { onFormChange(formState.copy(confirmPassword = it)) },
                                    placeholder = { Text("Re-enter your password", fontFamily = InterFontFamily, fontSize = 14.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                            Icon(
                                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                                tint = Color(0xFF94A3B8)
                                            )
                                        }
                                    },
                                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    isError = formState.confirmPassword.isNotEmpty() && !formState.passwordsMatch,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedRayPrimaryBlue, unfocusedBorderColor = Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (formState.confirmPassword.isNotEmpty() && !formState.passwordsMatch) {
                                    Text(
                                        "Passwords don't match",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (!error.isNullOrBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = error,
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }

                                Spacer(Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { currentStep = 1 },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = Color(0xFF475569)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Back",
                                            fontFamily = HeadingFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Button(
                                        onClick = onSubmit,
                                        enabled = formState.isValid && !isSubmitting,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MedRayPrimaryBlue,
                                            disabledContainerColor = Color(0xFF93C5FD)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .weight(2f)
                                            .fillMaxHeight()
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                "Sign Up Clinic",
                                                fontFamily = HeadingFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Text("Already registered?", fontFamily = InterFontFamily, fontSize = 12.sp, color = Color(0xFF64748B))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Sign in",
                            fontFamily = HeadingFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayPrimaryBlue,
                            modifier = Modifier.clickableText(onBackToLogin)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignupStepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        StepIndicatorPill(
            stepNumber = 1,
            label = "Clinic Profile",
            isActive = currentStep == 1,
            isCompleted = currentStep > 1
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (currentStep > 1) MedRayPrimaryBlue else Color(0xFFE2E8F0))
        )
        StepIndicatorPill(
            stepNumber = 2,
            label = "Admin Account",
            isActive = currentStep == 2,
            isCompleted = false
        )
    }
}

@Composable
private fun StepIndicatorPill(
    stepNumber: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> Color(0xFF16A34A)
                        isActive -> MedRayPrimaryBlue
                        else -> Color(0xFFE2E8F0)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontFamily = HeadingFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else Color(0xFF64748B)
                )
            }
        }
        Text(
            text = label,
            fontFamily = HeadingFontFamily,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) MedRayTextDark else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontFamily = HeadingFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Text(label, fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontFamily = InterFontFamily, fontSize = 14.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MedRayPrimaryBlue, unfocusedBorderColor = Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
