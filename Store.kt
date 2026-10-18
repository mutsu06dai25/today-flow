package jp.todayflow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Step(var place: String, var what: String, var done: Boolean)

/** 今日の流れを端末内に保存する。保存のたびにウィジェットも更新する。 */
object Store {
    private const val PREF = "today_flow"
    private const val KEY = "steps"

    fun load(c: Context): MutableList<Step> {
        val raw = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return mutableListOf()
        return try {
            val a = JSONArray(raw)
            MutableList(a.length()) { i ->
                val o = a.getJSONObject(i)
                Step(o.optString("place"), o.optString("what"), o.optBoolean("done"))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun save(c: Context, steps: List<Step>) {
        val a = JSONArray()
        steps.forEach {
            a.put(JSONObject().put("place", it.place).put("what", it.what).put("done", it.done))
        }
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
        FlowWidget.updateAll(c)
    }
}
