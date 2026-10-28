package com.example.ui.theme

import android.graphics.Matrix
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pillStar
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Material 3 Expressive generous large-radius container shape tokens.
 */
val ExpressiveShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(28.dp),
  extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Provides RoundedPolygon definitions and shape-morphing utilities (via androidx.graphics.shapes)
 * for checkboxes, FAB menus, and multi-select badges.
 */
object TaskNestExpressivePolygons {
  val SoftSquare: RoundedPolygon by lazy {
    RoundedPolygon(
      numVertices = 4,
      rounding = CornerRounding(radius = 0.35f, smoothing = 0.8f)
    ).normalized()
  }

  val CookieBurst: RoundedPolygon by lazy {
    RoundedPolygon.star(
      numVerticesPerRadius = 8,
      innerRadius = 0.78f,
      rounding = CornerRounding(radius = 0.22f, smoothing = 0.6f)
    ).normalized()
  }

  val ScallopBadge: RoundedPolygon by lazy {
    RoundedPolygon.pillStar(
      width = 1f,
      height = 1f,
      numVerticesPerRadius = 10,
      innerRadiusRatio = 0.84f,
      rounding = CornerRounding(radius = 0.25f, smoothing = 0.5f)
    ).normalized()
  }

  val CirclePolygon: RoundedPolygon by lazy {
    RoundedPolygon.circle(numVertices = 8).normalized()
  }

  val CheckboxUncheckedToCheckedMorph: Morph by lazy {
    Morph(start = SoftSquare, end = CookieBurst)
  }

  val FabCollapsedToExpandedMorph: Morph by lazy {
    Morph(start = SoftSquare, end = ScallopBadge)
  }
}

/**
 * Compose [Shape] that renders a [Morph] at a given [progress] (0f..1f), scaling to the target component bounds.
 */
class MorphPolygonShape(
  private val morph: Morph,
  private val progress: Float
) : Shape {
  private val matrix = Matrix()

  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    val androidPath = morph.toPath(progress = progress.coerceIn(0f, 1f))
    matrix.reset()
    matrix.setScale(size.width, size.height)
    androidPath.transform(matrix)
    val composePath: Path = androidPath.asComposePath()
    return Outline.Generic(composePath)
  }
}
