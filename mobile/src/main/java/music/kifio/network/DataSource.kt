package music.kifio.network

import android.content.Context
import com.google.gson.Gson
import music.kifio.R
import music.kifio.utils.parseClientId
import music.kifio.models.SearchResult
import okhttp3.*
import java.io.IOException
import java.util.*

/**
 * Created by kifio on 26.02.17.
 */

class DataSource(val listener: LoadingListener) : Callback {

    private val client  = OkHttpClient()

    override fun onResponse(call: Call?, response: Response?) {
        if (response!!.isSuccessful) {
            listener.onSuccess(Gson().fromJson(response.body().string(), SearchResult::class.java))
        }
    }

    override fun onFailure(call: Call?, e: IOException?) {
        e!!.printStackTrace()
    }

    fun load(context: Context) {

        val requestString = context.resources.getString(R.string.request)
        val request = Request.Builder()
                .url(String.format(Locale.getDefault(), requestString,
                        "Street Fever", parseClientId(context.resources.getXml(R.xml.credential))))
                .build()

        client.newCall(request).enqueue(this)
    }

    interface LoadingListener {

        fun onSuccess(result: SearchResult)

        fun onFail(e: Exception)
    }
}