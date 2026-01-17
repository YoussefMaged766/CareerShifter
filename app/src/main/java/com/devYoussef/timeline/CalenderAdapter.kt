package com.devYoussef.timeline

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.devYoussef.timeline.databinding.CalenderCellBinding
import java.util.Calendar

class CalenderAdapter(var calendarItems: List<CalendarItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DAY = 0
        private const val VIEW_TYPE_EMPTY = 1
    }

    class EmptyViewHolder(var binding: CalenderCellBinding) : ViewHolder(binding.root) {
        fun bind() {
            binding.txtCalenderDay.text = ""
            binding.txtPattern.text = ""
            binding.root.setBackgroundResource(R.color.white)
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
            } else {
                binding.root.setBackgroundResource(R.color.gray)
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = CalenderCellBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return when (viewType) {
            VIEW_TYPE_EMPTY -> EmptyViewHolder(binding)
            else -> CalenderViewHolder(binding)
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
        }
    }
}
