package wang.unclecat.agora;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import wang.unclecat.agora.utils.Logger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class NetPhoneTest {

    @BeforeClass
    public static void setUp() {
        Logger.d("setUp() called");
        Context appContext = InstrumentationRegistry.getTargetContext();
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> NetPhone.getInstance().init(appContext, "c80d5d52732f48caa15da50fac4b5f80", "100"));
    }

    @AfterClass
    public static void tearDown() {
        Logger.d("tearDown() called");
    }


    //接通后，主叫挂断
    @Test
    public void test1() {
        System.out.println("-----------------test1() called");
        sleep(2000);

        NetPhone.getInstance().setTestCase(PhoneMsg.CALLING_test1);
        dial();

        sleep(100);
        assertEquals(PhoneState.CONNECTING, NetPhone.getInstance().getHostState());
        System.out.println("--------CONNECTING---------");


        sleep(2000);
        assertEquals(PhoneState.SPEAKING, NetPhone.getInstance().getHostState());
        System.out.println("--------SPEAKING---------");

        NetPhone.getInstance().hangUp();
        sleep(1500);
        assertEquals(PhoneState.IDLE, NetPhone.getInstance().getHostState());
        System.out.println("----------IDLE-------");
    }

    //接通后，被叫挂断
    @Test
    public void test2() {
        System.out.println("-----------------test2() called");
    }

    //拒接
    @Test
    public void test3() {
        System.out.println("-----------------test3() called");
        sleep(2000);

        NetPhone.getInstance().setTestCase(PhoneMsg.CALLING_test3);
        dial();

        sleep(100);
        assertEquals(PhoneState.CONNECTING, NetPhone.getInstance().getHostState());
        System.out.println("--------CONNECTING---------");

        sleep(2500);
        assertEquals(PhoneState.IDLE, NetPhone.getInstance().getHostState());
        System.out.println("----------IDLE-------");
    }

    //接通前，取消呼叫
    @Test
    public void test4() {
        System.out.println("-----------------test4() called");
        sleep(2000);


        NetPhone.getInstance().setTestCase(PhoneMsg.CALLING_test4);
        dial();

        sleep(100);
        assertEquals(PhoneState.CONNECTING, NetPhone.getInstance().getHostState());
        System.out.println("--------CONNECTING---------");

        NetPhone.getInstance().hangUp();
        sleep(1500);
        assertEquals(PhoneState.IDLE, NetPhone.getInstance().getHostState());
        System.out.println("----------IDLE-------");
    }

    private static void sleep(long time) {

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private void dial() {
        DialBean bean = new DialBean();
        bean.setName("100小王");
        boolean dial = NetPhone.getInstance().dial("200", bean);

        assertTrue(dial);
        System.out.println("--------dial ok---------");
    }
}