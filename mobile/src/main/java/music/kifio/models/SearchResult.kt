package music.kifio.models

import com.google.gson.annotations.SerializedName

/**
 * Created by kifio on 26.02.17.
 */

data class SearchResult(@SerializedName("total_results") val totalResults: Int,
                        @SerializedName("collection") val collection: Array<Track>)