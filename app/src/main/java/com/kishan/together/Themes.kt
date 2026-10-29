package com.kishan.together

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val THEMES: Map<String, ColorScheme> = linkedMapOf(
    "Lavender" to darkColorScheme(),
    "Rose" to darkColorScheme(
        primary = Color(0xFFFFB1C8), onPrimary = Color(0xFF5E1133), secondaryContainer = Color(0xFF6B3A4C),
        background = Color(0xFF1F1A1C), surface = Color(0xFF1F1A1C), surfaceVariant = Color(0xFF524347),
        surfaceContainerLow = Color(0xFF2A2225),
    ),
    "Ocean" to darkColorScheme(
        primary = Color(0xFF8ECDFF), onPrimary = Color(0xFF00344F), secondaryContainer = Color(0xFF2E4A5E),
        background = Color(0xFF161C21), surface = Color(0xFF161C21), surfaceVariant = Color(0xFF3B4852),
        surfaceContainerLow = Color(0xFF1E262C),
    ),
    "Mint" to darkColorScheme(
        primary = Color(0xFF7DDBB4), onPrimary = Color(0xFF003827), secondaryContainer = Color(0xFF2F4D40),
        background = Color(0xFF171C1A), surface = Color(0xFF171C1A), surfaceVariant = Color(0xFF3C4A43),
        surfaceContainerLow = Color(0xFF1F2623),
    ),
    "Sunset" to darkColorScheme(
        primary = Color(0xFFFFB68A), onPrimary = Color(0xFF522300), secondaryContainer = Color(0xFF5E4031),
        background = Color(0xFF201A17), surface = Color(0xFF201A17), surfaceVariant = Color(0xFF52443C),
        surfaceContainerLow = Color(0xFF2A221E),
    ),
    "Midnight" to darkColorScheme(
        primary = Color(0xFFC9CEDC), onPrimary = Color(0xFF1E2230), secondaryContainer = Color(0xFF33353D),
        background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF2A2A2E),
        surfaceContainerLow = Color(0xFF121214),
    ),
    "Light" to lightColorScheme(),
)
