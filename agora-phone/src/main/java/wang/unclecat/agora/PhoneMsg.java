package wang.unclecat.agora;

import com.google.gson.Gson;

/**
 * 信令:自定义消息
 *
 * @author 喵叔catuncle    19-3-10
 */
class PhoneMsg {
    public static final int CALLING = 1;//来电(主叫 ---> 被叫)
    //for test-----
    public static final int CALLING_test1 = 10;//主叫断挂
    public static final int CALLING_test2 = 11;//被叫断挂
    public static final int CALLING_test3 = 12;//拒接
    public static final int CALLING_test4 = 13;//取消息呼叫
    //end for test-----
    public static final int ACCEPT = 2;//接听(被叫 ---> 主叫)
    public static final int JOINED = 3;//就绪(主叫 ---> 被叫)//主叫进入被叫相同的房间
    public static final int HANGUP = 4;//挂断(主叫或被叫都可发起)
    public static final int HANGUP_AUTO = 5;//占线(被叫 ---> 主叫)
    private int type;
    private String des;
    private String msg;

    public static PhoneMsg createDialMsg(DialBean bean) {

        if (NetPhone.getInstance().getTestCase() == 0) {

            return new PhoneMsg(CALLING, "来电(主叫 ---> 被叫)", bean.toString());
        } else {

            return new PhoneMsg(NetPhone.getInstance().getTestCase(), "test:来电(主叫 ---> 被叫)", bean.toString());
        }

    }

    public static PhoneMsg createAcceptMsg(String roomId) {
        return new PhoneMsg(ACCEPT, "接听(被叫 ---> 主叫)", roomId);
    }

    //主叫进入房间后，同步信息给被叫
    public static PhoneMsg createJoinedMsg() {
        return new PhoneMsg(JOINED, "就绪(主叫 ---> 被叫)", "");
    }

    public static PhoneMsg createHangUpMsg() {
        return new PhoneMsg(HANGUP, "挂断(主叫或被叫都可发起)", "");
    }

    public static PhoneMsg createHangUpAutoMsg() {
        return new PhoneMsg(HANGUP_AUTO, "占线(被叫 ---> 主叫)", "");
    }

    private PhoneMsg(int type, String des, String msg) {
        this.type = type;
        this.des = des;
        this.msg = msg;
    }

    public int getType() {
        return type;
    }

    public String getMsg() {
        return msg;
    }

    public String toJsonString() {
        return new Gson().toJson(this);
    }
}
