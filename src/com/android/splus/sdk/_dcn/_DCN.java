package com.android.splus.sdk._dcn;

import com.android.splus.sdk.apiinterface.APIConstants;
import com.android.splus.sdk.apiinterface.DateUtil;
import com.android.splus.sdk.apiinterface.IPayManager;
import com.android.splus.sdk.apiinterface.InitBean;
import com.android.splus.sdk.apiinterface.InitBean.InitBeanSuccess;
import com.android.splus.sdk.apiinterface.InitCallBack;
import com.android.splus.sdk.apiinterface.LoginCallBack;
import com.android.splus.sdk.apiinterface.LoginParser;
import com.android.splus.sdk.apiinterface.LogoutCallBack;
import com.android.splus.sdk.apiinterface.MD5Util;
import com.android.splus.sdk.apiinterface.NetHttpUtil;
import com.android.splus.sdk.apiinterface.NetHttpUtil.DataCallback;
import com.android.splus.sdk.apiinterface.RechargeCallBack;
import com.android.splus.sdk.apiinterface.RequestModel;
import com.android.splus.sdk.apiinterface.UserAccount;
import com.downjoy.Downjoy;
import com.downjoy.DownjoyError;
import com.downjoy.util.Util;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
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

    private int mUid;

    private String mPassport;

    private String mSessionid;

    private float mMoney ;

    private String mPayway="_DCN" ;

    private Downjoy mDownjoyinstance;

    private ProgressDialog mProgressDialog;


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

        private static final long serialVersionUID = 1L;

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
            Log.d(TAG, "mid:" + memberId + "\nusername:"+ username + "\nnickname:" + nickname+ "\ntoken:" + token);
            HashMap<String, Object> params = new HashMap<String, Object>();
            Integer gameid = mInitBean.getGameid();
            String partner = mInitBean.getPartner();
            String referer = mInitBean.getReferer();
            long unixTime = DateUtil.getUnixTime();
            String deviceno=mInitBean.getDeviceNo();
            String signStr =deviceno+gameid+partner+referer+unixTime+mInitBean.getAppKey();
            String sign=MD5Util.getMd5toLowerCase(signStr);

            params.put("deviceno", deviceno);
            params.put("gameid", gameid);
            params.put("partner",partner);
            params.put("referer", referer);
            params.put("time", unixTime);
            params.put("sign", sign);
            params.put("partner_sessionid", "");
            params.put("partner_uid",  memberId);
            params.put("partner_token", token);
            params.put("partner_nickname", nickname);
            params.put("partner_username", username);
            params.put("partner_appid", mAppId);
            String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.LOGIN_URL);
            System.out.println(hashMapTOgetParams);
            showProgressDialog(mActivity);
            NetHttpUtil.getDataFromServerPOST(mActivity,new RequestModel(APIConstants.LOGIN_URL, params, new LoginParser()),mLoginDataCallBack);

        }

        @Override
        public void onLoginError(DownjoyError error) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            Log.d(TAG, "onLoginError:" + errorCode + "|"+ errorMsg);
            if(errorCode==100){
                mLoginCallBack.backKey(errorMsg);
            }else{
                mLoginCallBack.loginFaile(errorMsg);
            }

        }

        @Override
        public void onError(Error error) {
            String errorMessage = error.getMessage();
            Log.d(TAG, "onError:" + errorMessage);
            mLoginCallBack.loginFaile(errorMessage);
        }

    };


    private DataCallback<JSONObject> mLoginDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            closeProgressDialog();
            Log.d(TAG, "mLoginDataCallBack---------"+paramObject.toString());
            try {
                if (paramObject != null && paramObject.optInt("code") == 1) {
                    JSONObject data = paramObject.optJSONObject("data");
                    mUid = data.optInt("uid");
                    mPassport = data.optString("passport");
                    mSessionid = data.optString("sessionid");
                    mUserModel=new UserAccount() {

                        @Override
                        public Integer getUserUid() {
                            return mUid;

                        }

                        @Override
                        public String getUserName() {
                            return mPassport;

                        }

                        @Override
                        public String getSession() {
                            return mSessionid;

                        }
                    };
                    mLoginCallBack.loginSuccess(mUserModel);

                } else {
                    mLoginCallBack.loginFaile(paramObject.optString("msg"));
                }
            } catch (Exception e) {
                mLoginCallBack.loginFaile(e.getLocalizedMessage());
            }
        }

        @Override
        public void callbackError(String error) {
            closeProgressDialog();
            mLoginCallBack.loginFaile(error);
        }

    };

    @Override
    public void recharge(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, RechargeCallBack rechargeCallBack) {
        rechargeByQuota(activity, serverId, serverName, roleId, roleName, outOrderid, pext, 0f, rechargeCallBack);
    }

    @Override
    public void rechargeByQuota(Activity activity, final Integer serverId, final String serverName, final Integer roleId, final String roleName, final String outOrderid, final String pext, Float money, RechargeCallBack rechargeCallBack) {
        this.mActivity = activity;
        this.mRechargeCallBack = rechargeCallBack;
        this.mMoney=money;
        // 已经是登录状态
        if (mMoney == 0) {
            final EditText editText = new EditText(activity);
            InputFilter[] filters = { new InputFilter.LengthFilter(6) };
            editText.setFilters( filters );
            editText.setInputType( InputType.TYPE_CLASS_NUMBER );
            new AlertDialog.Builder(activity).setTitle("请输入金额")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(TextUtils.isEmpty(editText.getText().toString())||editText.getText().toString().startsWith("0")){
                        Toast.makeText(mActivity, "请输入金额", Toast.LENGTH_SHORT).show();
                        return;
                    }else{
                        mMoney=Float.parseFloat(editText.getText().toString());
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        Integer gameid = mInitBean.getGameid();
                        String partner = mInitBean.getPartner();
                        String referer = mInitBean.getReferer();
                        long unixTime = DateUtil.getUnixTime();
                        String deviceno=mInitBean.getDeviceNo();
                        String signStr =gameid+serverName+deviceno+referer+partner+mUid+mMoney+mPayway+unixTime+mInitBean.getAppKey();
                        String sign=MD5Util.getMd5toLowerCase(signStr);

                        params.put("deviceno", deviceno);
                        params.put("gameid", gameid);
                        params.put("partner",partner);
                        params.put("referer", referer);
                        params.put("time", unixTime);
                        params.put("sign", sign);
                        params.put("uid",mUid);
                        params.put("passport",mPassport);
                        params.put("serverId",serverId);
                        params.put("serverName",serverName);
                        params.put("roleId",roleId);
                        params.put("roleName",roleName);
                        params.put("money",mMoney);
                        params.put("pext",pext);
                        params.put("money",mMoney);
                        params.put("payway",mPayway);
                        params.put("outOrderid",outOrderid);
                        String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.PAY_URL);
                        System.out.println(hashMapTOgetParams);
                        NetHttpUtil.getDataFromServerPOST(mActivity, new RequestModel(APIConstants.PAY_URL, params,new LoginParser()),mRechargeDataCallBack);

                    }
                }

            }).show();

        }else{
            HashMap<String, Object> params = new HashMap<String, Object>();
            Integer gameid = mInitBean.getGameid();
            String partner = mInitBean.getPartner();
            String referer = mInitBean.getReferer();
            long unixTime = DateUtil.getUnixTime();
            String deviceno=mInitBean.getDeviceNo();
            String signStr =gameid+serverName+deviceno+referer+partner+mUid+mMoney+mPayway+unixTime+mInitBean.getAppKey();
            String sign=MD5Util.getMd5toLowerCase(signStr);

            params.put("deviceno", deviceno);
            params.put("gameid", gameid);
            params.put("partner",partner);
            params.put("referer", referer);
            params.put("time", unixTime);
            params.put("sign", sign);
            params.put("uid",mUid);
            params.put("passport",mPassport);
            params.put("serverId",serverId);
            params.put("serverName",serverName);
            params.put("roleId",roleId);
            params.put("roleName",roleName);
            params.put("money",mMoney);
            params.put("pext",pext);
            params.put("payway",mPayway);
            params.put("outOrderid",outOrderid);
            String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.PAY_URL);
            System.out.println(hashMapTOgetParams);
            NetHttpUtil.getDataFromServerPOST(activity, new RequestModel(APIConstants.PAY_URL, params,new LoginParser()),mRechargeDataCallBack);

        }


    }

    private DataCallback<JSONObject> mRechargeDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            Log.d(TAG, "mRechargeDataCallBack---------"+paramObject.toString());

            if (paramObject != null && (paramObject.optInt("code") == 1||paramObject.optInt("code") == 24)) {
                JSONObject data = paramObject.optJSONObject("data");
                String orderid=data.optString("orderid");
                if (mDownjoyinstance == null) {
                    mDownjoyinstance=Downjoy.getInstance(mActivity, mMerchantId, mAppId,mServerSeqNum, mAppkey);
                }
                mDownjoyinstance.openPaymentDialog(mActivity,mMoney, "游戏道具", orderid, mRechargeCallbackListener);

            }else {
                Log.d(TAG, paramObject.optString("msg"));
                mRechargeCallBack.rechargeFaile(paramObject.optString("msg"));
            }

        }

        @Override
        public void callbackError(String error) {
            Log.d(TAG, error);
            mRechargeCallBack.rechargeFaile(error);

        }

    };


    com.downjoy.CallbackListener mRechargeCallbackListener=new com.downjoy.CallbackListener(){

        private static final long serialVersionUID = 1L;

        @Override
        public void onPaymentSuccess(String orderNo) {
            Util.alert(mActivity,"payment success! \n orderNo:"+ orderNo);
            mRechargeCallBack.rechargeSuccess(mUserModel);
        }

        @Override
        public void onPaymentError(DownjoyError error,String orderNo) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            if(errorCode==103){
                mRechargeCallBack.backKey(errorMsg);

            }else{
                mRechargeCallBack.rechargeFaile(errorMsg);

            }
            Log.d(TAG, "onPaymentError:"+ errorCode + "|" + errorMsg+ "\n orderNo:" + orderNo);
        }

        @Override
        public void onError(Error error) {
            Log.d(TAG, "onError:" + error.getMessage());
            mRechargeCallBack.rechargeFaile(error.getMessage());
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
        private static final long serialVersionUID = 1L;

        @Override
        public void onLogoutSuccess() {
            mLogoutCallBack.logoutCallBack();
        }

        @Override
        public void onLogoutError(DownjoyError error) {
            int errorCode = error.getMErrorCode();
            String errorMsg = error.getMErrorMessage();
            mLogoutCallBack.logoutCallBack();
            Log.d(TAG, "onLogoutError:" + errorCode + "|" + errorMsg);
        }

        @Override
        public void onError(Error error) {
            Log.d(TAG, "onError:" + error.getMessage());
            mLogoutCallBack.logoutCallBack();
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

        private static final long serialVersionUID = 1L;

        @Override
        public void onSwitchAccountAndRestart() {
            mLogoutCallBack.logoutCallBack();

        }

        @Override
        public void onError(Error error) {
            Log.d(TAG, "onError:" + error.getMessage());
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

    /**
     * @return void 返回类型
     * @Title: showProgressDialog(设置进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:36
     */
    protected void showProgressDialog(Activity activity) {
        if (! activity.isFinishing()) {
            try {
                this.mProgressDialog = new ProgressDialog(activity);// 实例化
                // 设置ProgressDialog 的进度条style
                this.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条风格，风格为圆形，旋转的
                this.mProgressDialog.setTitle("登陆");
                this.mProgressDialog.setMessage("加载中...");// 设置ProgressDialog 提示信息
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setIndeterminate(false);
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setCancelable(false);
                this.mProgressDialog.setCanceledOnTouchOutside(false);
                this.mProgressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @return void 返回类型
     * @Title: closeProgressDialog(关闭进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:30
     */
    protected void closeProgressDialog() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing())
            this.mProgressDialog.dismiss();
    }

}

