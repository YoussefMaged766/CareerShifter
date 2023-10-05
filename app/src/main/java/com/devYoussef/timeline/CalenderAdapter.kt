package com.devYoussef.timeline

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.devYoussef.timeline.databinding.CalenderCellBinding
import java.util.Calendar

class CalenderAdapter(var calendarDays : List<CalendarDay>) : RecyclerView.Adapter<CalenderAdapter.CalenderViewHolder>() {



    class CalenderViewHolder(var binding: CalenderCellBinding ) : ViewHolder(binding.root){
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        fun bind(data: CalendarDay ) {
            binding.txtCalenderDay.text = data.dayOfMonth.toString()
            binding.txtPattern.text = data.notePattern

            if (data.notePattern=="ليل"){
                binding.root.setBackgroundResource(R.color.yellow)
                binding.txtPattern.setTextColor(Color.WHITE)
                binding.txtCalenderDay.setTextColor(Color.WHITE)
            } else if (data.notePattern=="نهار"){
                binding.root.setBackgroundResource(R.color.rose)
            } else{
                binding.root.setBackgroundResource(R.color.gray)
            }

            if (data.dayOfMonth == currentDay && data.month == currentMonth){
                binding.root.setBackgroundResource(R.drawable.current_day_style)
            }




        }

    }
    fun updateData(newData: List<CalendarDay>) {
        calendarDays = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalenderViewHolder {
        val binding = CalenderCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CalenderViewHolder(binding)    }

    override fun getItemCount(): Int {
        return calendarDays.size
    }

    override fun onBindViewHolder(holder: CalenderViewHolder, position: Int) {
       holder.bind(calendarDays[position])
    }
}