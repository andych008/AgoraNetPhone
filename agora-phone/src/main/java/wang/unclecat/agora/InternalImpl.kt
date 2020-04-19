package wang.unclecat.agora

import android.media.MediaPlayer
import com.tinder.StateMachine
import io.agora.rtc.RtcEngine
import wang.unclecat.agora.utils.Logger
import java.io.IOException

internal class InternalImpl(private val netPhone: NetPhone,
                            private val rtcEngine: RtcEngine):Internal {

    var remoteAccount: String? = null
    var dialBean: DialBean? = null
    var startTime: Long = 0
    private var fromRemoteHangUp = false
    private val mMediaPlayer:MediaPlayer = MediaPlayer()


    /**
     * 是否被激活(即非idle状态)
     */
    override fun isActive(): Boolean {
        return phoneSM.state != PhoneState.IDLE
    }

    /**
     * 当前状态
     */
    override fun getState(): PhoneState {
        return phoneSM.state
    }

    override fun transition(event: Event) {
        phoneSM.transition(event)
    }

    /**
     * 挂断
     */
    override fun hangUp() {
        fromRemoteHangUp = false
        val ca = phoneSM.state
        Logger.d("hangUp() called with: ca = $ca")

        //告诉对方挂断呼叫
        if (isActive) {
            netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createHangUpMsg().toJsonString())
        }

        //挂断
        doHangUp(ca)
    }

    /**
     * 被告知挂断
     */
    override fun hangUp2() {
        fromRemoteHangUp = true
        val ca = phoneSM.state
        Logger.d("hangUp2() called with: ca = $ca")

        //挂断
        doHangUp(ca)
    }

    override fun afterHangUp() {
        netPhone.testCase = 0
        phoneSM.transition(Event.OnHangUp)
    }

    private fun doHangUp(ca: PhoneState) {
        if (ca == PhoneState.CONNECTED ||ca == PhoneState.SPEAKING) {
            rtcEngine.leaveChannel()
        } else {
            afterHangUp()
        }
    }

    private val speakTime: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000).toInt()


//    /**
//     * 主叫(动作)：        拨打                           进入2                             挂断
//     * 主叫(状态)：初始 -------------------------连接中-------------------------------接通---------  初始
//     * 被叫(状态)：初始 ------------- 响铃--------连接中---------连接上-----------------接通---------  初始
//     * 被叫(动作)：            来电         接听           进入1               进入3          挂断
//     */
    //状态切换，及切换成功后的回调。
    val phoneSM = StateMachine.create<PhoneState, Event, SideEffect> {
        initialState(PhoneState.IDLE)

        state<PhoneState.IDLE> {
            on<Event.OnDial> {
                Logger.d("主叫：拔打")
                transitionTo(PhoneState.CONNECTING, SideEffect.LogOnDial)
            }
            on<Event.OnReceive> {
                Logger.d("被叫：来电")
                transitionTo(PhoneState.RINGING, SideEffect.LogOnReceive)
            }
        }
        state<PhoneState.RINGING> {
            on<Event.OnHangUp> {
                Logger.d("被叫：拒接")
                transitionTo(PhoneState.IDLE, SideEffect.LogOnHangUp)
            }
            on<Event.OnAccept> {
                Logger.d("被叫：接听")
                transitionTo(PhoneState.CONNECTING, SideEffect.LogOnAccept)
            }
        }
        state<PhoneState.CONNECTING> {
            on<Event.OnHangUp> {
                Logger.d("主叫：取消拔打")
                transitionTo(PhoneState.IDLE, SideEffect.LogOnHangUp)
            }
            on<Event.OnHangUpAuto> {
                Logger.d("主叫：占线")
                transitionTo(PhoneState.IDLE, SideEffect.LogOnHangUp)
            }
            on<Event.OnJoin1> {
                Logger.d("被叫：进入房间（等待主叫加入房间，才可以通话）")
                transitionTo(PhoneState.CONNECTED, SideEffect.LogOnJoin)
            }
            //被叫加入房间后，主叫再加入房间。主叫加入房间后，直接从PhoneState.CONNECTING 到 State.SPEAKING 跳过 State.CONNECTED
            on<Event.OnJoin2> {
                Logger.d("主叫：加入房间-可通话状态")
                transitionTo(PhoneState.SPEAKING, SideEffect.LogOnSpeaking)
            }
        }
        state<PhoneState.CONNECTED> {
            //被叫：可通话状态（等待主叫加入房间，才可以通话）
            on<Event.OnJoin3> {
                Logger.d("被叫：可通话状态")
                transitionTo(PhoneState.SPEAKING, SideEffect.LogOnSpeaking)
            }
        }
        state<PhoneState.SPEAKING> {
            on<Event.OnHangUp> {
                if (fromRemoteHangUp) {
                    Logger.d("对方挂断")
                } else {
                    Logger.d("挂断")
                }
                transitionTo(PhoneState.IDLE, SideEffect.LogOnHangUp)
            }
        }
        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            when (validTransition.sideEffect) {
                SideEffect.LogOnDial -> {
                    netPhone.listener.onHostDial(dialBean)
                }
                SideEffect.LogOnReceive -> {
                    startMediaPlayer()
                    netPhone.listener.onClientReceiveDial(remoteAccount, dialBean)
                }
                SideEffect.LogOnAccept -> {
                    stopMediaPlayer()
                }
                SideEffect.LogOnJoin -> {

                }
                SideEffect.LogOnSpeaking -> {
                    rtcEngine.setEnableSpeakerphone(false)
                    startTime = System.currentTimeMillis()
                    netPhone.listener.onReadyToSpeak(remoteAccount)
                }
                SideEffect.LogOnHangUp -> {
                    stopMediaPlayer()
                    val connected = validTransition.fromState == PhoneState.SPEAKING
                    val totalDuration = if (connected) speakTime else 0
                    if (fromRemoteHangUp) {
                        netPhone.listener.onRemoteHangUp(connected, totalDuration, remoteAccount)
                    } else {
                        netPhone.listener.onHangUp(connected, totalDuration)
                    }
                }
            }
        }
    }


    sealed class Event {
        object OnDial : Event()
        object OnReceive : Event()
        object OnAccept : Event()
        object OnJoin1 : Event()
        object OnJoin2 : Event()
        object OnJoin3 : Event()
        object OnHangUp : Event()
        object OnHangUpAuto : Event()
    }

    sealed class SideEffect() {
        object LogOnDial : SideEffect()
        object LogOnReceive : SideEffect()
        object LogOnAccept : SideEffect()
        object LogOnJoin : SideEffect()
        object LogOnSpeaking : SideEffect()
        object LogOnHangUp : SideEffect()
    }



    private fun startMediaPlayer() {
        if (NetPhone.getInstance().getTestCase() > 0) {
            //单元测试时不开启声音
            return
        }
        try {
            val fileDescriptor = netPhone.mContext.assets.openFd("voice.mp3")
            mMediaPlayer.setDataSource(fileDescriptor.fileDescriptor, fileDescriptor.startOffset, fileDescriptor.length)
            mMediaPlayer.prepare()
        } catch (e: IOException) {
            Logger.e(e.message)
        }

        mMediaPlayer.start()
        mMediaPlayer.setOnCompletionListener { mediaPlayer: MediaPlayer? ->
            mMediaPlayer.start()
            mMediaPlayer.isLooping = true
        }
    }

    private fun stopMediaPlayer() {
        if (mMediaPlayer.isPlaying) {
            mMediaPlayer.reset()
        }
    }
}