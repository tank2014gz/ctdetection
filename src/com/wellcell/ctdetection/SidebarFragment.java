package com.wellcell.ctdetection;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

//侧边栏
public class SidebarFragment extends Fragment implements OnClickListener
{
	/**
	 * Remember the position of the selected item.
	 */
	private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

	/**
	 * Per the design guidelines, you should show the drawer on launch until the
	 * user manually expands it. This shared preference tracks this.
	 */
	private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

	// 侧边栏项选择回调接口
	public static interface SideBarSelCallbacks
	{
		void onSideBarItemSelected(int position);
	}

	private SideBarSelCallbacks m_cbSidebarSel;

	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle m_DrawerToggle;

	private DrawerLayout m_layoutDrawer;
	// private ListView m_lvDrawer; // 侧边栏内容项
	private View m_viewFragCont;

	private int m_nCurSelPos = 0;
	private boolean m_bSavedInstState;
	private boolean m_bUserDrawer;
	public static boolean DISPLAY = false;

	public SidebarFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Read in the flag indicating whether or not the user has demonstrated
		// awareness of the
		// drawer. See PREF_USER_LEARNED_DRAWER for details.
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		m_bUserDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

		if (savedInstanceState != null)
		{
			m_nCurSelPos = savedInstanceState.getInt(STATE_SELECTED_POSITION);
			m_bSavedInstState = true;
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		// Indicate that this fragment would like to influence the set of
		// actions in the action bar.
		setHasOptionsMenu(true);
	}

	private static int[] entries = { R.id.btn_tasktest, R.id.btn_diagnose, R.id.btn_aidinfo, R.id.btn_onekey, R.id.btn_testrec, R.id.btn_setting, R.id.btn_about, R.id.btn_quit };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_sidebar, null);
		for (int i : entries)
			view.findViewById(i).setOnClickListener(this);
		return view;
	}

	@Override
	public void onClick(View arg0)
	{
		// TODO Auto-generated method stub
		int index = -1;
		for (int i : entries)
		{
			index++;
			if (i == arg0.getId())
			{
				selectItem(index);
				return;
			}
		}
	}

	public boolean isDrawerOpen()
	{
		return m_layoutDrawer != null && m_layoutDrawer.isDrawerOpen(m_viewFragCont);
	}

	/**
	 * Users of this fragment must call this method to set up the navigation
	 * drawer interactions.
	 * 
	 * @param fragmentId
	 *            The android:id of this fragment in its activity's layout.
	 * @param drawerLayout
	 *            The DrawerLayout containing this fragment's UI.
	 */
	public void setUp(int fragmentId, DrawerLayout drawerLayout)
	{
		m_viewFragCont = getActivity().findViewById(fragmentId);
		m_layoutDrawer = drawerLayout;

		// set a custom shadow that overlays the main content when the drawer
		// opens
		m_layoutDrawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		// set up the drawer's list view with items and click listener

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the navigation drawer and the action bar app icon.
		m_DrawerToggle = new ActionBarDrawerToggle(getActivity(), /* host Activity */
		m_layoutDrawer, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.navigation_drawer_open, /*
											* "open drawer" description for
											* accessibility
											*/
		R.string.navigation_drawer_close /*
											* "close drawer" description for
											* accessibility
											*/
		)
		{
			@Override
			public void onDrawerClosed(View drawerView)
			{
				super.onDrawerClosed(drawerView);
				if (!isAdded())
				{
					return;
				}
				// make action menu disappear
				DISPLAY = false;
				getActivity().supportInvalidateOptionsMenu(); // calls
																// onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView)
			{
				super.onDrawerOpened(drawerView);
				if (!isAdded())
				{
					return;
				}

				if (!m_bUserDrawer)
				{
					// The user manually opened the drawer; store this flag to
					// prevent auto-showing
					// the navigation drawer automatically in the future.
					m_bUserDrawer = true;
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
					sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
				}
				// make action menu appear
				DISPLAY = true;
				getActivity().supportInvalidateOptionsMenu(); // calls
																// onPrepareOptionsMenu()
			}
		};

		// If the user hasn't 'learned' about the drawer, open it to introduce
		// them to the drawer,
		// per the navigation drawer design guidelines.
		if (!m_bUserDrawer && !m_bSavedInstState)
		{
			//			m_layoutDrawer.openDrawer(m_viewFragCont);
		}

		// Defer code dependent on restoration of previous instance state.
		m_layoutDrawer.post(new Runnable()
		{
			@Override
			public void run()
			{
				m_DrawerToggle.syncState();
			}
		});

		m_layoutDrawer.setDrawerListener(m_DrawerToggle);
	}

	// 侧边栏项选择
	private void selectItem(int position)
	{
		m_nCurSelPos = position;
		// if (m_lvDrawer != null)
		// m_lvDrawer.setItemChecked(position, true);

		if (m_layoutDrawer != null)
			m_layoutDrawer.closeDrawer(m_viewFragCont);

		if (m_cbSidebarSel != null)
			m_cbSidebarSel.onSideBarItemSelected(position);
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		try
		{
			m_cbSidebarSel = (SideBarSelCallbacks) activity;
		}
		catch (ClassCastException e)
		{
			throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
		}
	}

	@Override
	public void onDetach()
	{
		super.onDetach();
		m_cbSidebarSel = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_POSITION, m_nCurSelPos);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Forward the new configuration the drawer toggle component.
		m_DrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// If the drawer is open, show the global app actions in the action bar.
		// See also
		// showGlobalContextActionBar, which controls the top-left area of the
		// action bar.
		if (m_layoutDrawer != null && isDrawerOpen())
		{
			inflater.inflate(R.menu.global, menu);
			showGlobalContextActionBar();
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (m_DrawerToggle.onOptionsItemSelected(item))
			return true;
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Per the navigation drawer design guidelines, updates the action bar to
	 * show the global app 'context', rather than just what's in the current
	 * screen.
	 */
	private void showGlobalContextActionBar()
	{
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	private ActionBar getActionBar()
	{
		return ((ActionBarActivity) getActivity()).getSupportActionBar();
	}
}