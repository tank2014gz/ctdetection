package com.wellcell.SubFrag.TaskTest;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.wellcell.ctdetection.R;
import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.Common.CGlobal.TestState;
import com.wellcell.inet.Task.AbsTask;
import com.wellcell.inet.Task.TempAidInfo;
import com.wellcell.inet.Task.TaskPar.WebPar;
import com.wellcell.inet.Task.Video.VideoObject.VideoState;
import com.wellcell.inet.Task.Web.WebObject;
import com.wellcell.inet.Task.Web.WebTestTaskEx;
import com.wellcell.inet.Web.WebUtil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

//HTTP测试
public class HttpFragment extends Fragment implements OnClickListener
{
	private final static String TAG = "HttpFragment";
	
	private boolean[] m_bSelUrl = new boolean[10]; //选中项
	private ImageView[] m_ivUrls = new ImageView[10]; //网站
	private int[] ImgUrls = { R.drawable.w_wangyi, R.drawable.w_sina, R.drawable.w_sohu, R.drawable.w_renmin, R.drawable.w_fh, R.drawable.w_weibo, R.drawable.w_apple, R.drawable.w_tx, R.drawable.w_baidu, R.drawable.w_taobao };
	private int[] ImgUrlsHov = { R.drawable.w_wangyi_h, R.drawable.w_sina_h, R.drawable.w_sohu_h, R.drawable.w_renmin_h, R.drawable.w_fh_h, R.drawable.w_weibo_h, R.drawable.w_apple_h, R.drawable.w_tx_h, R.drawable.w_baidu_h, R.drawable.w_taobao_h };

	private WebPar m_webPar; //配置参数

	private List<WebObject> m_listWebObj = new ArrayList<WebObject>();
	private JSONArray m_jsonArrWebRec;	//所有测试记录
	//-----------------------------------------------------------------------------------
	private ProgressBar m_pbProc; //当前进度
	private Button m_btnStart; //开始测试
	private ScrollView m_scrollRet;
	private TextView m_tvLog; //测试日志
	private StringBuffer m_bfLog;

	public TestState m_curState = TestState.eReady; //当前测试状态
	private boolean m_bStop = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.httpfragment, null);

		if (savedInstanceState == null)
		{
			bindComponment(view);

			ImageViewDeal(R.id.imv_a);
			ImageViewDeal(R.id.imv_i);

			m_bfLog = new StringBuffer();
		}

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean("Save", true);
		super.onSaveInstanceState(outState);
	}

	private Handler m_hHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 0: //测试完毕
				m_curState = TestState.eStoped;
				
				new Thread(upLoadThread).start();	//数据上传

				m_btnStart.setBackgroundDrawable(getResources().getDrawable(R.drawable.sel_btn_start));
				//m_btnStart.setText("");
				m_pbProc.setVisibility(View.GONE);
				break;
			case 1: //更新进度
				int nPos = ((Integer) msg.obj).intValue();
				if(nPos == 0)	//开始测试
				{
					m_btnStart.setBackgroundDrawable(getResources().getDrawable(R.drawable.sel_btn_stop));
					//m_btnStart.setText("停止测试");
					
					new Thread(startTest).start();
				}
				
				if (m_listWebObj.size() > 0)
					m_pbProc.setProgress((nPos + 1) * 100 / (m_listWebObj.size() + 1));
				break;
			case 4: //日志信息更新
				m_bfLog.append((String) msg.obj);
				m_bfLog.append("\n");
				m_tvLog.setText(m_bfLog.toString());
				m_scrollRet.fullScroll(android.view.View.FOCUS_DOWN);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	//开始业务测试
	private void StartTest()
	{
		int nCount = 0;
		for (int i = 0; i < m_bSelUrl.length; i++)
		{
			if (m_bSelUrl[i] == true)
				nCount++;
		}

		if (nCount == 0)
		{
			Toast.makeText(getActivity(), "请选择需要测试的网站!", Toast.LENGTH_SHORT).show();
			return;
		}
		//------------------------------------------------------------------------------
		Init();
		//生成测试任务
		m_listWebObj.clear();
		for (int i = 0; i < m_bSelUrl.length; i++)
		{
			if (m_bSelUrl[i] == false)
				continue;

			m_webPar = new WebPar(i);
			m_listWebObj.add(new WebObject(getActivity(), m_webPar, m_hHandler, 4));
		}
		
		m_hHandler.sendMessage(Message.obtain(m_hHandler,1, 0)); //更新进度
	}

	private void Init()
	{
		m_bStop = false;
		
		m_bfLog.setLength(0);
		m_curState = TestState.eTesting;
		m_pbProc.setVisibility(View.VISIBLE);
		m_tvLog.setVisibility(View.VISIBLE);
		
		m_jsonArrWebRec = new JSONArray();
	}

	//测试线程
	private Runnable startTest = new Runnable()
	{
		@Override
		public void run()
		{
			for (int i = 0; i < m_listWebObj.size(); i++)
			{
				if(m_bStop)
					break;
				
				m_jsonArrWebRec = m_listWebObj.get(i).runningTask(m_jsonArrWebRec);	//开始测试
				
				m_hHandler.sendMessage(Message.obtain(m_hHandler, 1, i + 1)); //更新进度
				
				CGlobal.Sleep(2000);
			}

			m_hHandler.sendMessage(Message.obtain(m_hHandler,0));
		}
	};
	
	//上传测试数据
	private Runnable upLoadThread = new Runnable()
	{
		@Override
		public void run()
		{
			if(m_jsonArrWebRec == null)
				return;
			
			if(m_jsonArrWebRec.length() <= 0)
				return;
				
			JSONObject jsonRet = new JSONObject();
			try
			{
				jsonRet.put("web", m_jsonArrWebRec);
			}
			catch (JSONException e)
			{
				return ;
			}
			
			String strRet = WebUtil.uploadTestData(jsonRet.toString(),1);
			if (strRet.equals("ok"))
				Log.i(TAG, "上传成功...");
			else
				Log.i(TAG, "上传失败...");			
		}
	};

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.btn_test:
			if (m_curState == TestState.eReady || m_curState == TestState.eStoped)
				StartTest();
			else
				m_bStop = true;
			break;
		case R.id.imv_a:
		case R.id.imv_b:
		case R.id.imv_c:
		case R.id.imv_d:
		case R.id.imv_e:
		case R.id.imv_f:
		case R.id.imv_g:
		case R.id.imv_h:
		case R.id.imv_i:
		case R.id.imv_j:
			ImageViewDeal(v.getId());
		default:
			break;
		}
	}

	/**
	 * 功能: 视频源图标处理
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	private void ImageViewDeal(int nID)
	{
		if (m_curState != TestState.eReady && m_curState != TestState.eStoped)
			return;

		ImageView iv;
		for (int i = 0; i < m_ivUrls.length; i++)
		{
			iv = m_ivUrls[i];
			if (iv.getId() == nID) //新选中
			{
				if (m_bSelUrl[i] == true) //原来已选中
					iv.setImageResource(ImgUrls[i]);
				else
					iv.setImageResource(ImgUrlsHov[i]);

				m_bSelUrl[i] = !m_bSelUrl[i]; //反选

				break;
			}
		}
	}

	//绑定控件
	private void bindComponment(View view)
	{
		m_ivUrls[0] = (ImageView) view.findViewById(R.id.imv_a);
		m_ivUrls[1] = (ImageView) view.findViewById(R.id.imv_b);
		m_ivUrls[2] = (ImageView) view.findViewById(R.id.imv_c);
		m_ivUrls[3] = (ImageView) view.findViewById(R.id.imv_d);
		m_ivUrls[4] = (ImageView) view.findViewById(R.id.imv_e);
		m_ivUrls[5] = (ImageView) view.findViewById(R.id.imv_f);
		m_ivUrls[6] = (ImageView) view.findViewById(R.id.imv_g);
		m_ivUrls[7] = (ImageView) view.findViewById(R.id.imv_h);
		m_ivUrls[8] = (ImageView) view.findViewById(R.id.imv_i);
		m_ivUrls[9] = (ImageView) view.findViewById(R.id.imv_j);

		for (int i = 0; i < m_ivUrls.length; i++)
		{
			m_ivUrls[i].setOnClickListener(this);
		}
		//---------------------------------------------------------
		m_pbProc = (ProgressBar) view.findViewById(R.id.procbar);
		m_btnStart = (Button) view.findViewById(R.id.btn_test);
		m_btnStart.setOnClickListener(this);

		m_scrollRet = (ScrollView) view.findViewById(R.id.scroll_result);
		m_tvLog = (TextView) view.findViewById(R.id.tv_ret);
	}
}
