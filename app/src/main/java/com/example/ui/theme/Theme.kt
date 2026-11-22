package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticStatusColors(
    val verified: Color,
    val verifiedContainer: Color,
    val spoofed: Color,
    val spoofedContainer: Color,
    val caution: Color,
    val cautionContainer: Color,
    val telemetry: Color,
    val telemetryContainer: Color,
)

val LocalSemanticStatusColors = staticCompositionLocalOf {
    SemanticStatusColors(
        verified = VerifiedEmerald,
        verifiedContainer = VerifiedEmeraldContainer,
        spoofed = SpoofCrimson,
        spoofedContainer = SpoofCrimsonContainer,
        caution = CautionAmber,
        cautionContainer = CautionAmberContainer,
        telemetry = TelemetryBlue,
        telemetryContainer = TelemetryBlueContainer,
    )
}

private val AppLightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = PureWhite,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandBlue,
    onSecondary = PureWhite,
    secondaryContainer = BrandBlueLight,
    onSecondaryContainer = BrandBlueDark,
    tertiary = CautionAmber,
    onTertiary = PureWhite,
    tertiaryContainer = CautionAmberContainer,
    onTertiaryContainer = Color(0xFF7C3E00),
    error = SpoofCrimson,
    onError = PureWhite,
    errorContainer = SpoofCrimsonContainer,
    onErrorContainer = Color(0xFF8E0000),
    background = PureWhite,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = SoftGraySurface,
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFBDBDBD),
    outlineVariant = DividerGray,
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun MockDetectorTheme(
    content: @Composable () -> Unit,
) {
    val statusColors = SemanticStatusColors(
        verified = VerifiedEmerald,
        verifiedContainer = VerifiedEmeraldContainer,
        spoofed = SpoofCrimson,
        spoofedContainer = SpoofCrimsonContainer,
        caution = CautionAmber,
        cautionContainer = CautionAmberContainer,
        telemetry = TelemetryBlue,
        telemetryContainer = TelemetryBlueContainer,
    )

    CompositionLocalProvider(LocalSemanticStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = AppLightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
