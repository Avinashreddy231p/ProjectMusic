package com.mardous.projectmusic.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.mardous.projectmusic.R
import com.mardous.projectmusic.core.model.theme.EraFont
import com.mardous.projectmusic.util.Preferences

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

fun getGoogleFontFamily(name: String): FontFamily {
    val fontName = GoogleFont(name)
    return FontFamily(
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.ExtraLight),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Light),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
        Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.ExtraBold),
    )
}

val Inter = getGoogleFontFamily("Inter")
val Montserrat = getGoogleFontFamily("Montserrat")
val Outfit = getGoogleFontFamily("Outfit")
val Manrope = getGoogleFontFamily("Manrope")

val GoogleSansFlex = FontFamily(
    Font(R.font.googlesansflex_extralight, FontWeight.ExtraLight),
    Font(R.font.googlesansflex_light, FontWeight.Light),
    Font(R.font.googlesansflex_regular, FontWeight.Normal),
    Font(R.font.googlesansflex_medium, FontWeight.Medium),
    Font(R.font.googlesansflex_semibold, FontWeight.SemiBold),
    Font(R.font.googlesansflex_bold, FontWeight.Bold),
    Font(R.font.googlesansflex_extrabold, FontWeight.ExtraBold)
)

fun resolveFontFamily(eraFont: EraFont): FontFamily = when (eraFont) {
    EraFont.GOOGLE_SANS -> GoogleSansFlex
    EraFont.INTER -> Inter
    EraFont.MONTSERRAT -> Montserrat
    EraFont.OUTFIT -> Outfit
    EraFont.MANROPE -> Manrope
}

val defaultTypography = Typography()

fun getEraTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
)

val customTypography = getEraTypography(GoogleSansFlex)
