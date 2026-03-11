package edu.nd.pmcburne.hwapp.one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import java.util.*



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HWStarterRepoTheme{
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "NBA GAME EXPLORER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    LoadGamesButton(viewModel())
                }
        }
        }
    }
}

@Composable
fun LoadGamesButton(viewModel: GamesViewModel) {

    var statusMessage by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {

        Button(onClick = {
            // For simplicity, just use today's date
            val today = LocalDate.now()
            val year = today.year.toString()
            val month = "%02d".format(today.monthValue)
            val day = "%02d".format(today.dayOfMonth)

            // Launch API call
            viewModel.viewModelScope.launch {
                try {
                    viewModel.loadGames(year, month, day)
                    statusMessage = "Success! Games loaded."
                } catch (e: Exception) {
                    e.printStackTrace()
                    statusMessage = "Failed to load games."
                }
            }

        }) {
            Text("Load Games")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
class GamesViewModel : ViewModel() {

    var games by mutableStateOf<List<GameInfo>>(emptyList())
        private set

    fun loadGames(year: String, month: String, day: String) {

        viewModelScope.launch {

            try {

                val response = RetrofitClient.api.getGames(
                    gender = "men",
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

                        league = "men"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun DatePicker(viewModel: GamesViewModel) {

    val context = LocalContext.current
    val games = viewModel.games

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->

            val correctedMonth = month + 1

            selectedDate = LocalDate.of(year, correctedMonth, dayOfMonth)

            val y = year.toString()
            val m = "%02d".format(correctedMonth)
            val d = "%02d".format(dayOfMonth)

            viewModel.loadGames(y, m, d)

        },
        selectedDate.year,
        selectedDate.monthValue - 1,
        selectedDate.dayOfMonth
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Button(onClick = {
            datePickerDialog.show()
        }) {
            Text("Pick Date")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Selected Date: $selectedDate")

        Spacer(modifier = Modifier.height(20.dp))

        games.forEach { game ->
            Text(
                text = "${game.away} (${game.awayScore}) vs ${game.home} (${game.homeScore})",
                style = MaterialTheme.typography.bodyLarge
            )

            if (game.gameState == "ip") {
                Text("Period: ${game.currentPeriod} | Clock: ${game.clock}")
            } else if (game.gameState == "finished") {
                Text("Winner: ${game.winner}")
            } else {
                Text("Starts at: ${game.startTime}")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
