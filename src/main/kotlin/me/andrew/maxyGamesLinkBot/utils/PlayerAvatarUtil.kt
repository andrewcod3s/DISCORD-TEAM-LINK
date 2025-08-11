package me.arnxld.maxyGamesLinkBot.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit

object PlayerAvatarUtil {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    

    fun getPlayerAvatarUrl(playerUuid: UUID): String {
        val uuidString = playerUuid.toString().replace("-", "")
        return "https://nmsr.nickac.dev/head?uuid=$uuidString&overlay=true&size=128"
    }

  
    fun getPlayerBustUrl(playerUuid: UUID): String {
        val uuidString = playerUuid.toString().replace("-", "")
        return "https://nmsr.nickac.dev/bust/$uuidString"
    }
    

    fun getPlayerSkinUrl(playerUuid: UUID): String {
        val uuidString = playerUuid.toString().replace("-", "")
        return "https://nmsr.nickac.dev/skin?uuid=$uuidString"
    }
    

    fun getPlayer3DAvatarUrl(playerUuid: UUID): String {
        val uuidString = playerUuid.toString().replace("-", "")
        return "https://nmsr.nickac.dev/body?uuid=$uuidString&overlay=true&size=128"
    }

    private fun getDefaultAvatarUrl(uuidString: String): String {
        return "https://crafatar.com/renders/head/$uuidString?overlay&default=MHF_Steve"
    }
    

    fun getPlayerAvatarUrlByName(playerName: String): String {
        return "https://minotar.net/avatar/$playerName/128"
    }
}
