package music.kifio.models

import com.google.gson.annotations.SerializedName

data class Track(@SerializedName("kind") val kind: String,
                 @SerializedName("id") val id: Long,
                 @SerializedName("title") val title: String,
                 @SerializedName("artwork_url") val artwork: String,
                 @SerializedName("stream_url") val streamUrl: String,
                 @SerializedName("download_url") val downloadUrl: String,
                 @SerializedName("permalink_url") val permalinkUrl: String,
                 @SerializedName("user") val user: User,
                 @SerializedName("duration") val duration: Long)