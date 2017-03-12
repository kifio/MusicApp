package music.kifio.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import music.kifio.media.MediaService

/**
 * Created by kifio on 11.03.17.
 * Receive events, for ui updating ()
 */
class UiChangesReceiver(val listener: OnUiChangeListener) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == MediaService.ACTION_SET_POSITION) {
            listener.progressChange(intent.getIntExtra("position", 0))
        } else if (intent.action == MediaService.ACTION_ENABLE_CONTROLS) {
            listener.enableButtons(true)
        } else {
            listener.enableButtons(false)
        }
    }

    interface OnUiChangeListener {

        fun enableButtons(flag: Boolean)

        fun progressChange(position: Int)

    }
}

