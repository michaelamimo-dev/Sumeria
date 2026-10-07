package com.michaelamimo.sumeria.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michaelamimo.sumeria.R
import com.michaelamimo.sumeria.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image

@Composable
fun SplashScreen(
    onSplashFinished: (isLoggedIn: Boolean) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000), label = "alpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.9f,
        animationSpec = tween(durationMillis = 1000), label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onSplashFinished(viewModel.isUserLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            Text(
                text = "Sumeria",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = SumeriaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your boundless library, redefined.",
                style = MaterialTheme.typography.bodyLarge,
                color = SumeriaColors.Muted
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPage(page = page)
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) SumeriaColors.Accent else SumeriaColors.Muted.copy(alpha = 0.3f))
                    )
                }
            }

            // Dynamic Buttons based on Page
            when (pagerState.currentPage) {
                0 -> {
                    SumeriaPrimaryButton(text = "Get Started", onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Log in", color = SumeriaColors.TextPrimary)
                    }
                }
                1 -> {
                    SumeriaPrimaryButton(text = "Continue", onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin, modifier = Modifier.alpha(0f)) {
                        Text("Hidden Placeholder to maintain height") // Maintains layout stability
                    }
                }
                2 -> {
                    SumeriaPrimaryButton(text = "Create Your Account", onClick = onNavigateToSignUp)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Log in", color = SumeriaColors.TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    val title = when (page) {
        0 -> "A better place\nfor your books."
        1 -> "Your library,\nbeautifully organized."
        else -> "Make reading\nfeel like reading."
    }
    val description = when (page) {
        0 -> "Sumeria gives your reading life a calm, beautiful home."
        1 -> "Keep your PDFs and EPUBs together and make every book easier to find."
        else -> "Focus on the story with a clean, distraction-free reading experience."
    }

    // Select the correct imported illustration based on the page number
    val imageRes = when (page) {
        0 -> R.drawable.ic_onboarding_library_one
        1 -> R.drawable.ic_onboarding_library_two
        else -> R.drawable.ic_onboarding_library_three
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // The actual image implementation replacing the Box
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null, // Null because it's decorative
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Ensures the illustration scales nicely
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = SumeriaColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = SumeriaColors.Muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
            .systemBarsPadding() // FIXES THE STATUS BAR ISSUE
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        SumeriaTurtleLogo()

        Spacer(modifier = Modifier.height(24.dp))
        Text("Sign In", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = SumeriaColors.TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Continue your reading journey.", style = MaterialTheme.typography.bodyMedium, color = SumeriaColors.Muted)

        Spacer(modifier = Modifier.height(40.dp))

        SumeriaTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-mail",
            leadingIcon = painterResource(id = R.drawable.ic_email), // Add an ic_email.xml vector
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        SumeriaTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = painterResource(id = R.drawable.ic_lock), // Add an ic_lock.xml vector
            isPassword = true
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = { /* TODO: Forgot Password API */ }) {
                Text("Forgot password?", color = SumeriaColors.ActionPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SumeriaPrimaryButton(
            text = "Continue",
            onClick = { viewModel.login(email, password) },
            isLoading = authState is AuthState.Loading
        )

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(12.dp))
            // Apply the friendly error mapping for Login
            val displayError = getFriendlyErrorMessage((authState as AuthState.Error).message, isSignIn = true)
            Text(
                text = displayError,
                color = SumeriaColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        SumeriaDivider(text = "Don't have an account yet?")
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SumeriaColors.Muted.copy(alpha = 0.3f))
        ) {
            Text("Create an account", color = SumeriaColors.TextPrimary, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }

        Spacer(modifier = Modifier.height(16.dp))

        SumeriaSocialButton(text = "Sign in with Google", iconRes = R.drawable.ic_google_logo, onClick = { /* TODO */ })
        Spacer(modifier = Modifier.height(16.dp))
        SumeriaSocialButton(text = "Sign in with Microsoft", iconRes = R.drawable.ic_microsoft_logo, onClick = { /* TODO */ })

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    var localError by remember { mutableStateOf("") }

    // Live Password Validation
    val hasMinLength = password.length >= 8
    val hasUpper = password.any { it.isUpperCase() }
    val hasNumber = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            viewModel.resetState()
            onSignUpSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SumeriaColors.SurfaceWhite)
            .systemBarsPadding() // FIXES THE STATUS BAR ISSUE
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        SumeriaTurtleLogo()

        Spacer(modifier = Modifier.height(24.dp))
        Text("Create your account", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = SumeriaColors.TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Start building your personal reading space.", style = MaterialTheme.typography.bodyMedium, color = SumeriaColors.Muted, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        SumeriaTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-mail",
            leadingIcon = painterResource(id = R.drawable.ic_email),
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        SumeriaTextField(
            value = password,
            onValueChange = { password = it; localError = "" },
            label = "Password",
            leadingIcon = painterResource(id = R.drawable.ic_lock),
            isPassword = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Password Requirements Grid
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ValidationItem(text = "8+ characters", isValid = hasMinLength)
                ValidationItem(text = "1 uppercase", isValid = hasUpper)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ValidationItem(text = "1 number", isValid = hasNumber)
                ValidationItem(text = "1 special character", isValid = hasSpecial)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SumeriaTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = "" },
            label = "Confirm Password",
            leadingIcon = painterResource(id = R.drawable.ic_lock),
            isPassword = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        SumeriaPrimaryButton(
            text = "Create Account",
            onClick = {
                if (password != confirmPassword) {
                    localError = "Passwords do not match."
                } else if (!hasMinLength || !hasUpper || !hasNumber || !hasSpecial) {
                    localError = "Please meet all password requirements."
                } else {
                    viewModel.signUp(email, password)
                }
            },
            isLoading = authState is AuthState.Loading
        )

        if (localError.isNotEmpty() || authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(12.dp))

            // Determine the message: local validation takes priority, then we map Firebase errors
            val displayError = if (localError.isNotEmpty()) {
                localError
            } else {
                getFriendlyErrorMessage((authState as AuthState.Error).message, isSignIn = false)
            }

            Text(
                text = displayError,
                color = SumeriaColors.Error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        SumeriaDivider(text = "or")
        Spacer(modifier = Modifier.height(32.dp))

        SumeriaSocialButton(text = "Continue with Google", iconRes = R.drawable.ic_google_logo, onClick = { /* TODO */ })
        Spacer(modifier = Modifier.height(16.dp))
        SumeriaSocialButton(text = "Continue with Microsoft", iconRes = R.drawable.ic_microsoft_logo, onClick = { /* TODO */ })

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.padding(vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account? ", color = SumeriaColors.Muted)
            Text(
                text = "Sign In",
                color = SumeriaColors.ActionPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

@Composable
fun ValidationItem(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (isValid) "✓" else "✕",
            color = if (isValid) SumeriaColors.Accent else SumeriaColors.Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = if (isValid) SumeriaColors.TextPrimary else SumeriaColors.Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Maps raw Firebase error messages into user-friendly copy.
 */
fun getFriendlyErrorMessage(rawMessage: String, isSignIn: Boolean): String {
    return if (isSignIn) {
        // Generic, highly secure, and friendly login message requested
        "We couldn’t sign you in. Please check your email and password."
    } else {
        // Specific user-friendly mapping for account creation
        if (rawMessage.contains("badly formatted", ignoreCase = true)) {
            "That doesn’t look like a valid email address. Please check and try again."
        } else {
            rawMessage // Fallback to raw message for other registration errors (e.g., email already in use)
        }
    }
}