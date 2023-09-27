package com.example.timeline

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timeline.Constants.dataStore
import com.example.timeline.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var calendarAdapter: CalenderAdapter
    private val weekAdapter by lazy { WeekAdapter() }
    private lateinit var dataStore: DataStore<Preferences>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        lifecycleScope.launch {
            dataStore = applicationContext.dataStore
            if (dataStore.data.first().contains(stringPreferencesKey("pattern"))) {
                binding.txtPattern.setText(getPatternFromDataStore().joinToString(" "))
                calendarAdapter =
                    CalenderAdapter(
                        filterCalendarDataByMonth(
                            createCalendarData(2023, getPatternFromDataStore()),
                            Calendar.getInstance().get(Calendar.MONTH)
                        )
                    )

                binding.calendarRecyclerView.adapter = calendarAdapter
                calendarAdapter.notifyDataSetChanged()

            } else {
                binding.txtPattern.setText("Pattern not set")
            }

        }


        lifecycleScope.launch {
            if (getPatternFromDataStore().isNotEmpty()) {
                calendarAdapter =
                    CalenderAdapter(
                        filterCalendarDataByMonth(
                            createCalendarData(2023, getPatternFromDataStore()),
                            Calendar.getInstance().get(Calendar.MONTH)
                        )
                    )
                binding.calendarRecyclerView.adapter = calendarAdapter
            }

        }


        weekAdapter.submitList(createWeekData(2023, Calendar.getInstance().get(Calendar.MONTH)))
        binding.recyclerDaysOfWeeks.layoutManager =
            GridLayoutManager(this, 7, GridLayoutManager.VERTICAL, false)
        binding.recyclerDaysOfWeeks.adapter = weekAdapter
        binding.MonthSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH))

        getSelectedMonth { position ->
            lifecycleScope.launch {
                if (getPatternFromDataStore().isNotEmpty()) {
                    calendarAdapter =
                        CalenderAdapter(
                            filterCalendarDataByMonth(
                                createCalendarData(2023, getPatternFromDataStore()),
                                position
                            )
                        )
                }

            }

            weekAdapter.submitList(createWeekData(2023, position))
            binding.recyclerDaysOfWeeks.layoutManager =
                GridLayoutManager(this, 7, GridLayoutManager.VERTICAL, false)
            lifecycleScope.launch {
                if (getPatternFromDataStore().isNotEmpty()) {
                    calendarAdapter.updateData(
                        filterCalendarDataByMonth(
                            createCalendarData(2023, getPatternFromDataStore()),
                            position
                        )
                    )
                    binding.calendarRecyclerView.adapter = calendarAdapter
                    calendarAdapter.notifyDataSetChanged()
                }

            }


            binding.recyclerDaysOfWeeks.adapter = weekAdapter

        }



        binding.btnApply.setOnClickListener {

            lifecycleScope.launch {
                savePattern("pattern", binding.txtPattern.text.toString())

                calendarAdapter =
                    CalenderAdapter(
                        filterCalendarDataByMonth(
                            createCalendarData(2023, getPatternFromDataStore()),
                            8
                        )
                    )

                // Update the adapter data and notify the RecyclerView of the changes
                calendarAdapter.updateData(
                    filterCalendarDataByMonth(
                        createCalendarData(2023, getPattern()), 8
                    )
                )

                calendarAdapter.notifyDataSetChanged()
                binding.calendarRecyclerView.adapter = calendarAdapter
            }


        }


    }

//    private fun createCalendarData(year: Int, pattern: List<String>): List<CalendarDay> {
//        val calendarDays = mutableListOf<CalendarDay>()
//        val calendar = Calendar.getInstance()
//        calendar.set(year, 0, 1)
//
//        val patterns = pattern
//
//        var patternIndex = 0
//        while (calendar.get(Calendar.YEAR) == year) {
//            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
//            val month = calendar.get(Calendar.MONTH)
//            // Assign the pattern based on the day of the week
//            val notePattern = patterns[patternIndex]
////            val notePattern = patterns[(startingPatternIndex + dayOfMonth - 1) % patterns.size]
//
//            calendarDays.add(
//                CalendarDay(
//                    dayOfMonth = dayOfMonth,
//                    notePattern = notePattern,
//                    month = month
//
//                )
//            )
//
//            // Move to the next day
//            calendar.add(Calendar.DAY_OF_MONTH, 1)
//            patternIndex = (patternIndex + 1) % patterns.size
//        }
//        Log.e("createCalendarData: ", calendarDays.toString())
//
//
//        return calendarDays
//    }
        // worked after current month
    private fun createCalendarData( year: Int,pattern: List<String>): List<CalendarDay> {
        val calendarDays = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()

        val currentMonth = calendar.get(Calendar.MONTH)


        val patterns = pattern

        var patternIndex = 0
        for (month in currentMonth until Calendar.DECEMBER + 1) {
            calendar.set(year, month, 1)
            while (calendar.get(Calendar.MONTH) == month) {
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                // Assign the pattern based on the day of the week
                val notePattern = patterns[patternIndex]

                calendarDays.add(
                    CalendarDay(
                        dayOfMonth = dayOfMonth,
                        notePattern = notePattern,
                        month = month
                    )
                )

                // Move to the next day
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                patternIndex = (patternIndex + 1) % patterns.size
            }
        }
        Log.e("createCalendarData: ", calendarDays.toString())

        return calendarDays
    }




    private fun filterCalendarDataByMonth(
        data: List<CalendarDay>,
        selectedMonth: Int
    ): List<CalendarDay> {

        return data.filter { it.month == selectedMonth }
    }

    private fun getPattern(): List<String> {
        val list = mutableListOf<String>()
        val inputData = binding.txtPattern.text.toString()
        val words = inputData.split("\\s+".toRegex()).toTypedArray()
        for (word in words) {
            if (word.isNotEmpty()) { // Check if the word is not empty
                list.add(word)
            }
        }
        Log.e("getPattern: ", list.toString())
        return list
    }

    fun getSelectedMonth(onItemSelected: (position: Int) -> Unit) {
        binding.MonthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.e("onItemSelected: ", position.toString())
                (parent!!.getChildAt(0) as TextView).setTextColor(Color.BLACK)
                onItemSelected(position)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun createWeekData(year: Int, month: Int): List<CalendarDayOfWeek> {

        val calendarDays = mutableListOf<CalendarDayOfWeek>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // Determine the first day of the selected month
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) + 1

        // Array of day names
        val dayNames = arrayOf("سبت", "حد", "اتنين", "تلات", "اربع", "خميس", "جمعه")

        // Add days of the week to the list, starting with the first day of the month
        for (i in 0 until 7) {
            val dayName = dayNames[(firstDayOfMonth + i - 1) % 7] // Ensure it loops back to Sunday
            calendarDays.add(CalendarDayOfWeek(dayName = dayName))
        }

        return calendarDays
    }

    private suspend fun savePattern(key: String, value: String) {
        dataStore = applicationContext.dataStore
        val dataStoreKey = stringPreferencesKey(key)
        dataStore.edit {
            it[dataStoreKey] = value
        }
    }

    private suspend fun getPatternFromDataStore(): List<String> {
        dataStore = applicationContext.dataStore
        val dataStoreKey = stringPreferencesKey("pattern")
        val preferences = dataStore.data.first()
        val pattern = preferences[dataStoreKey] ?: " "
        val list = mutableListOf<String>()
        val inputData = pattern
        val words = inputData.split("\\s+".toRegex()).toTypedArray()
        for (word in words) {
            if (word.isNotEmpty()) { // Check if the word is not empty
                list.add(word)
            }
        }
        Log.e("getPattern: ", list.toString())
        return list
    }

}