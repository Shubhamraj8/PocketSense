package app.pocketsense.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.pocketsense.R

private val provider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plusJakartaSans = GoogleFont("Plus Jakarta Sans")

val PlusJakartaSans = FontFamily(
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.W400),
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.W600),
    Font(googleFont = plusJakartaSans, fontProvider = provider, weight = FontWeight.W700),
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 32.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.04).em,
        lineHeight = 38.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 24.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.03).em,
        lineHeight = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 20.sp,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.03).em,
        lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 17.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.02).em,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 15.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.01).em,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.em,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 15.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.em,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.em,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.em,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.06.em,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans,
        fontSize = 9.sp,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.08.em,
        lineHeight = 14.sp,
    ),
)
