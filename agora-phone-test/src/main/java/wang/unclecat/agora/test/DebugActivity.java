package wang.unclecat.agora.test;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;

import wang.unclecat.agora.NetPhone;
import wang.unclecat.agora.DefaultNetPhoneListener;
import wang.unclecat.agora.DialBean;
import wang.unclecat.agora.utils.Logger;

public class DebugActivity extends FragmentActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug);

        NetPhone.getInstance().addListener(new DefaultNetPhoneListener() {
            @Override
            public void onHostDial(DialBean dialBean) {
                Logger.d("onHostDial() called with: dialBean = [" + dialBean + "]");
            }

            @Override
            public void onClientReceiveDial(String remoteAccount, DialBean bean) {
                Logger.d("onClientReceiveDial() called with: remoteAccount = [" + remoteAccount + "], bean = [" + bean + "]");
            }

            @Override
            public void onReadyToSpeak(String remoteAccount) {
                Logger.d("onReadyToSpeak() called with: remoteAccount = [" + remoteAccount + "]");
            }

            @Override
            public void onTick(int elapsedTime) {
                Logger.d("onTick() called with: elapsedTime = [" + elapsedTime + "]");
            }

            @Override
            public void onHangUp(boolean connected, int totalDuration) {
                Logger.d("onHangUp() called with: connected = [" + connected + "], totalDuration = [" + totalDuration + "]");
            }

            @Override
            public void onRemoteHangUp(boolean connected, int totalDuration, String remoteAccount) {
                Logger.d("onRemoteHangUp() called with: connected = [" + connected + "], totalDuration = [" + totalDuration + "], remoteAccount = [" + remoteAccount + "]");
            }
        });

        setBtn(R.id.init1, "init 100", v -> NetPhone.getInstance().init(getApplicationContext(), "c80d5d52732f48caa15da50fac4b5f80", "100"));

        setBtn(R.id.init2, "init 200", v -> NetPhone.getInstance().init(getApplicationContext(), "c80d5d52732f48caa15da50fac4b5f80", "200"));

        setBtn(R.id.dial, "100 dial 200", v -> {
            DialBean bean = new DialBean();
            bean.setName("100小王");
            boolean dial = NetPhone.getInstance().dial("200", bean);
            if (!dial) {
                ToastUtils.showLong("不能拔打，请检查参数");
            }

        });

        setBtn(R.id.accept, "200 accept 100", v -> NetPhone.getInstance().accept());

        setBtn(R.id.hangup, "hungup挂断", v -> NetPhone.getInstance().hangUp());
        setBtn(R.id.cmd6, "静音", v -> {
            isMute = !isMute;
            NetPhone.getInstance().muteMic(isMute);
        });
    }

    private boolean isMute = false;

    @Override
    protected void onDestroy() {
        NetPhone.getInstance().destroy();
        super.onDestroy();
    }


    private void setBtn(@IdRes int id, String text, View.OnClickListener clickListener) {
        TextView cmd = findViewById(id);

        cmd.setAllCaps(false);
        cmd.setText(text);
        cmd.setOnClickListener(clickListener);
    }
}
