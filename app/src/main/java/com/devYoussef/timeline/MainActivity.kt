package com.devYoussef.timeline

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.devYoussef.timeline.Constants.dataStore
import com.devYoussef.timeline.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var calendarAdapter: CalenderAdapter? = null
    private val weekAdapter by lazy { WeekAdapter() }
    private var snapHelper: SnapHelper? = null
    private lateinit var dataStore: DataStore<Preferences>
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)


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
            
            // Load all 12 months starting from January (month 0)
            val calendarData = createMultiMonthCalendarData(
                currentYear,
                pattern,
                0, // Start from January
                savedMonth,
                12 // Show all 12 months
            )
            calendarAdapter = CalenderAdapter(calendarData)

            // Setup GridLayoutManager with span size lookup for month headers
            val layoutManager = GridLayoutManager(this@MainActivity, 7, GridLayoutManager.VERTICAL, false)
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (calendarAdapter?.calendarItems?.get(position)) {
                        is CalendarItem.MonthHeader -> 7 // Full width for month headers
                        else -> 1 // Single column for days and empty cells
                    }
                }
            }
            binding.calendarRecyclerView.layoutManager = layoutManager

            binding.calendarRecyclerView.adapter = calendarAdapter
            calendarAdapter?.notifyDataSetChanged()
            Log.e("onCreate: ", getMonthFromDataStore())
            
            // Find the first actual day in the calendar to sync week header
            val firstVisibleMonth = findFirstMonthInCalendar(calendarData)
            
            // Update month and year text views
            updateMonthTextView(firstVisibleMonth)
            updateYearTextView(currentYear)
            
            // Initialize week adapter with the first visible month
            weekAdapter.submitList(createWeekData(currentYear, firstVisibleMonth))
            binding.recyclerDaysOfWeeks.layoutManager =
                GridLayoutManager(this@MainActivity, 7, GridLayoutManager.VERTICAL, false)
            binding.recyclerDaysOfWeeks.adapter = weekAdapter
            
            // Add scroll listener to detect visible month
            setupScrollListener()
            
            // Don't use snap scrolling - it interferes with manual scrolling
            // setupSnapScrolling()

            // Scroll to current day after layout is complete (only happens in onCreate)
            binding.calendarRecyclerView.post {
                scrollToCurrentDay()
            }
        }

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
                    val calendarData = createMultiMonthCalendarData(
                        currentYear,
                        pattern,
                        0, // Start from January
                        savedMonth,
                        12 // Show all 12 months
                    )
                    calendarAdapter = CalenderAdapter(calendarData)

                    // Setup GridLayoutManager with span size lookup for month headers
                    val layoutManager = GridLayoutManager(this@MainActivity, 7, GridLayoutManager.VERTICAL, false)
                    layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (calendarAdapter?.calendarItems?.get(position)) {
                                is CalendarItem.MonthHeader -> 7 // Full width for month headers
                                else -> 1 // Single column for days and empty cells
                            }
                        }
                    }
                    binding.calendarRecyclerView.layoutManager = layoutManager

                    binding.calendarRecyclerView.adapter = calendarAdapter
                    calendarAdapter?.notifyDataSetChanged()
                    
                    // Find the first actual day in the calendar to sync week header
                    val firstVisibleMonth = findFirstMonthInCalendar(calendarData)
                    
                    // Update month and year text views
                    updateMonthTextView(firstVisibleMonth)
                    updateYearTextView(currentYear)
                    
                    // Initialize week adapter with the first visible month
                    weekAdapter.submitList(createWeekData(currentYear, firstVisibleMonth))
                    binding.recyclerDaysOfWeeks.adapter = weekAdapter
                    
                    // Setup scroll listener
                    setupScrollListener()
                    // Don't use snap scrolling - it interferes with manual scrolling
                    // setupSnapScrolling()

                    // Don't scroll to current day in onResume - only in onCreate
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

    private suspend fun createMultiMonthCalendarData(
        startYear: Int,
        pattern: List<String>,
        startMonth: Int,
        savedMonth: Int,
        numberOfMonths: Int
    ): List<CalendarItem> {
        val allCalendarItems = mutableListOf<CalendarItem>()
        
        // Calculate how many rows fit on screen (approximately 6 rows for full screen)
        val rowsPerScreen = 6
        val cellsPerScreen = rowsPerScreen * 7 // 42 cells per screen
        val calendar = Calendar.getInstance()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        
        val patterns = pattern
        if (patterns.isEmpty()) {
            return allCalendarItems
        }
        
        // Calculate starting pattern index for January (month 0)
        // We need to calculate backwards from saved month's current day to January
        var totalDaysFromStart = 0
        val savedYear = currentYear
        
        // Calculate days from January 1st to saved month's current day
        // This will be negative (going backwards), so we'll calculate it differently
        // For months before the current month, we'll calculate patterns in reverse
        
        // First, calculate days from saved month's current day to end of saved month
        calendar.set(savedYear, savedMonth, 1)
        val lastDayOfSavedMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysFromCurrentToEndOfSavedMonth = lastDayOfSavedMonth - currentDayOfMonth + 1
        
        // Calculate days from start of saved month to current day (excluding current day)
        val daysFromStartToCurrent = currentDayOfMonth - 1
        
        // For months before saved month, we need to calculate backwards
        // Start pattern index for January will be calculated based on how many days
        // we need to go backwards from the saved month's current day
        
        // Calculate total days from January 1st to saved month's current day
        for (month in 0 until savedMonth) {
            calendar.set(savedYear, month, 1)
            totalDaysFromStart += calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        totalDaysFromStart += daysFromStartToCurrent // Days from start of saved month to current day
        
        // Pattern index for January 1st: go backwards from saved month's current day
        // The saved month's current day should have pattern[0] (after reverse calculation)
        // So we need to calculate: what pattern should January 1st have?
        // We'll calculate this when processing each month
        var patternIndex = 0
        
        // Generate calendar data for multiple months
        var year = startYear
        var month = startMonth
        
        for (i in 0 until numberOfMonths) {
            // Add month header at the beginning of each month
            allCalendarItems.add(CalendarItem.MonthHeader(month, year))

            calendar.set(year, month, 1)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val isCurrentMonthInRealTime = (year == currentYear && month == currentMonth)
            
            // Calculate first day of week for the month to add leading empty cells
            calendar.set(year, month, 1)
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // Convert to 0-based offset from Saturday (سبت) since week header starts with Saturday
            // Saturday = 0, Sunday = 1, Monday = 2, etc.
            val firstDayOffset = (firstDayOfWeek - Calendar.SATURDAY + 7) % 7
            
            // Add leading empty cells to align first day of month
            for (empty in 0 until firstDayOffset) {
                allCalendarItems.add(CalendarItem.Empty)
            }
            
            if (isCurrentMonthInRealTime && currentDayOfMonth > 1) {
                // Current month - pattern starts from current day
                // Apply pattern in reverse order to days before current day
                // Current day gets patterns[0], day before gets patterns[size-1], etc.
                val reverseDays = mutableListOf<CalendarDay>()
                
                // Build days from currentDayOfMonth down to 1
                // Current day (currentDayOfMonth) gets patterns[0]
                // Day before (currentDayOfMonth - 1) gets patterns[size-1] (last element)
                // Day before that (currentDayOfMonth - 2) gets patterns[size-2], etc.
                for (day in currentDayOfMonth downTo 1) {
                    val daysFromCurrent = currentDayOfMonth - day
                    // For current day (daysFromCurrent = 0), use patterns[0]
                    // For day before (daysFromCurrent = 1), use patterns[size-1]
                    // For day before that (daysFromCurrent = 2), use patterns[size-2], etc.
                    // Formula: (size - daysFromCurrent) % size, ensure non-negative
                    val reversePatternIndex = ((patterns.size - daysFromCurrent) % patterns.size + patterns.size) % patterns.size
                    val notePattern = patterns[reversePatternIndex]
                    reverseDays.add(
                        CalendarDay(
                            dayOfMonth = day,
                            notePattern = notePattern,
                            month = month
                        )
                    )
                }
                
                // Reverse the list so days 1 to currentDayOfMonth are in correct order
                reverseDays.reverse()
                
                // Add days as CalendarItem.Day (now in correct order: day 1 to currentDayOfMonth)
                reverseDays.forEach { day: CalendarDay ->
                    allCalendarItems.add(CalendarItem.Day(day))
                }
                
                // After applying pattern for days 1 to currentDayOfMonth,
                // the current day has patternIndex = 0, so next day uses patternIndex = 1
                patternIndex = 1 % patterns.size
                
                // Add remaining days of current month
                for (day in (currentDayOfMonth + 1)..lastDayOfMonth) {
                    val notePattern = patterns[patternIndex % patterns.size]
                    allCalendarItems.add(
                        CalendarItem.Day(
                            CalendarDay(
                                dayOfMonth = day,
                                notePattern = notePattern,
                                month = month
                            )
                        )
                    )
                    patternIndex = (patternIndex + 1) % patterns.size
                }
            } else if (month < currentMonth || (year < currentYear)) {
                // Month before current month - calculate pattern backwards
                // Calculate total days from end of this month to saved month's current day
                var daysFromEndOfThisMonthToCurrent = 0
                
                // Add days from months between this month and saved month
                for (m in (month + 1) until savedMonth) {
                    calendar.set(year, m, 1)
                    daysFromEndOfThisMonthToCurrent += calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                
                // Add days from start of saved month to current day
                daysFromEndOfThisMonthToCurrent += (currentDayOfMonth - 1)
                
                // Pattern index for last day of this month: go backwards from saved month's current day
                // Saved month's current day has pattern[0] (after reverse)
                // Last day of this month is (daysFromEndOfThisMonthToCurrent + 1) days before current day
                // So pattern index for last day = (patterns.size - (daysFromEndOfThisMonthToCurrent + 1)) % patterns.size
                var monthPatternIndex = ((patterns.size - (daysFromEndOfThisMonthToCurrent + 1)) % patterns.size + patterns.size) % patterns.size
                
                // Build days in reverse order first, then add them in correct order
                val monthDays = mutableListOf<CalendarDay>()
                for (day in lastDayOfMonth downTo 1) {
                    val notePattern = patterns[monthPatternIndex % patterns.size]
                    monthDays.add(
                        CalendarDay(
                            dayOfMonth = day,
                            notePattern = notePattern,
                            month = month
                        )
                    )
                    monthPatternIndex = (monthPatternIndex - 1 + patterns.size) % patterns.size
                }
                
                // Reverse to get correct order (day 1 to lastDayOfMonth)
                monthDays.reverse()
                
                // Add days to calendar items
                monthDays.forEach { day ->
                    allCalendarItems.add(CalendarItem.Day(day))
                }
                
                // Update patternIndex for next month (continue backwards)
                patternIndex = (monthPatternIndex + patterns.size) % patterns.size
            } else {
                // Month after current month - apply pattern normally forward
                // Calculate pattern index based on days from saved month's current day
                var daysFromSaved = 0
                
                // Days from saved month's current day to end of saved month
                daysFromSaved += daysFromCurrentToEndOfSavedMonth
                
                // Add days from months between saved month and this month
                for (m in (savedMonth + 1) until month) {
                    calendar.set(year, m, 1)
                    daysFromSaved += calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                
                // Pattern index starts from 1 (since current day used pattern[0])
                patternIndex = (daysFromSaved % patterns.size)
                
                // Apply pattern forward
                for (day in 1..lastDayOfMonth) {
                    val notePattern = patterns[patternIndex % patterns.size]
                    allCalendarItems.add(
                        CalendarItem.Day(
                            CalendarDay(
                                dayOfMonth = day,
                                notePattern = notePattern,
                                month = month
                            )
                        )
                    )
                    patternIndex = (patternIndex + 1) % patterns.size
                }
            }
            
            // Calculate total cells used for this month (leading empty + days)
            val totalCellsThisMonth = firstDayOffset + lastDayOfMonth
            
            // Add trailing empty cells to fill the screen
            val remainingCells = cellsPerScreen - (totalCellsThisMonth % cellsPerScreen)
            if (remainingCells < cellsPerScreen) {
                // Add empty cells to fill to next screen boundary
                for (empty in 0 until remainingCells) {
                    allCalendarItems.add(CalendarItem.Empty)
                }
            }
            
            // Add 2 empty rows (14 cells) between months, except after the last month
            if (i < numberOfMonths - 1) {
//                for (empty in 0 until 14) {
//                    allCalendarItems.add(CalendarItem.Empty)
//                }
            }
            
            // Move to next month
            month++
            if (month > 11) {
                month = 0
                year++
            }
        }
        
        return allCalendarItems
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


    private fun updateMonthTextView(month: Int) {
        val months = resources.getStringArray(R.array.months)
        if (month >= 0 && month < months.size) {
            binding.MonthTextView.text = months[month]
        }
    }
    
    private fun updateYearTextView(year: Int) {
        binding.YearTextView.text = year.toString()
    }
    
    private fun setupSnapScrolling() {
        // Only attach if not already attached
        if (snapHelper != null) {
            return
        }
        
        // Create a custom snap helper that snaps to first day of each month
        snapHelper = object : SnapHelper() {
            private fun findFirstDayOfMonth(position: Int, adapter: CalenderAdapter): Int {
                if (position < 0 || position >= adapter.calendarItems.size) return -1
                
                val currentItem = adapter.calendarItems[position]
                if (currentItem is CalendarItem.Day) {
                    val currentMonth = currentItem.calendarDay.month
                    // Look backwards to find first day of this month
                    for (i in position downTo 0) {
                        val item = adapter.calendarItems[i]
                        if (item is CalendarItem.Day) {
                            if (item.calendarDay.month != currentMonth) {
                                // Found previous month, return next position (first day of current month)
                                return i + 1
                            }
                        } else if (item is CalendarItem.Empty && i < position) {
                            // Skip empty cells
                            continue
                        }
                    }
                    // If we reached position 0, this is the first month
                    return 0
                }
                return -1
            }
            
            override fun calculateDistanceToFinalSnap(
                layoutManager: RecyclerView.LayoutManager,
                targetView: android.view.View
            ): IntArray? {
                val out = IntArray(2)
                val gridLayoutManager = layoutManager as? GridLayoutManager ?: return null
                val position = gridLayoutManager.getPosition(targetView)
                val adapter = calendarAdapter ?: return null
                
                if (position < 0 || position >= adapter.calendarItems.size) {
                    return out
                }
                
                // Find first day of month for this position
                val firstDayPosition = findFirstDayOfMonth(position, adapter)
                if (firstDayPosition >= 0) {
                    val firstDayView = gridLayoutManager.findViewByPosition(firstDayPosition)
                    if (firstDayView != null) {
                        out[0] = 0
                        out[1] = firstDayView.top - gridLayoutManager.paddingTop
                    }
                }
                
                return out
            }
            
            override fun findSnapView(layoutManager: RecyclerView.LayoutManager): android.view.View? {
                val gridLayoutManager = layoutManager as? GridLayoutManager ?: return null
                val firstVisible = gridLayoutManager.findFirstVisibleItemPosition()
                val adapter = calendarAdapter ?: return null
                
                if (firstVisible < 0 || firstVisible >= adapter.calendarItems.size) {
                    return null
                }
                
                // Find first day of month
                val firstDayPosition = findFirstDayOfMonth(firstVisible, adapter)
                if (firstDayPosition >= 0 && firstDayPosition < adapter.calendarItems.size) {
                    return gridLayoutManager.findViewByPosition(firstDayPosition)
                }
                
                return null
            }
            
            override fun findTargetSnapPosition(
                layoutManager: RecyclerView.LayoutManager,
                velocityX: Int,
                velocityY: Int
            ): Int {
                val gridLayoutManager = layoutManager as? GridLayoutManager ?: return RecyclerView.NO_POSITION
                val adapter = calendarAdapter ?: return RecyclerView.NO_POSITION
                val currentPosition = gridLayoutManager.findFirstVisibleItemPosition()
                
                if (currentPosition < 0 || currentPosition >= adapter.calendarItems.size) {
                    return RecyclerView.NO_POSITION
                }
                
                val scrollingDown = velocityY > 0
                val currentFirstDay = findFirstDayOfMonth(currentPosition, adapter)
                
                if (scrollingDown) {
                    // Find next month's first day
                    val currentItem = adapter.calendarItems.getOrNull(currentFirstDay)
                    if (currentItem is CalendarItem.Day) {
                        val currentMonth = currentItem.calendarDay.month
                        for (i in (currentFirstDay + 1) until adapter.calendarItems.size) {
                            val item = adapter.calendarItems[i]
                            if (item is CalendarItem.Day && item.calendarDay.month != currentMonth) {
                                return i
                            }
                        }
                    }
                } else {
                    // Find previous month's first day
                    for (i in currentFirstDay downTo 0) {
                        val item = adapter.calendarItems[i]
                        if (item is CalendarItem.Day) {
                            val firstDay = findFirstDayOfMonth(i, adapter)
                            if (firstDay < currentFirstDay) {
                                return firstDay
                            }
                        }
                    }
                }
                
                return currentFirstDay
            }
        }
        snapHelper?.attachToRecyclerView(binding.calendarRecyclerView)
    }
    
    private fun setupScrollListener() {
        binding.calendarRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as? GridLayoutManager
                if (layoutManager != null) {
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                    val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                    val adapter = calendarAdapter
                    if (adapter != null && firstVisiblePosition >= 0) {
                        val calendarItems = adapter.calendarItems
                        if (firstVisiblePosition < calendarItems.size) {
                            // Check if we've reached the end of the scroll (last month - December)
                            val isAtEnd = lastVisiblePosition >= calendarItems.size - 1 || 
                                         (lastVisiblePosition >= 0 && lastVisiblePosition >= calendarItems.size - 10)
                            
                            if (isAtEnd) {
                                // Find the last month (December) in the calendar
                                var lastMonth: Int? = null
                                for (i in calendarItems.size - 1 downTo 0) {
                                    if (calendarItems[i] is CalendarItem.Day) {
                                        val day = (calendarItems[i] as CalendarItem.Day).calendarDay
                                        lastMonth = day.month
                                        break
                                    }
                                }
                                
                                // If we found December (month 11), set it
                                if (lastMonth != null && lastMonth == 11) {
                                    if (currentMonth != 11) {
                                        currentMonth = 11
                                        updateMonthTextView(11)
                                        weekAdapter.submitList(createWeekData(currentYear, 11))
                                        binding.recyclerDaysOfWeeks.adapter = weekAdapter
                                    }
                                    return
                                }
                            }
                            
                            // Find the month based on the visible position
                            var visibleMonth: Int? = null
                            
                            // Check if current position is an empty cell (white area)
                            val currentItem = calendarItems[firstVisiblePosition]
                            if (currentItem is CalendarItem.Empty) {
                                // In white area - find the closest month
                                // Look backwards for previous month
                                var previousMonth: Int? = null
                                var previousMonthDistance = Int.MAX_VALUE
                                for (i in firstVisiblePosition downTo 0) {
                                    if (calendarItems[i] is CalendarItem.Day) {
                                        val day = (calendarItems[i] as CalendarItem.Day).calendarDay
                                        previousMonth = day.month
                                        previousMonthDistance = firstVisiblePosition - i
                                        break
                                    }
                                }

                                // Look forwards for next month
                                var nextMonth: Int? = null
                                var nextMonthDistance = Int.MAX_VALUE
                                for (i in firstVisiblePosition until calendarItems.size) {
                                    if (calendarItems[i] is CalendarItem.Day) {
                                        val day = (calendarItems[i] as CalendarItem.Day).calendarDay
                                        nextMonth = day.month
                                        nextMonthDistance = i - firstVisiblePosition
                                        break
                                    }
                                }

                                // Choose the closest month
                                when {
                                    previousMonth != null && nextMonth != null -> {
                                        visibleMonth = if (previousMonthDistance <= nextMonthDistance) {
                                            previousMonth
                                        } else {
                                            nextMonth
                                        }
                                    }
                                    previousMonth != null -> visibleMonth = previousMonth
                                    nextMonth != null -> visibleMonth = nextMonth
                                }
                            } else if (currentItem is CalendarItem.Day) {
                                // Directly on a day - use its month
                                visibleMonth = currentItem.calendarDay.month
                            } else {
                                // Fallback: search for nearest day
                                for (i in firstVisiblePosition downTo 0) {
                                    if (calendarItems[i] is CalendarItem.Day) {
                                        val day = (calendarItems[i] as CalendarItem.Day).calendarDay
                                        visibleMonth = day.month
                                        break
                                    }
                                }
                                
                                if (visibleMonth == null) {
                                    for (i in firstVisiblePosition until calendarItems.size) {
                                        if (calendarItems[i] is CalendarItem.Day) {
                                            val day = (calendarItems[i] as CalendarItem.Day).calendarDay
                                            visibleMonth = day.month
                                            break
                                        }
                                    }
                                }
                            }
                            
                            visibleMonth?.let { month ->
                                if (month != currentMonth) {
                                    currentMonth = month
                                    // Update month text view
                                    updateMonthTextView(month)
                                    // Update week adapter with current year
                                    weekAdapter.submitList(createWeekData(currentYear, month))
                                    binding.recyclerDaysOfWeeks.adapter = weekAdapter
                                }
                            }
                        }
                    }
                }
            }
        })
    }
    
    private fun refreshCalendar() {
        lifecycleScope.launch {
            val adapter = calendarAdapter
            val pattern = getPatternFromDataStore()
            if (pattern.isNotEmpty() && adapter != null) {
                val savedMonth = getMonthFromDataStore().toInt()
                val calendarData = createMultiMonthCalendarData(
                    currentYear,
                    pattern,
                    currentMonth,
                    savedMonth,
                    12
                )
                adapter.updateData(calendarData)
                adapter.notifyDataSetChanged()
                
                // Update week adapter
                weekAdapter.submitList(createWeekData(currentYear, currentMonth))
            }
        }
    }
    
    
    private fun scrollToMonth(month: Int) {
        val adapter = calendarAdapter
        if (adapter != null) {
            // Find first day of the selected month (this will be after the empty cells)
            val position = adapter.calendarItems.indexOfFirst { item ->
                item is CalendarItem.Day && (item as CalendarItem.Day).calendarDay.month == month
            }
            if (position >= 0) {
                // Use post to ensure layout is complete before scrolling
                binding.calendarRecyclerView.post {
                    binding.calendarRecyclerView.smoothScrollToPosition(position)
                }
            }
        }
    }
    
    private fun scrollToCurrentDay() {
        val adapter = calendarAdapter
        if (adapter != null) {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            
            // Find the first day of the current month
            val firstDayPosition = adapter.calendarItems.indexOfFirst { item ->
                item is CalendarItem.Day && (item as CalendarItem.Day).calendarDay.month == currentMonth
            }
            
            if (firstDayPosition >= 0) {
                binding.calendarRecyclerView.post {
                    val layoutManager = binding.calendarRecyclerView.layoutManager as? GridLayoutManager
                    if (layoutManager != null) {
                        // Find the empty cells (white area) before the current month
                        // This is the detection area where month changes are detected
                        // We want to scroll to a position in the white area so the detector picks up current month
                        var scrollPosition = firstDayPosition
                        
                        // Look backwards from first day to find the start of empty cells or month header
                        for (i in firstDayPosition downTo 0) {
                            if (adapter.calendarItems[i] is CalendarItem.Empty) {
                                scrollPosition = i
                            } else if (adapter.calendarItems[i] is CalendarItem.MonthHeader) {
                                // Found the month header for current month, scroll to it
                                scrollPosition = i
                                break
                            } else if (adapter.calendarItems[i] is CalendarItem.Day) {
                                // Found a day from previous month, use the position after it
                                // This will be in the white area before current month
                                scrollPosition = i + 1
                                break
                            }
                        }
                        
                        // Scroll to position the white area (detection area) at the top
                        // This ensures the month detector will pick up the current month
                        layoutManager.scrollToPositionWithOffset(scrollPosition, 0)
                        
                        // Manually update the month text view to current month
                        // since we scrolled to it
                        updateMonthTextView(currentMonth)
                        weekAdapter.submitList(createWeekData(currentYear, currentMonth))
                        binding.recyclerDaysOfWeeks.adapter = weekAdapter
                    } else {
                        binding.calendarRecyclerView.smoothScrollToPosition(firstDayPosition)
                    }
                }
            }
        }
    }

    private fun findFirstMonthInCalendar(calendarItems: List<CalendarItem>): Int {
        // Find the first actual day item in the calendar
        for (item in calendarItems) {
            if (item is CalendarItem.Day) {
                return item.calendarDay.month ?: currentMonth
            }
        }
        // Fallback to current month if no day found
        return currentMonth
    }

    private fun createWeekData(year: Int, month: Int): List<CalendarDayOfWeek> {
        // Fixed week header - always shows the same order like standard calendars
        // Saturday, Sunday, Monday, Tuesday, Wednesday, Thursday, Friday
        val dayNames = arrayOf("سبت", "حد", "اتنين", "تلات", "اربع", "خميس", "جمعه")
        
        val calendarDays = mutableListOf<CalendarDayOfWeek>()
        
        // Always return the same fixed order of day names
        for (dayName in dayNames) {
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