package com.dkajan.notifixer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.*

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_anim)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(primaryColor),
            keyPath = arrayOf("**")
        )
    )

    LaunchedEffect(progress) {
        if (progress == 1f) {
            onAnimationFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            dynamicProperties = dynamicProperties,
            modifier = Modifier.size(250.dp)
        )
    }
}