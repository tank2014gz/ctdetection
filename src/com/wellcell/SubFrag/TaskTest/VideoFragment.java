package com.wellcell.SubFrag.TaskTest;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wellcell.ctdetection.R;
import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.Task.TaskPar.VideoPar;
import com.wellcell.inet.Task.TempAidInfo;
import com.wellcell.inet.Task.Video.VideoObject;
import com.wellcell.inet.Task.Video.VideoObject.VideoCbType;
import com.wellcell.inet.Task.Video.VideoObject.VideoMsgCallBack;
import com.wellcell.inet.Task.Video.VideoObject.VideoState;
import com.wellcell.inet.Task.Video.VideoRet;
import com.wellcell.inet.Web.WebUtil;

//视频测试
public class VideoFragment extends Fragment implements OnClickListener, VideoMsgCallBack
{
	private final String TAG = "VideoFragment";
	SurfaceView m_Preview;
	private SurfaceHolder m_surfaceHolder;
	private VideoObject m_videoObj;
	//private String m_strPath;

	private VideoPar m_videoPar;
	private VideoRet m_videoRet; //视频结果信息
	private TempAidInfo m_aidInfo; //辅助信息
	private boolean m_bUpLoaded = false; //上传控制
	private VideoState m_videoState = VideoState.eReady; //视频状态

	private long m_lStartSize = 0; //开始流量
	private long m_lSizePre = 0; //前一刻流量
	private long m_lTimePre = 0; //前一刻
	private boolean m_bGetSpeed = true; //是否计算速率
	private int m_nUid;

	private long m_lVideoDur = 0; //视频长度

	private Timer m_timerUpdate = null; //更新信息定时器
	//----------------------------------------------
	private int m_nPreSelUrl = 0; //前一个选择
	private ImageView[] m_ivUrls = new ImageView[5]; //视频源
	private int[] ImgUrls = { R.drawable.v_youku, R.drawable.v_tudou, R.drawable.v_aiqiyi, R.drawable.v_sohu, R.drawable.v_tx };
	private int[] ImgUrlsHov = { R.drawable.v_youku_h, R.drawable.v_tudou_h, R.drawable.v_aiqiyi_h, R.drawable.v_sohu_h, R.drawable.v_tx_h };
	private final String[] VideoUrls = { "http://115.102.0.36/sohu.vodnew.lxdns.com/sohu/s26h23eab6/192/159/VIPs5E1k3kK8z5XVeyAW44.mp4", "http://14.146.229.118:2045/video/tudouLte.mp4", "http://14.146.229.118:2045/video/aiqiyiliudehuamudi.mp4", "http://14.146.229.118:2045/video/sohuASUSMemoPad7.mp4", "http://14.146.229.118:2045/video/qqLte.flv", };

	private SeekBar m_sbPos; //当前播放进度
	private TextView m_tvPos; //当前播放位置
	private Button m_btnStart; //开始测试

	private TextView m_tvStatus; //状态
	private TextView m_tvVideoSrc; //视频源
	private TextView m_tvSpeedCur; //当前
	private TextView m_tvStuckCountCur; //卡顿次数
	//
	//结果
	private LinearLayout m_layoutRet;
	private TextView m_tvSpeedAvg; //平均
	private TextView m_tvSpeedMax; //峰值速率
	private TextView m_tvLinkDelay; //连接时延
	private TextView m_tvPlayDelay; //播放时延
	private TextView m_tvStuckCountRet; //卡顿次数
	//private TextView m_tvStuckDelay; //卡顿时长
	private TextView m_tvTrafSize; //流量

	//-----------------------------------------------

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		m_nUid = CGlobal.getUID(getActivity()); //获取User ID
		m_aidInfo = new TempAidInfo(getActivity().getApplicationContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.videofragment, null);

		m_Preview = (SurfaceView) view.findViewById(R.id.videoView);
		m_surfaceHolder = m_Preview.getHolder();

		m_videoPar = new VideoPar();
		m_videoObj = new VideoObject(getActivity().getApplicationContext(), m_videoPar, this, m_surfaceHolder);//

		bindComponment(view); //绑定控件
		ImageViewDeal(R.id.imv_youku); //初始选中优库
		Init();

		return view;
	}

	private Handler m_hHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 0: //状态
				m_videoState = (VideoState) msg.obj;
				updateStatus();
				break;
			case 1: //速率
				m_bGetSpeed = false;
				int nSpeed = ((Integer) msg.obj).intValue();
				m_tvSpeedCur.setText("当前速率：" + CGlobal.getSpeedString(nSpeed * 1000));
				break;
			case 2: //卡顿
				m_videoRet = (VideoRet) msg.obj;
				m_tvStuckCountCur.setText("卡顿次数：" + m_videoRet.m_nStuckCount);
				break;
			case 3: //结果
				m_videoState = VideoState.eStop;
				m_aidInfo.getCpuUsageInfo(1); //结束CPU时间信息
				updateStatus();

				m_videoRet = (VideoRet) msg.obj;
				m_videoRet.m_nSizeSum = (int) (CGlobal.getCurTrafficRx(m_nUid) - m_lStartSize); //单次总流量
				showRet(m_videoRet);
				onStopDeal();
				break;
			case 4: //补充速率(byte/s)
				m_tvSpeedCur.setText("当前速率：" + CGlobal.getSpeedString(((Double) msg.obj).doubleValue()));
				break;
			case 5: //播放进度
				m_Preview.invalidate();
				long lCurPos = m_videoObj.getCurPos();
				m_sbPos.setProgress((int) (lCurPos * 100 / m_lVideoDur));
				m_tvPos.setText(CGlobal.getMMSS(lCurPos) + "/" + CGlobal.getMMSS(m_lVideoDur));
				break;
			default:
				break;
			}

			super.handleMessage(msg);
		}
	};

	//上传测试数据
	private Runnable upLoadThread = new Runnable()
	{
		@Override
		public void run()
		{
			m_bUpLoaded = true;

			JSONObject jsonRet = new JSONObject();
			JSONArray jsonVideo = new JSONArray();
			JSONArray jsonRec = new JSONArray(); //单条
			try
			{
				jsonRec = m_aidInfo.getJsonVal(jsonRec);
				jsonRec = m_videoRet.getJsonArr(jsonRec);

				jsonVideo.put(jsonRec);
				jsonRet.put("video", jsonVideo);
			}
			catch (JSONException e)
			{
				return;
			}

			String strRet = WebUtil.uploadTestData(jsonRet.toString(), 1);
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
			try
			{
				if (m_videoState == VideoState.eReady || m_videoState == VideoState.eStop)
				{
					Init();
					setTimerUpdate(); //设置定时器

					m_videoState = VideoState.eLink;
					m_tvStatus.setText("正在连接资源...");

					m_lStartSize = CGlobal.getCurTrafficRx(m_nUid); //开始流量大小
					m_lTimePre = System.currentTimeMillis();
					m_lSizePre = m_lStartSize;

					m_aidInfo.updateStartInfo(); //更新信息
					m_videoObj.startTest();

					m_btnStart.setBackgroundDrawable(getResources().getDrawable(R.drawable.sel_btn_stop));
					//m_btnStart.setText("停止测试");
				}
				else
				{
					m_videoObj.stopTest();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			break;
		case R.id.imv_youku:
		case R.id.imv_tudou:
		case R.id.imv_aiqiyi:
		case R.id.imv_souhu:
		case R.id.imv_tx:
			ImageViewDeal(v.getId());
			break;
		default:
			break;
		}
	}

	//视频测试信息回调
	@Override
	public void onReceived(String msg)
	{
	}

	//视频测试状态及结果信息回调
	@Override
	public void onUpdateInfo(VideoCbType type, Object obj)
	{
		m_hHandler.sendMessage(m_hHandler.obtainMessage(type.ordinal(), obj));
	}

	//设置定时器
	private void setTimerUpdate()
	{
		TimerTask tt = new TimerTask()
		{
			@Override
			public void run()
			{
				if (m_videoState == VideoState.eReady || m_videoState == VideoState.eStop)
					return;

				//----------------------------------------------
				//速率
				if (m_bGetSpeed) //控件返回速率前
				{
					long lSizeCur = CGlobal.getCurTrafficRx(m_nUid);
					long lTimeCur = System.currentTimeMillis();
					if (m_lTimePre != 0) //初始
					{
						long lSize = lSizeCur - m_lSizePre;
						if (lSize >= 0 && lTimeCur - m_lTimePre > 0)
							m_hHandler.sendMessage(m_hHandler.obtainMessage(4, lSize * 1000.0 / (lTimeCur - m_lTimePre)));
					}
					m_lSizePre = lSizeCur;
					m_lTimePre = lTimeCur;
				}

				//-----------------------------------------------
				//当前播放进度
				if (m_videoState == VideoState.ePlaying)
					m_hHandler.sendMessage(m_hHandler.obtainMessage(5));
			}

		};
		m_timerUpdate.schedule(tt, 100, 1000);
	}

	//更新测试信息
	private void updateStatus()
	{
		if (m_videoState == null)
			return;

		String strInfo = "";
		switch (m_videoState)
		{
		case eReady:
			strInfo = "就绪";
			break;
		case eLink:
			strInfo = "正在连接资源...";
			break;
		case eBuffering:
			strInfo = "连接成功，正在缓冲...";
			break;
		case ePlaying:
			m_lVideoDur = m_videoObj.getVideoDuration();
			strInfo = "正在播放...";
			break;
		case eStop:
			strInfo = "测试完毕";
			break;
		default:
			break;
		}
		m_tvStatus.setText(strInfo);
	}

	//显示结果信息
	private void showRet(VideoRet videoRet)
	{
		//状态
		switch (videoRet.m_nRet)
		{
		case VideoRet.Video_Suc: //成功
			m_tvStatus.setText("测试成功，测试停止");
			break;
		case VideoRet.Video_LinkTO: //连接超时
			m_tvStatus.setText("连接超时，测试停止");
			break;
		case VideoRet.Video_LinkError://连接错误
			m_tvStatus.setText("连接错误，测试停止");
			break;
		case VideoRet.Video_BufTO: //缓冲超时
			m_tvStatus.setText("缓冲超时，测试停止");
			break;
		case VideoRet.Video_BufError://缓冲错误
			m_tvStatus.setText("缓冲错误，测试停止");
			break;
		case VideoRet.Video_PlayError://播放错误
			m_tvStatus.setText("播放错误，测试停止");
			break;
		case VideoRet.Video_Unknow://未知错误
			m_tvStatus.setText("未知错误，测试停止");
			break;
		default:
			break;
		}

		//------------------------------------------
		m_tvSpeedCur.setVisibility(View.GONE);
		m_tvStuckCountCur.setVisibility(View.GONE);

		//视频源
		m_tvVideoSrc.setVisibility(View.VISIBLE);
		m_tvVideoSrc.setText("视  频  源：" + m_videoPar.getVideoType());

		m_tvSpeedAvg.setVisibility(View.VISIBLE);
		m_tvSpeedMax.setVisibility(View.VISIBLE);
		m_layoutRet.setVisibility(View.VISIBLE);
		m_tvSpeedAvg.setText("平均速率：" + CGlobal.getSpeedString(videoRet.m_dSpeedAvg));
		m_tvSpeedMax.setText("峰值速率：" + CGlobal.getSpeedString(videoRet.m_dSpeedMax));
		m_tvLinkDelay.setText("连接时延：" + CGlobal.getMsString(videoRet.m_lLinkDelay) + "ms");
		m_tvPlayDelay.setText("播放时延：" + CGlobal.getSecondString(videoRet.m_lPlayDelay) + "s");
		m_tvStuckCountRet.setText("卡顿次数：" + videoRet.m_nStuckCount);
		m_tvTrafSize.setText("消耗流量：" + CGlobal.getSizeString(videoRet.m_nSizeSum));
	}

	/**
	 * 功能: 视频源图标处理
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	private void ImageViewDeal(int nID)
	{
		if (m_videoState != VideoState.eReady && m_videoState != VideoState.eStop)
			return;

		if (nID == m_nPreSelUrl)
			return;

		ImageView iv;
		for (int i = 0; i < m_ivUrls.length; i++)
		{
			iv = m_ivUrls[i];
			if (iv.getId() == nID) //新选中
			{
				iv.setImageResource(ImgUrlsHov[i]);
				m_videoPar.m_strUrl = VideoUrls[i];
				m_videoPar.m_nType = i + 2;
			}

			if (iv.getId() == m_nPreSelUrl) //撤销选中
				iv.setImageResource(ImgUrls[i]);
		}

		m_nPreSelUrl = nID;
	}

	//初始化处理
	private void Init()
	{
		m_lSizePre = 0;
		m_lTimePre = 0;
		m_bGetSpeed = true;
		m_bUpLoaded = false;

		m_lVideoDur = 0;

		if (m_timerUpdate == null)
			m_timerUpdate = new Timer();
		//--------------------------------------------
		m_sbPos.setProgress(0); //初始化进度条
		m_tvPos.setText("");
		m_tvStatus.setText("就绪");

		//显示实时控件
		m_tvSpeedCur.setVisibility(View.VISIBLE);
		m_tvSpeedCur.setText("当前速率：-");
		m_tvStuckCountCur.setVisibility(View.VISIBLE);
		m_tvStuckCountCur.setText("卡顿次数：-");

		//隐藏结果空间
		m_tvVideoSrc.setVisibility(View.GONE);
		m_tvSpeedAvg.setVisibility(View.GONE);
		m_tvSpeedMax.setVisibility(View.GONE);
		m_layoutRet.setVisibility(View.GONE);
	}

	//停止处理
	private void onStopDeal()
	{
		try
		{
			if (m_timerUpdate != null)
			{
				m_timerUpdate.cancel();
				m_timerUpdate.purge();
				m_timerUpdate = null;
			}

			m_btnStart.setBackgroundDrawable(getResources().getDrawable(R.drawable.sel_btn_start));
			//m_btnStart.setText("");

			if (m_bUpLoaded == false)
				new Thread(upLoadThread).start(); //数据上传
		}
		catch (Exception e)
		{
		}
	}

	//绑定控件
	private void bindComponment(View view)
	{
		m_ivUrls[0] = (ImageView) view.findViewById(R.id.imv_youku);
		m_ivUrls[1] = (ImageView) view.findViewById(R.id.imv_tudou);
		m_ivUrls[2] = (ImageView) view.findViewById(R.id.imv_aiqiyi);
		m_ivUrls[3] = (ImageView) view.findViewById(R.id.imv_souhu);
		m_ivUrls[4] = (ImageView) view.findViewById(R.id.imv_tx);

		for (int i = 0; i < m_ivUrls.length; i++)
		{
			m_ivUrls[i].setOnClickListener(this);
		}
		//---------------------------------------------------------

		m_sbPos = (SeekBar) view.findViewById(R.id.videoBar);
		m_sbPos.setEnabled(false);

		m_tvPos = (TextView) view.findViewById(R.id.tv_pos);

		m_btnStart = (Button) view.findViewById(R.id.btn_test);
		m_btnStart.setOnClickListener(this);

		m_tvStatus = (TextView) view.findViewById(R.id.videostatus);
		m_tvVideoSrc = (TextView) view.findViewById(R.id.videosrc);
		m_tvSpeedCur = (TextView) view.findViewById(R.id.speedcur);
		m_tvStuckCountCur = (TextView) view.findViewById(R.id.stuckcountcur);

		//结果
		m_layoutRet = (LinearLayout) view.findViewById(R.id.layout_ret);
		m_tvSpeedAvg = (TextView) view.findViewById(R.id.speedavg);
		m_tvSpeedMax = (TextView) view.findViewById(R.id.speedmax);
		m_tvLinkDelay = (TextView) view.findViewById(R.id.linkdelay);
		m_tvPlayDelay = (TextView) view.findViewById(R.id.playdelay);
		m_tvStuckCountRet = (TextView) view.findViewById(R.id.stuckcountret);
		//m_tvStuckDelay = (TextView) view.findViewById(R.id.stuckdelay);
		m_tvTrafSize = (TextView) view.findViewById(R.id.trafficsize);

	}

	@Override
	public void onHiddenChanged(boolean hidden)
	{
		super.onHiddenChanged(hidden);
		if (hidden)
			m_Preview.setVisibility(View.GONE);
		else
		{
			m_Preview.setVisibility(View.VISIBLE);
		}
	}
}