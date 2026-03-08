package com.openclaw.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.openclaw.mobile.ui.chat.ChatFragment
import com.openclaw.mobile.ui.dashboard.DashboardFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    
    // Agent IDs
    private val AGENT_SPARK = "spark"
    private val AGENT_DATA = "data"
    private val AGENT_NUMBERONE = "numberone"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setupTabs()
        
        // 預設顯示第一個 Tab (Spark)
        if (savedInstanceState == null) {
            showFragment(ChatFragment.newInstance(AGENT_SPARK, "Spark"))
        }
    }
    
    private fun setupTabs() {
        tabLayout = findViewById(R.id.tabLayout)
        
        // 建立 4 個 Tab
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_spark))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_data))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_numberone))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_dash))
        
        // Tab 切換監聽
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val fragment = when (it.position) {
                        0 -> ChatFragment.newInstance(AGENT_SPARK, "Spark")
                        1 -> ChatFragment.newInstance(AGENT_DATA, "Data")
                        2 -> ChatFragment.newInstance(AGENT_NUMBERONE, "NumberOne")
                        3 -> DashboardFragment.newInstance()
                        else -> ChatFragment.newInstance(AGENT_SPARK, "Spark")
                    }
                    showFragment(fragment)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
