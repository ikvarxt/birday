package com.minar.birday.fragments.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.minar.birday.R
import com.minar.birday.databinding.LayoutDateSelectorBinding
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarMonth
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import com.nlf.calendar.util.LunarUtil
import java.time.LocalDate
import kotlin.math.abs

class LunarDateSelector(
    context: Context,
    placeholder: Lunar,
) {

    private val binding = LayoutDateSelectorBinding.inflate(LayoutInflater.from(context))
    val view get() = binding.root

    // temporary data
    private var curYear: LunarYear = LunarYear(placeholder.year)
    private var curMonth: LunarMonth = curYear.getMonth(placeholder.month)
    private var curMonthList: List<LunarMonth> = curYear.monthsInYear
    private var curDayIndex: Int = placeholder.day - 1

    private val curDay get() = curDayIndex + 1

    // wheel change listener
    private val yearChanged = { _: NumberPicker, _: Int, newVal: Int ->
        curYear = lunarYearList[newVal]

        curMonthList = curYear.monthsInYear
        binding.monthPicker.setupMonthPicker()
    }

    private val monthChanged = { _: NumberPicker, _: Int, index: Int ->
        curMonth = curMonthList[index]
        binding.dayPicker.setupDayPicker()
    }

    private val dayChanged = { _: NumberPicker, _: Int, index: Int -> curDayIndex = index }

    init {
        binding.apply {
            yearPicker.fillData(yearStringList)
            yearPicker.value = curYear.year - yearRange.first
            yearPicker.setOnValueChangedListener(yearChanged)

            monthPicker.setupMonthPicker()
            monthPicker.setOnValueChangedListener(monthChanged)

            dayPicker.setupDayPicker()
            dayPicker.value = curDayIndex
            dayPicker.setOnValueChangedListener(dayChanged)
        }
    }

    private fun NumberPicker.setupMonthPicker() {
        fillData(curMonthStringList())

        val new = curMonthList.indexOfFirst { it.month == curMonth.month }

        // get the new month index or prev value
        val newMonthIndex = if (new != -1) new else value
        value = newMonthIndex.coerceAtMost(maxValue)
        // reset current month in case year changed affect that
        curMonth = curMonthList[value]
    }

    private fun curMonthStringList() = curMonthList.map {
        val month = it.month
        (if (it.isLeap) "闰" else "") + LunarUtil.MONTH[abs(month)] + "月"
    }

    private fun NumberPicker.fillData(dataList: List<String>) {
        // first remove cache to reset maxValue, avoid crash in ensureCachedScrollSelectorValue()
        displayedValues = null
        value = value.coerceAtMost(dataList.lastIndex)
        maxValue = dataList.lastIndex
        wrapSelectorWheel = false
        displayedValues = dataList.toTypedArray()
    }

    private fun NumberPicker.setupDayPicker() {
        if (displayedValues == null) fillData(dayListString)

        val lastDayIndex = (if (curMonth.isLeap) 30 else 29) - 1

        value = value.coerceAtMost(lastDayIndex)
        maxValue = lastDayIndex
    }

    fun getSelection(): LocalDate {
        // exclude leap month
        val lunar = Lunar.fromYmd(curYear.year, abs(curMonth.month), curDay)
        val solar = lunar.solar
        return LocalDate.of(solar.year, solar.month, solar.day)
    }

    companion object {

        private val yearRange = (1900..2100)
        private val yearList: List<Int> = buildList { yearRange.forEach { add(it) } }
        private val lunarYearList = yearList.map { LunarYear.fromYear(it) }
        private val yearStringList = lunarYearList.map { "${it.chineseName()} ${it.ganZhi}" }

        private val dayListString: List<String> = (1..30).map { LunarUtil.DAY[it] }

        private fun LunarYear.chineseName(): String {
            val year = year.toString()

            val s = StringBuilder()
            var i = 0
            while (i < year.length) {
                s.append(LunarUtil.NUMBER[year[i].code - '0'.code])
                i++
            }
            return s.toString()
        }

        fun build(
            context: Context,
            date: LocalDate = LocalDate.now(),
            onSelect: (Long) -> Unit,
        ): AlertDialog {
            return MaterialAlertDialogBuilder(context).apply {

                setTitle(context.getString(R.string.insert_date_hint))
                val solar = Solar(date.year, date.monthValue, date.dayOfMonth)
                val selector = LunarDateSelector(context, solar.lunar)
                setView(selector.view)

                setPositiveButton(android.R.string.ok) { _, _ ->
                    onSelect(selector.getSelection().toEpochDay() * 24 * 3600 * 1000)
                }
            }.create()
        }

        fun show(
            context: Context,
            date: LocalDate = LocalDate.now(),
            onSelect: (Long) -> Unit
        ) {
            build(context, date, onSelect).show()
        }
    }
}