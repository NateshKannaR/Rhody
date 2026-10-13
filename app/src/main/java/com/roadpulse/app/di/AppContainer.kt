package com.roadpulse.app.di

import android.content.Context
import com.roadpulse.app.data.database.RoadPulseDatabase
import com.roadpulse.app.data.repository.TripRepository

class AppContainer(context: Context) {
    val database: RoadPulseDatabase = RoadPulseDatabase.getInstance(context)
    val tripRepository: TripRepository = TripRepository(database)
}
