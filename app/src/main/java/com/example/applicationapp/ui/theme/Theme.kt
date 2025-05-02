package com.example.applicationapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// الوضع الفاتح
private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    background = BackgroundColor,
    surface = SurfaceColor,
    onBackground = TextDark,
    onSurface = TextDark
)

// الوضع الداكن
private val DarkColors = darkColorScheme(
    primary = PrimaryColorDark,
    onPrimary = OnPrimaryColor,
    background = BackgroundColorDark,
    surface = SurfaceColorDark,
    onBackground = TextLight,
    onSurface = TextLight
)

// قديم - لأغراض التوافق مع الشاشات السابقة
private val pricesLightColors = lightColorScheme(
    primary = PricesPrimaryColor,
    onPrimary = Color.White,
    background = PricesBackgroundColor,
    surface = PricesSurfaceColor,
    onBackground = PricesTextPrimary,
    onSurface = PricesTextPrimary
)

// الثيم الأساسي الجديد
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

// ثيم مؤقت قديم
@Composable
fun PricesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = pricesLightColors,
        typography = AppTypography,
        content = content
    )
}



