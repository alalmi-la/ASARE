package com.example.applicationapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.applicationapp.R

// ⚠️ تأكد من وضع ملفات الخطوط داخل res/font (مثل poppins_regular.ttf وغيرها)
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Poppins,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp
    )
)
