package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "games_db"
        ).build()
        setContent {
            val dao = db.gameDao()

            val vm: GamesViewModel = viewModel(
                factory = GamesViewModelFactory(dao)
            )

            HWStarterRepoTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "NCAA GAME EXPLORER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GameSettingsPanel(vm)
                    Divider(
                        color = Color.Gray,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    GamesList(vm, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

class GamesViewModelFactory(
    private val dao: GameDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class GamesViewModel(private val dao: GameDao) : ViewModel() {
    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var games by mutableStateOf<List<GameInfo>>(emptyList())
        private set

    var league by mutableStateOf("men")
        private set

    fun updateLeague(newLeague: String) {
        league = newLeague
    }

    fun setDate(date: LocalDate) {
        selectedDate = date
    }

    fun loadGames(year: String, month: String, day: String) {

        viewModelScope.launch {

            val date = "%02d/%02d/%04d".format(month.toInt(), day.toInt(), year.toInt())

            isLoading = true

            try {

                val response = RetrofitClient.api.getGames(
                    gender = league,
                    year = year,
                    month = month,
                    day = day
                )

                val entities = response.games.map { wrapper ->

                    val g = wrapper.game

                    GameEntity(
                        gameID = g.gameID,
                        home = g.home.names.short,
                        away = g.away.names.short,
                        homeScore = g.home.score,
                        awayScore = g.away.score,
                        gameState = g.gameState,
                        startDate = g.startDate,
                        startTime = g.startTime,
                        currentPeriod = g.currentPeriod,
                        clock = g.contestClock,
                        winner =
                            if (g.home.winner) g.home.names.short
                            else if (g.away.winner) g.away.names.short
                            else null,
                        league = league
                    )
                }

                dao.insertGames(entities)

            } catch (e: Exception) {
                println("Offline — using cached scores")
            }

            val cachedGames = dao.getGames(date, league)

            games = cachedGames.map {
                GameInfo(
                    gameID = it.gameID,
                    home = it.home,
                    away = it.away,
                    homeScore = it.homeScore?.toString() ?: "",
                    awayScore = it.awayScore?.toString() ?: "",
                    gameState = it.gameState,
                    startDate = it.startDate,
                    startTime = it.startTime,
                    currentPeriod = it.currentPeriod?.toString(),
                    clock = it.clock,
                    winner = it.winner,
                    league = it.league
                )
            }

            isLoading = false
        }
    }
}

@Composable
fun DatePicker(viewModel: GamesViewModel) {
    val context = LocalContext.current
    val selectedDate = viewModel.selectedDate

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.setDate(LocalDate.of(year, month + 1, dayOfMonth))
        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Date",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { datePickerDialog.show() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = selectedDate.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun LeaguePicker(viewModel: GamesViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLeague = viewModel.league

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select League",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLeague.capitalize(),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            containerColor = MaterialTheme.colorScheme.onPrimary
        ) {
            DropdownMenuItem(
                text = { Text("Men") },
                onClick = {
                    viewModel.updateLeague("men")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Women") },
                onClick = {
                    viewModel.updateLeague("women")
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun LoadGamesButton(viewModel: GamesViewModel) {
    val date = viewModel.selectedDate
    val loading = viewModel.isLoading

    Button(onClick = {
        val year = date.year.toString()
        val month = "%02d".format(date.monthValue)
        val day = "%02d".format(date.dayOfMonth)

        viewModel.loadGames(year, month, day)
    },
        modifier = Modifier.fillMaxWidth()
    ) {
        if (loading) {
            Text("Load Games")
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("Load Games")
        }

    }
}

@Composable
fun GameSettingsPanel(viewModel: GamesViewModel) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DatePicker(viewModel)
                Spacer(modifier = Modifier.height(12.dp))
                LeaguePicker(viewModel)
                Spacer(modifier = Modifier.height(12.dp))
                LoadGamesButton(viewModel)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodyLarge
            )
        }

    }
}

@Composable
fun GamesList(viewModel: GamesViewModel, modifier: Modifier) {
    val games = viewModel.games
    val loading = viewModel.isLoading

    if (loading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    else if (games.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(horizontal=10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("0 Games Loaded\n")
                    }
                    append("All games for the selected date and league will appear here upon clicking 'Load Games' when connected to the Internet or offline receiving cached games.")
                },
                textAlign = TextAlign.Center,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(games) { game ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(8.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AWAY",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "HOME",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))


                    if(game.gameState == "final" || game.gameState == "live"){
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${game.away} (${game.awayScore})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${game.home} (${game.homeScore})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else{
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${game.away}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${game.home}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val (statusText, statusColor) = when (game.gameState) {
                            "live" -> "LIVE ●" to Color(0xFF039431)
                            "final" -> "FINISHED ⏱" to Color(0xFFA61502)
                            else -> "UPCOMING ➜" to Color(0xFFFF9800)
                        }

                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )

                        val infoText = when (game.gameState) {
                            "live" -> "Period: ${game.currentPeriod} | Clock: ${game.clock}"
                            "final" -> "Final | Winner: ${game.winner}"
                            else -> "Starts at: ${game.startTime}"
                        }

                        Text(infoText)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}




