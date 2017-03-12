package music.kifio.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import music.kifio.R

/**
 * Created by kifio on 27.02.17.
 */

class TrackHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {

    val artist = itemView!!.findViewById(R.id.artist) as TextView
    val title = itemView!!.findViewById(R.id.title) as TextView
    val artwork = itemView!!.findViewById(R.id.artwork) as ImageView

}