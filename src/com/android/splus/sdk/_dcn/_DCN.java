package com.android.splus.sdk._dcn;

import com.android.splus.sdk.apiinterface.IPayManager;
import com.android.splus.sdk.apiinterface.InitBean;
import com.android.splus.sdk.apiinterface.InitBean.InitBeanSuccess;
import com.android.splus.sdk.apiinterface.InitCallBack;
import com.android.splus.sdk.apiinterface.LoginCallBack;
import com.android.splus.sdk.apiinterface.LogoutCallBack;
import com.android.splus.sdk.apiinterface.RechargeCallBack;
import com.android.splus.sdk.apiinterface.UserAccount;
import com.downjoy.Downjoy;
import com.downjoy.DownjoyError;
import com.downjoy.util.Util;

import android.app.Activity;
import android.os.Bundle;

import java.util.Properties;

public class _DCN implements IPayManager{

    private static final String TAG = "_DCN";

    private static _DCN _mDCN;

    // 平台参数
    private Properties mProperties;

    private String mAppId;

    private String mAppkey;

    private String mMerchantId;

    private String mServerSeqNum;

    private InitBean mInitBean;

    private InitCallBack mInitCallBack;

    private Activity mActivity;

    private LoginCallBack mLoginCallBack;

    private RechargeCallBack mRechargeCallBack;

    private LogoutCallBack mLogoutCallBack;


    // 下面参数仅在测试时用
    private UserAccount mUserModel;

    private int mUid = 0;

    private String mPassport = "";

    private String mSessionid = "";

    private Downjoy mDownjoyinstance;


    /**
     * @Title: _DCN
     * @Description:( 将构造函数私有化)
     */
    private _DCN() {

    }

    /**
     * @Title: getInstance(获取实例)
     * @author xiaoming.yuan
     * @data 2014-2-26 下午2:30:02
     * @return _DCN 返回类型
     */
    public static _DCN getInstance() {

        if (_mDCN == null) {
            synchronized (_DCN.class) {
                if (_mDCN == null) {
                    _mDCN = new _DCN();
                }
            }
        }
        return _mDCN;
    }

    @Override
    public void setInitBean(InitBean bean) {
        this.mInitBean = bean;
        this.mProperties = mInitBean.getProperties();
        if (mProperties != null) {
            mAppId = mProperties.getProperty("dcn_appid") == null ? "0" : mProperties.getProperty("dcn_appid");
            mAppkey = mProperties.getProperty("dcn_appkey") == null ? "0" : mProperties.getProperty("dcn_appkey");
            mServerSeqNum = mProperties.getProperty("dcn_serverSeqNum") == null ? "1" : mProperties.getProperty("dcn_serverSeqNum");
            mMerchantId = mProperties.getProperty("dcn_merchantId") == null ? "0" : mProperties.getProperty("dcn_merchantId");
        }
    }

    @Override
    public void init(Activity activity, Integer gameid, String appkey, InitCallBack initCallBack, boolean useUpdate, Integer orientation) {

        this.mInitCallBack = initCallBack;
        this.mActivity = activity;
        mInitBean.initSplus(activity, initCallBack ,new InitBeanSuccess() {
            @Override
            public void initBeaned(boolean initBeanSuccess) {
                mDownjoyinstance = Downjoy.getInstance(mActivity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
                mInitCallBack.initSuccess("初始化成功", null);
            }});
    }

    @Override
    public void login(Activity activity, LoginCallBack loginCallBack) {
        this.mActivity=activity;
        this.mLoginCallBack=loginCallBack;
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.openLoginDialog(activity, mLoginCallbackListener);

    }

    com.downjoy.CallbackListener mLoginCallbackListener=new com.downjoy.CallbackListener(){

        @Override
        public void onLoginSuccess(Bundle bundle) {
            String memberId = bundle
                            .getString(Downjoy.DJ_PREFIX_STR + "mid");
            String username = bundle
                            .getString(Downjoy.DJ_PREFIX_STR + "username");
            String nickname = bundle
                            .getString(Downjoy.DJ_PREFIX_STR + "nickname");
            String token = bundle.getString(Downjoy.DJ_PREFIX_STR
                            + "token");

            Util.alert(mActivity, "mid:" + memberId + "\nusername:"
                            + username + "\nnickname:" + nickname
                            + "\ntoken:" + token);
            mLoginCallBack.loginSuccess(null);

        }

        @Override
        public void onLoginError(DownjoyError error) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            if(errorCode==100){
                mLoginCallBack.backKey(errorMsg);
            }else{
                mLoginCallBack.loginFaile(errorMsg);
            }
            Util.alert(mActivity, "onLoginError:" + errorCode + "|"+ errorMsg);
        }

        @Override
        public void onError(Error error) {
            String errorMessage = error.getMessage();
            Util.alert(mActivity, "onError:" + errorMessage);
            mLoginCallBack.loginFaile(errorMessage);
        }

    };

    @Override
    public void recharge(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, RechargeCallBack rechargeCallBack) {
        rechargeByQuota(activity, serverId, serverName, roleId, roleName, outOrderid, pext, 0f, rechargeCallBack);
    }

    @Override
    public void rechargeByQuota(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, Float money, RechargeCallBack rechargeCallBack) {
        this.mActivity = activity;
        this.mRechargeCallBack = rechargeCallBack;

        String productName = "测试商品"; // 商品名称
        String extInfo = "123"; // CP自定义信息，多为CP订单号

        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.openPaymentDialog(activity,money, productName, extInfo, mRechargeCallbackListener);

    }


    com.downjoy.CallbackListener mRechargeCallbackListener=new com.downjoy.CallbackListener(){


        @Override
        public void onPaymentSuccess(String orderNo) {
            Util.alert(mActivity,"payment success! \n orderNo:"+ orderNo);
            mRechargeCallBack.rechargeSuccess(null);
        }

        @Override
        public void onPaymentError(DownjoyError error,
                        String orderNo) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            if(errorCode==103){
                mRechargeCallBack.backKey(errorMsg);

            }else{
                mRechargeCallBack.rechargeFaile(errorMsg);

            }
            Util.alert(mActivity, "onPaymentError:"
                            + errorCode + "|" + errorMsg
                            + "\n orderNo:" + orderNo);
        }

        @Override
        public void onError(Error error) {
            Util.alert(mActivity,"onError:" + error.getMessage());
        }
    };

    @Override
    public void exitSDK() {


    }


    @Override
    public void logout(Activity activity, LogoutCallBack logoutCallBack) {
        this.mLogoutCallBack=logoutCallBack;
        this.mActivity=activity;
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.logout(activity, mLogoutCallbackListener1);

    }


    com.downjoy.CallbackListener mLogoutCallbackListener1=new com.downjoy.CallbackListener(){



        @Override
        public void onLogoutSuccess() {
            Util.alert(mActivity, "logout ok");
            mLogoutCallBack.logoutCallBack();
        }

        @Override
        public void onLogoutError(DownjoyError error) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            Util.alert(mActivity, "onLogoutError:" + errorCode + "|" + errorMsg);
        }

        @Override
        public void onError(Error error) {
            Util.alert(mActivity, "onError:" + error.getMessage());
        }


    };
    @Override
    public void setDBUG(boolean logDbug) {
    }

    @Override
    public void enterUserCenter(Activity activity, LogoutCallBack logoutCallBack) {
        this.mActivity=activity;
        this.mLogoutCallBack=logoutCallBack;
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.openMemberCenterDialog(activity, mLogoutCallbackListener);
    }
    com.downjoy.CallbackListener mLogoutCallbackListener=new com.downjoy.CallbackListener(){


        @Override
        public void onSwitchAccountAndRestart() {
           mLogoutCallBack.logoutCallBack();

        }

        @Override
        public void onError(Error error) {
            String errorMessage = error.getMessage();
            Util.alert(mActivity, "onError:" + errorMessage);
        }


    };


    @Override
    public void sendGameStatics(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String level) {
    }

    @Override
    public void enterBBS(Activity activity) {
    }

    @Override
    public void creatFloatButton(Activity activity, boolean showlasttime, int align, float position) {
        // 设置登录成功后属否显示当乐游戏中心的悬浮按钮
        // 注意：
        // 此处应在调用登录接口之前设置，默认值是true，即登录成功后自动显示当乐游戏中心的悬浮按钮。
        // 如果此处设置为false，登录成功后，不显示当乐游戏中心的悬浮按钮。
        int place = 1;
        if (align == 0 && position < 0.5f) {
            place =Downjoy.LOCATION_LEFT_TOP;
        } else if (align == 0 && position == 0.5f) {
            place = Downjoy.LOCATION_LEFT_CENTER_VERTICAL;
        } else if (align == 0 && position > 0.5f) {
            place =  Downjoy.LOCATION_LEFT_BOTTOM;
        } else if (align !=0 && position < 0.5f) {
            place = Downjoy.LOCATION_RIGHT_TOP;
        } else if (align !=0 && position == 0.5f) {
            place = Downjoy.LOCATION_RIGHT_CENTER_VERTICAL;
        } else if (align !=0 && position > 0.5f) {
            place = Downjoy.LOCATION_RIGHT_BOTTOM;
        }
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.showDownjoyIconAfterLogined(true);
        mDownjoyinstance.setInitLocation(place);

    }

    @Override
    public void onResume(Activity activity) {
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.resume(activity);

    }

    @Override
    public void onPause(Activity activity) {
//        if (mDownjoyinstance == null) {
//            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
//        }
//        mDownjoyinstance.pause();


    }

    @Override
    public void onStop(Activity activity) {
    }

    @Override
    public void onDestroy(Activity activity) {
        if (mDownjoyinstance == null) {
            mDownjoyinstance=Downjoy.getInstance(activity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
        }
        mDownjoyinstance.destroy();

    }
}

