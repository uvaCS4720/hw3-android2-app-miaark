// SOURCE 1 USED: ChatGPT
// Usage: API GET request formatting
package edu.nd.pmcburne.hwapp.one
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("https://ncaa-api.henrygd.me/scoreboard/basketball-{gender}/d1/{year}/{month}/{day}")
    suspend fun getGames(
        @Path("gender") gender: String,
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String
    ): GameResponse
}