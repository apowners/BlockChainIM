package cn.leiyu.blockchainim.activity.home

import android.app.Activity
import android.content.Intent
import android.database.SQLException
import android.text.TextUtils
import android.view.View
import android.widget.ListView
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnItemClick
import cn.leiyu.base.utils.AddressUtil
import cn.leiyu.base.utils.BaseRefreshUtil
import cn.leiyu.base.weak.AbsWeakContext
import cn.leiyu.blockchainim.LoginActivity
import cn.leiyu.blockchainim.MainActivity
import cn.leiyu.blockchainim.R
import cn.leiyu.blockchainim.activity.AbsParentBaseActivity
import cn.leiyu.blockchainim.activity.SubBaseFragment
import cn.leiyu.blockchainim.activity.home.contact.AddContactActivity
import cn.leiyu.blockchainim.activity.home.contact.DetailActivity
import cn.leiyu.blockchainim.activity.user.CreateActivity
import cn.leiyu.blockchainim.adapters.ContactAdapter
import cn.leiyu.blockchainim.beans.UserBean
import cn.leiyu.blockchainim.db.LocalDBManager
import cn.leiyu.blockchainim.db.tables.UserOperaDao
import cn.leiyu.blockchainim.utils.ProductLableUtil
import com.chanven.lib.cptr.PtrClassicFrameLayout

/**
 * 主界面 - 通讯录布局
 */
class HomeFragment : SubBaseFragment(), BaseRefreshUtil.IRefreshCallback<HomeFragment> {

    @BindView(R.id.title)
    lateinit var title: TextView
    @BindView(R.id.opera)
    lateinit var addFriend: TextView
    @BindView(android.R.id.list)
    lateinit var listView: ListView
    @BindView(R.id.refresh_view)
    lateinit var refreshView: PtrClassicFrameLayout
    private lateinit var refreshUtil: BaseRefreshUtil<HomeFragment>
    private lateinit var user: UserBean
    private lateinit var userOperaDao: UserOperaDao
    private var adapter: ContactAdapter? = null
    private var page: Int = 1

    companion object{
        public var gPrivateKey:String = ""
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_home
    }

    override fun initView() {
        super.initView()
        title.text = getString(R.string.contact_title)
        addFriend.text = getString(R.string.contact_add)
        refreshUtil = BaseRefreshUtil(this, refreshView)
        refreshUtil.initView()
        initData()

        val addressUtil = AddressUtil(activity?.filesDir.toString())
        HomeFragment.gPrivateKey = addressUtil.getPrivateKey("");
    }

    override fun initData() {
        if(mContext is AbsParentBaseActivity) {
            user = (mContext as AbsParentBaseActivity).getUser()
        }
        userOperaDao = LocalDBManager(mContext!!).getTableOperation(UserOperaDao::class.java)
        readUser()
    }

    @OnItemClick(android.R.id.list)
    fun onItemClick(position: Int){
        activity?.startActivityForResult(Intent(mContext!!, DetailActivity::class.java)
            .putExtra("item", adapter?.getItem(position) as UserBean),
            (activity as? MainActivity)!!.FLAG_SEND)
    }

    @OnClick(R.id.opera)
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.opera->{
                startActivityForResult(Intent(mContext!!, AddContactActivity::class.java),
                    102)
            }
            else-> super.onClick(v)
        }
    }

    override fun onPullDownRefresh() {
        page = 1
        readUser()
        refreshUtil.resetRefresh(page)
    }

    override fun onPullUpRefresh() {
        page++
        readUser()
        refreshUtil.resetRefresh(page)
    }

    override fun getRefreshUtil(): BaseRefreshUtil<HomeFragment> {
        return refreshUtil
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 102 && resultCode == Activity.RESULT_OK){
            //重新加载
            onPullDownRefresh()
        }else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun readUser(){
        val users = userOperaDao.query(user, page)
        if(adapter == null || page == 1){
            adapter = ContactAdapter(mContext!!, users)
            listView.adapter = adapter
        }else adapter?.addData(users, page == 1)
    }
}