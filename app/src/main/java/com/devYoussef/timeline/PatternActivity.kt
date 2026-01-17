package com.devYoussef.timeline

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.devYoussef.timeline.Constants.dataStore
import com.devYoussef.timeline.databinding.ActivityPatternBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class PatternActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatternBinding
    private lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatternBinding.inflate(layoutInflater)
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
            // Load existing pattern if available
            val existingPattern = getPatternFromDataStore()
            if (existingPattern.isNotEmpty() && existingPattern[0] != " ") {
                binding.txtPattern.setText(existingPattern.joinToString(" "))
            }
        }

        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                val patternText = binding.txtPattern.text.toString().trim()
                if (patternText.isNotEmpty()) {
                    savePattern("pattern", patternText)
                    saveMonth("month", Calendar.getInstance().get(Calendar.MONTH).toString())
                    
                    // Navigate to MainActivity and finish this activity
                    val intent = Intent(this@PatternActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private suspend fun savePattern(key: String, value: String) {
        dataStore = applicationContext.dataStore
        val dataStoreKey = stringPreferencesKey(key)
        dataStore.edit {
            it[dataStoreKey] = value
        }
    }

    private suspend fun saveMonth(key: String, value: String) {
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
            if (word.isNotEmpty()) {
                list.add(word)
            }
        }
        return list
    }
}
