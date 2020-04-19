package wang.unclecat.agora

import io.agora.rtc.RtcEngine
import wang.unclecat.agora.utils.Logger

/**
 * 主叫
 *
 * @author 喵叔catuncle    19-12-28
 */
internal class CallingSide(private val netPhone: NetPhone, private val mRtcEngine: RtcEngine,
                           private val internal: InternalImpl) : Internal by internal {


    /**
     * 拨打
     *
     * @param remoteAccount 被叫方id
     * @param dialBean      拨打参数
     */
    fun dial(remoteAccount: String, dialBean: DialBean?): Boolean {
        Logger.d("dial with: remoteAccount = $remoteAccount, dialBean = $dialBean")
        if (dialBean != null) {
            // TODO: by catuncle 19-12-28 对方未登录，怎么处理
            if (netPhone.checkStatus()) {
                mRtcEngine.setDefaultAudioRoutetoSpeakerphone(false)
                netPhone.sendPeerMessage(remoteAccount, PhoneMsg.createDialMsg(dialBean).toJsonString())
                internal.remoteAccount = remoteAccount
                transition(InternalImpl.Event.OnDial)
            } else {
                Logger.d("用户未登录")
            }
            return true
        }
        return false
    }

    fun join(channel: String?) {
        netPhone.joinChannel(channel)
    }

    fun handleOnJoinChannelSuccess() {
        netPhone.sendPeerMessage(internal.remoteAccount, PhoneMsg.createJoinedMsg().toJsonString())
        transition(InternalImpl.Event.OnJoin2)
    }

}