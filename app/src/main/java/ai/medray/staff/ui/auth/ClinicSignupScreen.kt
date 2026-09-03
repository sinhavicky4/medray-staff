package ai.medray.staff.ui.auth

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.R
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
    val passwordsMatch: Boolean get() = password == confirmPassword
    val isValid: Boolean
        get() = clinicName.isNotBlank() && adminFullName.isNotBlank() &&
            adminEmail.isNotBlank() && adminPhone.isNotBlank() &&
            password.length >= 8 && passwordsMatch
}

/**
 * "Sign Up Your Clinic" — mirrors web's /signup form field-for-field (same
 * publicClinicSignup.ts backend, same two-gate flow: verify email, then a
 * Super Admin approves on the web portal). On success, swaps to a "check
 * your email" confirmation panel rather than navigating anywhere — there's
 * nothing to navigate to yet until both gates clear (see LoginScreen's
 * awaiting-approval/unverified-email error copy for what happens next).
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
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientMiddle, HeroGradientEnd)))
    ) {
        WavyBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    "MedRay Staff",
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
                modifier = Modifier.fillMaxWidth()
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

                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackToLogin, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF64748B))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Sign Up Your Clinic",
                            fontFamily = HeadingFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayTextDark
                        )
                    }
                    Text(
                        "Register your clinic and become its first Clinic Admin.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 18.dp)
                    )

                    SectionLabel("Clinic Details")
                    LabeledField("Clinic Name", formState.clinicName, { onFormChange(formState.copy(clinicName = it)) }, "e.g. Dr Sharma Clinic")
                    Spacer(Modifier.height(10.dp))
                    Text("Address (optional)", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
                    Spacer(Modifier.height(6.dp))
                    AddressAutocompleteField(
                        value = formState.clinicAddress,
                        onValueChange = { onFormChange(formState.copy(clinicAddress = it)) },
                        repository = placesRepository,
                        placeholder = "Start typing an address…",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    LabeledField("Clinic Phone (optional)", formState.clinicPhone, { onFormChange(formState.copy(clinicPhone = it)) }, "Reception phone number", keyboardType = KeyboardType.Phone)

                    Spacer(Modifier.height(18.dp))
                    SectionLabel("You (Clinic Admin)")
                    LabeledField("Your Full Name", formState.adminFullName, { onFormChange(formState.copy(adminFullName = it)) }, "Full name")
                    Spacer(Modifier.height(10.dp))
                    LabeledField("Email Address", formState.adminEmail, { onFormChange(formState.copy(adminEmail = it)) }, "you@clinic.com", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(10.dp))
                    LabeledField("Phone", formState.adminPhone, { onFormChange(formState.copy(adminPhone = it)) }, "Your mobile number", keyboardType = KeyboardType.Phone)
                    Spacer(Modifier.height(10.dp))

                    Text("Password", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = { onFormChange(formState.copy(password = it)) },
                        placeholder = { Text("At least 8 characters", fontFamily = InterFontFamily, fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF94A3B8))
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

                    Text("Confirm Password", fontFamily = HeadingFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedRayTextDark)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = formState.confirmPassword,
                        onValueChange = { onFormChange(formState.copy(confirmPassword = it)) },
                        placeholder = { Text("Re-enter your password", fontFamily = InterFontFamily, fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF94A3B8))
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
                        Text("Passwords don't match", fontFamily = InterFontFamily, fontSize = 11.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 4.dp))
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
                    Button(
                        onClick = onSubmit,
                        enabled = formState.isValid && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayPrimaryBlue, disabledContainerColor = Color(0xFF93C5FD)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign Up Your Clinic", fontFamily = HeadingFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
