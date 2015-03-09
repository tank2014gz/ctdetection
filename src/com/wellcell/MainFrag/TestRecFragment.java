package com.wellcell.MainFrag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wellcell.SubFrag.TestRec.BrowseFragment;
import com.wellcell.ctdetection.R;

//测试记录主界面
public class TestRecFragment extends AbsMainFragment
{
	@Override
	View buildView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_browse, null);
		m_bSingleFrag = true;
		
		m_classfrags = new Class[] {BrowseFragment.class};
		m_nImgViews = new int[] { R.drawable.tab_message_btn};//, R.drawable.tab_selfinfo_btn, R.drawable.tab_square_btn, R.drawable.tab_more_btn };
		m_strTabLables = new String[] { "测速"};
		return view;
	}
}
