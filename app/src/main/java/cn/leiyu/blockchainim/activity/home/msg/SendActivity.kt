package cn.leiyu.blockchainim.activity.home.msg

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ListView
import butterknife.BindView
import butterknife.OnClick
import cn.leiyu.base.http.JsonVolleyUtil
import cn.leiyu.base.http.VolleyListenerInterface
import cn.leiyu.base.utils.BaseRefreshUtil
import cn.leiyu.blockchainim.Constant
import cn.leiyu.blockchainim.IMApp
import cn.leiyu.blockchainim.IRefreshMsgCallback
import cn.leiyu.blockchainim.R
import cn.leiyu.blockchainim.activity.SubBaseActivity
import cn.leiyu.blockchainim.adapters.SendMsgAdapter
import cn.leiyu.blockchainim.beans.MsgBean
import cn.leiyu.blockchainim.beans.UserBean
import cn.leiyu.blockchainim.db.LocalDBManager
import cn.leiyu.blockchainim.db.tables.MsgOperaDao
import com.android.volley.VolleyError
import com.chanven.lib.cptr.PtrClassicFrameLayout
import org.json.JSONObject
import java.security.MessageDigest
import cn.leiyu.blockchainim.utils.Base58

/**
 * 消息-发送
 */
class SendActivity: SubBaseActivity(), BaseRefreshUtil.IRefreshCallback<SendActivity>
    , IRefreshMsgCallback {

    @BindView(android.R.id.list)
    lateinit var listView: ListView
    @BindView(R.id.refresh_view)
    lateinit var refreshView: PtrClassicFrameLayout
    @BindView(R.id.inputMsg)
    lateinit var mETMsg: EditText
    private lateinit var msgOperaDao: MsgOperaDao

    private lateinit var refreshUtil: BaseRefreshUtil<SendActivity>
    private lateinit var msgAdapter: SendMsgAdapter
    private var page = 1

    private lateinit var msgBean: MsgBean
    private lateinit var userBean: UserBean
    private lateinit var sharedEdit: SharedPreferences.Editor

    override fun getLayoutId(): Int {
        return R.layout.activity_sendmsg
    }

    override fun initView() {
        refreshUtil = BaseRefreshUtil(this, refreshView)
        refreshUtil.initView()
        refreshView.isLoadMoreEnable = false
    }

    override fun initData() {
        userBean = getUser()
        msgBean = intent.getParcelableExtra("bean")!!
        topTitle.text = msgBean.peerName
        msgOperaDao = LocalDBManager(this).getTableOperation(MsgOperaDao::class.java)
        //将未读数置空
        msgOperaDao.update(msgBean)
        //是否存在草稿
        val shared = getSharedPreferences(Constant.configFileName, Context.MODE_PRIVATE)
        mETMsg.setText(shared.getString("${Constant.SUBFIX_DRAFT}${msgBean.peerId}", ""))
//        sharedEdit = shared.edit()
//        if(mETMsg.text.toString() != "")
//            sharedEdit.putString("${Constant.SUBFIX_DRAFT}${msgBean.peerId}", "").apply()
        msgAdapter = SendMsgAdapter(this, arrayListOf(), msgBean)
        listView.adapter = msgAdapter
        onPullDownRefresh()
    }

    override fun onStart() {
        super.onStart()
        IMApp.refreshMsgCallback = this
    }

    override fun onDestroy() {
        super.onDestroy()
        IMApp.refreshMsgCallback = null
    }

    override fun onBackPressed() {
//        val msg = mETMsg.text.toString().trim()
//        if(!TextUtils.isEmpty(msg)){
//            //有信息需要保存到草稿
//            sharedEdit.putString("${Constant.SUBFIX_DRAFT}${msgBean.peerId}", msg)
//                .apply()
//        }
        super.onBackPressed()
    }

    @OnClick(R.id.sendMsg, R.id.inputMsg)
    override fun onClick(v: View?) {
        when(v?.id){
//            R.id.inputMsg-> listView.smoothScrollToPosition(msgAdapter.count)
            R.id.sendMsg->{
                try{
                    val msg = mETMsg.text.toString().trim()
                    if(TextUtils.isEmpty(msg)){
                        showToast(getString(R.string.dont_null, getString(R.string.msg_title)))
                        return
                    }
                    v.isEnabled = false
                    sendMsg(msg)
                }catch (e: Exception){
                    e.printStackTrace()
                    v.isEnabled = true
                }
            }
            else-> super.onClick(v)
        }
    }

    override fun onPullDownRefresh() {
        val data = msgOperaDao.query(msgBean.peerId, userBean, page)
        msgAdapter.addData(data, isReset = (page != 1))
        refreshView.refreshComplete()

        if(page == 1){
            if(data.isEmpty())return
            listView.smoothScrollToPosition(data.size)
        }
        page++
    }

    override fun onPullUpRefresh() {}

    override fun getRefreshUtil(): BaseRefreshUtil<SendActivity> {
        return refreshUtil
    }

    override fun refreshMsg() {
        refreshList()
    }
    private fun sendMsg(msg: String){
        val time = System.currentTimeMillis() / 1000
        //先写库
        val cv = ContentValues()
        cv.put("sendId", msgBean.sendId)
        cv.put("peerId", msgBean.peerId)
        cv.put("msg", msg)
        cv.put("time",time)
        cv.put("isRead", 0)
        cv.put("sendSuccess", -1)
        val id = msgOperaDao.insert(null, cv, SQLiteDatabase.CONFLICT_ABORT)
        if(id > 0){
//            JsonVolleyUtil.request(this, Constant.API.SERVICE_HOST, "sendMsg",
//                """{"msgType":"sendmsg", "userID":"${userBean.address}",
//                |"peerID":"${msgBean.peerAddress}","time":"$time",
//                |"msg":"$msg"}""".trimMargin(),
            val sb = StringBuffer();
            sb.append(userBean.address);
            sb.append(msgBean.peerAddress);
            sb.append(msg);
            sb.append(time.toString());
            val digest = MessageDigest.getInstance("SHA-256")
            val result = digest.digest(sb.toString().toByteArray());
            val hash = Base58.encode(result)

            JsonVolleyUtil.request(this, Constant.API.SERVICE_HOST, "sendMsg",
                """{"command":"message", "from":"${userBean.address}",
                |"to":"${msgBean.peerAddress}","timestamp":"$time","crypto":"no","description":"test",
                |"content":"$msg","hash":"${hash}"}""".trimMargin(),
                object: VolleyListenerInterface(this, id, mListener, mErrorListener){
                    override fun onMySuccess(result: String?) {
                        result?.let {
                            val json = JSONObject(it)
                            if(json.optInt("errCode", -1) == 0){
                                //成功 修改发送状态
                                val tmp = mContext.get() as? SendActivity
                                tmp?.msgOperaDao?.update(localMsgId)
                            }
                        }
                    }

                    override fun onMyError(error: VolleyError?) {
                        (mContext.get() as? SendActivity)?.let{
                            it.msgOperaDao.update(localMsgId, 0)
                            it.refreshList()
                        }
                    }
                }, false)
            findViewById<View>(R.id.sendMsg).isEnabled = true
            mETMsg.setText("")
            refreshList()
        }
    }

    private fun refreshList(){
        page = 1
        onPullDownRefresh()
    }
}