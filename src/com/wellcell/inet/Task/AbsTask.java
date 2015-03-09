package com.wellcell.inet.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.Common.CGlobal.TestState;
import com.wellcell.inet.DataProvider.PhoneDataProvider;
import com.wellcell.inet.Database.GzipHelper;
import com.wellcell.inet.Database.SqliteHelper;
import com.wellcell.inet.Database.TestDataLoal;
import com.wellcell.inet.SignalTest.SignalStrengthPar;
import com.wellcell.inet.SignalTest.TelStrengthInfo;
import com.wellcell.inet.Task.Ftp.FtpTestTask;
import com.wellcell.inet.Task.Ping.PingTestTask;
import com.wellcell.inet.Task.Video.VideoTask;
import com.wellcell.inet.Task.Web.WebTestTaskEx;
import com.wellcell.inet.Web.WebUtil;

public abstract class AbsTask
{
	final static String tag = "AbsTask";
	
	//达标点各门限值
	private final static double StandRsrp = -105;
	private final static double StandSinr = -3;
	private final static double StandRx3g = -90;
	private final static double StandEcio3g = -6;
	private final static double StandRx2g = -90;
	private final static double StandEcio2g = -12;
	private final static double StandRxGsm = -90;
	
	public static TaskRankPar m_taskRankPar = null;	//评分体系
	//----------------------------------------------------------------------------
	protected Context m_Context;
	protected StringBuilder m_sbTaskLog = new StringBuilder();	//业务过程数据

	private SignalStrength m_curSignalInfo = null;	//当前的信号信息
	private TelStrengthInfo m_telStreInfo; //信号信息
	private boolean m_bFinish = false;	//是否完成

	public String m_strTaskID;			//业务ID--配置参数附带
	public String m_strTaskName;		//业务名称
	private boolean m_bAnteTest = true;	//是否天馈测量
	private boolean m_bBtsInfo = true; //是否获取基站详细信息
	
	protected Handler m_hHandler;
	protected int m_nWhat;
	
	protected TempAidInfo m_aidInfo;		//辅助信息
	protected JSONArray m_jsonArrRec = null;

	/*public enum TaskType //业务类型
	{
		eFTP, 			//FTP
		ePing,			//Ping
		eWeb,			//Web
		eSinaWeibo,		//新浪微博
		MEDIA,
		eTxWeibo,		//腾讯微博
		eDial,			//语音
		eVideo			//视频
	}*/
	public enum TaskType //业务类型
	{
		FTP, 			//FTP
		PING,			//Ping
		WEBSITE,		//Web
		IM,				//新浪微博
		MEDIA,
		TencentWeibo,	//腾讯微博
		DIAL,			//语音
		VIDEO			//视频
	}

	public TestState m_curState;  //当前测试状态
	
	//1---------------------业务信息--------------------------
	public String m_strJobID;		//任务ID
	public String m_strTID;			//业务ID
	public TaskType m_taskType;		//业务类型
	public boolean m_bAuto = false;	//是否是自动测试
	public long m_lStartTime;		//业务开始时间
	public long m_lEndTime;			//业务结束时间
	
	public boolean m_bLostLink = false;		//是否掉线
	public int m_nLostLinkCount = 0;		//掉线次数
	public boolean m_bSwitchTo1X = false;	//是否有切换到1x
	public int m_nSwitchTo1XCount = 0;		//切换到1x次数
	public int m_nNetWorkAcc;				//接入时网络类型

	// 补充网络占用时长信息（按1秒计算）
	public int m_nDisconnTime;		//离线时长
	public int m_nConnTime;			//在线时长
	
	public class NetInfo //网络信息
	{
		public String strName;	//名称
		public int nType;		//类型
		public int nSubType;	//子类型
		public int nTime;		//所在网络时长
		
		public NetInfo()
		{
			strName = "";
			nType = 0;
			nSubType = 0;
			nTime = 0;
		}
	}
	public NetInfo[] m_netInfos = {new NetInfo(),new NetInfo(),new NetInfo()};	//三个网络信息
	//业务结束经纬度
	public String m_strLonRel = "";
	public String m_strLatRel = "";

	//1.1.1---------------------无线网络信息--------------------------------
	
	public TelStrengthInfo m_cellAcc; 	//LTE接入小区
	public TelStrengthInfo m_cellRel;	//LTE释放小区
	
	//各指标分段
	//LTE
	public int m_nStandLte = 0;	//达标点数
	public int m_nSumLte = 0;	//有效记录数
	public IndicRank m_rankRsrp = new IndicRank(-105,-90,-80,-70);		//RSRP
	public IndicRank m_rankRsrq = new IndicRank(-12,-9,-6,-3);			//RSRQ
	public IndicRank m_rankRssi = new IndicRank(-105,-90,-80,-70);		//RSSI
	public IndicRank m_rankSinr = new IndicRank(-3,0,10,20);			//SINR
	public IndicRank m_rankCqi = new IndicRank(-105,-90,-80,-70);		//CQI
	
	//CDMA
	public int m_nStandEvdo = 0;	//达标点数
	public int m_nSumEvdo = 0;		//有效记录数
	public IndicRank m_rankRx3g = new IndicRank(-105,-90,-75,-60);		//Rx3G
	public IndicRank m_rankEcio3g = new IndicRank(-12,-9,-6,-3);		//Ecio3G
	public IndicRank m_rankSnr = new IndicRank(-3,0,10,20);				//Sinr3g

	public int m_nStandCdma = 0;	//达标点数
	public int m_nSumCdma = 0;		//有效记录数
	public IndicRank m_rankRx2g = new IndicRank(-105,-90,-75,-60);		//Rx2g
	
	//Ecio2g
	public IndicRank m_rankEcio2g = new IndicRank(-12,-9,-6,-3);
	
	//Gsm
	public int m_nStandGsm = 0;	//达标点数
	public int m_nSumGsm = 0;		//有效记录数
	public IndicRank m_rankRxGsm = new IndicRank(-105,-90,-75,-60);
	
	public int m_nRecCount = -1;		//记录数

	//1.1.2---------------------天馈测量信息--------------------------------
	public class AnteInfo //天馈信息
	{
		public String strCarry;	//载频号
		public double dMain;	//主集
		public double dDiv;		//分集
		public double dUserNum;	//在线用户数
		
		public AnteInfo()
		{
			strCarry = "";
			dMain = 0;
			dDiv = 0;
			dUserNum = 0;
		}
	}
	// 网管信息
	public String m_strRot = "";		//ROT
	public String m_strUserNum = "";	//用户数
	public String m_strRssi = "";		//小区RSSI
	public String m_strVswr = "";		//小区驻波比
	
	// 网管信息详细	
	//Rot1~6
	public AnteInfo[] m_anteRot = {new AnteInfo(),new AnteInfo(),new AnteInfo(),
			new AnteInfo(),	new AnteInfo(),new AnteInfo()};
	
	//用户数1~6
	public AnteInfo[] m_anteUserNum = {new AnteInfo(),new AnteInfo(),new AnteInfo(),
			new AnteInfo(),	new AnteInfo(),new AnteInfo()};
	
	//RSSI 1~6
	public AnteInfo[] m_anteRssi = {new AnteInfo(),new AnteInfo(),new AnteInfo(),
			new AnteInfo(),new AnteInfo(),new AnteInfo()};

	public double VSWR_1;	//暂时无用
	//---------------------------------------------------------------
	// 评分字段，不入库
	public int m_nTaskVal = 0;		//评价得分
	public int m_nSpeedVal = 0;		//速率得分
	public int m_nDelayVal = 0;		//时延得分
	public int m_nSucRateVal = 0;	//成功率得分

	protected Timer m_timerRec = new Timer();	//采集定时器
	protected int m_nInterval = 100;			//时间间隔

	// 记录无线环境
	protected TaskReceiver m_recNetWork;	//网络状态监听
	protected StateListener m_listenState;	//信号小区监听器
	protected TelephonyManager m_teleMag;
	protected List<SignalStrength> m_listSignalInfo = new ArrayList<SignalStrength>();	//信号信息
	protected List<NetworkInfo> m_listNetworkInfo = new ArrayList<NetworkInfo>();		//网络信息
	
	//-----------------------------------------------------------------
	protected abstract void runningTask();		//开始业务测试
	
	protected abstract JSONArray getTaskDataJson();	//获取业务数据json
	protected abstract JSONArray getTaskRecJson(JSONArray objRec); //获取过程数据json
	protected abstract String getTaskDetailReport();	//任务信息
	protected abstract String getTaskScoreReport();		//业务的评价
	public abstract String getSimpReport();				//业务的总体报告

	protected abstract double getSpeedAvg();	//获取平均速率
	protected abstract double getDelayAvg();	//获取平均延时
	protected abstract double getSucRate();		//获取成功率
	//-----------------------------------------------------------------
	// 记录无线环境
	protected TimerTask recSignalInfo = new TimerTask()
	{
		@Override
		public void run()
		{
			if(m_curSignalInfo != null)
				m_listSignalInfo.add(m_curSignalInfo);
		}
	};

	// 记录网络信息
	protected TimerTask recNetworkInfo = new TimerTask()
	{
		@Override
		public void run()
		{
			m_listNetworkInfo.add(PhoneDataProvider.getAvaiableNetwork(m_Context));
		}
	};
	
	public AbsTask()
	{
	};
	
	public AbsTask(TaskType type, Context context, boolean bAuto,String strTaskID,boolean bAnteTest,boolean bBtsInfo)
	{
		//初始化评分体系
		if(m_taskRankPar == null)
			m_taskRankPar = TaskRankPar.getInsant(context);
		
		m_curState = TestState.eStoped;

		m_taskType = type;
		m_bAuto = bAuto;
		m_Context = context;
		m_strTaskID = strTaskID;	//业务ID
		m_bAnteTest = bAnteTest;	//是否进行天馈测量
		m_bBtsInfo = bBtsInfo;		//是否获取基站信息
		
		SignalStrengthPar signalPar = SignalStrengthPar.getSignalStrengthFar(m_Context); //小区信号参数
		m_telStreInfo = new TelStrengthInfo(context.getApplicationContext());
		m_telStreInfo.setSignalPar(signalPar); //设置小区信号参数
		
		//接入小区
		m_cellAcc = new TelStrengthInfo(context.getApplicationContext());
		m_cellAcc.setSignalPar(signalPar); //设置小区信号参数
		
		//释放小区
		m_cellRel = new TelStrengthInfo(context.getApplicationContext());
		m_cellRel.setSignalPar(signalPar); //设置小区信号参数

		m_teleMag = (TelephonyManager) m_Context.getSystemService(Context.TELEPHONY_SERVICE);
		m_listenState = new StateListener();

		m_recNetWork = new TaskReceiver();
	}

	//开始业务测试
	protected Runnable runTask = new Runnable()
	{
		@Override
		public void run()
		{
			m_curState = TestState.eTesting;

			int networkType = PhoneDataProvider.getNetworkType(m_Context);
			switch (networkType)
			{
			case TelephonyManager.NETWORK_TYPE_1xRTT:
			case TelephonyManager.NETWORK_TYPE_CDMA:
			case TelephonyManager.NETWORK_TYPE_EVDO_0:
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
			case TelephonyManager.NETWORK_TYPE_LTE:
				getAnteInfoForTask();
				break;
			default:
				break;
			}

			runningTask();	//开始任务
			
			if (m_curState == TestState.eTesting) // 如果已经中断，则不停止任务
				StopTask();
		}
	};
	
	//业务测试是否全部完成
	public boolean IsFinished()
	{
		return m_bFinish;
	}

	//---------------------------------start-------------------------------------
	//开始测试
	public void StartTask()
	{
		if (m_bFinish)
			return;

		// PhoneDataProvider.acuireWakeLock(mContext, false);// 获取WakeLock
		m_curState = TestState.eStarting;

		recordStart();
		prepearTask();
		
		new Thread(runTask).start();	// 开启任务线程
	}
	
	void recordStart() //获取开始状态相关信息
	{
		m_lStartTime = System.currentTimeMillis();	//开始时间
		m_strTID = PhoneDataProvider.getIMEI(m_Context) + "_" + System.currentTimeMillis();	//TID
		
		m_nNetWorkAcc = PhoneDataProvider.getAvaiableNetworkType(m_Context);	//接入网络类型
		
		// 记录接入基站
		m_cellAcc.getCellInfoActive();
	}

	protected void prepearTask()
	{
		try
		{
			// 注册接收器，监听任务过程的网络连接变化
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			m_Context.registerReceiver(m_recNetWork, intentFilter);

			// 监听信号变化
			m_teleMag.listen(m_listenState, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			if (Build.VERSION.SDK_INT < 17) // 低版本
				m_teleMag.listen(m_listenState, PhoneStateListener.LISTEN_CELL_LOCATION 
						| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			else// 高版本
			{
				m_teleMag.listen(m_listenState, PhoneStateListener.LISTEN_CELL_LOCATION 
						| PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
			}
						m_timerRec.schedule(recSignalInfo, 0, 1000);		// 改成固定1秒记录信号
			m_timerRec.schedule(recNetworkInfo, 0, 1000);	// 固定1秒获取一次网络信息

			// 清空
			m_listSignalInfo.clear();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	//---------------------------------stop-------------------------------------------------------
	public void StopTask()	//停止测试
	{
		beforeStop();
		recordStop();
		getTaskValue();

		m_curState = TestState.eStoped;  //等收尾工作完成后才设置状态

		//----------------------------------------------------------
		// 保存记录
/*		TestDataLoal testDataLoal = null;
		try
		{
			testDataLoal = new TestDataLoal(m_Context); 
			testDataLoal.AddTaskRecord(this);
			Log.d(tag, "保存完成");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			testDataLoal.close(); //关闭
		}
		//--------------------------------------------------------------
*/		
		if(m_hHandler != null)
			m_hHandler.sendMessage(Message.obtain(m_hHandler, 12, this));
		
		// PhoneDataProvider.releaseWakeLock();// 释放WakeLock
		m_bFinish = true;
	}

	public void StopTaskWithoutSaving()
	{
		beforeStop();
		m_curState = TestState.eStoped;
	}
	
	protected void beforeStop()
	{
		// 取消接收器，监听任务过程的变化
		/*
		 * if (receiver != null) { mContext.unregisterReceiver(receiver); }
		 */

		// 取消监听信号变化
		m_teleMag.listen(m_listenState, PhoneStateListener.LISTEN_NONE);
		m_timerRec.cancel();
		m_timerRec.purge();
	}
	
	public void recordStop() //获取结束状态信息
	{
		m_lEndTime = System.currentTimeMillis();	//结束时间
		
		buildSignalData();	// 记录信号强度区间
		buildNetworkInfo();	// 记录网络信息

		m_cellRel.getCellInfoActive();	// 记录释放基站

		// 记录经纬度
		Location loc = getLastKnownLocation();
		if (loc != null)
		{
			this.m_strLatRel = CGlobal.floatFormatString(loc.getLatitude(), 5);
			this.m_strLonRel = CGlobal.floatFormatString(loc.getLongitude(),5);
		}
	}
	//----------------------------------------------------------------------------
	/**
	 * 功能: 获取网管信息
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	private void getAnteInfoForTask()
	{
		/*if(m_bBtsInfo)
			m_cellAcc.updateAllBtsInfo();	//获取接入小区信息

		// 获取RSSI
		if ( m_bAnteTest && !m_cellAcc.m_strCID.equals("")) //CDMA
		{
			sendMessage("正在获取网管信息（ROT、用户数、RSSI、驻波比），请稍候...");
			Log.d("", "获取网管数据");
			String strUser = UserInfoHelper.GetLMTUserID(m_Context);
			BtsBasicInfo btsCdmaAcc = m_cellAcc.getBtsCDMA();	//获取CDMA接入小区信息
			String strBscName = UserInfoHelper.GetBscName(btsCdmaAcc, m_Context);
			String strJson = WebUtil.getAnteInfoForTask(strUser, strBscName, 
					btsCdmaAcc.m_strBtsNo, btsCdmaAcc.m_strCellId);	//获取天馈信息
			try
			{
				buildAnteInfo(strJson); //解析天馈测试数据
			}
			catch (JSONException e)
			{
			}
			sendMessage(m_strRot);
			sendMessage(m_strUserNum);
			sendMessage(m_strRssi);
			sendMessage(m_strVswr);
		}*/
	}
	
	// 从JSON获取网管信息
	protected void buildAnteInfo(String json) throws JSONException
	{
		// 简易信息
		JSONObject obj = new JSONObject(json);
		JSONObject simp = obj.getJSONObject("info");
		m_strRssi = simp.getString("rssi");
		m_strVswr = simp.getString("vswr");
		m_strRot = simp.getString("rot");
		m_strUserNum = simp.getString("usernum");

		// 详细信息
		// ROT
		JSONArray arrRot = obj.getJSONArray("rot");
		JSONObject objRot;
		for (int i = 0; i < 6 && i < arrRot.length(); i++)
		{
			objRot = arrRot.getJSONObject(i);
			m_anteRot[i].strCarry = objRot.getString("CRRID");
			m_anteRot[i].dMain = objRot.getDouble("ROT_M");
			m_anteRot[i].dDiv = objRot.getDouble("ROT_D");
		}

		// USERNUM
		JSONObject objUsernum;
		JSONArray arrUsernum = obj.getJSONArray("usernum");
		for (int i = 0; i < 6 && i < arrUsernum.length(); i++)
		{
			objUsernum = arrUsernum.getJSONObject(i);

			m_anteUserNum[i].strCarry = objUsernum.getString("CRRID");
			m_anteUserNum[i].dUserNum = objUsernum.getDouble("USERNUM");
		}

		// RSSI
		JSONObject objRssi;
		JSONArray arrRssi = obj.getJSONArray("rssi");
		for (int i = 0; i < 6 & i < arrRssi.length(); i++)
		{
			objRssi = arrRssi.getJSONObject(i);
			
			m_anteRssi[i].strCarry = objRssi.getString("CRRID");
			m_anteRssi[i].dMain = objRssi.getDouble("RSSI_M");
			m_anteRssi[i].dDiv = objRssi.getDouble("RSSI_D");
		}

		// VSWR
		JSONObject objVswr;
		JSONArray arrVswr = obj.getJSONArray("vswr");
		for (int i = 0; i < 1 & i < arrVswr.length(); i++)
		{
			objVswr = arrVswr.getJSONObject(i);
			switch (i)
			{
			case 0:
				this.VSWR_1 = objVswr.getDouble("VSWR_1");
				break;
			default:
				break;
			}
		}
	}

	//组织网络信息
	protected void buildNetworkInfo()
	{
		m_nConnTime = 0;
		m_nDisconnTime = 0;

		Hashtable<String, Integer> htNetworkCount = new Hashtable<String, Integer>();
		Hashtable<String, String> htNetworkName = new Hashtable<String, String>();

		String strKey;
		for (NetworkInfo netInfo : m_listNetworkInfo)
		{
			if (netInfo != null)
			{
				m_nConnTime++;

				strKey = netInfo.getType() + "_" + netInfo.getSubtype();
				if (!htNetworkCount.containsKey(strKey))
				{
					htNetworkCount.put(strKey, 1);
					switch (netInfo.getType())
					{
					case ConnectivityManager.TYPE_WIFI:
						htNetworkName.put(strKey, "WiFi");
						break;
					default:
						htNetworkName.put(strKey, netInfo.getSubtypeName());
						break;
					}
				}
				else
				{
					int count = htNetworkCount.get(strKey) + 1;
					htNetworkCount.remove(strKey);
					htNetworkCount.put(strKey, count);
				}
			}
			else
				m_nDisconnTime++;
		}

		// 循环去掉最小值，直到剩下3个
		while (htNetworkCount.size() > 3)
		{
			int nValmin = 9999;
			String strMinKey = "";

			for (String key : htNetworkCount.keySet())
			{
				int value = htNetworkCount.get(key);
				if (value < nValmin)
					strMinKey = key;
			}
			// 去掉最小值
			htNetworkCount.remove(strMinKey);
		}

		// 赋值
		String[] strKeys = new String[htNetworkCount.size()];
		htNetworkCount.keySet().toArray(strKeys);
		for (int i = 0; i < strKeys.length && i < m_netInfos.length; i++)
		{
			m_netInfos[i].strName = htNetworkName.get(strKeys[0]);
			try
			{
				m_netInfos[i].nType = Integer.parseInt(strKeys[0].split("_")[0]);
				m_netInfos[i].nSubType = Integer.parseInt(strKeys[0].split("_")[1]);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
			m_netInfos[i].nTime = htNetworkCount.get(strKeys[0]);
		}
	}

	//组织测试数据
	protected void buildSignalData()
	{
		//初始化
		m_rankRsrp.Init();
		m_rankRsrq.Init();
		m_rankSinr.Init();
		m_rankCqi.Init();
		m_rankRx3g.Init();
		m_rankEcio3g.Init();
		m_rankSnr.Init();
		m_rankRx2g.Init();
		m_rankEcio2g.Init();
		m_rankRxGsm.Init();

		boolean bFirstStand = false;	//第一个指标达标
		boolean bSecondStand = false;	//第二个指标达标
		m_nRecCount = m_listSignalInfo.size(); //信号记录数
		for (SignalStrength strenInfo : m_listSignalInfo) //遍历所有信号,并进行分区间
		{
			if (strenInfo != null)
			{
				m_telStreInfo.getSignalInfo(strenInfo, 0, 0); //解析信号
				//LTE
				if (m_telStreInfo.IsValidSignal(0))
				{
					//RSRP
					bFirstStand = TelStrengthInfo.IsValidRSRP(m_telStreInfo.m_dRsrp);
					if (bFirstStand)
						m_rankRsrp.setRank(m_telStreInfo.m_dRsrp);

					//RSRQ
					if (TelStrengthInfo.IsValidRSRQ(m_telStreInfo.m_dRsrq))
						m_rankRsrq.setRank(m_telStreInfo.m_dRsrq);
					
					//RSSI
					if(TelStrengthInfo.IsValidRssi(m_telStreInfo.m_dRssiLte))
						m_rankRssi.setRank(m_telStreInfo.m_dRssiLte);
					
					//SINR
					bSecondStand = TelStrengthInfo.IsValidSinr(m_telStreInfo.m_dSinrLte);
					if(bSecondStand)
						m_rankSinr.setRank(m_telStreInfo.m_dSinrLte);
					
					//CQI
					if(TelStrengthInfo.IsValidCqi(m_telStreInfo.m_nCqiLte))
						m_rankCqi.setRank(m_telStreInfo.m_nCqiLte);
					
					//达标点统计
					if(bFirstStand && bSecondStand)
					{
						m_nSumLte++;
						
						if(m_telStreInfo.m_dRsrp >= StandRsrp && m_telStreInfo.m_dSinrLte >= StandSinr)
							m_nStandLte++;
					}
				}

				//EVDO
				if(m_telStreInfo.IsValidSignal(1))
				{
					bFirstStand = TelStrengthInfo.IsValidRx(m_telStreInfo.m_dRx3G);
					if(bFirstStand)
						m_rankRx3g.setRank(m_telStreInfo.m_dRx3G);
					
					bSecondStand = TelStrengthInfo.IsValidEcio(m_telStreInfo.m_dEcio3G);
					if(bSecondStand)
						m_rankEcio3g.setRank(m_telStreInfo.m_dEcio3G);
					
					if(TelStrengthInfo.IsValidSnr(m_telStreInfo.m_dSnr3G))
						m_rankSnr.setRank(m_telStreInfo.m_dSnr3G);
					
					//达标点统计
					if(bFirstStand && bSecondStand)
					{
						m_nSumEvdo++;
						if(m_telStreInfo.m_dRx3G >= StandRx3g && m_telStreInfo.m_dEcio3G >= StandEcio3g)
							m_nStandEvdo++;
					}
				}
				
				//CDMA
				if(m_telStreInfo.IsValidSignal(2))
				{
					bFirstStand = TelStrengthInfo.IsValidRx(m_telStreInfo.m_dRx2G);
					if(bFirstStand)
						m_rankRx2g.setRank(m_telStreInfo.m_dRx2G);
					
					bSecondStand = TelStrengthInfo.IsValidEcio(m_telStreInfo.m_dEcio2G);
					if(bSecondStand)
						m_rankEcio2g.setRank(m_telStreInfo.m_dEcio2G);
					
					//达标点统计
					if(bFirstStand && bSecondStand)
					{
						m_nSumCdma++;
						if(m_telStreInfo.m_dRx2G >= StandRx2g && m_telStreInfo.m_dEcio2G >= StandEcio2g)
							m_nStandCdma++;
					}
				}

				//Gsm
				if(m_telStreInfo.IsValidSignal(3))
				{
					m_rankRxGsm.setRank(m_telStreInfo.m_dRxGsm);
					
					//达标点统计
					m_nSumGsm++;
					if(m_telStreInfo.m_dRxGsm >= StandRxGsm)
						m_nStandGsm++;
				}
			}
		}
	}

	//-----------------------------------------------------------------------------------------
	protected class TaskReceiver extends BroadcastReceiver //网络监听
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String strAction = intent.getAction();
			// 连接变化，用于判断切换1X和掉线
			if (strAction.equals("android.net.conn.CONNECTIVITY_CHANGE"))
			{
				// 判断是否发生掉线
				if (!PhoneDataProvider.isMobileConnected(m_Context))
				{
					m_bLostLink = true;
					m_nLostLinkCount++;
				}
				else
				{
					// 移动网络有连接，判断是否连接到1X
					String network = PhoneDataProvider.getAvaiableNetworkName(m_Context);
					if (network.toLowerCase().contains("1x"))
					{
						m_bSwitchTo1X = true;
						m_nSwitchTo1XCount++;
					}
				}
			}
		}
	}

	protected class StateListener extends PhoneStateListener
	{
		// lev 17一下小区信息
		@Override
		public void onCellLocationChanged(CellLocation location)
		{
			super.onCellLocationChanged(location);
			try
			{
				m_telStreInfo.getNetWorkType(); // 更新网咯类型

				String strCellLocName = location.getClass().getName(); // 类名

				if (strCellLocName.equals(CdmaCellLocation.class.getName())) // CDMA
					m_telStreInfo.getCdmaCellInfo(location);
				else if (strCellLocName.equals(GsmCellLocation.class.getName())) // GSM
					m_telStreInfo.getGsmCellInfo(location);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// 17以上小区信息变更
		@Override
		public void onCellInfoChanged(List<CellInfo> cellInfo)
		{
			super.onCellInfoChanged(cellInfo);
			try
			{
				m_telStreInfo.getNetWorkType(); // 更新当前网络类型

				String strCellLocName;
				for (CellInfo cell : cellInfo)
				{
					strCellLocName = cell.getClass().getName();
					if (strCellLocName.equals(CellInfoLte.class.getName())) // lte
						m_telStreInfo.getLteCellInfoEx(cell); // 提取cell信息
					else if (strCellLocName.equals(CellInfoCdma.class.getName())) // cdma
						m_telStreInfo.getCdmaCellInfoEx(cell); // 更新基站信息
					else if (strCellLocName.equals(CellInfoGsm.class.getName())) // gsm
						m_telStreInfo.getGsmCellInfoEx(cell);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// 信号强度
		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength)
		{
			super.onSignalStrengthsChanged(signalStrength);

			m_curSignalInfo = signalStrength;	//保存信号信息
		}
	}

	//获取最后的地点信息
	protected Location getLastKnownLocation()
	{
		LocationManager lm = (LocationManager) m_Context.getSystemService(Context.LOCATION_SERVICE);
		return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	

	public void setNotify(Handler handler, int what,JSONArray jsonArr)
	{
		this.m_hHandler = handler;
		this.m_nWhat = what;
		m_jsonArrRec = jsonArr;
	}
	
	protected void sendMessage(String msg)
	{
		Log.d(tag, msg);
		m_sbTaskLog.append(msg);	//保存过程数据
		m_sbTaskLog.append("\n");

		if (m_hHandler != null)
			m_hHandler.sendMessage(Message.obtain(m_hHandler, m_nWhat, msg));
	}

	//获取业务过程信息
	public String getLog()
	{
		return m_sbTaskLog.toString();
	}

	//================================================================================
	//获取业务测试报告
	public Hashtable<String, String> getDetailReport()
	{
		Hashtable<String, String> htDetail = new Hashtable<String, String>();
		htDetail.put("Order", "评价,网络信息,无线环境,任务信息");

		// 任务基本信息
		StringBuilder sb = new StringBuilder();

		htDetail.put("评价", getTaskScoreReport());// 评价

		// 网络信息
		sb = new StringBuilder();
		sb.append(String.format("是否断线：%1s\n", m_bLostLink));
		sb.append(String.format("断线次数：%1s\n", m_nLostLinkCount));
		sb.append(String.format("是否切换到1X：%1s\n", m_bSwitchTo1X));
		sb.append(String.format("切换到1X的次数：%1s\n", m_nSwitchTo1XCount));
		if (m_bAnteTest)
		{
			sb.append(String.format("接入时的ROT：\n%1s\n", m_strRot));
			sb.append(String.format("接入时的用户数：\n%1s\n", m_strUserNum));
			sb.append(String.format("接入时的RSSI：\n%1s\n", m_strRssi));
			sb.append(String.format("接入时的驻波比：\n%1s\n", m_strVswr));
		}
		
		//if(m_bBtsInfo)
		//	m_cellRel.updateAllBtsInfo();	//获取释放小区信息
		
		sb.append(String.format("接入时的LTE基站：%1s\n", m_cellAcc.m_strBtsNameLte));
		sb.append(String.format("接入时的CDMA基站：%1s\n", m_cellAcc.m_strBtsNameCdma));
		sb.append(String.format("释放时的LTE基站：%1s\n", m_cellRel.m_strBtsNameLte));
		sb.append(String.format("释放时的CDMA基站：%1s\n", m_cellRel.m_strBtsNameCdma));

		htDetail.put("网络信息", sb.toString());

		// 地理信息
		//sb = new StringBuilder();
		//sb.append(String.format("释放时的经度：%1s\n", this.m_strLatRel));
		//sb.append(String.format("释放时的纬度：%1s\n", this.m_strLonRel));

		// 无线环境
		sb = new StringBuilder();
		sb.append(String.format("无线测量总次数：%1s\n", this.m_nRecCount));
		
		sb.append("-----4G_RSRP分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", m_rankRsrp.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", m_rankRsrp.m_nPart1));
		sb.append(String.format("(-90,-80]：%1s\n", m_rankRsrp.m_nPart2));
		sb.append(String.format("(-80,-70]：%1s\n", m_rankRsrp.m_nPart3));
		sb.append(String.format("(-70,+∞)：%1s\n", m_rankRsrp.m_nPart4));

		/*sb.append("-----4G_RSRQ分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", m_rankRsrq.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", m_rankRsrq.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", m_rankRsrq.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", m_rankRsrq.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", m_rankRsrq.m_nPart4));
		*/
		sb.append("-----4G_RSSNR分布-----\n");
		sb.append(String.format("(-∞,-3]：%1s\n", m_rankSinr.m_nPart0));
		sb.append(String.format("(-3,0]：%1s\n", m_rankSinr.m_nPart1));
		sb.append(String.format("(0,10]：%1s\n", m_rankSinr.m_nPart2));
		sb.append(String.format("(10,20]：%1s\n", m_rankSinr.m_nPart3));
		sb.append(String.format("(20,+∞)：%1s\n", m_rankSinr.m_nPart4));
	
		sb.append("-----3G_Ecio分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", m_rankEcio3g.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", m_rankEcio3g.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", m_rankEcio3g.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", m_rankEcio3g.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", m_rankEcio3g.m_nPart4));
		
		sb.append("-----3G_Rx分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", m_rankRx3g.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", m_rankRx3g.m_nPart1));
		sb.append(String.format("(-90,-75]：%1s\n", m_rankRx3g.m_nPart2));
		sb.append(String.format("(-80,-75]：%1s\n", m_rankRx3g.m_nPart3));
		sb.append(String.format("(-60,0)：%1s\n", m_rankRx3g.m_nPart4));

		sb.append("-----2G_Ecio分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", m_rankEcio2g.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", m_rankEcio2g.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", m_rankEcio2g.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", m_rankEcio2g.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", m_rankEcio2g.m_nPart4));

		sb.append("-----2G_Rx分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", m_rankRx2g.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", m_rankRx2g.m_nPart1));
		sb.append(String.format("(-90,-75]：%1s\n", m_rankRx2g.m_nPart2));
		sb.append(String.format("(-80,-75]：%1s\n", m_rankRx2g.m_nPart3));
		sb.append(String.format("(-60,0)：%1s\n", m_rankRx2g.m_nPart4));
		
		htDetail.put("无线环境", sb.toString());

		// 任务信息
		htDetail.put("任务信息", getTaskDetailReport());

		return htDetail;
	}

	// 获取任务汇总信息
	public static String getJobReport(Context context, List<AbsTask> listTask)
	{
		int nCountSuc = 0;		//成功记录
		int nCountSum = 0;		//总记录数

		//流量
		int nSizeSum = 0; //总流量
		int nTaskDelay = 0;	//业务测试时间--》计算平均速率
		double dSpeedAvg = 0;	//平均速率(bps)

		double dDelayMax = Double.MIN_VALUE;
		double dDelayMin = Double.MAX_VALUE;
		double dDelayAvg = 0;
		
		//网页KQI
		double dLinkDelaySum = 0;	//连接时延累加值
		int nLinkSucCount = 0;		//连接成功次数
		double dOpenDelaySum = 0;	//打开时延累加值
		int nWebSucCount = 0;		//网页成功次数
		
		//视频KQI
		int nVideoSizeSum = 0;	//视频流量
		double dVideoSpeedSum = 0;	//速率累加值
		int nVideoSpeedCountSum = 0;	//速率计数累加值
		double dVideoSpeedAvg = 0;	//平均速率
		double dVideoSpeedMax = Integer.MIN_VALUE;	//峰值速率

		if(m_taskRankPar == null)
			m_taskRankPar = TaskRankPar.getInsant(context); //获取评分标准
		
		int nValue = 0;
		int nWeight = 0;

		// 构建数据
		for (AbsTask task : listTask)
		{
			switch (task.m_taskType)
			{
			case PING:
				nValue += task.m_nTaskVal * m_taskRankPar.SumPing;
				nWeight += m_taskRankPar.SumPing;

				PingTestTask pingTask = (PingTestTask) task;

				nCountSuc += pingTask.m_nCountSuc;
				nCountSum += pingTask.m_nTestCount;

				dDelayMax = pingTask.m_dDelayMax;
				dDelayMin = pingTask.m_dDelayMin;
				dDelayAvg = pingTask.m_dDelayAvg;
				break;
			case WEBSITE:
				nValue += task.m_nTaskVal * m_taskRankPar.SumWeb;
				nWeight += m_taskRankPar.SumWeb;


					WebTestTaskEx webTask = (WebTestTaskEx) task;

					nCountSuc += webTask.m_nSucCount;
					nCountSum += webTask.m_nTestCount;

					nSizeSum += webTask.m_lSizeSum;
					nTaskDelay += webTask.m_nOpenDelaySum;
					
					dLinkDelaySum += webTask.m_nLinkDelaySum;	//连接时延总和
					nLinkSucCount += webTask.m_nLinkSucCount;	//连接成功次数
					dOpenDelaySum += webTask.m_nOpenDelaySum;	//打开时延总和
					nWebSucCount += webTask.m_nSucCount;	//成功次数
				break;
			case FTP:
				nValue += task.m_nTaskVal * m_taskRankPar.SumFtp;
				nWeight += m_taskRankPar.SumFtp;

				FtpTestTask ftpTask = (FtpTestTask) task;

				nCountSuc += ftpTask.m_nCountSuc;
				nCountSum += ftpTask.m_nTestCount;

				nSizeSum += ftpTask.m_lSizeSuc;
				nTaskDelay += ftpTask.m_nTimeSuc;
				break;
			/*case IM:
			case TencentWeibo:
				nValue += task.m_nTaskVal * m_taskRankPar.SumWeibo;
				nWeight += m_taskRankPar.SumWeibo;

				WeiboTask weiboTask = (WeiboTask) task;

				nCountSuc += weiboTask.m_nCountSuc;
				nCountSum += weiboTask.m_nTestCount;

				nSizeSum += weiboTask.m_lTrafficSuc;
				nTaskDelay += weiboTask.m_nTimeSuc;
				break;
			case MEDIA:
				break;
			case DIAL:
				nValue += task.m_nTaskVal * m_taskRankPar.SumWeb;
				nWeight += m_taskRankPar.SumWeb;

				DialTestTask dialTask = (DialTestTask) task;

				nCountSuc += dialTask.n_nCountSuc;
				nCountSum += dialTask.m_nTestCount;
				break;*/
			case VIDEO:
				nValue += task.m_nTaskVal * m_taskRankPar.SumVideo;
				nWeight += m_taskRankPar.SumVideo;

				VideoTask videoTask = (VideoTask) task;

				nCountSuc += videoTask.m_nCountSuc;
				nCountSum += videoTask.m_nCountSum;
				
				if(videoTask.m_lSizeSum > 0)
					nVideoSizeSum += videoTask.m_lSizeSum;
				
				dVideoSpeedSum += videoTask.m_dSpeedSum;
				nVideoSpeedCountSum += videoTask.m_nSpeedCountSum;
				
				//速率峰值
				if(dVideoSpeedMax < videoTask.m_dSpeedMax)
					dVideoSpeedMax = videoTask.m_dSpeedMax;
				
				break;
			default:
				break;
			}
		}
		//---------------------------------------------------------
		//计算总体平均
		if(nTaskDelay > 0) //网页+FTP+微薄
			dSpeedAvg = nSizeSum * 1000.0 / nTaskDelay;
		
		//补充视频速率
		if(nVideoSpeedCountSum > 0)
			dVideoSpeedAvg = dVideoSpeedSum / nVideoSpeedCountSum;
		
		if(dSpeedAvg != 0 && dVideoSpeedAvg != 0)
			dSpeedAvg = (dSpeedAvg + dVideoSpeedAvg)/2;
		else if(dSpeedAvg == 0)
			dSpeedAvg = dVideoSpeedAvg;
		//---------------------------------------------------------
		// 生成报表
		StringBuilder sb = new StringBuilder();

		// 评价
		int nScore = 0;
		if(nWeight > 0)
			nScore = nValue / nWeight;
		
		sb.append(String.format("评价：%1s（%2s）\n", m_taskRankPar.getTaskRankName(nScore), nScore));

		sb.append(String.format("成功次数/总次数：%1s/%2s\n", nCountSuc, nCountSum));
		sb.append(String.format("总流量：%1s\n", CGlobal.getSizeString(nSizeSum + nVideoSizeSum)));
		sb.append(String.format("平均速率：%1s\n", CGlobal.getSpeedString(dSpeedAvg)));
		
		//--------------------------浏览类KQI------------------------------------------------------------------
		if(nLinkSucCount > 0)
			sb.append(String.format("首包延时(KQI)：%1s毫秒\n", (int)(dLinkDelaySum / nLinkSucCount)));
		else
			sb.append("首包延时(KQI)：-\n");
		
		if(nWebSucCount > 0)
			sb.append(String.format("网页打开延时(KQI)：%1s秒\n", CGlobal.getSecondString(dOpenDelaySum / nWebSucCount)));
		else
			sb.append("网页打开延时(KQI)：-\n");
		
		//--------------------------视频类KQI-----------------------------------------------------------------------------
		if(nVideoSpeedCountSum > 0)
			sb.append(String.format("视频速率(KQI)：%1s\n", CGlobal.getSpeedString(dVideoSpeedAvg)));
		else
			sb.append("视频速率(KQI)：-\n");
		
		if(dVideoSpeedMax != Integer.MIN_VALUE)
			sb.append(String.format("视频峰值速率(KQI)：%1s\n", CGlobal.getSpeedString(dVideoSpeedMax)));
		else
			sb.append("视频峰值速率(KQI)：-\n");
		
		//--------------------------------------------------------------------------------------------------------------

		if (dDelayMax != Double.MIN_VALUE)
		{
			sb.append(String.format("最大/平均/最小用时：\n%1s/%2s/%3s秒\n", 
					CGlobal.getSecondString(dDelayMax), CGlobal.getSecondString(dDelayAvg),
					CGlobal.getSecondString(dDelayMin)));
		}

		return sb.toString();
	}

	// 获取汇总无线环境
	public static String getNetWorkReport(List<AbsTask> listTask)
	{
		//汇总指标
		IndicRank rankRsrp = new IndicRank();
		IndicRank rankRsrq = new IndicRank();
		IndicRank rankSinr = new IndicRank();
		IndicRank rankRx3g = new IndicRank();
		IndicRank rankEcio3g = new IndicRank();
		IndicRank rankRx2g = new IndicRank();
		IndicRank rankEcio2g = new IndicRank();

		int nRecCount = 0;	//总记录数
		for (AbsTask task : listTask)
		{
			nRecCount += task.m_nRecCount;	//累加每个业务的记录数
			
			rankEcio2g.m_nPart0 += task.m_rankEcio2g.m_nPart0;
			rankEcio2g.m_nPart1 += task.m_rankEcio2g.m_nPart1;
			rankEcio2g.m_nPart2 += task.m_rankEcio2g.m_nPart2;
			rankEcio2g.m_nPart3 += task.m_rankEcio2g.m_nPart3;
			rankEcio2g.m_nPart4 += task.m_rankEcio2g.m_nPart4;

			rankRx2g.m_nPart0 += task.m_rankRx2g.m_nPart0;
			rankRx2g.m_nPart1 += task.m_rankRx2g.m_nPart1;
			rankRx2g.m_nPart2 += task.m_rankRx2g.m_nPart2;
			rankRx2g.m_nPart3 += task.m_rankRx2g.m_nPart3;
			rankRx2g.m_nPart4 += task.m_rankRx2g.m_nPart4;
	
			rankEcio3g.m_nPart0 += task.m_rankEcio3g.m_nPart0;
			rankEcio3g.m_nPart1 += task.m_rankEcio3g.m_nPart1;
			rankEcio3g.m_nPart2 += task.m_rankEcio3g.m_nPart2;
			rankEcio3g.m_nPart3 += task.m_rankEcio3g.m_nPart3;
			rankEcio3g.m_nPart4 += task.m_rankEcio3g.m_nPart4;

			rankRx3g.m_nPart0 += task.m_rankRx3g.m_nPart0;
			rankRx3g.m_nPart1 += task.m_rankRx3g.m_nPart1;
			rankRx3g.m_nPart2 += task.m_rankRx3g.m_nPart2;
			rankRx3g.m_nPart3 += task.m_rankRx3g.m_nPart3;
			rankRx3g.m_nPart4 += task.m_rankRx3g.m_nPart4;
			
			rankSinr.m_nPart0 += task.m_rankSinr.m_nPart0;
			rankSinr.m_nPart1 += task.m_rankSinr.m_nPart1;
			rankSinr.m_nPart2 += task.m_rankSinr.m_nPart2;
			rankSinr.m_nPart3 += task.m_rankSinr.m_nPart3;
			rankSinr.m_nPart4 += task.m_rankSinr.m_nPart4;

			rankRsrq.m_nPart0 += task.m_rankRsrq.m_nPart0;
			rankRsrq.m_nPart1 += task.m_rankRsrq.m_nPart1;
			rankRsrq.m_nPart2 += task.m_rankRsrq.m_nPart2;
			rankRsrq.m_nPart3 += task.m_rankRsrq.m_nPart3;
			rankRsrq.m_nPart4 += task.m_rankRsrq.m_nPart4;

			rankRsrp.m_nPart0 += task.m_rankRsrp.m_nPart0;
			rankRsrp.m_nPart1 += task.m_rankRsrp.m_nPart1;
			rankRsrp.m_nPart2 += task.m_rankRsrp.m_nPart2;
			rankRsrp.m_nPart3 += task.m_rankRsrp.m_nPart3;
			rankRsrp.m_nPart4 += task.m_rankRsrp.m_nPart4;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(String.format("无线测量总次数：%1s\n", nRecCount));
		sb.append("-----4G_RSRP分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", rankRsrp.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", rankRsrp.m_nPart1));
		sb.append(String.format("(-90,-80]：%1s\n", rankRsrp.m_nPart2));
		sb.append(String.format("(-80,-70]：%1s\n", rankRsrp.m_nPart3));
		sb.append(String.format("(-70,+∞)：%1s\n", rankRsrp.m_nPart4));

		sb.append("-----4G_RSSNR分布-----\n");
		sb.append(String.format("(-∞,-3]：%1s\n", rankSinr.m_nPart0));
		sb.append(String.format("(-3,0]：%1s\n", rankSinr.m_nPart1));
		sb.append(String.format("(0,10]：%1s\n", rankSinr.m_nPart2));
		sb.append(String.format("(10,20]：%1s\n", rankSinr.m_nPart3));
		sb.append(String.format("(20,+∞)：%1s\n", rankSinr.m_nPart4));
		
		/*sb.append("-----4G_RSRQ分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", rankRsrq.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", rankRsrq.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", rankRsrq.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", rankRsrq.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", rankRsrq.m_nPart4));
		*/
		sb.append("-----3G_Ecio分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", rankEcio3g.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", rankEcio3g.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", rankEcio3g.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", rankEcio3g.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", rankEcio3g.m_nPart4));
		
		sb.append("-----3G_Rx分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", rankRx3g.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", rankRx3g.m_nPart1));
		sb.append(String.format("(-90,-75]：%1s\n", rankRx3g.m_nPart2));
		sb.append(String.format("(-80,-75]：%1s\n", rankRx3g.m_nPart3));
		sb.append(String.format("(-60,0)：%1s\n", rankRx3g.m_nPart4));

		sb.append("-----2G_Ecio分布-----\n");
		sb.append(String.format("(-∞,-12]：%1s\n", rankEcio2g.m_nPart0));
		sb.append(String.format("(-12,-9]：%1s\n", rankEcio2g.m_nPart1));
		sb.append(String.format("(-9,-6]：%1s\n", rankEcio2g.m_nPart2));
		sb.append(String.format("(-6,-3]：%1s\n", rankEcio2g.m_nPart3));
		sb.append(String.format("(-3,0)：%1s\n", rankEcio2g.m_nPart4));

		sb.append("-----2G_Rx分布-----\n");
		sb.append(String.format("(-∞,-105]：%1s\n", rankRx2g.m_nPart0));
		sb.append(String.format("(-105,-90]：%1s\n", rankRx2g.m_nPart1));
		sb.append(String.format("(-90,-75]：%1s\n", rankRx2g.m_nPart2));
		sb.append(String.format("(-80,-75]：%1s\n", rankRx2g.m_nPart3));
		sb.append(String.format("(-60,0)：%1s\n", rankRx2g.m_nPart4));
		return sb.toString();
	}
	//=========================================================================
	//天馈测量信息是否有效
	public boolean isAnteInfoValid()
	{
		//不进行天馈测量
		if(!m_bAnteTest)
			return false;
		
		try
		{
			if (this.m_strRot == null || this.m_strRot.length() == 0)
				return false;
			
			if (this.m_strUserNum == null || this.m_strUserNum.length() == 0)
				return false;
			
			if (this.m_strRssi == null || this.m_strRssi.length() == 0)
				return false;
			
			if (this.m_strVswr == null || this.m_strVswr.length() == 0)
				return false;
			
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	//获取天馈测量信息
	public String getAnteInfo()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("接入时的ROT：\n%1s\n", this.m_strRot));
		sb.append(String.format("接入时的用户数：\n%1s\n", this.m_strUserNum));
		sb.append(String.format("接入时的RSSI：\n%1s\n", this.m_strRssi));
		sb.append(String.format("接入时的驻波比：\n%1s\n", this.m_strVswr));
		return sb.toString();
	}

	/**
	 * 功能: 获取业务各项评估得分(包括: 评价,平均速率得分,平均时延得分,成功率得分)
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	protected void getTaskValue()
	{
		try
		{
			// 生成评分
			if(m_taskRankPar == null)
				m_taskRankPar = TaskRankPar.getInsant(m_Context);
			
			this.m_nSpeedVal = m_taskRankPar.getSpeedValue(getSpeedAvg());	//速率得分
			this.m_nDelayVal = m_taskRankPar.getDelayValue(getDelayAvg());	//时延得分
			this.m_nSucRateVal = m_taskRankPar.getSucRateValue(getSucRate());//成功率得分

			switch (this.m_taskType)
			{
			case PING:
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedPing + m_nDelayVal * m_taskRankPar.DelayPing + m_nSucRateVal * m_taskRankPar.DropLinkPing) 
									/ (m_taskRankPar.SpeedPing + m_taskRankPar.DelayPing + m_taskRankPar.DropLinkPing);
				break;
			case WEBSITE:
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedWeb + m_nDelayVal * m_taskRankPar.DelayWeb + m_nSucRateVal * m_taskRankPar.DropLinkWeb) 
									/ (m_taskRankPar.SpeedWeb + m_taskRankPar.DelayWeb + m_taskRankPar.DropLinkWeb);
				break;
			case FTP:
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedFtp + m_nDelayVal * m_taskRankPar.DelayFtp + m_nSucRateVal * m_taskRankPar.DropLinkFtp) 
									/ (m_taskRankPar.SpeedFtp + m_taskRankPar.DelayFtp + m_taskRankPar.DropLinkFtp);
				break;
			case IM:
			case TencentWeibo:
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedWeibo + m_nDelayVal * m_taskRankPar.DelayWeibo + m_nSucRateVal * m_taskRankPar.DropLinkWeibo) 
									/ (m_taskRankPar.SpeedWeibo + m_taskRankPar.DelayWeibo + m_taskRankPar.DropLinkWeibo);
				break;
			case MEDIA:
				m_nTaskVal = 0;
				break;
			case DIAL:
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedDial + m_nDelayVal * m_taskRankPar.DelayDial + m_nSucRateVal * m_taskRankPar.DropLinkDial) 
									/ (m_taskRankPar.SpeedDial + m_taskRankPar.DelayDial + m_taskRankPar.DropLinkDial);
				break;
			case VIDEO:	
				m_nTaskVal = (m_nSpeedVal * m_taskRankPar.SpeedVideo + m_nDelayVal * m_taskRankPar.DelayVideo + m_nSucRateVal * m_taskRankPar.DropLinkVideo) 
									/ (m_taskRankPar.SpeedVideo + m_taskRankPar.DelayVideo + m_taskRankPar.DropLinkVideo);
				break;
			default:
				m_nTaskVal = 0;
				break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//获取速率得分字符串
	protected String getSpeedValString()
	{
		String strRet = "-";
		switch (m_taskType)
		{
		case PING:
			if( m_taskRankPar.SpeedPing != 0)
				strRet = m_nSpeedVal + "";
			break;
		case WEBSITE:
			if(m_taskRankPar.SpeedWeb != 0)
				strRet = m_nSpeedVal + "";
			break;
		case FTP:
			if(m_taskRankPar.SpeedFtp != 0)
				strRet = m_nSpeedVal + "";
			break;
		case IM:
		case TencentWeibo:
			if(m_taskRankPar.SpeedWeibo != 0)
				strRet = m_nSpeedVal + "";
			break;
		case MEDIA:
			break;
		case DIAL:
			if(m_taskRankPar.SpeedDial != 0)
				strRet = m_nSpeedVal + "";
			break;
		case VIDEO:
			if(m_taskRankPar.SpeedVideo != 0)
				strRet = m_nSpeedVal + "";
			break;
		default:
			break;
		}
		return strRet;
	}
	
	//获取时延得分字符串
	protected String getDelayValString()
	{
		String strRet = "-";
		switch (m_taskType)
		{
		case PING:
			 if(m_taskRankPar.DelayPing != 0)
				 strRet = m_nDelayVal + "";
			break;
		case WEBSITE:
			if(m_taskRankPar.DelayWeb != 0)
				strRet = m_nDelayVal + "";
			break;
		case FTP:
			if(m_taskRankPar.DelayFtp != 0)
				strRet = m_nDelayVal + "";
			break;
		case IM:
		case TencentWeibo:
			if(m_taskRankPar.DelayWeibo != 0)
				strRet = m_nDelayVal + "";
			break;
		case MEDIA:
			break;
		case DIAL:
			if(m_taskRankPar.DelayDial != 0)
				strRet = m_nDelayVal + "";
			break;
		case VIDEO:
			if(m_taskRankPar.DelayVideo != 0)
				strRet = m_nDelayVal + "";
			break;
		default:
			break;
		}
		return strRet;
	}
	
	//获取成功率得分字符串
	protected String getSucRateValString()
	{
		String strRet = "-";
		switch (m_taskType)
		{
		case PING:
			if(m_taskRankPar.DropLinkPing != 0)
				strRet = m_nSucRateVal + "";
			break;
		case WEBSITE:
			if(m_taskRankPar.DropLinkWeb != 0)
				strRet = m_nSucRateVal + "";
			break;
		case FTP:
			if(m_taskRankPar.DropLinkFtp != 0)
				strRet = m_nSucRateVal + "";
			break;
		case IM:
		case TencentWeibo:
			if(m_taskRankPar.DropLinkWeibo != 0)
				strRet = m_nSucRateVal + "";
			break;
		case MEDIA:
			break;
		case DIAL:
			if(m_taskRankPar.DropLinkDial != 0)
				strRet = m_nSucRateVal + "";
			break;
		case VIDEO:
			if(m_taskRankPar.DropLinkVideo != 0)
				strRet = m_nSucRateVal + "";
			break;
		default:
			break;
		}
		return strRet;
	}
	//-------------------------------------------------------------------------------------------------------
	//获取业务对应的业务表信息json
	protected JSONArray getTaskInfoJson()
	{
		JSONArray obj = new JSONArray();
		
		obj.put(m_strJobID);
		obj.put(m_strTID);
		obj.put(getTaskTypeString());
		obj.put(getAutoString());
		obj.put(m_lStartTime + "");
		obj.put(m_lEndTime + "");
		
		obj.put(m_nLostLinkCount + "");		//掉线次数
		obj.put(m_nSwitchTo1XCount + "");	//1x切换次数
		obj.put(m_nNetWorkAcc + "");		//网络类型
		obj.put(m_nDisconnTime  + "");		//离线市场
		obj.put(m_nConnTime + "");			//在线时长

		//网络1
		for (int i = 0; i < m_netInfos.length; i++)
		{
			obj.put(m_netInfos[i].strName);			//名称
			obj.put(m_netInfos[i].nType + "");		//类型
			obj.put(m_netInfos[i].nSubType + "");	//子类型
			obj.put(m_netInfos[i].nTime + "");		//在线时长
		}
	
		//释放经纬度
		obj.put(m_strLonRel);
		obj.put(m_strLatRel);
		
		return obj;
	}
	
	//获取业务对应的无线网络信息json
	protected JSONArray getNetWorkJson()
	{
		JSONArray obj = new JSONArray();
		
		obj.put(m_strJobID);
		obj.put(m_strTID);
		
		//-----------------接入---------------
		//LTE接入基站(空字符)
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");

		//LTE接入小区
		obj.put(m_cellAcc.m_strMcc);
		obj.put(m_cellAcc.m_strMnc);
		obj.put(m_cellAcc.m_strCi);
		obj.put(m_cellAcc.m_strPci);
		obj.put(m_cellAcc.m_strTac);
	
		//CDMA接入基站(空字符)
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");

		//CDMA接入小区
		obj.put(m_cellAcc.m_strCID);
		obj.put(m_cellAcc.m_strSID);
		obj.put(m_cellAcc.m_strNID);
		
		//GSM
		obj.put(m_cellAcc.m_strCIDGsm);
		obj.put(m_cellAcc.m_strLacGsm);
		//--------------释放------------------
		//LTE基站(空字符)
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");

		//LTE接入小区
		obj.put(m_cellRel.m_strMcc);
		obj.put(m_cellRel.m_strMnc);
		obj.put(m_cellRel.m_strCi);
		obj.put(m_cellRel.m_strPci);
		obj.put(m_cellRel.m_strTac);
	
		//CDMA接入基站(空字符)
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");
		obj.put("");

		//CDMA接入小区
		obj.put(m_cellRel.m_strCID);
		obj.put(m_cellRel.m_strSID);
		obj.put(m_cellRel.m_strNID);
		
		//GSM
		obj.put(m_cellRel.m_strCIDGsm);
		obj.put(m_cellRel.m_strLacGsm);
		//---------------------------------------------
		//RSRP
		obj.put(m_nStandLte + "");			//达标数
		obj.put(m_nSumLte + "");			//记录总数
		obj.put(CGlobal.floatFormatString(m_rankRsrp.m_dSum,2));	//加权分子
		obj.put(m_rankRsrp.m_nCount + "");	//记录数
		obj.put(m_rankRsrp.m_nPart0 + "");
		obj.put(m_rankRsrp.m_nPart1 + "");
		obj.put(m_rankRsrp.m_nPart2 + "");
		obj.put(m_rankRsrp.m_nPart3 + "");
		obj.put(m_rankRsrp.m_nPart4 + "");
		
		//RSRQ
		obj.put(CGlobal.floatFormatString(m_rankRsrq.m_dSum,2));	//加权分子
		obj.put(m_rankRsrq.m_nCount + "");	//记录数
		obj.put(m_rankRsrq.m_nPart0 + "");
		obj.put(m_rankRsrq.m_nPart1 + "");
		obj.put(m_rankRsrq.m_nPart2 + "");
		obj.put(m_rankRsrq.m_nPart3 + "");
		obj.put(m_rankRsrq.m_nPart4 + "");

		//RSSI
		obj.put(CGlobal.floatFormatString(m_rankRssi.m_dSum,2));	//加权分子
		obj.put(m_rankRssi.m_nCount + "");	//记录数
		obj.put(m_rankRssi.m_nPart0 + "");
		obj.put(m_rankRssi.m_nPart1 + "");
		obj.put(m_rankRssi.m_nPart2 + "");
		obj.put(m_rankRssi.m_nPart3 + "");
		obj.put(m_rankRssi.m_nPart4 + "");

		//SINR
		obj.put(CGlobal.floatFormatString(m_rankSinr.m_dSum,2));	//加权分子
		obj.put(m_rankSinr.m_nCount + "");	//记录数
		obj.put(m_rankSinr.m_nPart0 + "");
		obj.put(m_rankSinr.m_nPart1 + "");
		obj.put(m_rankSinr.m_nPart2 + "");
		obj.put(m_rankSinr.m_nPart3 + "");
		obj.put(m_rankSinr.m_nPart4 + "");
		
		//CQI
		obj.put(CGlobal.floatFormatString(m_rankCqi.m_dSum,2));		//加权分子
		obj.put(m_rankCqi.m_nCount + "");	//记录数
		obj.put(m_rankCqi.m_nPart0 + "");
		obj.put(m_rankCqi.m_nPart1 + "");
		obj.put(m_rankCqi.m_nPart2 + "");
		obj.put(m_rankCqi.m_nPart3 + "");
		obj.put(m_rankCqi.m_nPart4 + "");
		//--------------cdma-----------------
		//Rx3g
		obj.put(m_nStandEvdo + "");				//达标数
		obj.put(m_nSumEvdo + "");
		obj.put(CGlobal.floatFormatString(m_rankRx3g.m_dSum,2));		//加权分子
		obj.put(m_rankRx3g.m_nCount + "");		//记录数
		obj.put(m_rankRx3g.m_nPart0 + "");
		obj.put(m_rankRx3g.m_nPart1 + "");
		obj.put(m_rankRx3g.m_nPart2 + "");
		obj.put(m_rankRx3g.m_nPart3 + "");
		obj.put(m_rankRx3g.m_nPart4 + "");

		//Ecio3g
		obj.put(CGlobal.floatFormatString(m_rankEcio3g.m_dSum,2));		//加权分子
		obj.put(m_rankEcio3g.m_nCount + "");	//记录数
		obj.put(m_rankEcio3g.m_nPart0 + "");
		obj.put(m_rankEcio3g.m_nPart1 + "");
		obj.put(m_rankEcio3g.m_nPart2 + "");
		obj.put(m_rankEcio3g.m_nPart3 + "");
		obj.put(m_rankEcio3g.m_nPart4 + "");

		//SNR
		obj.put(CGlobal.floatFormatString(m_rankSnr.m_dSum,2));		//加权分子
		obj.put(m_rankSnr.m_nCount + "");	//记录数
		obj.put(m_rankSnr.m_nPart0 + "");
		obj.put(m_rankSnr.m_nPart1 + "");
		obj.put(m_rankSnr.m_nPart2 + "");
		obj.put(m_rankSnr.m_nPart3 + "");
		obj.put(m_rankSnr.m_nPart4 + "");

		//Rx2g
		obj.put(m_nStandCdma + "");			//达标数
		obj.put(m_nSumCdma + "");
		obj.put(CGlobal.floatFormatString(m_rankRx2g.m_dSum,2));	//加权分子
		obj.put(m_rankRx2g.m_nCount + "");	//记录数
		obj.put(m_rankRx2g.m_nPart0 + "");
		obj.put(m_rankRx2g.m_nPart1 + "");
		obj.put(m_rankRx2g.m_nPart2 + "");
		obj.put(m_rankRx2g.m_nPart3 + "");
		obj.put(m_rankRx2g.m_nPart4 + "");

		//Ecio2g
		obj.put(CGlobal.floatFormatString(m_rankEcio2g.m_dSum,2));	//加权分子
		obj.put(m_rankEcio2g.m_nCount + "");//记录数
		obj.put(m_rankEcio2g.m_nPart0 + "");
		obj.put(m_rankEcio2g.m_nPart1 + "");
		obj.put(m_rankEcio2g.m_nPart2 + "");
		obj.put(m_rankEcio2g.m_nPart3 + "");
		obj.put(m_rankEcio2g.m_nPart4 + "");

		//RxGsm
		obj.put(m_nStandGsm + "");			//达标点数
		obj.put(m_nSumGsm + "");
		obj.put(CGlobal.floatFormatString(m_rankRxGsm.m_dSum,2));	//加权分子
		obj.put(m_rankRxGsm.m_nCount + "");	//记录数
		obj.put(m_rankRxGsm.m_nPart0 + "");
		obj.put(m_rankRxGsm.m_nPart1 + "");
		obj.put(m_rankRxGsm.m_nPart2 + "");
		obj.put(m_rankRxGsm.m_nPart3 + "");
		obj.put(m_rankRxGsm.m_nPart4 + "");

		return obj;
	}
	
	//获取业务对应的天馈信息json
	protected JSONArray getAnteJson()
	{
		JSONArray obj = new JSONArray();
		
		obj.put(m_strTID);
		
		obj.put("");	//开始时ROT
		obj.put("");	//开始时RSSI
		obj.put("");	//开始时用户数
		obj.put("");	//开始时VSWR
		
		//ROT
		for (int i = 0; i < m_anteRot.length; i++)
		{
			obj.put(m_anteRot[i].strCarry);	//载频号
			obj.put(CGlobal.floatFormatString(m_anteRot[i].dMain,2));	//主集
			obj.put(CGlobal.floatFormatString(m_anteRot[i].dDiv,2));	//分集
		}
		//------------------------------
		//用户数
		for (int i = 0; i < m_anteUserNum.length; i++)
		{
			obj.put(m_anteUserNum[i].strCarry);	//载频号
			obj.put(CGlobal.floatFormatString(m_anteUserNum[i].dUserNum,2));	//在线用户数
		}
		//-------------------------------
		//RSSI
		for (int i = 0; i < m_anteRssi.length; i++)
		{
			obj.put(m_anteRssi[i].strCarry);		//载频号
			obj.put(CGlobal.floatFormatString(m_anteRssi[i].dMain,2));		//主集
			obj.put(m_anteRssi[i].dDiv + "");		//分集
		}

		return obj;
	}

	//获取业务类型自定义值
	public String getTaskTypeString()
	{
		String strRet = "";
		switch (m_taskType)
		{
		case PING:
			strRet = "1";
			break;
		case WEBSITE:
			strRet = "2";
			break;
		case FTP:
			strRet = "3";
			break;
		case DIAL:
			strRet = "4";
			break;
		case VIDEO:
			strRet = "5";
			break;
		case IM:
		case TencentWeibo:
			strRet = "6";
			break;
		default:
			break;
		}
		return strRet;
	}
	
	//获取自动测试状态字符串
	public String getAutoString()
	{
		return m_bAuto ? "1" : "0";
	}
	
	//-------------------------------------------------------
	protected boolean uploadToServer()
	{
		JSONArray arr = new JSONArray();
		arr.put(this.toJsonObj());
		String json = arr.toString();
		json = GzipHelper.getGzipCompress(json);

		// 上传
		/*if (WebUtil.uploadTask(json, this.m_taskType))
		{
			Log.d(tag, "上传成功");
			// 更新状态
			SqliteHelper sqliteHelper = new SqliteHelper(m_Context);
			sqliteHelper.UpdateTaskUploadStatus(this);
			sqliteHelper.closeDb();
			return true;
		}
		else
		{
			Log.d(tag, "上传失败");
			return false;
		}*/
		
		return false;
	}
	
	public JSONObject toJsonObj()
	{
		JSONObject obj = new JSONObject();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		try
		{
			//obj.put("TID", this.m_strJobID);
			// obj.put("TASKTYPE", this.taskType.name());
			// obj.put("ISAUTO", this.isAuto);
			//obj.put("TASK_STARTTIME", sdf.format(this.m_dateTaskStart));
			//obj.put("TASK_ENDTIME", sdf.format(this.m_dateTaskEnd));
			// obj.put("ISLOSTLINK", this.isLostLink);
			obj.put("LOSTLINKCOUNT", this.m_nLostLinkCount);
			// obj.put("ISSWITCHTO1X", this.isSwitchTo1X);
			obj.put("SWITCHTO1XCOUNT", this.m_nSwitchTo1XCount);
			obj.put("ROT", this.m_strRot);
			obj.put("USERNUM", this.m_strUserNum);
			obj.put("RSSI", this.m_strRssi);
			// obj.put("VSWR", this.vswr);
			//obj.put("ACC_SID", this.m_nSIDAcc);
			//obj.put("ACC_NID", this.m_nNIDAcc);
			//obj.put("ACC_BASEID", this.m_nCIDAcc);
			//obj.put("RLS_SID", this.m_nSIDRel);
			//obj.put("RLS_NID", this.m_nNIDRel);
			//obj.put("RLS_BASEID", this.m_nCIDRel);
			obj.put("ACC_LAT", this.m_strLatRel);
			obj.put("ACC_LNG", this.m_strLonRel);

			obj.put("ss2GEcio_3", this.m_rankEcio2g.m_nPart0);
			obj.put("ss2GEcio_3_6", this.m_rankEcio2g.m_nPart1);
			obj.put("ss2GEcio_6_9", this.m_rankEcio2g.m_nPart2);
			obj.put("ss2GEcio_9_12", this.m_rankEcio2g.m_nPart3);
			obj.put("ss2GEcio_12", this.m_rankEcio2g.m_nPart4);
			
			obj.put("ss2GRx_60", this.m_rankRx2g.m_nPart4);
			obj.put("ss2GRx_60_75", this.m_rankRx2g.m_nPart3);
			obj.put("ss2GRx_75_90", this.m_rankRx2g.m_nPart2);
			obj.put("ss2GRx_90_105", this.m_rankRx2g.m_nPart1);
			obj.put("ss2GRx_105", this.m_rankRx2g.m_nPart0);
			
			obj.put("ss3GCI_3", this.m_rankEcio3g.m_nPart0);
			obj.put("ss3GCI_3_6", this.m_rankEcio3g.m_nPart1);
			obj.put("ss3GCI_6_9", this.m_rankEcio3g.m_nPart2);
			obj.put("ss3GCI_9_12", this.m_rankEcio3g.m_nPart3);
			obj.put("ss3GCI_12", this.m_rankEcio3g.m_nPart4);
			obj.put("ss3GRx_60", this.m_rankRx3g.m_nPart4);
			obj.put("ss3GRx_60_75", this.m_rankRx3g.m_nPart3);
			obj.put("ss3GRx_75_90", this.m_rankRx3g.m_nPart2);
			obj.put("ss3GRx_90_105", this.m_rankRx3g.m_nPart1);
			obj.put("ss3GRx_105", this.m_rankRx3g.m_nPart0);
			obj.put("ss4GRSRQ_3", this.m_rankRsrq.m_nPart0);
			obj.put("ss4GRSRQ_3_6", this.m_rankRsrq.m_nPart1);
			obj.put("ss4GRSRQ_6_9", this.m_rankRsrq.m_nPart2);
			obj.put("ss4GRSRQ_9_12", this.m_rankRsrq.m_nPart3);
			obj.put("ss4GRSRQ_12", this.m_rankRsrq.m_nPart4);
			obj.put("ss4GRSRP_60", this.m_rankRsrp.m_nPart4);
			obj.put("ss4GRSRP_60_75", this.m_rankRsrp.m_nPart3);
			obj.put("ss4GRSRP_75_90", this.m_rankRsrp.m_nPart2);
			obj.put("ss4GRSRP_90_105", this.m_rankRsrp.m_nPart1);
			obj.put("ss4GRSRP_105", this.m_rankRsrp.m_nPart0);
			obj.put("ssCount", this.m_nRecCount);

			//addJsonObj(obj);

			// obj.put("IMSI", this.IMSI);
			// obj.put("IMEI", this.IMEI);
			// obj.put("DEVID", this.DEVID);
			//obj.put("TASKREMARK", this.m_strHotName);
			// obj.put("netWorkName", this.networkName);
			obj.put("NetWorkType", 0);
			// obj.put("POID", this.poid);
			obj.put("ISAUTO", 0);

/*			obj.put("ROT_CARR_1", this.m_strRotCarr1);
			obj.put("ROT_MAIN_1", this.m_dRotMain1);
			obj.put("ROT_DIV_1", this.m_dRotDiv1);
			obj.put("ROT_CARR_2", this.m_strRotCarr2);
			obj.put("ROT_MAIN_2", this.m_dRotMain2);
			obj.put("ROT_DIV_2", this.m_dRotDiv2);
			obj.put("ROT_CARR_3", this.m_strRotCarr3);
			obj.put("ROT_MAIN_3", this.m_dRotMain3);
			obj.put("ROT_DIV_3", this.m_dRotDiv3);
			obj.put("ROT_CARR_4", this.m_strRotCarr4);
			obj.put("ROT_MAIN_4", this.m_dRotMain4);
			obj.put("ROT_DIV_4", this.m_dRotDiv4);
			obj.put("ROT_CARR_5", this.m_strRotCarr5);
			obj.put("ROT_MAIN_5", this.m_dRotMain5);
			obj.put("ROT_DIV_5", this.m_dRotDiv5);
			obj.put("ROT_CARR_6", this.m_strRotCarr6);
			obj.put("ROT_MAIN_6", this.m_dRotMain6);
			obj.put("ROT_DIV_6", this.m_dRotDiv6);
			obj.put("USERNUM_CARR_1", this.m_strUserNumCarr1);
			obj.put("USERNUM_1", this.m_dUserNum1);
			obj.put("USERNUM_CARR_2", this.m_strUserNumCarr2);
			obj.put("USERNUM_2", this.m_dUserNum2);
			obj.put("USERNUM_CARR_3", this.m_strUserNumCarr3);
			obj.put("USERNUM_3", this.m_dUserNum3);
			obj.put("USERNUM_CARR_4", this.m_strUserNumCarr4);
			obj.put("USERNUM_4", this.m_dUserNum4);
			obj.put("USERNUM_CARR_5", this.m_strUserNumCarr5);
			obj.put("USERNUM_5", this.m_dUserNum5);
			obj.put("USERNUM_CARR_6", this.m_strUserNumCarr6);
			obj.put("USERNUM_6", this.m_dUserNum6);
			obj.put("RSSI_CARR_1", this.m_strRssiCarr1);
			obj.put("RSSI_MAIN_1", this.m_dRssiMain1);
			obj.put("RSSI_DIV_1", this.m_dRssiDiv1);
			obj.put("RSSI_CARR_2", this.m_strRssiCarr2);
			obj.put("RSSI_MAIN_2", this.m_dRssiMain2);
			obj.put("RSSI_DIV_2", this.m_dRssiDiv2);
			obj.put("RSSI_CARR_3", this.m_strRssiCarr3);
			obj.put("RSSI_MAIN_3", this.m_dRssiMain3);
			obj.put("RSSI_DIV_3", this.m_dRssiDiv3);
			obj.put("RSSI_CARR_4", this.m_strRssiCarr4);
			obj.put("RSSI_MAIN_4", this.m_dRssiMain4);
			obj.put("RSSI_DIV_4", this.m_dRssiDiv4);
			obj.put("RSSI_CARR_5", this.m_strRssiCarr5);
			obj.put("RSSI_MAIN_5", this.m_dRssiMain5);
			obj.put("RSSI_DIV_5", this.m_dRssiDiv5);
			obj.put("RSSI_CARR_6", this.m_strRssiCarr6);
			obj.put("RSSI_MAIN_6", this.m_dRssiMain6);
			obj.put("RSSI_DIV_6", this.m_dRssiDiv6);
			// obj.put("VSWR_1", this.VSWR_1);
			obj.put("VSWR", this.VSWR_1);
*/
			obj.put("NETWORK_DISCONNECT_TIME", this.m_nDisconnTime);
			obj.put("NETWORK_CONNECT_TIME", this.m_nConnTime);
			/*obj.put("NETWORK_CONNECT_1_NAME", this.m_strNetName1);
			obj.put("NETWORK_CONNECT_1_TYPE", this.m_nNetType1);
			obj.put("NETWORK_CONNECT_1_SUBTYPE", this.m_nNetSubType1);
			obj.put("NETWORK_CONNECT_1_TIME", this.m_nNetTime1);
			obj.put("NETWORK_CONNECT_2_NAME", this.m_strNetName2);
			obj.put("NETWORK_CONNECT_2_TYPE", this.m_nNetType2);
			obj.put("NETWORK_CONNECT_2_SUBTYPE", this.m_nNetSubType2);
			obj.put("NETWORK_CONNECT_2_TIME", this.m_nNetTime2);
			obj.put("NETWORK_CONNECT_3_NAME", this.m_strNetName3);
			obj.put("NETWORK_CONNECT_3_TYPE", this.m_nNetType3);
			obj.put("NETWORK_CONNECT_3_SUBTYPE", this.m_nNetSubType3);
			obj.put("NETWORK_CONNECT_3_TIME", this.m_nNetTime3);*/
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		return obj;
	}
}
