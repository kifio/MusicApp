package music.kifio.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import music.kifio.media.MediaService

/**
 * Created by kifio on 11.03.17.
 */
class NoisyReceiver(val service: MediaService) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        service.pause()
    }

}