package com.openclaw.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.openclaw.mobile.ui.chat.ChatFragment
import com.openclaw.mobile.ui.dashboard.DashboardFragment
import com.openclaw.mobile.ui.pairing.PairingFragment
import com.openclaw.mobile.auth.TokenManager

/**
 * MainActivity - 主活動
 * 
 * 包含 4 個 Tab：
 * - Spark（一般對話）
 * - Data（數據搜集）⭐
 * - NumberOne（編碼專家）
 * - Dash（系統監控）
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        const val AGENT_SPARK = "spark"
        const val AGENT_DATA = "data"
        const val AGENT_NUMBERONE = "numberone"
    }
    
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tokenManager: TokenManager
    
    private val tabTitles = arrayOf(
        "Spark",
        "Data",
        "NumberOne",
        "Dash"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeTokenManager()
        setupUI()
        checkPairingStatus()
    }
    
    private fun initializeTokenManager() {
        tokenManager = TokenManager(this)
    }
    
    private fun setupUI() {
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        
        adapter.setupViewPager(viewPager)
        setupTabLayout()
    }
    
    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
    
    private fun checkPairingStatus() {
        if (!tokenManager.isPaired()) {
            // 顯示配對畫面以強制用戶配對
            showPairingDialog()
        }
    }
    
    private fun showPairingDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("需要配對")
            .setMessage("請先與 OpenClaw 主機配對才能使用")
            .setPositiveButton("立即配對") { _, _ ->
                // 切換到配對 Tab
                viewPager.currentItem = 3 // Dash Tab 可能有配對功能
            }
            .setNegativeButton("取消") { _, _ ->
                finish() // 退出應用
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Tab 頁面適配器
     */
    inner class Adapter : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
        
        fun setupViewPager(viewPager: ViewPager2) {
            viewPager.adapter = this
        }
        
        override fun getItemCount(): Int = 4
        
        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return when (position) {
                0 -> ChatFragment(AGENT_SPARK)
                1 -> ChatFragment(AGENT_DATA)
                2 -> ChatFragment(AGENT_NUMBERONE)
                3 -> DashboardFragment()
                else -> ChatFragment(AGENT_SPARK)
            }
        }
    }
}

// 廣告類
private val chatFragments = arrayOfNulls<ChatFragment>(3)
