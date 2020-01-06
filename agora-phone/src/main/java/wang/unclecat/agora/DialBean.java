package wang.unclecat.agora;


import com.google.gson.Gson;
import com.google.gson.annotations.Expose;


/**
 * 拨打电话传入的参数(比如头像、名字等信息)
 *
 * @author 喵叔catuncle    19-2-28
 */
public class DialBean {

    @Expose
    private String userId;
    @Expose
    private String name;
    @Expose
    private String avatar;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
