package com.wellcell.ctdetection;

import io.vov.vitamio.LibsChecker;

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wellcell.MainFrag.AboutActivity;
import com.wellcell.MainFrag.AboutFragment;
import com.wellcell.MainFrag.AidInfoFragment;
import com.wellcell.MainFrag.DiagnosisFragment;
import com.wellcell.MainFrag.SettingActivity;
import com.wellcell.MainFrag.SettingFragment;
import com.wellcell.MainFrag.TaskTestFragment;
import com.wellcell.MainFrag.TestRecFragment;
import com.wellcell.ctdetection.CommonActivity.MainModule;
import com.wellcell.MainFrag.TaskListActivity;

//主界面
public class DetectionActivity extends ActionBarActivity implements SidebarFragment.SideBarSelCallbacks
{

	private SidebarFragment m_sidebarFrag; // 侧边栏
	private MainModule m_modulePreSel; // 上一次选择的菜单项

	// 保存已实例化的fragments，当切换时调用hide函数，返回时调用show，有利于状态回复。
	HashMap<MainModule, Fragment> fragments = new HashMap<MainModule, Fragment>();
	Fragment current = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_common);
		// main fragment
		if (savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction().add(R.id.mainlayout, new TaskTestFragment()).commit();
		}
		// side bar
		m_sidebarFrag = (SidebarFragment) getSupportFragmentManager().findFragmentById(R.id.sidebardrawer);
		getTitle();

		// Set up the drawer.
		m_sidebarFrag.setUp(R.id.sidebardrawer, (DrawerLayout) findViewById(R.id.drawer_layout));

		new Thread(loadVitamioLibrary).start(); // vitamio检测
	}

	// --------------------------------------------------------------------------------------------------------------------
	// 检测解码包的代码（解压解码包，Vitamio会根据当前CPU的类型自动解压相应平台的库）
	private Runnable loadVitamioLibrary = new Runnable()
	{
		public void run()
		{
			LibsChecker.checkVitamioLibs(DetectionActivity.this);// vitamio库检测
		}
	};

	// -------------------------------------------------------------------------------------------------------------------
	// 切换模块
	private void flip(MainModule key)
	{
		if (!fragments.containsKey(key))
			switch (key)
			{
			case eAbout:
				fragments.put(key, new AboutFragment());
				break;
			case eSetting:
				fragments.put(key, new SettingFragment());
				break;
			case eTestRec:
				fragments.put(key, new TestRecFragment());
				break;
			case eAidInfo:
				fragments.put(key, new AidInfoFragment());
				break;
			case eDiagnosis:
				fragments.put(key, new DiagnosisFragment());
				break;
			case eTaskTest:
				fragments.put(key, new TaskTestFragment());
				break;
			}
		if (current != null && current.equals(fragments.get(key)))
			return;

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
		if (current != null)
			transaction.hide(current);

		if (fragments.get(key).isAdded())
			transaction.show(fragments.get(key));
		else
			transaction.add(R.id.mainlayout, fragments.get(key));
		transaction.addToBackStack(null);
		transaction.commit();
		current = fragments.get(key);
		// set action bar title
		switch (key)
		{
		case eAbout:
			getActionBar().setTitle(R.string.About);
			break;
		case eSetting:
			getActionBar().setTitle(R.string.Setting);
			break;
		case eTestRec:
			getActionBar().setTitle(R.string.TestRec);
			break;
		case eOneKey:
			//getActionBar().setTitle(R.string.OneKey);
			break;
		case eAidInfo:
			getActionBar().setTitle(R.string.AidInfo);
			break;
		case eDiagnosis:
			getActionBar().setTitle(R.string.Diagnosis);
			break;
		case eTaskTest:
			getActionBar().setTitle(R.string.TaskTest);
			break;
		}
	}

	// 侧边栏选择
	@Override
	public void onSideBarItemSelected(int position)
	{
		// update the main content by replacing fragments
		MainModule moduleCur = MainModule.values()[position];
		if (moduleCur == null)
			return;

		if (moduleCur == m_modulePreSel)
			return;

		Intent intent = null;
		switch (moduleCur)
		{
		case eSetting:
			intent = new Intent(this, SettingActivity.class);
			break;
		case eOneKey:
			intent = new Intent(this, TaskListActivity.class);
			break;
		case eAbout:
			intent = new Intent(this, AboutActivity.class);
			break;
		default:
			return;
		}
		if (intent != null)
			startActivity(intent);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment
	{
		private static final String ARG_SECTION_NUMBER = "section_number";

		public static PlaceholderFragment newInstance(int sectionNumber)
		{
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
		{
			// View rootView = inflater.inflate(R.layout.fragment_detection,
			// container, false);
			View rootView = inflater.inflate(R.layout.maimfragment, container, false);
			return rootView;
		}

		@Override
		public void onAttach(Activity activity)
		{
			super.onAttach(activity);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.global, menu);
		// custom menus
		if (SidebarFragment.DISPLAY)
			menu.findItem(R.id.action_settings).setVisible(false);
		return super.onCreateOptionsMenu(menu);
		// android.R.drawable.ic_menu_preferences
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private long exitTime;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
		{

			if ((System.currentTimeMillis() - exitTime) > 2000) //System.currentTimeMillis()无论何时调用，肯定大于2000  
			{
				Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			}
			else
			{
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}