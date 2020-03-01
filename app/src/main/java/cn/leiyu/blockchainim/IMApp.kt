package cn.leiyu.blockchainim

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import cn.leiyu.base.App
import cn.leiyu.base.http.JsonVolleyUtil
import cn.leiyu.base.http.VolleyListenerInterface
import cn.leiyu.base.utils.AddressUtil
import cn.leiyu.base.utils.LogUtil
import cn.leiyu.base.utils.encoded.EcKeyUtils
import cn.leiyu.base.utils.encoded.EcUtils
import cn.leiyu.blockchainim.activity.home.HomeFragment
import cn.leiyu.blockchainim.beans.MsgInterBean
import cn.leiyu.blockchainim.beans.UserBean
import cn.leiyu.blockchainim.db.LocalDBManager
import cn.leiyu.blockchainim.db.tables.MsgOperaDao
import cn.leiyu.blockchainim.db.tables.UserOperaDao
import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.security.interfaces.ECPrivateKey

/**
 * 应用主程序
 */
class IMApp: App() {

    companion object Sub{

        /**
         * 消息回到接口
         */
        @JvmStatic
        var refreshMsgCallback: IRefreshMsgCallback? = null
    }
}

/**
 * 消息回调接口
 */
interface IRefreshMsgCallback{

    /**
     * 发送消息
     */
    fun getMsg(context: Context){
        val tmp = context.getSharedPreferences(Constant.configFileName, Context.MODE_PRIVATE)
            ?.getString(Constant.CURRENT_ACCOUNT, "{}")
        val user = Gson().fromJson(tmp, UserBean::class.java)
        if(TextUtils.isEmpty(user.address))return
        JsonVolleyUtil.request(context,
            Constant.API.SERVICE_HOST,
            "getMsg", """{"command":"getmsg","user":"${user?.address}"}""",
            object : VolleyListenerInterface(context, mListener, mErrorListener){
                override fun onMySuccess(result: String?) {
                    result?.let {
                        try{
                            val tmp = Gson().fromJson(it,
                                object : TypeToken<MsgInterBean>() {}.type) as MsgInterBean
                            if (tmp.errCode == 0 && tmp.data.isNotEmpty()) {
                                //写库
                                val db = LocalDBManager(mContext.get() ?: App.getAppContext())
                                for(bean in tmp.data){
                                    try {
                                        //查询是否是在通讯录范围
                                        val users = db.getTableOperation(UserOperaDao::class.java).query(arrayOf("_id"),
                                            "address = ? and loginId = ?",
                                            arrayOf(bean.from, "${user.loginId}"))
                                        if(users == null || users.isEmpty()){
                                            //用户不再通讯范围 可能是好友
                                            LogUtil.e("IRefreshMsgCallback", "消息库操作 ${bean.from} 用户不在通讯录，请谨慎添加！ ")
                                        }else{
                                            //在通讯录范围
                                            val cv = ContentValues(5)
                                            cv.put("sendId", users[0][0])
                                            cv.put("peerId", user._id)
                                            cv.put("msg", bean.content)
                                            cv.put("time", bean.timestamp)
//                                            cv.put("from", users[0][0])
//                                            cv.put("to", user._id)
//                                            cv.put("content", bean.content)
//                                            cv.put("timestamp", bean.timestamp)
                                            cv.put("isRead", 1)
                                            cv.put("sendSuccess", 1)
                                            db.getTableOperation(MsgOperaDao::class.java)
                                                .insert(null, cv, SQLiteDatabase.CONFLICT_ABORT)

                                            //val addressUtil = AddressUtil(getWeakContext()?.filesDir.toString())
                                            val sign = EcKeyUtils.signReturnBase64(HomeFragment.gPrivateKey, bean.hash.toByteArray());
                                            JsonVolleyUtil.request(context,
                                                Constant.API.SERVICE_HOST,
                                                "complete", """{"command":"complete","from":"${bean.from}","to":"${bean.to}", "hash":"${bean.hash}", "sign":"${sign}"}""",
                                                object : VolleyListenerInterface(context, mListener, mErrorListener) {
                                                    override fun onMySuccess(result: String?) {
                                                        LogUtil.e("SendComplete", result)
                                                    }
                                                    override fun onMyError(error: VolleyError?) {
                                                        LogUtil.e("SendComplete", " 错误 e = $error")
                                                    }
                                                }, false);
                                        }
                                    }catch (e: SQLException){
                                        e.printStackTrace()
                                        LogUtil.e("IRefreshMsgCallback", "消息库操作异常 ${bean.to}  ${e.localizedMessage}")
                                    }
                                }
                                refreshMsg()
                            }
                        }catch (e: JsonSyntaxException){
                            e.printStackTrace()
                            LogUtil.e("IRefreshMsgCallback", "消息格式异常 = ${e.localizedMessage}")
                        }
                    }
                }

                override fun onMyError(error: VolleyError?) {
                    LogUtil.e("IRefreshMsgCallback", "获取消息异常 e = $error")
                }

                val json = """{
	"data": [{
		"msg": "f111",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573978856",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "dwa",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573979968",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "12323df",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573980236",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "shibushi",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573980980",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "shujujieguo",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573981215",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "dfewsfd",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573981499",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "iele",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573982601",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}, {
		"msg": "fd",
		"peerID": "CFWCHAIN2yCGewFrJiWT5GVvqCG15HVV6dQMU9Mf6GZpiuJugdKBjR9NwMPS1otr9fXiZ1XUxeZgtWFTRfJyLRm5ncL6fgDgMXX6JmxhNNKv51S73hwqRxTy2NUA8CYkgXUJQqWRzWGJ6A",
		"time": "1573987887",
		"userID": "OSN2yCG1pAnXhDpNPRDTVkd6asV8bJLyHFeMRDnYHkhAwxMZumRgotu3hVR8Z1QxWzNNRSi4TLstmaH5cwTje1QufotxTLkoAaGaeh2tHUT3w7cRjPS6XTCE25drZbTKLrJvxHHnd"
	}],
	"errCode": "0"
}"""
            },
            false)
    }

    /**
     * 刷新消息
     */
    fun refreshMsg()
}