package ai.medray.staff.ui.auth

import ai.medray.staff.core.config.BrandConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.medray.staff.BuildConfig
import ai.medray.staff.R
import ai.medray.staff.ui.common.WavyBackground
import ai.medray.staff.ui.theme.*
import org.json.JSONObject

private val HeroGradientStart = Color(0xFFEFF6FF)
private val HeroGradientMiddle = Color(0xFFEEF2FF)
private val HeroGradientEnd = Color(0xFFDBEAFE)
private val MedRayPrimaryBlue = Color(0xFF2563EB)
private val MedRayTextDark = Color(0xFF0F172A)
private val CardBorderColor = Color(0xFFE2E8F0)

private fun formatErrorMessage(raw: String?): String {
    if (raw.isNullOrBlank()) return "An error occurred. Please try again."
    val extracted = try {
        val obj = JSONObject(raw)
        obj.optString("error", obj.optString("message", raw))
    } catch (_: Exception) {
        raw
    }

    return when {
        extracted.contains("No account registered with this Google email", ignoreCase = true) ||
        extracted.contains("google account does not exist", ignoreCase = true) ||
        extracted.contains("no_account", ignoreCase = true) ->
            "No staff account found for this Google email. Please ensure your Clinic Admin has registered this Google email to your staff account, or use Mobile OTP."
        extracted.contains("Invalid verification code", ignoreCase = true) ->
            "Invalid OTP code. Please check and re-enter."
        extracted.contains("Only Doctor accounts", ignoreCase = true) ->
            "This account is not authorized for mobile staff triage. Please contact your Clinic Admin."
        extracted.contains("verify your email", ignoreCase = true) ->
            "Please verify your email first — check your inbox for the verification link we sent when you signed up."
        extracted.contains("awaiting approval", ignoreCase = true) ->
            "Your clinic is still awaiting approval. We'll notify you by email once a Super Admin reviews and approves it."
        else -> extracted
    }
}

@Composable
fun LoginScreen(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    isSendingOtp: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onPasswordLogin: () -> Unit,
    isPasswordLoggingIn: Boolean,
    onGoogleSignIn: () -> Unit,
    isGoogleSigningIn: Boolean,
    error: String?,
    onSignUpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Phone OTP, 1: Password
    var showPassword by remember { mutableStateOf(false) }
    val isValidPhone = phone.length == 10

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
            // Brand Bar
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

                Column {
                    Text(
                        BrandConfig.APP_NAME,
                        fontFamily = HeadingFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedRayTextDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "MOBILE WORKSPACE",
                            fontFamily = HeadingFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayPrimaryBlue
                        )
                    }
                }
            }

            // Main Card matching Doctor Tablet Login Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Welcome Back 👋",
                        fontFamily = HeadingFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedRayTextDark
                    )

                    Text(
                        "Sign in to access your OPD triage queue & front desk.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Tab Selector
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Surface(
                                color = if (selectedTab == 0) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(9.dp),
                                shadowElevation = if (selectedTab == 0) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 7.dp)
                            ) {
                                Text(
                                    "Mobile OTP",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) MedRayPrimaryBlue else Slate600,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                color = if (selectedTab == 1) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(9.dp),
                                shadowElevation = if (selectedTab == 1) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = 1 }
                                    .padding(vertical = 7.dp)
                            ) {
                                Text(
                                    "Staff Password",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) MedRayPrimaryBlue else Slate600,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    if (selectedTab == 0) {
                        // Mobile OTP Input
                        Text(
                            "Registered Mobile Number",
                            fontFamily = HeadingFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayTextDark
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(
                                    1.5.dp,
                                    if (isValidPhone) MedRayPrimaryBlue else Color(0xFFCBD5E1),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Country Code Pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("🇮🇳", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "+91",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayPrimaryBlue
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            BasicTextField(
                                value = phone,
                                onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) onPhoneChange(it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedRayTextDark,
                                    letterSpacing = 1.sp
                                ),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (phone.isEmpty()) {
                                            Text(
                                                "10-digit mobile number",
                                                fontFamily = InterFontFamily,
                                                fontSize = 13.sp,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            AnimatedVisibility(visible = isValidPhone, enter = fadeIn(), exit = fadeOut()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFFDCFCE7), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Valid",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            "We'll send a 6-digit OTP to verify your identity.",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = formatErrorMessage(error),
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
                            onClick = onSendOtp,
                            enabled = isValidPhone && !isSendingOtp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MedRayPrimaryBlue,
                                disabledContainerColor = Color(0xFF93C5FD)
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Requesting OTP...",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "Get OTP & Continue",
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
                        // Password Mode
                        Text(
                            "Staff Email Address",
                            fontFamily = HeadingFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayTextDark
                        )
                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            placeholder = { Text("staff@medray.ai", fontFamily = InterFontFamily, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedRayPrimaryBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Password",
                            fontFamily = HeadingFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedRayTextDark
                        )
                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            placeholder = { Text("Enter your password", fontFamily = InterFontFamily, fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedRayPrimaryBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = formatErrorMessage(error),
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
                            onClick = onPasswordLogin,
                            enabled = email.isNotBlank() && password.isNotBlank() && !isPasswordLoggingIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedRayPrimaryBlue),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isPasswordLoggingIn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    "Sign In with Password",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // OR CONTINUE WITH Divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(color = CardBorderColor, modifier = Modifier.weight(1f))
                        Text(
                            "OR CONTINUE WITH",
                            fontFamily = HeadingFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(color = CardBorderColor, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(18.dp))

                    // Google OAuth Button
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        enabled = !isGoogleSigningIn && !isSendingOtp && !isPasswordLoggingIn,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isGoogleSigningIn) {
                            CircularProgressIndicator(
                                color = MedRayPrimaryBlue,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Continue with Google Workspace",
                                    fontFamily = HeadingFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MedRayTextDark
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // New clinic sign-up entry point
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "New clinic?",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        TextButton(onClick = onSignUpClick, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                            Text(
                                "Sign Up Your Clinic",
                                fontFamily = HeadingFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedRayPrimaryBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Security assurance
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Authorized healthcare providers only",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "${BuildConfig.VERSION_NAME_DISPLAY} · Secure Clinical Portal",
                        fontFamily = InterFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun OtpVerificationScreen(
    phone: String,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    isVerifying: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientMiddle, HeroGradientEnd)))
    ) {
        WavyBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = null,
                            tint = MedRayPrimaryBlue,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Verify Mobile OTP",
                        fontFamily = HeadingFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedRayTextDark
                    )

                    Text(
                        "6-digit security code sent to +91 $phone",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onOtpChange(it) },
                        label = { Text("6-Digit Code", fontFamily = InterFontFamily) },
                        placeholder = { Text("123456", fontFamily = InterFontFamily) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MedRayPrimaryBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!error.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = formatErrorMessage(error),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onVerify,
                        enabled = otpCode.length >= 4 && !isVerifying,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedRayPrimaryBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "Verify & Continue",
                                fontFamily = HeadingFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(onClick = onResend) {
                        Text(
                            "Resend Code",
                            fontFamily = HeadingFontFamily,
                            color = MedRayPrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
