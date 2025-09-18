package com.margelo.nitro.nitrotimer

import android.view.View
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext
import androidx.core.graphics.toColorInt

@DoNotStrip
class HybridNitroTimer(val context: ThemedReactContext) : HybridNitroTimerSpec() {

  // View
  override val view: View = View(context)

  // Props
  private var _color = "#000"
  override var color: String
      get() = _color
      set(value) {
          _color = value
          val color = value.toColorInt()
          view.setBackgroundColor(color)
      }
}
