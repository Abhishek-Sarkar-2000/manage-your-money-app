package com.manageyourmoney.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Ports index.html:29-30's `--radius:12px` / `--radius-sm:8px`, plus the `.btn`/
 *  `.pill-btn` fully-rounded (999px) pill shape used for every button (index.html:111,123). */
object MoneyShapes {
    val RadiusSmall = 8.dp
    val Radius = 12.dp
    val Pill = 999.dp
}

val MoneyShapeScheme = Shapes(
    extraSmall = RoundedCornerShape(MoneyShapes.RadiusSmall),
    small = RoundedCornerShape(MoneyShapes.RadiusSmall),
    medium = RoundedCornerShape(MoneyShapes.Radius),
    large = RoundedCornerShape(MoneyShapes.Radius),
    extraLarge = RoundedCornerShape(MoneyShapes.Pill),
)

val PillShape = RoundedCornerShape(MoneyShapes.Pill)
