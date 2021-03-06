package com.wellcell.MainFrag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wellcell.SubFrag.AidInfo.CustSignalFragment;
import com.wellcell.SubFrag.AidInfo.MapFragment;
import com.wellcell.SubFrag.AidInfo.PhoneFragment;
import com.wellcell.ctdetection.R;

//辅助信息主界面
public class AidInfoFragment extends AbsMainFragment
{
	@Override
	View buildView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.maimfragment, null);
		m_classfrags = new Class[] { CustSignalFragment.class, MapFragment.class, PhoneFragment.class };
		m_nImgViews = new int[] { R.drawable.btn_wuxianhuanjing, R.drawable.btn_zuobianxinxi, R.drawable.btn_shoujixianguan };
		m_strTabLables = new String[] { "无线环境", "地图", "手机" };
		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// set action bar title
		getActivity().getActionBar().setTitle(R.string.AidInfo);
	}
}