package jp.todayflow

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import java.util.Calendar
import java.util.Collections

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var progress: TextView
    private lateinit var bar: ProgressBar
    private lateinit var empty: TextView
    private lateinit var dateView: TextView
    private lateinit var inPlace: EditText
    private lateinit var inWhat: EditText
    private var steps = mutableListOf<Step>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list = findViewById(R.id.list)
        progress = findViewById(R.id.progress)
        bar = findViewById(R.id.bar)
        empty = findViewById(R.id.empty)
        dateView = findViewById(R.id.date)
        inPlace = findViewById(R.id.in_place)
        inWhat = findViewById(R.id.in_what)

        findViewById<View>(R.id.add_btn).setOnClickListener { add() }
        inWhat.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { add(); true } else false
        }
        findViewById<View>(R.id.reset).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("今日の流れを全部消しますか？")
                .setPositiveButton("消す") { _, _ -> steps.clear(); commit() }
                .setNegativeButton("やめる", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        steps = Store.load(this)
        val c = Calendar.getInstance()
        val wd = "日月火水木金土"[c.get(Calendar.DAY_OF_WEEK) - 1]
        dateView.text = "${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.DAY_OF_MONTH)} ${wd}曜日"
        render()
    }

    private fun add() {
        val w = inWhat.text.toString().trim()
        if (w.isEmpty()) { inWhat.requestFocus(); return }
        // 場所は残す（同じ場所での作業が続くことが多いため）
        steps.add(Step(inPlace.text.toString().trim(), w, false))
        inWhat.setText("")
        commit()
    }

    private fun commit() {
        Store.save(this, steps)
        render()
    }

    private fun render() {
        list.removeAllViews()
        val nowIdx = steps.indexOfFirst { !it.done }
        empty.visibility = if (steps.isEmpty()) View.VISIBLE else View.GONE

        steps.forEachIndexed { i, s ->
            val v = layoutInflater.inflate(R.layout.item_step, list, false)
            val card = v.findViewById<View>(R.id.card)
            val check = v.findViewById<TextView>(R.id.check)
            val placeRow = v.findViewById<View>(R.id.place_row)
            val place = v.findViewById<TextView>(R.id.place)
            val tag = v.findViewById<View>(R.id.now_tag)
            val what = v.findViewById<TextView>(R.id.what)
            val up = v.findViewById<TextView>(R.id.up)
            val down = v.findViewById<TextView>(R.id.down)
            val arrow = v.findViewById<ImageView>(R.id.arrow)
            val isNow = i == nowIdx

            card.setBackgroundResource(if (isNow) R.drawable.card_now_bg else R.drawable.card_bg)
            card.alpha = if (s.done) 0.55f else 1f
            check.setBackgroundResource(
                when {
                    s.done -> R.drawable.check_on
                    isNow -> R.drawable.check_now
                    else -> R.drawable.check_off
                }
            )
            check.setTextColor(if (s.done) getColor(R.color.on_accent) else 0x00000000)

            place.text = s.place
            place.visibility = if (s.place.isEmpty()) View.GONE else View.VISIBLE
            tag.visibility = if (isNow) View.VISIBLE else View.GONE
            placeRow.visibility = if (s.place.isEmpty() && !isNow) View.GONE else View.VISIBLE

            what.text = s.what
            what.paintFlags = if (s.done) what.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else what.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            what.setTextColor(getColor(if (s.done) R.color.muted else R.color.ink))

            check.setOnClickListener { s.done = !s.done; commit() }
            card.setOnLongClickListener { edit(i); true }

            up.alpha = if (i == 0) 0.25f else 1f
            down.alpha = if (i == steps.size - 1) 0.25f else 1f
            up.setOnClickListener { if (i > 0) { Collections.swap(steps, i, i - 1); commit() } }
            down.setOnClickListener { if (i < steps.size - 1) { Collections.swap(steps, i, i + 1); commit() } }
            v.findViewById<View>(R.id.edit).setOnClickListener { edit(i) }

            arrow.visibility = if (i == steps.size - 1) View.GONE else View.VISIBLE
            arrow.setColorFilter(getColor(if (s.done) R.color.accent else R.color.line))

            list.addView(v)
        }

        val done = steps.count { it.done }
        progress.text = "$done / ${steps.size} 完了"
        bar.progress = if (steps.isEmpty()) 0 else done * 100 / steps.size
    }

    private fun edit(i: Int) {
        val s = steps[i]
        val pad = (20 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val p = EditText(this).apply { hint = "場所"; setText(s.place); setSingleLine() }
        val w = EditText(this).apply { hint = "やること"; setText(s.what); setSingleLine() }
        box.addView(p)
        box.addView(w)
        AlertDialog.Builder(this)
            .setTitle("編集")
            .setView(box)
            .setPositiveButton("保存") { _, _ ->
                val t = w.text.toString().trim()
                if (t.isNotEmpty()) {
                    s.place = p.text.toString().trim()
                    s.what = t
                    commit()
                }
            }
            .setNeutralButton("削除") { _, _ -> steps.removeAt(i); commit() }
            .setNegativeButton("やめる", null)
            .show()
    }
}
