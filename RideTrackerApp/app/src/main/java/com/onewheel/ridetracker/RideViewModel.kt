package com.onewheel.ridetracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onewheel.ridetracker.data.BoardEntity
import com.onewheel.ridetracker.data.BoardRepository
import com.onewheel.ridetracker.data.Ride
import com.onewheel.ridetracker.data.RideDatabase
import com.onewheel.ridetracker.data.RideJsonImport
import com.onewheel.ridetracker.data.RideRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class NewRideInput(
    val board: String,
    val capacityWh: Double,
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

    private val db = RideDatabase.get(application)
    private val repo = RideRepository(db.rideDao())
    private val boardRepo = BoardRepository(db.boardDao())

    private val _boardFilter = MutableStateFlow<String?>(null) // null = all
    val boardFilter: StateFlow<String?> = _boardFilter.asStateFlow()

    private val _showTrendlines = MutableStateFlow(true)
    val showTrendlines: StateFlow<Boolean> = _showTrendlines.asStateFlow()

    // One-shot status message for the last JSON import attempt (success/failure summary).
    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    fun clearImportMessage() { _importMessage.value = null }

    // One-shot status message for board save/delete problems (e.g. "can't delete, has rides").
    private val _boardMessage = MutableStateFlow<String?>(null)
    val boardMessage: StateFlow<String?> = _boardMessage.asStateFlow()

    fun clearBoardMessage() { _boardMessage.value = null }

    private val _boardsLoaded = MutableStateFlow(false)

    val boards: StateFlow<List<BoardEntity>> = boardRepo.boards
        .onEach { _boardsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** True once the first read from the boards table has completed — avoids flashing the
     * first-run setup screen for a split second before real (non-empty) data arrives. */
    val boardsLoaded: StateFlow<Boolean> = _boardsLoaded.asStateFlow()

    val allRides: StateFlow<List<Ride>> = repo.rides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleRides: StateFlow<List<Ride>> = combine(allRides, boardFilter) { rides, filter ->
        val filtered = if (filter == null) rides else rides.filter { it.board == filter }
        filtered.sortedWith(compareByDescending<Ride> { it.date }.thenByDescending { it.session })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBoardFilter(board: String?) { _boardFilter.value = board }

    fun toggleTrendlines() { _showTrendlines.value = !_showTrendlines.value }

    fun addRide(input: NewRideInput) {
        val pct = if (input.capacityWh > 0) input.whUsed / input.capacityWh * 100 else 0.0
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

    /** Parses a JSON file's text and imports whatever valid ride records it contains. */
    fun importRidesFromJson(jsonText: String) {
        viewModelScope.launch {
            val capacities = boards.value.associate { it.name to it.capacityWh }
            val (rides, errors) = try {
                RideJsonImport.parse(jsonText, capacities)
            } catch (e: Exception) {
                _importMessage.value = "Couldn't read that file: ${e.message ?: "invalid JSON"}"
                return@launch
            }

            if (rides.isEmpty()) {
                _importMessage.value = if (errors.isNotEmpty()) {
                    "No valid rides found. ${errors.first()}"
                } else {
                    "No rides found in that file."
                }
                return@launch
            }

            val inserted = repo.importRides(rides)
            val skipped = rides.size - inserted
            _importMessage.value = buildString {
                append("Imported $inserted ride${if (inserted == 1) "" else "s"}.")
                if (skipped > 0) append(" Skipped $skipped already-imported.")
                if (errors.isNotEmpty()) append(" ${errors.size} entr${if (errors.size == 1) "y" else "ies"} couldn't be read.")
            }
        }
    }

    /** Add or update (by name) a board. */
    fun saveBoard(board: BoardEntity) {
        viewModelScope.launch { boardRepo.save(board) }
    }

    /** No-ops and reports a message if this board still has rides logged against it. */
    fun deleteBoard(name: String) {
        viewModelScope.launch {
            val ok = boardRepo.delete(name)
            if (!ok) {
                val count = boardRepo.rideCountForBoard(name)
                _boardMessage.value = "Can't delete \"$name\" — it still has $count ride${if (count == 1) "" else "s"} logged. Delete those rides first."
            }
        }
    }
}
