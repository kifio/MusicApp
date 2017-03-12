package music.kifio.models

import com.google.gson.annotations.SerializedName

/**
 * Created by kifio on 28.02.17.
 */
data class User(@SerializedName("username") val username: String, @SerializedName("permalink_url") val permalinkUrl: String)
