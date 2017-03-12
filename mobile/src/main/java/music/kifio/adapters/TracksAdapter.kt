package music.kifio.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import music.kifio.R
import music.kifio.activities.MainActivity
import music.kifio.utils.MusicProvider
import music.kifio.models.SearchResult
import music.kifio.models.Track
import music.kifio.network.DataSource
import music.kifio.utils.loadUrl
import java.util.*

/**
 * Created by kifio on 26.02.17.
 */

class TracksAdapter(val context: MainActivity, val listener: OnSelectTrackListener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DataSource.LoadingListener {

    private val results = ArrayList<Track>()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {

        val trackHolder: TrackHolder = holder as TrackHolder
        val track = results[position]

        trackHolder.artwork.loadUrl(track.artwork)
        trackHolder.artist.text = track.user.username
        trackHolder.title.text = track.title
        trackHolder.itemView.setOnClickListener {
            listener.onTrackSelected(track)
            MusicProvider.instance.setCurrent(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return TrackHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.i_track, parent, false))
    }

    override fun getItemCount(): Int {
        return results.size
    }

    override fun onSuccess(result: SearchResult) {
        results.addAll(result.collection)
        MusicProvider.instance.setTracks(results)
        context.runOnUiThread { context.updateTrackList() }
    }

    override fun onFail(e: Exception) {
        e.printStackTrace()
    }

    interface OnSelectTrackListener {
        fun onTrackSelected(track: Track)
    }
}