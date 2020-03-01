package cn.leiyu.blockchainim.activity.home.contact

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import butterknife.BindView
import butterknife.OnClick
import cn.leiyu.base.utils.LogUtil
import cn.leiyu.blockchainim.R
import cn.leiyu.blockchainim.activity.SubBaseActivity
import cn.leiyu.blockchainim.beans.UserBean
import cn.leiyu.blockchainim.db.LocalDBManager
import cn.leiyu.blockchainim.db.tables.UserOperaDao
import cn.leiyu.blockchainim.utils.ProductLableUtil
import com.google.gson.Gson
import com.google.zxing.CaptureActivity

/**
 * 添加联系人
 */
open class AddContactActivity: SubBaseActivity() {

    @BindView(R.id.friendId)
    lateinit var friendId: EditText
    @BindView(R.id.friendNick)
    lateinit var friendNick: EditText
    @BindView(R.id.remark)
    lateinit var remark: EditText
    //是否添加成功
    protected var isSuccess = false

    override fun getLayoutId(): Int {
        return R.layout.activity_addcontact
    }

    override fun initView() {
        topTitle.text = getString(R.string.contact_add)
        topMenu.text = getString(R.string.confirm)
    }

    override fun initData() {
    }

    @OnClick(R.id.scanFriend)
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.scanFriend->{
                v.isEnabled = false
                startActivityForResult(Intent(this, CaptureActivity::class.java),
                    101)
            }
            R.id.toolbar_menu->{
                try {
                    checkFriend(v)
                }catch (e: Exception){
                    e.printStackTrace()
                    v.isEnabled = true
                    LogUtil.e(TAG!!, "检查信息异常 ${e.localizedMessage}")
                }
            }
            else-> super.onClick(v)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 101){
            findViewById<View>(R.id.scanFriend).isEnabled = true
            if(resultCode == Activity.RESULT_OK){
                //解析二维码字段 并显示
//                val json = Gson().fromJson<UserBean>(data?.getStringExtra(CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN),
//                    UserBean::class.java)
                friendId.setText(data?.getStringExtra(CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN))
//                friendNick.setText(if(TextUtils.isEmpty(json.nickName)) json.loginName else json.nickName)
            }
        }
    }

    override fun onBackPressed() {
        if(isSuccess)setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }

    protected open fun checkFriend(v: View){
        //ID必传
        val params = arrayOfNulls<String>(3)
        params[0] = friendId.text.toString().trim()
        if(TextUtils.isEmpty(params[0])){
            showToast(getString(R.string.input_hint, getString(R.string.contact_id)))
            return
        }
        params[1] = friendNick.text.toString().trim()
        if(TextUtils.isEmpty(params[1])){
            params[1] = " "
        }
        params[2] = remark.text.toString().trim()
        if(TextUtils.isEmpty(params[2]))params[2] = ""
        v.isEnabled = false
        //保存用户
        val login = getUser()
        val bean = UserBean(loginId = login.loginId, loginName = "",
            address = params[0]!!, nickName = params[1]!!, remark = params[2],
            lableColor = ProductLableUtil.getLableColor())
        val hintId = try{
            //存库
            LocalDBManager(this).getTableOperation(UserOperaDao::class.java).insertUser(bean)
            //返回是更新界面
            isSuccess = true
            //重置信息
            friendId.setText("")
            friendNick.setText("")
            remark.setText("")
            R.string.success_hint
        }catch (e: SQLException){
            e.printStackTrace()
            LogUtil.e(TAG!!, "添加联系人异常 ${e.localizedMessage}")
            R.string.failed_hint
        }
        //解禁 给出提示
        v.isEnabled = true
        showToast(getString(hintId, getString(R.string.contact_add)))
    }
}