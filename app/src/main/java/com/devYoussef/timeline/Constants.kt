package com.devYoussef.timeline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object Constants {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore("save")


}