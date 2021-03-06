package com.wellcell.inet.Task;

import org.json.JSONArray;

import android.content.Context;

import com.wellcell.ctdetection.DetectionApp;
import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.DataProvider.PhoneDataProvider;
import com.wellcell.inet.DataProvider.PhoneDataProvider.CMDetail;
import com.wellcell.inet.SignalTest.TelStrengthInfo;
import com.wellcell.inet.entity.AddrInfo;

//临时业务以外信息封装
public class TempAidInfo
{
	private Context m_contextApp;
	
	public String m_strSubType = "-1";	//任务子类型,问题反馈有值,其他为-1-->转成运营商类型;0:电信,1:移动,2:联通
	public long m_lStartTime;			//开始时间
	public long m_lEndTime;				//结束时间
	
	public String m_strIMSI;			//手机IMSI号码
	public String m_strIMEI;			//手机设备国际识别码
	public String m_strModel;			//手机终端型号
	public String m_strOsVersion;		//OS版本
	public String m_strBaseBand;		//基带版本
	public String m_strKernel;			//内核版本
	public String m_strInnerVersion;		//内部版本
	
	public double m_dRamUsage;			//内存占用率
	public double m_dCpuUsage;			//CPU使用率
	
	private CMDetail m_cmInfoStart;	//开始CPU信息
	private CMDetail m_cmInfoEnd;		//结束CPU信息
	
	public int m_nUpLoadNetWork;		//上传数据使用网络
	//-----------------------------------------------
	public AddrInfo m_addInfo;		//位置信息
	
	public String m_strInnerIP;		//内部IP
	public String m_strOuterIP;		//外部IP

	public TelStrengthInfo m_telStrenInfo; 	//接入信号信息
	
	public TempAidInfo(Context contextApp)
	{
		m_contextApp = contextApp;
		m_telStrenInfo = new TelStrengthInfo(m_contextApp);
		
		m_strIMSI = PhoneDataProvider.getIMSI(m_contextApp);	//IMSI
		m_strIMEI = PhoneDataProvider.getIMEI(m_contextApp);	//IMEI
		m_strModel = PhoneDataProvider.getModel();			//型号
		m_strOsVersion = PhoneDataProvider.getSystemName();
		m_strBaseBand = PhoneDataProvider.getBaseBand();
		m_strKernel = PhoneDataProvider.getKernelVersion();
		m_strInnerVersion = PhoneDataProvider.getRomVersion();
		
		m_strInnerIP = PhoneDataProvider.getLocalIPAddress();
		m_strOuterIP = ((DetectionApp)m_contextApp).getWebIP();
	}
	
	//更新IP+无线信息
	public void updateStartInfo()
	{
		m_lStartTime = System.currentTimeMillis();
		
		m_addInfo = ((DetectionApp)m_contextApp).m_curAddrInfo;
		m_telStrenInfo = ((DetectionApp)m_contextApp).m_telStreInfo;	//暂时引用-->拷贝
		
		getCpuUsageInfo(0);	//开始cpu时间信息
		
		m_strOuterIP = ((DetectionApp)m_contextApp).getWebIP();
	}
	
	/**
	 * 功能: 获取CPU开始/结束时时间信息
	 * 参数:0: 开始;	1: 结束
	 * 返回值:
	 * 说明:
	 */
	public void getCpuUsageInfo(int nType)
	{
		if(nType == 0)
		{
			m_cmInfoStart = PhoneDataProvider.getCpuTimeInfo();
			m_cmInfoStart = PhoneDataProvider.getMemInfo(m_cmInfoStart);
		}
		else 
			m_cmInfoEnd = PhoneDataProvider.getCpuTimeInfo();
	}
	
	//获取测试过程CPU利用率
	private double getCpuUsage()
	{
		if(m_cmInfoStart != null)
			return m_cmInfoStart.getUseage(m_cmInfoEnd);
		return 0;
	}
	
	//组装Json部分字段
	public JSONArray getJsonVal(JSONArray jsonArr)
	{
		if(jsonArr == null)
			jsonArr = new JSONArray();
		
		//PhoneInfo
		jsonArr.put(m_strIMSI);
		jsonArr.put(m_strIMEI);
		jsonArr.put(m_strModel);
		jsonArr.put(m_strOsVersion);
		jsonArr.put(m_strBaseBand);
		jsonArr.put(m_strKernel);
		jsonArr.put(m_strInnerVersion);
		jsonArr.put(CGlobal.floatFormat(m_cmInfoStart.getMenUsage(), 2) + "");
		jsonArr.put(CGlobal.floatFormat(getCpuUsage(), 2) + "");
		
		//PositionInfo
		jsonArr.put(m_addInfo.m_dLon + "");
		jsonArr.put(m_addInfo.m_dLat + "");
		jsonArr.put(m_addInfo.toString());	//位置描述
		jsonArr.put(m_addInfo.m_strProv);
		jsonArr.put(m_addInfo.m_strCity);
		
		//NetInfo
		jsonArr = m_telStrenInfo.getJsonVal(jsonArr);
		jsonArr.put(m_strInnerIP);	//内部IP
		jsonArr.put(m_strOuterIP);	//外部IP
		
		return jsonArr;
	}

}
