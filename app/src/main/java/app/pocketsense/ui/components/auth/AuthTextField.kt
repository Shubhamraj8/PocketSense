package app.pocketsense.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.pocketsense.ui.auth.AuthColors
import app.pocketsense.ui.auth.LocalAuthPalette

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibleChange: ((Boolean) -> Unit)? = null,
) {
    val palette = LocalAuthPalette.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val hasError = !errorText.isNullOrBlank()
    val borderColor = when {
        hasError -> AuthColors.errorText
        focused -> palette.textPrimary
        else -> palette.textTertiary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            color = palette.textTertiary,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            singleLine = singleLine,
            isError = hasError,
            shape = RoundedCornerShape(4.dp),
            interactionSource = interaction,
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = if (isPassword && onPasswordVisibleChange != null) {
                {
                    IconButton(
                        onClick = { onPasswordVisibleChange(!passwordVisible) },
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = palette.textSecondary,
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                disabledContainerColor = palette.surface,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
                errorBorderColor = AuthColors.errorText,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                errorTextColor = palette.textPrimary,
                cursorColor = palette.textPrimary,
                errorCursorColor = palette.textPrimary,
                focusedPlaceholderColor = palette.textTertiary,
                unfocusedPlaceholderColor = palette.textTertiary,
            ),
        )

        helperText?.let {
            Text(
                text = it,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        AnimatedVisibility(
            visible = hasError,
            enter = fadeIn(animationSpec = tween(200)),
        ) {
            Text(
                text = errorText.orEmpty(),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = AuthColors.errorText,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
