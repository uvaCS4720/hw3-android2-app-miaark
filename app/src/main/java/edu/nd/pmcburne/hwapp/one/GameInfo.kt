package edu.nd.pmcburne.hwapp.one

data class GameInfo(
    val home: String,
    val away: String,

    val homeScore: String,
    val awayScore: String,

    val gameState: String,      // pre, live, final

    val startDate: String,
    val startTime: String,

    val currentPeriod: String?,
    val clock: String?,

    val winner: String?,

    val league: String          // men's or women's
)
