package music.kifio.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.a_main.*
import music.kifio.R
import music.kifio.adapters.TracksAdapter
import music.kifio.media.MediaService
import music.kifio.models.Track
import music.kifio.network.DataSource
import music.kifio.utils.MusicProvider
import music.kifio.utils.init
import music.kifio.utils.loadUrl
import java.util.*


/**
 * Created by kifio on 26.02.17.
 */

class MainActivity : AppCompatActivity(), TracksAdapter.OnSelectTrackListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener, ServiceConnection {

    private val mAdapter = TracksAdapter(this, this)

    private var mControlsEnabled = false

    private var mBound = false

    private var mMediaService: MediaService? = null

    private var mTrack: Track? = null

    private var mHandler = Handler(Handler.Callback { msg ->

        if (msg!!.what == MediaService.UPDATE_PROGRESS) {
            progress.progress = msg.obj as Int
        } else {
            enableControls(msg.what == MediaService.ENABLE_CONTROLS)
        }

        true
    })

    override public fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)
        startService(getMediaServiceIntent())
        list.init(this, mAdapter)
        resume.setOnClickListener(this)
        pause.setOnClickListener(this)
        logo.setOnClickListener(this)
        trackTitle.setOnClickListener(this)
        artist.setOnClickListener(this)
        progress.setOnSeekBarChangeListener(this)
        DataSource(mAdapter).load(this)
    }

    override fun onDestroy() {
        if (mBound) unbindService(this)
        stopService(Intent(this, MediaService::class.java))
        super.onDestroy()
    }

    private fun enableControls(state: Boolean) {
        mControlsEnabled = state
        progress.isEnabled = state
    }

    override fun onClick(v: View?) {

        if (progress.progress < progress.max && mControlsEnabled && mMediaService != null) {

            when (v!!.id) {

                resume.id -> {
                    mMediaService!!.resume()
                }

                pause.id -> {
                    mMediaService!!.pause()
                }

                (logo!!.id or trackTitle!!.id) -> {
                    if (mTrack != null) {
                        lookAtWeb(mTrack!!.permalinkUrl)
                    }
                }

                artist!!.id -> {
                    if (mTrack != null) {
                        lookAtWeb(mTrack!!.user.permalinkUrl)
                    }
                }
            }
        }
    }

    override fun onTrackSelected(track: Track) {
        mTrack = track

        val intent = getMediaServiceIntent()

        intent.setAction(MediaService.ACTION_PLAY)
                .putExtra(MediaService.EXTRA_MEDIA, MusicProvider.instance.getMeta(track))
                .putExtra(MediaService.EXTRA_MAX_PROGRESS, progress.max)

        if (!mBound) {
            bindService(intent, this, BIND_AUTO_CREATE)
        } else if (mMediaService != null) {
            mMediaService!!.play(MusicProvider.instance.getMeta(track))
        }

        updateHeader(track)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if (mMediaService != null) {
            mMediaService!!.seekTo(progress.progress)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mBound = true
        mMediaService = (service as MediaService.MediaBinder).getService()
        mMediaService!!.setHandler(mHandler)
        Toast.makeText(this, String.format(Locale.getDefault(),
                getString(R.string.toast_service_bound, service.javaClass.name, this.javaClass.name)),
                Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mBound = false
        mMediaService = null
    }

    override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    private fun getMediaServiceIntent(): Intent {
        return Intent(this, MediaService::class.java)
    }

    private fun updateHeader(track: Track) {
        artwork.loadUrl(track.artwork)
        artist.text = track.user.username
        trackTitle.text = track.title
        progress.isEnabled = false
        progress.progress = 0
    }

    private fun lookAtWeb(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
    }

    fun updateTrackList() {
        mAdapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }
}