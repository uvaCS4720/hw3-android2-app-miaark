// SOURCE 1 USED: ChatGPT
// Usage: How to make Entity into table and connect to API parts (define DB structure)
package edu.nd.pmcburne.hwapp.one

import androidx.room.Entity

@Entity(tableName = "games",
    primaryKeys = ["gameID", "league"])
data class GameEntity(

    val gameID: String,

    val home: String,
    val away: String,

    val homeScore: String?,
    val awayScore: String?,

    val gameState: String,

    val startDate: String,
    val startTime: String,

    val currentPeriod: String?,
    val clock: String?,

    val winner: String?,

    val league: String
)
