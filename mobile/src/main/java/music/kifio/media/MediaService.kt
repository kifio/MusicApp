package music.kifio.media

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import music.kifio.R
import music.kifio.receivers.NoisyReceiver
import music.kifio.utils.parseClientId
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Created by kifio on 02.03.17.
 */
class MediaService : Service(), AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener {

    companion object {

        // https://en.wikipedia.org/wiki/Ducking
        private val VOLUME_DUCK = 0.2f
        private val VOLUME_NORMAL = 1.0f

        val ACTION_PLAY = "music.kifio.media.ACTION_PLAY"
        val ACTION_PAUSE = "music.kifio.media.ACTION_PAUSE"
        val ACTION_RESUME = "music.kifio.media.ACTION_RESUME"
        val ACTION_SEEK_TO = "music.kifio.media.ACTION_SEEK_TO"

        val ACTION_SET_POSITION = "music.kifio.media.ACTION_SET_POSITION"
        val ACTION_DISABLE_CONTROLS = "music.kifio.media.ACTION_DISABLE_CONTROLS"
        val ACTION_ENABLE_CONTROLS = "music.kifio.media.ACTION_ENABLE_CONTROLS"

    }

    private var mMediaPlayer: MediaPlayer? = null

    private var mAudioManager: AudioManager? = null

    private var mWifiLock: WifiManager.WifiLock? = null

    private var mCurrentPosition = 0

    private var mCurrentTrack: MediaMetadataCompat? = null

    private var mNoisyReceiver: NoisyReceiver? = null

    private var mExecutorService: ScheduledExecutorService? = null

    private var mScheduledFuture: ScheduledFuture<*>? = null

    private var mTimeDelta = 0L

    private val mUpdateProgressTask = Runnable {
        sendBroadcast(Intent().setAction(ACTION_SET_POSITION).putExtra("position", mCurrentPosition++))
    }

    override fun onCreate() {
        super.onCreate()

        // AudioManager provides access to volume and ringer mode control.
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mExecutorService = Executors.newSingleThreadScheduledExecutor() as ScheduledExecutorService

        mNoisyReceiver = NoisyReceiver(this)

        mWifiLock = (getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "kifio_lock")

        registerReceiver(mNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (!requestAudioFocus() || intent == null) {
            stopSelf()
        } else if (intent.action == ACTION_PAUSE) {
            pause()
        } else if (intent.action == ACTION_PLAY) {
            play(intent.getParcelableExtra("media"))
        } else if (intent.action == ACTION_RESUME) {
            resume()
        } else if (intent.action == ACTION_SEEK_TO) {
            seekTo(intent.getIntExtra("position", 0))
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mExecutorService!!.shutdown()
        unregisterReceiver(mNoisyReceiver)
        releaseResources()
        removeAudioFocus()
    }

    override fun onBind(intent: Intent?): IBinder? {
        startService(intent)
        return MediaBinder()
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        if (mp!!.isPlaying) {

            sendBroadcast(Intent(ACTION_ENABLE_CONTROLS))

            if (mScheduledFuture != null) {
                mScheduledFuture!!.cancel(true)
            }

            if (!mExecutorService!!.isShutdown) {
                mScheduledFuture = mExecutorService!!.scheduleWithFixedDelay(mUpdateProgressTask, 0,
                        mTimeDelta, TimeUnit.MILLISECONDS) as ScheduledFuture<*>?
            }
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        sendBroadcast(Intent(ACTION_DISABLE_CONTROLS))
        pause()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        stopSelf()
        return true
    }

    override fun onPrepared(mp: MediaPlayer?) {
        resume()
    }

    override fun onAudioFocusChange(focusChange: Int) {

        when (focusChange) {

            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (mMediaPlayer != null && mMediaPlayer!!.isPlaying()) {
                    mMediaPlayer!!.setVolume(VOLUME_DUCK, VOLUME_DUCK)
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager!!.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun removeAudioFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager!!.abandonAudioFocus(this)
    }

    private fun releaseResources() {

        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }

        if (mWifiLock!!.isHeld) {
            mWifiLock!!.release()
        }
    }

    fun play(currentTrack: MediaMetadataCompat) {

        if (mScheduledFuture != null) {
            mScheduledFuture!!.cancel(true)
        }

        if (mCurrentTrack != currentTrack) {
            mCurrentPosition = 0
            mCurrentTrack = currentTrack
        }

        mTimeDelta = currentTrack.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 255

        try {

            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer!!.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                mMediaPlayer!!.setOnPreparedListener(this)
                mMediaPlayer!!.setOnCompletionListener(this)
                mMediaPlayer!!.setOnErrorListener(this)
                mMediaPlayer!!.setOnBufferingUpdateListener(this)
            } else {
                mMediaPlayer!!.reset()
            }

            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setVolume(VOLUME_NORMAL, VOLUME_NORMAL)

            if (mCurrentTrack != null) {
                val streamUrl = mCurrentTrack!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
                val clientId = parseClientId(resources.getXml(R.xml.credential))
                val url = "$streamUrl?client_id=$clientId"
                mMediaPlayer!!.setDataSource(url)
            }

            mMediaPlayer!!.prepareAsync()
            mWifiLock!!.acquire()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.start()
        }
    }

    fun seekTo(pos: Int) {

        if (mMediaPlayer != null) {
            mCurrentPosition = pos
            mMediaPlayer!!.seekTo((mCurrentPosition * mTimeDelta).toInt())
        }
    }

    fun pause() {

        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
        }

        if (mScheduledFuture != null) {
            mScheduledFuture!!.cancel(true)
        }
    }

    inner class MediaBinder : Binder() {

        public fun getService() : MediaService {
            return this@MediaService
        }

    }
}