package com.michaelamimo.sumeria.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.michaelamimo.sumeria.R

// --- SUMERIA COLOR PALETTE ---
object SumeriaColors {
    val TextPrimary = Color(0xFF051F20)
    val ActionPrimary = Color(0xFF0B2B26)
    val SurfaceDark = Color(0xFF163832)
    val Accent = Color(0xFF235347)
    val Muted = Color(0xFF8EB69B)
    val BackgroundSoft = Color(0xFFDAF1DE)
    val SurfaceWhite = Color(0xFFFDFDFD) // Slightly off-white neutral
    val Error = Color(0xFFBA1A1A)
}

// --- REUSABLE COMPONENTS ---

@Composable
fun SumeriaTurtleLogo(modifier: Modifier = Modifier) {
    // Target size 48-72dp as requested
    Icon(
        painter = painterResource(id = R.drawable.ic_sumeria_turtle), // Needs to be added
        contentDescription = "Sumeria",
        tint = Color.Unspecified, // Preserves original SVG colors
        modifier = modifier.size(64.dp)
    )
}

@Composable
fun SumeriaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SumeriaColors.ActionPrimary,
            contentColor = SumeriaColors.SurfaceWhite,
            disabledContainerColor = SumeriaColors.Muted.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = SumeriaColors.SurfaceWhite,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Processing...", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        } else {
            Text(text, fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SumeriaSocialButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SumeriaColors.Muted.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SumeriaColors.TextPrimary,
            containerColor = SumeriaColors.SurfaceWhite
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified, // Keeps original Google/Microsoft colors
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SumeriaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: Painter,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = SumeriaColors.Muted) },
        leadingIcon = {
            Icon(painter = leadingIcon, contentDescription = null, tint = SumeriaColors.ActionPrimary, modifier = Modifier.size(20.dp))
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Password", tint = SumeriaColors.Muted)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SumeriaColors.Accent,
            unfocusedBorderColor = SumeriaColors.Muted.copy(alpha = 0.3f),
            focusedTextColor = SumeriaColors.TextPrimary,
            unfocusedTextColor = SumeriaColors.TextPrimary,
            focusedContainerColor = SumeriaColors.BackgroundSoft.copy(alpha = 0.3f),
            unfocusedContainerColor = SumeriaColors.BackgroundSoft.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun SumeriaDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SumeriaColors.Muted.copy(alpha = 0.3f))
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = SumeriaColors.Muted
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = SumeriaColors.Muted.copy(alpha = 0.3f))
    }
}