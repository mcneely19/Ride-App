package com.onewheel.ridetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onewheel.ridetracker.data.Ride
import com.onewheel.ridetracker.data.RideDatabase
import com.onewheel.ridetracker.data.RideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class NewRideInput(
    val board: String,
    val date: String,
    val whUsed: Double,
    val miles: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val temp: Int,
    val psi: Double?,
    val notes: String
)

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RideRepository(RideDatabase.get(application).rideDao())

    private val _boardFilter = MutableStateFlow<String?>(null) // null = all
    val boardFilter: StateFlow<String?> = _boardFilter.asStateFlow()

    private val _showTrendlines = MutableStateFlow(true)
    val showTrendlines: StateFlow<Boolean> = _showTrendlines.asStateFlow()

    val allRides: StateFlow<List<Ride>> = repo.rides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleRides: StateFlow<List<Ride>> = combine(allRides, boardFilter) { rides, filter ->
        val filtered = if (filter == null) rides else rides.filter { it.board == filter }
        filtered.sortedWith(compareByDescending<Ride> { it.date }.thenByDescending { it.session })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    fun setBoardFilter(board: String?) { _boardFilter.value = board }

    fun toggleTrendlines() { _showTrendlines.value = !_showTrendlines.value }

    fun addRide(input: NewRideInput) {
        val pct = input.whUsed / com.onewheel.ridetracker.data.Board.valueOf(input.board).capacityWh * 100
        val ride = Ride(
            id = "${input.board.lowercase()}-${input.date}-${UUID.randomUUID().toString().take(8)}",
            board = input.board,
            date = input.date,
            session = "",
            whUsed = input.whUsed,
            pctUsed = pct,
            miles = input.miles,
            avgSpeed = input.avgSpeed,
            maxSpeed = input.maxSpeed,
            temp = input.temp,
            psi = input.psi,
            notes = input.notes
        )
        viewModelScope.launch { repo.addRide(ride) }
    }

    fun deleteRide(id: String) {
        viewModelScope.launch { repo.deleteRide(id) }
    }
}
