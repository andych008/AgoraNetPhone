package wang.unclecat.agora

/**
 * 电话状态
 *
 * @author 喵叔catuncle    20-1-5
 */
sealed class PhoneState {
    object Idle : PhoneState()
    object ACalling : PhoneState()
    object BRinging : PhoneState()
    object BAccepting : PhoneState()
    object BJoined : PhoneState()
    object Speaking : PhoneState()
}