package wang.unclecat.agora

/**
 * 电话状态
 *
 * @author 喵叔catuncle    20-1-5
 */
sealed class PhoneState {
    object IDLE : PhoneState()
    object RINGING : PhoneState()
    object CONNECTING : PhoneState()
    object CONNECTED : PhoneState()
    object SPEAKING : PhoneState()
}