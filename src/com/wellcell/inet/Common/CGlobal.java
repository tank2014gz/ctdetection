package com.wellcell.inet.Common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.TrafficStats;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.wellcell.MainFrag.SettingFragment.SettingPar;
import com.wellcell.inet.Database.LocalCache;

//全局变量及函数
public class CGlobal
{
	private static String TAG = "CGlobal";
	public static String ModuleType = "ModuleType";	//模块类型key
	public static String PreClass = "PreClass";		//跳转class的key
	
	private static int m_nSizeType = -1;	//获取流量的类型;0:进程流量,1:系统流量
	
	public static final String[] WebAddrs = new String[] { "www.baidu.com","www.163.com","www.qq.com"};
	
	//功能模块索引
	public static enum ModuleIndex
	{
		eMain,			//主界面
		eCQT,			//CQT
		eDT,			//DT
		eAuto,			//定点引爆
		eCust,			//大众版测试
		eProbback,		//问题热点反馈
		eSignal,		//信号测量
		eCustTest,		//大众版
		eAnte,			//天馈测量
		eRunningData,	//运行数据
		eCompQury,		//综合查询
		eMap,			//地图
		eSetting		//设置
	}
	
	
	public enum TestState	//测试状态
	{
		eReady,		//准备
		eStarting,	//正在开始
		eTesting,	//正在测试
		eUploading,	//上传
		eUploaded,	//已上传
		ePause,		//暂停
		eStoping,	//正在停止
		eStoped,	//停止
		eComplete	//完成
	}

	//====================================================================================
	/**
	 * 功能: 获取软件更新信息
	 *  参数: 
	 *  返回值: 
	 *  说明:
	 */
	public static String checkUpdateInfo(Context context)
	{
		PackageInfo info = null;
		String strInfo = null;
		try
		{
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0); // 获取安装包信息
			//if (info != null)
				//strInfo = WebUtil.getUpdateInfo(info.versionCode);	// 获取更新信息
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return strInfo;
	}
	
	/**
	 * 功能: 隐藏软键盘
	 * 参数:	context: 
	 * 		view: 输入view
	 * 返回值:
	 * 说明:
	 */
	public static void HideSoftInput(Context context,View view)
	{
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);//(0, InputMethodManager.HIDE_NOT_ALWAYS);
	}
	
	/**
	 * 功能:文件字节数转换成适配的单位 
	 * 参数: sizeInByte: 文件字节数 
	 * 返回值: 
	 * 说明:
	 */
	public static String getSizeString(long sizeInByte)
	{
		if(sizeInByte < 0)
			return "- ";
		
		try
		{
			if (sizeInByte < 1024)
				return String.format("%1sB", sizeInByte);
			else if (sizeInByte < 1024 * 1024)
				return String.format("%1sKB", floatFormat(sizeInByte * 1.0 / 1024,2));
			else if (sizeInByte < 1024 * 1024 * 1024)
				return String.format("%1sMB", floatFormat(sizeInByte * 1.0 / 1024 / 1024,2));
			else
				return String.format("%1sGB", floatFormat(sizeInByte * 1.0 / 1024 / 1024 / 1024,2));
		}
		catch (Exception e)
		{
			return "-";
		}
	}
	
	/**
	 * 功能:文件字节数转换成适配的单位 
	 * 参数: sizeInByte: 速率(Byte/s) 
	 * 返回值: 
	 * 说明:返回单位bps,由原来的1024进制换成1000进制(20141225)
	 */
	public static String getSpeedString(double dSpeedBps)
	{
		if(dSpeedBps == Double.NaN || dSpeedBps < 0)
			return "- ";
		
		dSpeedBps = dSpeedBps * 8.0;	//转换成bps
		if (dSpeedBps < 1000)
			return String.format("%1sbps", floatFormat(dSpeedBps,2));
		else if (dSpeedBps < 1000 * 1000)
			return String.format("%1sKbps", floatFormat(dSpeedBps * 1.0 / 1000,2));
		else if (dSpeedBps < 1000 * 1000 * 1000)
			return String.format("%1sMbps", floatFormat(dSpeedBps * 1.0 / 1000 / 1000,2));
		else
			return String.format("%1sGbps", floatFormat(dSpeedBps * 1.0 / 1000 / 1000 / 1000,2));
	}
	
	/**
	 * 功能:文件字节数转换成适配的单位 
	 * 参数: sizeInByte: 速率(Byte/s) 
	 * 返回值: 
	 * 说明:返回单位Kbps,由原来的1024进制换成1000进制(20141225)
	 */
	public static String getSpeedKbps(double dSpeedBps)
	{
		if(dSpeedBps == Double.NaN || dSpeedBps < 0)
			return "- ";
		
		dSpeedBps = dSpeedBps * 8.0;	//转换成bps
		return String.format("%1s", floatFormat(dSpeedBps * 1.0 / 1000,2));
	}
	
	/**
	 * 功能:文件字节数转换成适配的单位 
	 * 参数: sizeInByte: 速率(Byte/s) 
	 * 返回值: 
	 * 说明:返回单位Mbps,由原来的1024进制换成1000进制(20141225)
	 */
	public static double getSpeedMbps(double dSpeedBps)
	{
		if(dSpeedBps == Double.NaN || dSpeedBps < 0)
			return -1;
		
		dSpeedBps = dSpeedBps * 8.0;	//转换成bps
		return floatFormat(dSpeedBps * 1.0 / 1000 / 1000,2);
	}
	
	/**
	 * 功能: 获取User-ID
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static int getUID(Context context)
	{
		int nRet = 0;
		try
		{
			PackageInfo pakInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			nRet = pakInfo.applicationInfo.uid;
		}
		catch (NameNotFoundException e)
		{
		}
		return nRet;
	}
	
	/**
	 * 功能: 睡眠
	 * 参数: nMs: 睡眠时间(ms)
	 * 返回值:
	 * 说明:
	 */
	public static void Sleep(int nMs)
	{
		try
		{
			Thread.sleep(nMs);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	//------------------------------------流量统计--------------------------------------------------
	/**
	 * 功能: 获取当前接收流量
	 * 参数:	nUID: 
	 * 返回值:
	 * 说明:
	 */
	public static long getCurTrafficRx(Context context)
	{
		int nUID = getUID(context);	//进程ID
		
		long lRet = 0;
		if (m_nSizeType == -1) //第一次获取
		{
			if (nUID > 0) //UID有效
			{
				lRet = getRxTrafficFromFile(nUID); //兼顾低版本,优先从文件获取
				if (lRet <= 0)
					lRet = TrafficStats.getUidRxBytes(nUID);
				
				m_nSizeType = 0;
			}
			else //UID无效则使用总流量替代
			{
				lRet = TrafficStats.getTotalRxBytes();
				m_nSizeType = 1;
			}
		}
		else //根据上一次获取方法
		{
			switch (m_nSizeType)
			{
			case 0: //进程流量
				lRet = getRxTrafficFromFile(nUID); //兼顾低版本,优先从文件获取
				if (lRet <= 0)
					lRet = TrafficStats.getUidRxBytes(nUID);
				break;
			case 1:	//系统流量
				lRet = TrafficStats.getTotalRxBytes();
				break;
			default:
				break;
			}
		}

		return lRet;
	}

	/**
	 * 功能: 获取当前接收流量
	 * 参数:	nUID: 
	 * 返回值:
	 * 说明:
	 */
	public static long getCurTrafficRx(int nUID)
	{
		long lRet = 0;
		if(nUID > 0)	//UID有效
		{
			lRet = getRxTrafficFromFile(nUID);	//兼顾低版本,优先从文件获取
			
			if(lRet <= 0)
				lRet = TrafficStats.getUidRxBytes(nUID);
		}
		else //UID无效则使用总流量替代
			lRet = TrafficStats.getTotalRxBytes();
		
		return lRet;
	}
	
	/**
	 * 功能:	获取接收的字节数
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static long getRxTrafficFromFile(int uid)
	{
		String strPath = String.format("/proc/uid_stat/%s/tcp_rcv", uid);
		StringBuilder sbRet = new StringBuilder();
		long lRet = 0;
		try
		{
			FileReader reader = new FileReader(strPath);
			BufferedReader in = new BufferedReader(reader, 8 * 1024);

			String strLine = "";
			try
			{
				while ((strLine = in.readLine()) != null)
				{
					sbRet.append(strLine);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			lRet = Long.parseLong(sbRet.toString());
		}
		catch (Exception e)
		{
		}
		
		return lRet;
	}
	
	/**
	 * 功能:低版本获取流量信息
	 * 参数: context: 
	 * 返回值:
	 * 说明:
	 */
	private static final String CACHE_DATABASE_FILE = "webviewCache.db";
	public static long getCacheTotalSize(Context context)
	{
		SQLiteDatabase mCacheDatabase = null;
		try
		{
			mCacheDatabase = context.openOrCreateDatabase(CACHE_DATABASE_FILE, 0, null);
		}
		catch (SQLiteException e)
		{
			// try again by deleting the old db and create a new one
			if (context.deleteDatabase(CACHE_DATABASE_FILE))
			{
				mCacheDatabase = context.openOrCreateDatabase(CACHE_DATABASE_FILE, 0, null);
			}
		}

		long size = 0;
		if (mCacheDatabase != null)
		{
			try
			{
				Cursor cursor = null;
				final String query = "SELECT SUM(contentlength) as sum FROM cache";
				try
				{
					cursor = mCacheDatabase.rawQuery(query, null);
					if (cursor.moveToFirst())
						size = cursor.getLong(0);
				}
				catch (IllegalStateException e)
				{
					Log.e("", "getCacheTotalSize", e);
				}
				finally
				{
					if (cursor != null)
						cursor.close();
					
					if (mCacheDatabase != null)
						mCacheDatabase.close();
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return size;
	}
	//-------------------------------------------------------------------------------------------
	
	/**
	 * 功能:	服务是否正在运行
	 * 参数:	context:
	 * 		className: 服务类名
	 * 返回值:
	 * 说明:
	 */
	public static boolean IsServiceRunning(Context context, String className)
	{
		boolean bRunning = false;

		ActivityManager activityMag = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceInfo = activityMag.getRunningServices(100);
		String strName;
		for (RunningServiceInfo service : serviceInfo)
		{
			strName = service.service.getClassName();
			//Log.i(TAG, strName);
			if (strName.equals(className))
			{
				bRunning = true;
				break;
			}
		}

		return bRunning;
	}
	
	/**
	 * 功能: 获取毫秒对应的时间字符串
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static String getTimeString(long timeSpan)
	{
		long lHour = timeSpan / (1000 * 60 * 60);
		long lMin = (timeSpan - timeSpan / (1000 * 60 * 60) * (1000 * 60 * 60)) / (1000 * 60);
		long lSec = (timeSpan - timeSpan / (1000 * 60 * 60) * (1000 * 60 * 60) - timeSpan / (1000 * 60) * (1000 * 60)) / 1000;

		StringBuilder sb = new StringBuilder();
		if (lHour > 0)
			sb.append(String.format("%1s小时", lHour));
		
		if (lMin > 0)
			sb.append(String.format("%1s分钟", lMin));
		
		if (lSec > 0)
			sb.append(String.format("%1s秒", lSec));
		
		return sb.toString();
	}
	
	/**
	 * 功能: 格式化时间为MM:SS
	 * 参数: lTime: ms
	 * 返回值:
	 * 说明:
	 */
	public static String getMMSS(long lTime)
	{
		lTime = lTime / 1000; //s
		long lSecond = lTime % 60;
		long lMin = lTime / 60;
		
		StringBuffer strBuf = new StringBuffer();
		strBuf.append(lMin > 9 ? (lMin) : ("0" + lMin));
		strBuf.append(":");
		strBuf.append(lSecond > 9 ? (lSecond) : ("0" + lSecond));
		return strBuf.toString();
	}
	
	/**
	 * 功能: 时间戳转换成标准时间格式
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static String TimestampToDate(long lTime)
	{
		try
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
			return df.format(new Date(lTime));
		}
		catch (Exception e)
		{
			return "";
		}
	}
	
	/**
	 * 功能:格式化浮点数,保留N文有效数
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static double floatFormat(double dVal,int nDeci)
	{
		try
		{
			BigDecimal bigDec = new BigDecimal(dVal);
			return bigDec.setScale(nDeci, BigDecimal.ROUND_HALF_UP).doubleValue();
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	/**
	 * 功能:格式化浮点数,保留N文有效数,返回字符串结果
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static String floatFormatString(double dVal,int nDeci)
	{
		BigDecimal bigDec = new BigDecimal(dVal);
		
		//String strFormat = "%" + nDeci + "f";
		String strFormat = bigDec.setScale(nDeci, BigDecimal.ROUND_HALF_UP).toString();
		return strFormat;
		//return String.format(strFormat, bigDec.setScale(nDeci, BigDecimal.ROUND_HALF_UP).doubleValue());
	}
		
	/**
	 * 功能: 判断当前手机是否有root权限
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static boolean getRootState()
	{
		boolean bRoot = false;
		try
		{
			if ((!new File("/system/bin/su").exists()) && (!new File("/system/xbin/su").exists()))
				bRoot = false;
			else
				bRoot = true;
		}
		catch (Exception e)
		{
		}
		return bRoot;
	}
	
	/**
	 * 功能: 设置手机飞行模式
	 * 参数:	context:
	 * 		enabling true:设置为飞行模式	false:取消飞行模式
	 * 返回值:
	 * 说明:
	 */
	public static void setAirplaneModeOn(Context context, boolean enabling)
	{
		Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enabling ? 1 : 0);
		Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		intent.putExtra("state", enabling);
		context.sendBroadcast(intent);
	}

	/**
	 * 判断手机是否是飞行模式
	 * @param context
	 * @return
	 */
	public static boolean getAirplaneMode(Context context)
	{
		int isAirplaneMode = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
		return (isAirplaneMode == 1) ? true : false;
	}
	
	/**
	 * 功能:设置百度000定位参数---定位模式，定位的坐标系，定位时间间隔，是否反地理编码（只在网络连接中有用） 参数: 返回值:
	 * 说明:定位模式三种：LocationMode
	 * .Hight_Accuracy,LocationMode.Battery_Saving,LocationMode .Device_Sensors
	 * 定位坐标系三种：tempcoor="gcj02",tempcoor="bd09ll",tempcoor="bd09"
	 */
	public static LocationClient setLocationOption(LocationClient locClient )
	{
		if(locClient == null)
			return null;
		
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationMode.Hight_Accuracy); // 设置定位模式
		option.setCoorType("bd09ll"); 	// 返回的定位结果是百度经纬度，默认值gcj02
		option.setScanSpan(2000); 		// 设置发起定位请求的间隔时间为1000ms
		option.setOpenGps(true);		//打开GPS
		option.setIsNeedAddress(true); // 反地理编码选择上

		locClient.setLocOption(option);
		return locClient;
	}
	
	//毫秒有效字符串
	public static String getMsString(int millSec)
	{
		if(millSec == Integer.MAX_VALUE || millSec < 0)
			return "- ";
		
		return floatFormatString(millSec,0);
	}
	
	//毫秒有效字符串
	public static String getMsString(long millSec)
	{
		if(millSec == Integer.MAX_VALUE || millSec < 0)
			return "- ";
		
		return floatFormatString(millSec,0);
	}
	
	//毫秒秒转换成秒
	public static String getSecondString(int millSec)
	{
		if(millSec == Integer.MAX_VALUE || millSec < 0)
			return "- ";
		
		return floatFormatString(millSec / 1000.0,3);
	}

	public static String getSecondString(double millSec)
	{
		if(millSec == Integer.MAX_VALUE || millSec < 0)
			return "- ";
		
		return floatFormatString(millSec / 1000,3);
	}
	
	
	//------------------------------------------------------------------
	
	//-------------各类型数值的有效值--------------------------------------
	//说明:统一对比Integer的最大最小值,以-1作为无效值
	public static long getInvalidVal(long nVal)
	{
		if (nVal == Integer.MAX_VALUE || nVal == Integer.MIN_VALUE)
			return -1;
		
		return nVal;
	}

	public static int getInvalidVal(int nVal)
	{
		if (nVal == Integer.MAX_VALUE || nVal == Integer.MIN_VALUE)
			return -1;
		
		return nVal;
	}

	public static double getInvalidVal(double nVal)
	{
		if (nVal == Integer.MAX_VALUE || nVal == Integer.MIN_VALUE)
			return -1;
		
		return nVal;
	}
	
	//----------------------------------------------------------------------
	/**
	 * 功能: 获取本地系统配置
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static SettingPar getSettingPar(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(LocalCache.SP_Setting, Context.MODE_PRIVATE);
		String strJson = LocalCache.getValueFromSharePreference(sp, SettingPar.class.getSimpleName());
		
		SettingPar settingPar = null;
		if(strJson.length() > 0)
			settingPar = new SettingPar(strJson);
		
		return settingPar;
	}
	
	/**
	 * 功能:分享
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public static void ShareApp(Context context)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "感知测试");
			String strUrl = "http://14.146.229.118:2045/apk/CT-Detection.apk";
			intent.putExtra(Intent.EXTRA_TEXT, "用珠海纬地-感知测试，随时了解用户网络体验，下载地址：" + strUrl);
			context.startActivity(Intent.createChooser(intent, "怎样分享感知测试？"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
