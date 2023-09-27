package com.example.timeline

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.timeline.databinding.CalenderCellBinding
import com.example.timeline.databinding.ItemCalendarDayBinding

class WeekAdapter : ListAdapter<CalendarDayOfWeek, WeekAdapter.CalenderViewHolder>(CalenderDiffCallback()) {

    class CalenderDiffCallback : DiffUtil.ItemCallback<CalendarDayOfWeek>() {
        override fun areItemsTheSame(oldItem: CalendarDayOfWeek, newItem: CalendarDayOfWeek): Boolean {
            return oldItem.dayName == newItem.dayName
        }

        override fun areContentsTheSame(oldItem: CalendarDayOfWeek, newItem: CalendarDayOfWeek): Boolean {
            return oldItem.dayName== newItem.dayName
        }
    }

    class CalenderViewHolder(var binding: ItemCalendarDayBinding) : ViewHolder(binding.root){
        fun bind(data: CalendarDayOfWeek) {
            binding.dayNameTextView.text = data.dayName.toString()


        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalenderViewHolder {
        val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalenderViewHolder(binding)    }

    override fun onBindViewHolder(holder: CalenderViewHolder, position: Int) {
       holder.bind(getItem(position))
    }
}