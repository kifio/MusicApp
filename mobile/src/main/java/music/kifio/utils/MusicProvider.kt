package music.kifio.utils

import music.kifio.models.Track
import java.util.*
import android.support.v4.media.MediaMetadataCompat

/**
 * Created by kifio on 04.03.17.
 */

class MusicProvider private constructor() {

    private var current = 0

    private var tracks = ArrayList<Track>()

    private object Singleton {
        val provider = MusicProvider()
    }

    companion object {
        val instance: MusicProvider by lazy { Singleton.provider }
    }

    fun setTracks(tracks: ArrayList<Track>) {
        this.tracks = tracks
    }

    fun setCurrent(current: Int) {
        this.current = current
    }

    fun getCurrent() : Track {
        return tracks[current]
    }

    fun getMeta(track: Track): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, track.user.username)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, track.artwork)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, track.streamUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id.toString())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                .build()
    }
}