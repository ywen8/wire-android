package com.waz.zclient.cursor

import android.content.Context
import android.util.AttributeSet
import com.nineoldandroids.animation.{Animator, AnimatorListenerAdapter, ValueAnimator}
import com.waz.threading.Threading
import com.waz.utils.returning
import com.waz.zclient.ui.animation.interpolators.penner.Expo
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.utils._
import com.waz.zclient.R

import scala.concurrent.duration._

class SendButton(context: Context, attrs: AttributeSet, defStyleAttr: Int) extends CursorIconButton(context, attrs, defStyleAttr) { view =>
  def this(context: Context, attrs: AttributeSet) { this(context, attrs, 0) }
  def this(context: Context) { this(context, null) }

  val fadeInDuration = getInt(R.integer.animation_duration_medium).millis

  val fadeInAnimator = returning(ValueAnimator.ofFloat(0, 1)) { anim =>
    anim.setInterpolator(new Expo.EaseOut)
    anim.setDuration(fadeInDuration.toMillis)
    anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener {
      override def onAnimationUpdate(animation: ValueAnimator): Unit = {
        view.setAlpha(animation.getAnimatedValue.asInstanceOf[java.lang.Float])
      }
    })
    anim.addListener(new AnimatorListenerAdapter {
      override def onAnimationStart(animation: Animator): Unit = {
        view.setVisible(true)
      }
    })
  }

  menuItem ! Some(CursorMenuItem.Send)

  override def onFinishInflate(): Unit = {
    super.onFinishInflate()

    setTextColor(getColor(R.color.text__primary_dark))

    controller.sendButtonVisible.on(Threading.Ui) {
      case true =>
        fadeInAnimator.start()
      case false =>
        fadeInAnimator.cancel()
        view.setVisible(false)
        view.setAlpha(0)
    }
  }
}
