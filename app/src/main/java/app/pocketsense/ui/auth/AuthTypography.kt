package app.pocketsense.ui.auth

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.unit.sp
import app.pocketsense.R

private val provider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private fun jakarta(weight: FontWeight) = FontFamily(
    Font(
        googleFont = GoogleFont("Plus Jakarta Sans"),
        fontProvider = provider,
        weight = weight,
    ),
)

val AuthTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = jakarta(FontWeight.W700),
        fontWeight = FontWeight.W700,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.04f * 32).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = jakarta(FontWeight.W700),
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.03f * 24).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = jakarta(FontWeight.W700),
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.03f * 20).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = jakarta(FontWeight.W600),
        fontWeight = FontWeight.W600,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02f * 17).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = jakarta(FontWeight.W600),
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01f * 15).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = jakarta(FontWeight.W600),
        fontWeight = FontWeight.W600,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = jakarta(FontWeight.W400),
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = jakarta(FontWeight.W400),
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = jakarta(FontWeight.W400),
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = jakarta(FontWeight.W600),
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = (0.06f * 11).sp,
    ),
    labelSmall = TextStyle(
        fontFamily = jakarta(FontWeight.W600),
        fontWeight = FontWeight.W600,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = (0.08f * 9).sp,
    ),
)
