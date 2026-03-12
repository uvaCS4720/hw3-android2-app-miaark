// SOURCE 1 USED: ChatGPT
// Usage: Inserting into DB formatting, how to interact with Room DB
package edu.nd.pmcburne.hwapp.one

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Query("SELECT * FROM games WHERE startDate = :date AND league = :league")
    suspend fun getGames(date: String, league: String): List<GameEntity>
}