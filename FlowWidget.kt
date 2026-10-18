package jp.todayflow

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** ホーム画面ウィジェット：「いまここ」と次の数件を表示し、ボタンで現在の項目を完了にできる。 */
class FlowWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { m.updateAppWidget(it, build(c)) }
    }

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        if (intent.action == ACTION_DONE) {
            val steps = Store.load(c)
            val idx = steps.indexOfFirst { !it.done }
            if (idx >= 0) {
                steps[idx].done = true
                Store.save(c, steps)
            }
        }
    }

    companion object {
        const val ACTION_DONE = "jp.todayflow.ACTION_DONE"

        fun updateAll(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, FlowWidget::class.java))
            ids.forEach { m.updateAppWidget(it, build(c)) }
        }

        private fun build(c: Context): RemoteViews {
            val steps = Store.load(c)
            val v = RemoteViews(c.packageName, R.layout.widget)
            val done = steps.count { it.done }
            val idx = steps.indexOfFirst { !it.done }

            v.setTextViewText(R.id.w_prog, if (steps.isEmpty()) "" else "$done / ${steps.size}")

            when {
                steps.isEmpty() -> {
                    v.setTextViewText(R.id.w_place, "")
                    v.setViewVisibility(R.id.w_place, View.GONE)
                    v.setTextViewText(R.id.w_what, "タップして今日の流れを作る")
                    v.setViewVisibility(R.id.w_next, View.GONE)
                    v.setViewVisibility(R.id.w_done, View.GONE)
                }
                idx < 0 -> {
                    v.setViewVisibility(R.id.w_place, View.GONE)
                    v.setTextViewText(R.id.w_what, "今日の流れはすべて完了")
                    v.setViewVisibility(R.id.w_next, View.GONE)
                    v.setViewVisibility(R.id.w_done, View.GONE)
                }
                else -> {
                    val now = steps[idx]
                    val label = if (now.place.isEmpty()) "いまここ" else "いまここ｜${now.place}"
                    v.setTextViewText(R.id.w_place, label)
                    v.setViewVisibility(R.id.w_place, View.VISIBLE)
                    v.setTextViewText(R.id.w_what, now.what)
                    val next = steps.drop(idx + 1).take(3).joinToString("\n") {
                        if (it.place.isEmpty()) "↓ ${it.what}" else "↓ ${it.what}（${it.place}）"
                    }
                    v.setTextViewText(R.id.w_next, next)
                    v.setViewVisibility(R.id.w_next, if (next.isEmpty()) View.GONE else View.VISIBLE)
                    v.setViewVisibility(R.id.w_done, View.VISIBLE)
                }
            }

            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_root, open)

            val doneIntent = Intent(c, FlowWidget::class.java).setAction(ACTION_DONE)
            val donePi = PendingIntent.getBroadcast(
                c, 1, doneIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_done, donePi)
            return v
        }
    }
}
