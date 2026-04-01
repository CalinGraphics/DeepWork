package com.deepwork

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.ui.graphics.vector.ImageVector

internal data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    /** Dacă e setat, înlocuiește [subtitle] cu paragrafe separate (formatare clară). */
    val bodyParagraphs: List<String>? = null
)

internal val onboardingSlides = listOf(
    OnboardingSlide(
        title = "KARA",
        subtitle = "Focus clar, fără aglomerație. Timer, taskuri și companion desktop într-un singur flow.",
        icon = Icons.Rounded.DoNotDisturbOn,
        bodyParagraphs = listOf(
            "Sesiunile se salvează local, iar progresul tău rămâne între deschideri.",
            "Poți începe rapid cu 25 min sau seta exact durata dorită."
        )
    ),
    OnboardingSlide(
        title = "Timer + Task curent",
        subtitle = "Pornești sesiunea, urmărești timpul și legi focusul de taskul activ.",
        icon = Icons.Rounded.Bolt
    ),
    OnboardingSlide(
        title = "Companion PC",
        subtitle = "Conectezi telefonul la desktop prin Wi-Fi sau USB și controlezi sesiunea cross-device.",
        icon = Icons.Rounded.PhoneAndroid
    ),
    OnboardingSlide(
        title = "HCI Gestures",
        subtitle = "Gesturile de pe telefon pot trimite acțiuni live către companion când conexiunea e activă.",
        icon = Icons.Rounded.PrecisionManufacturing
    ),
    OnboardingSlide(
        title = "Insights",
        subtitle = "Vezi streak, XP și heatmap din date reale locale.",
        icon = Icons.Rounded.Analytics
    )
)
