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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme {

                val vm: GamesViewModel = viewModel()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "NCAA GAME EXPLORER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row {
                        DatePicker(vm)
                        LoadGamesButton(vm)
                    }
                    LeaguePicker(vm)

                    Spacer(modifier = Modifier.height(20.dp))

                    GamesList(vm, modifier = Modifier.weight(1f))
                }
            }
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
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth()
        ) {
            items(games) { game ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if(game.gameState == "final" || game.gameState == "live"){
                        Text(
                            text = "${game.away} (${game.awayScore}) vs. ${game.home} (${game.homeScore})",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else{
                        Text(
                            text = "${game.away} vs ${game.home}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (game.gameState == "live") {
                        Text("Period: ${game.currentPeriod} | Clock: ${game.clock}")
                    }
                    else if (game.gameState == "final") {
                        Text("Winner: ${game.winner}")
                    }
                    else {
                        Text("Starts at: ${game.startTime}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
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

    }) {
        if (loading) {
            Text("Load Games")
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
class GamesViewModel : ViewModel() {
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

            isLoading = true

            try {

                val response = RetrofitClient.api.getGames(
                    gender = league,
                    year = year,
                    month = month,
                    day = day
                )

                games = response.games.map { wrapper ->

                    val g = wrapper.game

                    GameInfo(
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

            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
        }
    }
}

@Composable
fun LeaguePicker(viewModel: GamesViewModel) {

    var expanded by remember { mutableStateOf(false) }
    val selectedLeague = viewModel.league

    Column {

        Button(onClick = { expanded = true }) {
            Text("League: $selectedLeague")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
fun DatePicker(viewModel: GamesViewModel) {
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->

            val correctedMonth = month + 1
            selectedDate = LocalDate.of(year, correctedMonth, dayOfMonth)

            viewModel.setDate(selectedDate)

        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Button(onClick = {
            datePickerDialog.show()
        }) {
            Text("Pick Date ")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Selected Date: $selectedDate")
    }
}
