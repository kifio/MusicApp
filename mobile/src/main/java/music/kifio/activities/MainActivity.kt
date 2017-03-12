package music.kifio.activities

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.a_main.*
import music.kifio.R
import music.kifio.adapters.TracksAdapter
import music.kifio.media.MediaService
import music.kifio.media.MediaService.Companion.ACTION_ENABLE_CONTROLS
import music.kifio.media.MediaService.Companion.ACTION_DISABLE_CONTROLS
import music.kifio.media.MediaService.Companion.ACTION_SET_POSITION
import music.kifio.utils.MusicProvider
import music.kifio.models.Track
import music.kifio.network.DataSource
import music.kifio.receivers.UiChangesReceiver
import music.kifio.utils.loadUrl


/**
 * Created by kifio on 26.02.17.
 */

class MainActivity : AppCompatActivity(), TracksAdapter.OnSelectTrackListener,
        View.OnClickListener, SeekBar.OnSeekBarChangeListener, UiChangesReceiver.OnUiChangeListener, ServiceConnection {

    private val mAdapter = TracksAdapter(this, this)

    private var mMediaEventsReceiver: UiChangesReceiver? = null

    private var mControlsEnabled = false

    private var mBound = false

    private var mMediaService: MediaService? = null

    private var mTrack: Track? = null

    override public fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_main)

        list.adapter = mAdapter
        list.layoutManager = LinearLayoutManager(this)
        list.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        resume.setOnClickListener(this)
        pause.setOnClickListener(this)
        logo.setOnClickListener(this)
        trackTitle.setOnClickListener(this)
        artist.setOnClickListener(this)
        progress.setOnSeekBarChangeListener(this)
        DataSource(mAdapter).load(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState!!.putBoolean("controls_enabled", mControlsEnabled)
    }

    override fun onStart() {
        super.onStart()
        val mediaEventsFilter = IntentFilter()
        mediaEventsFilter.addAction(ACTION_DISABLE_CONTROLS)
        mediaEventsFilter.addAction(ACTION_ENABLE_CONTROLS)
        mediaEventsFilter.addAction(ACTION_SET_POSITION)

        mMediaEventsReceiver = UiChangesReceiver(this)
        registerReceiver(mMediaEventsReceiver, mediaEventsFilter)
    }

    override fun onStop() {

        if (mMediaEventsReceiver != null) {
            unregisterReceiver(mMediaEventsReceiver)
            mMediaEventsReceiver = null
        }

        super.onStop()
    }

    override fun onDestroy() {
        unbindService(this)
        stopService(Intent(this, MediaService::class.java))
        super.onDestroy()
    }

    override fun enableButtons(flag: Boolean) {
        mControlsEnabled = flag
        progress.isEnabled = flag
    }

    override fun progressChange(position: Int) {
        progress.progress = position
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
        val intent = Intent(this, MediaService::class.java).setAction(MediaService.ACTION_PLAY)
                .putExtra("media", MusicProvider.instance.getMeta(track))
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
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mBound = false
        mMediaService = null
    }

    override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

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