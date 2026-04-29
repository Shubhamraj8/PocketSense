package app.pocketsense.ui.components.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import app.pocketsense.ui.auth.LocalAuthPalette

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalAuthPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "primaryButtonScale",
    )

    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            disabledContainerColor = palette.accent.copy(alpha = 0.3f),
            disabledContentColor = palette.onAccent.copy(alpha = 0.3f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = palette.onAccent,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}
