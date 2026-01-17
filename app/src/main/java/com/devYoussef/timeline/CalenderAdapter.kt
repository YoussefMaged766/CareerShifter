package com.devYoussef.timeline

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.devYoussef.timeline.databinding.CalenderCellBinding
import com.devYoussef.timeline.databinding.ItemMonthHeaderBinding
import java.util.Calendar

class CalenderAdapter(var calendarItems: List<CalendarItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DAY = 0
        private const val VIEW_TYPE_EMPTY = 1
        private const val VIEW_TYPE_MONTH_HEADER = 2
    }

    class EmptyViewHolder(var binding: CalenderCellBinding) : ViewHolder(binding.root) {
        fun bind() {
            binding.txtCalenderDay.text = ""
            binding.txtPattern.text = ""
            binding.root.setBackgroundResource(R.color.white)
        }
    }

    class MonthHeaderViewHolder(var binding: ItemMonthHeaderBinding) : ViewHolder(binding.root) {
        fun bind(month: Int, year: Int) {
            val monthNames = arrayOf(
                "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
            )
            binding.txtMonthHeader.text = "${monthNames[month]} $year"
        }
    }

    class CalenderViewHolder(var binding: CalenderCellBinding) : ViewHolder(binding.root) {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        
        fun bind(data: CalendarDay) {
            binding.txtCalenderDay.text = data.dayOfMonth.toString()
            binding.txtPattern.text = data.notePattern

            if (data.notePattern == "ليل") {
                binding.root.setBackgroundResource(R.color.yellow)
                binding.txtPattern.setTextColor(Color.WHITE)
                binding.txtCalenderDay.setTextColor(Color.WHITE)
            } else if (data.notePattern == "نهار") {
                binding.root.setBackgroundResource(R.color.rose)
                binding.txtCalenderDay.setTextColor(Color.BLACK)
                binding.txtPattern.setTextColor(Color.BLACK)
            } else {
                binding.root.setBackgroundResource(R.color.gray)
                binding.txtCalenderDay.setTextColor(Color.BLACK)
                binding.txtPattern.setTextColor(Color.BLACK)
            }

            if (data.dayOfMonth == currentDay && data.month == currentMonth) {
                binding.root.setBackgroundResource(R.drawable.current_day_style)
            }
        }
    }

    fun updateData(newData: List<CalendarItem>) {
        calendarItems = newData
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (calendarItems[position]) {
            is CalendarItem.Day -> VIEW_TYPE_DAY
            is CalendarItem.Empty -> VIEW_TYPE_EMPTY
            is CalendarItem.MonthHeader -> VIEW_TYPE_MONTH_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EMPTY -> {
                val binding = CalenderCellBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                EmptyViewHolder(binding)
            }
            VIEW_TYPE_MONTH_HEADER -> {
                val binding = ItemMonthHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MonthHeaderViewHolder(binding)
            }
            else -> {
                val binding = CalenderCellBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                CalenderViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int {
        return calendarItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = calendarItems[position]) {
            is CalendarItem.Day -> {
                (holder as CalenderViewHolder).bind(item.calendarDay)
            }
            is CalendarItem.Empty -> {
                (holder as EmptyViewHolder).bind()
            }
            is CalendarItem.MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item.month, item.year)
            }
        }
    }
}
