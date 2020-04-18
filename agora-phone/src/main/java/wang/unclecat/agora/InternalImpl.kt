package wang.unclecat.agora

import io.agora.rtc.RtcEngine
import wang.unclecat.agora.utils.Logger

internal class InternalImpl(private val netPhone: NetPhone,
                            private val rtcEngine: RtcEngine,
                            private val phoneSM: PhoneSM) : Internal {

    var remoteAccount: String? = null
    var startTime: Long = 0
    private var fromRemoteHangUp = false

    /**
     * 是否被激活(即非idle状态)
     */
    override fun isActive(): Boolean {
        val ca = phoneSM.currentState
        return ca > PhoneState.IDLE
    }

    /**
     * 当前状态
     */
    override fun getState(): PhoneState {
        return phoneSM.currentState
    }

    /**
     * 挂断
     */
    override fun hangUp() {
        fromRemoteHangUp = false
        val ca = phoneSM.currentState
        Logger.d("hangUp() called with: ca = $ca")

        //告诉对方挂断呼叫
        if (ca > PhoneState.IDLE) {
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
        val ca = phoneSM.currentState
        Logger.d("hangUp2() called with: ca = $ca")

        //挂断
        doHangUp(ca)
    }

    private fun doHangUp(ca: PhoneState) {
        if (ca >= PhoneState.CONNECTED) {
            rtcEngine.leaveChannel()
        } else {
            onHangUp()
        }
    }

    /**
     * 挂断后更新状态
     */
    override fun onHangUp() {
        Logger.d("onHangUp() called")
        netPhone.testCase = 0
        phoneSM.fire(PhoneSM.Event.HangUp)
    }

    val speakTime: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000).toInt()

    fun handleHangUp(from: PhoneState) {
        val connected = from == PhoneState.SPEAKING
        val totalDuration = if (connected) speakTime else 0
        if (fromRemoteHangUp) {
            netPhone.listener.onRemoteHangUp(connected, totalDuration, remoteAccount)
        } else {
            netPhone.listener.onHangUp(connected, totalDuration)
        }
    }
}