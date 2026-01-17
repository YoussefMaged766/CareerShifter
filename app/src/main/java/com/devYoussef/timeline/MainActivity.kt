package com.devYoussef.timeline

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.devYoussef.timeline.Constants.dataStore
import com.devYoussef.timeline.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var calendarAdapter: CalenderAdapter? = null
    private val weekAdapter by lazy { WeekAdapter() }
    private lateinit var dataStore: DataStore<Preferences>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply window insets for status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            WindowInsetsCompat.CONSUMED
        }

        lifecycleScope.launch {
            dataStore = applicationContext.dataStore
            // Check if pattern exists - if not, navigate to PatternActivity
            if (!dataStore.data.first()
                    .contains(stringPreferencesKey("pattern")) || !dataStore.data.first()
                    .contains(stringPreferencesKey("month"))
            ) {
                // Pattern not found, navigate to PatternActivity
                val intent = Intent(this@MainActivity, PatternActivity::class.java)
                startActivity(intent)
                finish()
                return@launch
            }

            // Pattern exists, load calendar
            val pattern = getPatternFromDataStore()
            val savedMonth = getMonthFromDataStore().toInt()
            val calendarData = createCalendarData2(
                Calendar.getInstance().get(Calendar.YEAR),
                pattern,
                Calendar.getInstance().get(Calendar.MONTH),
                savedMonth
            )
            calendarAdapter = CalenderAdapter(calendarData)

            binding.calendarRecyclerView.adapter = calendarAdapter
            calendarAdapter?.notifyDataSetChanged()
            Log.e("onCreate: ", getMonthFromDataStore())
            
            // Set up month spinner callback after calendarAdapter is initialized
            setupMonthSpinner()
            // Set selection after listener is set up to avoid triggering callback prematurely
            binding.MonthSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH), false)
        }


        weekAdapter.submitList(createWeekData(Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)))
        binding.recyclerDaysOfWeeks.layoutManager =
            GridLayoutManager(this, 7, GridLayoutManager.VERTICAL, false)
        binding.recyclerDaysOfWeeks.adapter = weekAdapter
        // Don't set selection yet - will be set after calendarAdapter is initialized

        // Edit pattern button - navigate to PatternActivity
        binding.btnEditPattern.setOnClickListener {
            val intent = Intent(this, PatternActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
        // Reload calendar when returning from PatternActivity
        lifecycleScope.launch {
            dataStore = applicationContext.dataStore
            if (dataStore.data.first()
                    .contains(stringPreferencesKey("pattern")) && dataStore.data.first()
                    .contains(stringPreferencesKey("month"))
            ) {
                val pattern = getPatternFromDataStore()
                if (pattern.isNotEmpty() && pattern[0] != " ") {
                    val savedMonth = getMonthFromDataStore().toInt()
                    val calendarData = createCalendarData2(
                        Calendar.getInstance().get(Calendar.YEAR),
                        pattern,
                        Calendar.getInstance().get(Calendar.MONTH),
                        savedMonth
                    )
                    calendarAdapter = CalenderAdapter(calendarData)
                    binding.calendarRecyclerView.adapter = calendarAdapter
                    calendarAdapter?.notifyDataSetChanged()
                    
                    // Ensure month spinner is set up
                    if (binding.MonthSpinner.onItemSelectedListener == null) {
                        setupMonthSpinner()
                    }
                    // Update selection without triggering callback
                    binding.MonthSpinner.setSelection(Calendar.getInstance().get(Calendar.MONTH), false)
                }
            }
        }
    }

    // worked after current month
    private fun createCalendarData(year: Int, pattern: List<String>): List<CalendarDay> {
        val calendarDays = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()

        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)


        val patterns = pattern

        var patternIndex = 0
        for (month in currentMonth until Calendar.DECEMBER + 1) {
            calendar.set(year, month, if (month == currentMonth) currentDayOfMonth else 1)
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

    private suspend fun createCalendarData2(
        year: Int,
        pattern: List<String>,
        selectedMonth: Int,
        savedMonth: Int
    ): List<CalendarDay> {
        val calendarDays = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val patterns = pattern
        if (patterns.isEmpty()) {
            return calendarDays
        }

        // Check if selected month is the current month
        val isCurrentMonth = (year == currentYear && selectedMonth == currentMonth)
        val isSavedMonth = (year == currentYear && selectedMonth == savedMonth)

        // Get the last day of the selected month
        calendar.set(year, selectedMonth, 1)
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Calculate starting pattern index for the selected month
        var startingPatternIndex = 0
        
        if (isCurrentMonth && currentDayOfMonth > 1) {
            // For current month: apply pattern in reverse from current day to day 1, then forward from current day
            
            // Step 1: Apply pattern backwards from current day to day 1
            var patternIndex = 0
            for (day in currentDayOfMonth downTo 1) {
                val notePattern = patterns[patternIndex % patterns.size]
                calendarDays.add(
                    CalendarDay(
                        dayOfMonth = day,
                        notePattern = notePattern,
                        month = selectedMonth
                    )
                )
                patternIndex++
            }
            
            // Reverse the list so days 1 to currentDay are in correct order
            calendarDays.reverse()
            
            // Step 2: Apply pattern forward from current day + 1 to end of month
            // Continue pattern from where current day left off (patternIndex = 1, since current day used pattern[0])
            patternIndex = 1
            for (day in (currentDayOfMonth + 1)..lastDayOfMonth) {
                val notePattern = patterns[patternIndex % patterns.size]
                calendarDays.add(
                    CalendarDay(
                        dayOfMonth = day,
                        notePattern = notePattern,
                        month = selectedMonth
                    )
                )
                patternIndex++
            }
        } else {
            // For other months: calculate total days from saved month's current day to selected month
            // This ensures pattern continues sequentially across months
            
            var totalDays = 0
            
            if (isSavedMonth && currentDayOfMonth > 1) {
                // If selected month is the saved month, calculate from current day
                // Days from current day to end of saved month
                calendar.set(currentYear, savedMonth, 1)
                val lastDayOfSavedMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                totalDays = lastDayOfSavedMonth - currentDayOfMonth + 1 // +1 to include current day
                
                // Calculate days in all months between saved month and selected month
                for (month in (savedMonth + 1) until selectedMonth) {
                    calendar.set(year, month, 1)
                    totalDays += calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
            } else if (selectedMonth > savedMonth || (year > currentYear)) {
                // For months after saved month: calculate from saved month's current day
                calendar.set(currentYear, savedMonth, 1)
                val lastDayOfSavedMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                totalDays = lastDayOfSavedMonth - currentDayOfMonth + 1 // +1 to include current day
                
                // Calculate days in all months between saved month and selected month
                for (month in (savedMonth + 1) until selectedMonth) {
                    calendar.set(year, month, 1)
                    totalDays += calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
            }
            // For past months (selectedMonth < savedMonth), totalDays remains 0, pattern starts from beginning
            
            // Starting pattern index for selected month (continues from where previous month ended)
            startingPatternIndex = totalDays % patterns.size
            
            // Apply pattern starting from calculated index
            var patternIndex = startingPatternIndex
            for (day in 1..lastDayOfMonth) {
                val notePattern = patterns[patternIndex % patterns.size]
                calendarDays.add(
                    CalendarDay(
                        dayOfMonth = day,
                        notePattern = notePattern,
                        month = selectedMonth
                    )
                )
                patternIndex++
            }
        }

        Log.e("createCalendarData2: ", "Month: $selectedMonth, Days: ${calendarDays.size}, StartIndex: $startingPatternIndex")
        return calendarDays
    }


    private fun filterCalendarDataByMonth(
        data: List<CalendarDay>,
        selectedMonth: Int
    ): List<CalendarDay> {

        return data.filter { it.month == selectedMonth }
    }


    private fun setupMonthSpinner() {
        binding.MonthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.e("onItemSelected: ", position.toString())
                (parent!!.getChildAt(0) as TextView).setTextColor(Color.BLACK)
                
                weekAdapter.submitList(createWeekData(Calendar.getInstance().get(Calendar.YEAR), position))
                binding.recyclerDaysOfWeeks.layoutManager =
                    GridLayoutManager(this@MainActivity, 7, GridLayoutManager.VERTICAL, false)
                binding.recyclerDaysOfWeeks.adapter = weekAdapter
                
                lifecycleScope.launch {
                    val adapter = calendarAdapter
                    val pattern = getPatternFromDataStore()
                    if (pattern.isNotEmpty() && adapter != null) {
                        // Get saved month to calculate pattern continuation
                        val savedMonth = getMonthFromDataStore().toInt()
                        // Create calendar data for the selected month (position)
                        val calendarData = createCalendarData2(
                            Calendar.getInstance().get(Calendar.YEAR),
                            pattern,
                            position,
                            savedMonth
                        )
                        adapter.updateData(calendarData)
                        binding.calendarRecyclerView.adapter = adapter
                        adapter.notifyDataSetChanged()
                    }
                }
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


    private suspend fun getMonthFromDataStore(): String {
        dataStore = applicationContext.dataStore
        val dataStoreKey = stringPreferencesKey("month")
        val preferences = dataStore.data.first()
        val month = preferences[dataStoreKey] ?: " "
        return month
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