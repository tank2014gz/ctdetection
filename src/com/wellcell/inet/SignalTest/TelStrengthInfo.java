package com.wellcell.inet.SignalTest;


import java.lang.reflect.Field;
import java.util.List;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.wellcell.ctdetection.DetectionApp;
import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.DataProvider.PhoneDataProvider;
import com.wellcell.inet.Log.SignalLog;
import com.wellcell.inet.Web.WebUtil;

/*
 * 所有手机信号信息,包括基站信息,信号信息
 * 说明: 信号强度无效值为:Integer.MAX_VALUE
 */
public class TelStrengthInfo 
{
	//private static String TAG = "TelStrengthInfo";
	private static final int INVALID = Integer.MAX_VALUE;	//无效值
	
	private SignalStrengthPar m_SignalPar = new SignalStrengthPar();	//默认配置参数
	private SignalLog m_signalOrg = new SignalLog();					//保存原始值
	
	//反射LTE函数名
	//private static String LteRsrp = "getLteRsrp";
	//private static String LteRsrq = "getLteRsrq";
	//private static String LteRssnr = "getLteRssnr";
	//private static String LteCqi = "getLteCqi";
	//反射LTE成员变量名
	private static String LteRsrp = "rsrp";
	private static String LteRsrq = "rsrq";
	private static String LteRssi = "ltesignalstrength";
	private static String LteRssnr = "rssnr";
	private static String LteCqi = "cqi";
	//---------------------------------------------------------------
	//信令界面结果对象ID
	public static String LteCellObj = "45250";
	public static String LteSignalObj = "45459";
	public static String CdmaCellObj = "5383";
	public static String[] CdmaSignalObjs = {"4506","4507","4508","4509","4510","4511","4512","4513","4514","4515","4516","4517","4518"};
	public static String EvdoRxObj = "4201";	//Rx3g
	public static String EvdoEcioObj = "4709";	//Ecio3g
	public static String EvdoSnrObj = "4191";	//Snr
	public static String GsmCellObj = "";
	public static String GsmSignalObj = "";
	//---------------------------------------------------------------
	//private Context m_contextAct;		//Activity Context
	private Context m_contextApp;		//app context
	
	public int m_nSdkVer = 0;			//SDK版本号
	
	public int m_nCurNetWorkType = 0;	//当前网络类型
	public String m_strCurNetWorkType = "";;	//当前网络类型
	public String m_strSSID = "";		//wifi下的SSID
	//========================================
	//----------------LTE--------------------
	public boolean m_bLteChanged = false;	//lte基站是否更新
	
	public String m_strCityLte = "";		//地市
	public String m_strDevTypeLte = "";		//设备类型
	
	public String m_strCellIdLte = "";		//扇区ID-->服务器获取

	public String m_strBtsIDLte = "";		//基站ID
	public String m_strBtsNameLte = "";		//基站名

	public String m_strMcc = "";			//国家代码
	public String m_strMnc = "";			//网络号
	public String m_strCi = "";				//扇区混合信息
	public String m_strPci = "";			//Physical Cell Id 
	public String m_strTac = "";			//TAC
	public double m_dFreqLte = -1;			//频段号

	//---------------CDMA-----------------------------------
	public boolean m_bCdmaChanged = false;	//cdma基站是否更新
	
	public String m_strCityCdma = "";		//地市
	public String m_strDevTypeCdma = "";	//设备类型
	
	public String m_strCellIdCdma = "";		//扇区ID-->服务器获取

	public String m_strBtsIDCdma = "";		//基站ID
	public String m_strBtsNameCdma = "";	//基站名

	public String m_strBSCNo = "";			//BSC
	public String m_strSID = "";			//SID
	public String m_strNID = "";			//NID
	public String m_strCID = "";			//CID
	public String m_strPN = "";				//PN-->网络获取
	
	//-----------------GSM---------------------------------
	public String m_strCIDGsm = "";			//Cell id
	public String m_strLacGsm = "";			//local area code 区域码

	//===========基站经纬度=====================
	public double m_dLonLte = 0;	//经度
	public double m_dLatLte = 0;	//维度
	
	//CDMA
	public double m_dLonCdma = 0;	//经度
	public double m_dLatCdma = 0;	//维度

	//Gsm
	public double m_dLonGsm = 0;	//经度
	public double m_dLatGsm = 0;	//维度
	
	//终端
	public double m_dLonDev = 0;	//经度
	public double m_dLatDev = 0;	//维度

	//=======================================
	
	//===============信号强度====================
	//LTE
	public double m_dRssiLte = 0;	//RSSI
	public double m_dRsrp = 0;		//RSRP
	public double m_dRsrq = 0;		//RSRQ
	public double m_dSinrLte = 0;	//SINR
	public int m_nCqiLte = 0;		//CQI
	
	//1x
	public double m_dRx2G = 0;		//信号场强
	public double m_dEcio2G = 0;	//ECIO 载干比
	
	//do
	public double m_dRx3G = -120;	//信号场强
	public double m_dEcio3G = -120;	//ECIO 载干比
	public double m_dSnr3G = -120;	//SINR
	//GSM
	public double m_dRxGsm = -120;
	
	public TelStrengthInfo(Context context)
	{
		m_contextApp = context;
	}
	
	//设置信号配置参数
	public void setSignalPar(SignalStrengthPar signalPar)
	{
		if(signalPar != null)
			m_SignalPar = signalPar;
	}
	
	//初始化
	public void Init()
	{
		//lte
		m_bLteChanged = false;
		m_strCityLte = "";
		m_strDevTypeLte = "";
		m_strCellIdLte = "";
		m_strBtsIDLte = "";
		m_strBtsNameLte = "";
		m_strMcc = "";
		m_strMnc = "";
		m_strCi = "";
		m_strPci = "";
		m_strTac = "";
		m_dFreqLte = -1;
		//---------------CDMA-----------------------------------
		m_bCdmaChanged = false;
		m_strCityCdma = "";
		m_strDevTypeCdma = "";
		m_strCellIdCdma = "";
		m_strBtsIDCdma = "";
		m_strBtsNameCdma = "";
		m_strBSCNo = "";
		m_strSID = "";
		m_strNID = "";
		m_strCID = "";
		m_strPN = "";
		//-----------------GSM---------------------------------
		m_strCIDGsm = "";
		m_strLacGsm = "";
		//===========基站经纬度=====================
		m_dLonLte = 0;
		m_dLatLte = 0;
		//CDMA
		m_dLonCdma = 0;
		m_dLatCdma = 0;
		//Gsm
		m_dLonGsm = 0;
		m_dLatGsm = 0;
		//终端
		m_dLonDev = 0;
		m_dLatDev = 0;
		//===============信号强度====================
		//LTE
		m_dRssiLte = 0;
		m_dRsrp = 0;
		m_dRsrq = 0;
		m_dSinrLte = 0;
		m_nCqiLte = 0;
		//1x
		m_dRx2G = 0;
		m_dEcio2G = 0;
		//do
		m_dRx3G = -120;
		m_dEcio3G = -120;
		m_dSnr3G = -120;
		//GSM
		m_dRxGsm = -120;
	}
	
	/**
	 * 功能: 从另外一个对象提取小区信息
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public void getCellInfoFromObj(TelStrengthInfo obj)
	{
		if(obj == null)
			return;
		
		//---------------LTE---------------------------
		m_bLteChanged = true;
		m_strMnc = obj.m_strMnc;
		m_strMcc = obj.m_strMcc;
		m_strCi = obj.m_strCi;
		m_strPci = obj.m_strPci;
		m_strTac = obj.m_strTac;
		
		//m_strBtsNameLte = obj.m_strBtsNameLte;
		//---------------------------------------
		m_bCdmaChanged = true;
		m_strCID = obj.m_strCID;
		m_strSID = obj.m_strSID;
		m_strNID = obj.m_strNID;
		//m_strBtsNameCdma = obj.m_strBtsNameCdma;
		//-------------------------------------------
		m_strCIDGsm = obj.m_strCIDGsm;
		m_strLacGsm = obj.m_strLacGsm;
		
	}
	
	//-----------------------------------------------------------------------	
	/**
	 * 功能: 获取CDMA小区信息(低版本)
	 * 参数:location:
	 * 返回值:
	 * 说明:
	 */
	public void getCdmaCellInfo(CellLocation location)
	{
		CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) location;
		
		// 提取key ID
		String strCIDPre = m_strCID;	//保存改变前的值
		int nCID = cdmaCellLocation.getBaseStationId();
		m_signalOrg.m_strCIDOrg = nCID + "";	//保存原始值
		nCID = nCID * m_SignalPar.m_nCidPar;
		if(nCID > 0 && nCID != Integer.MAX_VALUE)
			m_strCID = nCID + ""; // CID
		
		String strSIDPre = m_strSID;
		int nSID = cdmaCellLocation.getSystemId();
		m_signalOrg.m_strSIDOrg = nSID + "";
		nSID = nSID * m_SignalPar.m_nSidPar;
		if(nSID > 0 && nSID != Integer.MAX_VALUE)
			m_strSID = nSID + ""; //SID
		
		String strNIDPre = m_strNID;
		int nNID = cdmaCellLocation.getNetworkId();
		m_signalOrg.m_strNIDOrg = nNID + "";
		nNID = nNID * m_SignalPar.m_nNidPar;
		if(nNID > 0 && nNID != Integer.MAX_VALUE)
			m_strNID = nNID + ""; // NID

		// 经纬度
		/*int nLon = cdmaCellLocation.getBaseStationLongitude();
		int nLat = cdmaCellLocation.getBaseStationLatitude();
		if (nLon != Integer.MAX_VALUE && nLat != Integer.MAX_VALUE)
		{
			m_dLonCdma = nLon / 14400.0;
			m_dLatCdma = nLat / 14400.0;
		}
		else
		{
			m_dLonCdma = nLon;
			m_dLatCdma = nLat;
		}*/

		//防止出现回调但小区没改变而重复获取小区信息
		if(m_strCID.equals(strCIDPre) == false || m_strSID.equals(strSIDPre) == false
				|| m_strNID.equals(strNIDPre) == false)
		{
			m_bCdmaChanged = true; // 更新标志
		}
	}
	
	/**
	 * 功能: 获取GSM小区信息(低版本)
	 * 参数:location: 
	 * 返回值:
	 * 说明:
	 */
	public void getGsmCellInfo(CellLocation location)
	{
		GsmCellLocation gsmCellLocation = (GsmCellLocation) location;

		int nCellId = gsmCellLocation.getCid();
		m_signalOrg.m_strCIGsmOrg = nCellId + "";
		nCellId = nCellId * m_SignalPar.m_nCellIDPar;
		if (nCellId > 0 && nCellId != Integer.MAX_VALUE)
			m_strCIDGsm = nCellId + ""; // cell id

		int nLac = gsmCellLocation.getLac();
		m_signalOrg.m_strLacGsmOrg = nLac + "";
		nLac = nLac * m_SignalPar.m_nLacPar;
		if (nLac > 0 && nLac != Integer.MAX_VALUE)
			m_strLacGsm = nLac + ""; // 区域吗
	}
	
	/**
	 * 功能: 主动获取当前连接的小区信息
	 * 参数:	context:
	 * 		nType: 网络类型 ;0:LTE,1:CDMA,2:GSM
	 * 返回值:小区信息数组(LTE: MNC,MCC,CI,PCI,LAC; CDMA: CID,SID,NID; GSM: CellID,Lac)
	 * 说明:
	 */
	@SuppressLint("NewApi")
	public void getCellInfoActive()
	{
		try
		{
			TelephonyManager telMag = (TelephonyManager) m_contextApp.getSystemService(Context.TELEPHONY_SERVICE);
			
			String strCellName;
			if(Build.VERSION.SDK_INT < 17 )
			{
				CellLocation curCellLoc = telMag.getCellLocation();
				if (curCellLoc != null)
				{
					strCellName = curCellLoc.getClass().getName();
					if (strCellName.equals(CdmaCellLocation.class.getName())) //CDMA
						getCdmaCellInfo(curCellLoc);
					else if (strCellName.equals(GsmCellLocation.class.getName())) //GSM
						getGsmCellInfo(curCellLoc);
				}
				else //无法获取则从全局监听获取
				{
					//if(m_contextApp != null)
					//	getCellInfoFromObj(((DetectionApp)m_contextApp).m_appTelStrenInfo);
				}
			}
			else
			{
				List<CellInfo> listCell = telMag.getAllCellInfo();
				if (listCell != null)
				{
					CellInfo curCell;
					for (int i = 0; i < listCell.size(); i++)
					{
						curCell = listCell.get(i);
						strCellName = curCell.getClass().getName();

						if (strCellName.equals(CellInfoLte.class.getName())) // lte
							getLteCellInfoEx(curCell); // 提取cell信息
						else if (strCellName.equals(CellInfoCdma.class.getName())) // cdma
							getCdmaCellInfoEx(curCell); // 更新基站信息
						else if (strCellName.equals(CellInfoGsm.class.getName())) // gsm
							getGsmCellInfoEx(curCell);
					}
				}
				else //无法获取则从全局监听获取
				{
					//if(m_contextApp != null)
					//	getCellInfoFromObj(((InetApplication)m_contextApp).m_appTelStrenInfo);
				}
			}		
		}
		catch (Exception e)
		{
		}
	}
	//----------------------------------------------------------------------
	/**
	 * 功能: 获取LTE小区信息(高版本)
	 * 参数: cellInfo: 
	 * 返回值:
	 * 说明:
	 */
	@SuppressLint("NewApi")
	public void getLteCellInfoEx(CellInfo cellInfo)
	{
		CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
		CellIdentityLte cellIdLte = cellInfoLte.getCellIdentity();
		
		//CellSignalStrengthLte signalLte = cellInfoLte.getCellSignalStrength();
		//Log.i(TAG, "LTE-->" + signalLte.toString());
		
		String strCiPre = m_strCi;	//保留改变前的值
		int nCI = cellIdLte.getCi();
		m_signalOrg.m_strCiOrg = nCI + "";
		nCI = nCI * m_SignalPar.m_nCiPar;
		if(nCI > 0 && nCI != Integer.MAX_VALUE)
		{
			m_strCi = nCI + ""; // CI
			m_dFreqLte = getFrequenceLte(m_strCi); //获取频段号
		}
		
		String strMccPre = m_strMcc;
		int nMcc = cellIdLte.getMcc();
		m_signalOrg.m_strMccOrg = nMcc + "";
		nMcc = nMcc * m_SignalPar.m_nMccPar;
		if( nMcc > 0 && nMcc != Integer.MAX_VALUE)
			m_strMcc = nMcc + ""; // mcc
		
		String strMncPre = m_strMnc;
		int nMnc = cellIdLte.getMnc();
		m_signalOrg.m_strMncOrg = nMcc + "";
		nMnc = nMnc * m_SignalPar.m_nMncPar;
		if( nMnc > 0 && nMnc != Integer.MAX_VALUE)
			m_strMnc = nMnc + ""; // mnc
		
		int nPci = cellIdLte.getPci();
		m_signalOrg.m_strPciOrg = nPci + "";
		nPci = nPci * m_SignalPar.m_nPciPar;
		if(nPci > 0 && nPci != Integer.MAX_VALUE)
			m_strPci = nPci + ""; // pci
		
		//Tac
		int nTac = cellIdLte.getTac();
		m_signalOrg.m_strTacOrg = nTac + "";
		nTac = nTac * m_SignalPar.m_nTacPar;
		if( nTac > 0 && nTac != Integer.MAX_VALUE)
			m_strTac = nTac + "";
		
		//防止出现回调但小区没改变而重复获取小区信息
		if(m_strCi.equals(strCiPre) == false || m_strMcc.equals(strMccPre) == false
				|| m_strMnc.equals(strMncPre) == false)
		{
			m_bLteChanged = true; // 更新标志
		}
	}
	
	/**
	 * 功能: 获取CDMA小区信息(高版本)
	 * 参数:cellInfo:
	 * 返回值:
	 * 说明:
	 */
	public void getCdmaCellInfoEx(CellInfo cellInfo)
	{
		CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
		CellIdentityCdma cellIdCdma = cellInfoCdma.getCellIdentity();
		
		//CellSignalStrengthCdma signalCdma = cellInfoCdma.getCellSignalStrength();
		//Log.i(TAG, "CDMA-->" + signalCdma.toString());
		
		// 提取key word
		String strCIDPre = m_strCID;	//保存改变前的值
		int nCID = cellIdCdma.getBasestationId();
		m_signalOrg.m_strCIDOrg = nCID + "";
		nCID = nCID * m_SignalPar.m_nCidPar;
		if( nCID > 0 && nCID != Integer.MAX_VALUE)
			m_strCID = nCID + ""; // CID
		
		String strSIDPre = m_strSID;
		int nSID = cellIdCdma.getSystemId();
		m_signalOrg.m_strSIDOrg = nSID + "";
		nSID = nSID * m_SignalPar.m_nSidPar;
		if( nSID > 0 && nSID != Integer.MAX_VALUE)
			m_strSID = nSID + ""; // SID
		
		String strNIDPre = m_strNID;
		int nNID = cellIdCdma.getNetworkId();
		m_signalOrg.m_strNIDOrg = nNID + "";
		nNID = nNID * m_SignalPar.m_nNidPar;
		if(nNID > 0 && nNID != Integer.MAX_VALUE)
			m_strNID = nNID + ""; // NID
		
		//经纬度
		/*int nLon = cellIdCdma.getLongitude();
		int nLat = cellIdCdma.getLatitude();
		if(nLon != Integer.MAX_VALUE && nLat != Integer.MAX_VALUE)
		{
			m_dLonCdma = nLon / 14400.0;
			m_dLatCdma = nLat / 14400.0;
		}
		else 
		{
			m_dLonCdma = nLon;
			m_dLatCdma = nLat;
		}
		*/
		//防止出现回调但小区没改变而重复获取小区信息
		if(m_strCID.equals(strCIDPre) == false || m_strSID.equals(strSIDPre) == false
				|| m_strNID.equals(strNIDPre) == false)
		{
			m_bCdmaChanged = true; // 更新标志
		}
	}
	
	/**
	 * 功能: 获取GSM小区信息(高版本)
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
	public void getGsmCellInfoEx(CellInfo cellInfo)
	{
		CellInfoGsm cellGsm = (CellInfoGsm)cellInfo;
		
		//CellSignalStrengthGsm signalGsm = cellGsm.getCellSignalStrength();
		//Log.i(TAG, "GSM-->" + signalGsm.toString());
		
		int nCellId = cellGsm.getCellIdentity().getCid();
		m_signalOrg.m_strCIGsmOrg = nCellId + "";
		nCellId = nCellId * m_SignalPar.m_nCellIDPar;
		if(nCellId > 0 && nCellId != Integer.MAX_VALUE)
			m_strCIDGsm = nCellId + ""; // cell id
		
		int nLac = cellGsm.getCellIdentity().getLac();
		m_signalOrg.m_strLacGsmOrg = nLac + "";
		nLac = nLac * m_SignalPar.m_nLacPar;
		if(nLac > 0 && nLac != Integer.MAX_VALUE)
			m_strLacGsm = nLac + "";
	}
	
	//------------------------------------------------------------------------------
	/**
	 * 功能: 获取所有网络信号信息
	 * 参数: signalStrength: 
	 * 		signalFormat: 数据格式
	 * 返回值:
	 * 说明:
	 */
	public void getSignalInfo(SignalStrength signalInfo,double dLon,double dLat)
	{
		int nVal= INVALID;
		// lte
		nVal = getLteSignalInfoRefectEx(signalInfo, LteRsrp);
		m_signalOrg.m_dRsrpOrg = nVal;
		if(nVal != INVALID)	//系统无效值判断
			m_dRsrp = nVal * m_SignalPar.m_dRsrpPar;
		else
			m_dRsrp = INVALID;

		//rsrq
		nVal = getLteSignalInfoRefectEx(signalInfo, LteRsrq);
		m_signalOrg.m_dRsrqOrg = nVal;
		if(nVal != INVALID)	//系统无效值判断
			m_dRsrq = nVal * m_SignalPar.m_dRsrqPar;
		else 
			m_dRsrq = INVALID;
		
		//RSSI
		m_signalOrg.m_dRssiLteOrg = getLteSignalInfoRefectEx(signalInfo, LteRssi);	//反射获取	
		if(m_dRsrp != INVALID && m_dRsrq != INVALID)
			m_dRssiLte = getLteRssi(m_dRsrp,m_dRsrq,m_SignalPar); 
		else
			m_dRssiLte = INVALID;
		
		//SINR
		nVal = getLteSignalInfoRefectEx(signalInfo, LteRssnr);
		m_signalOrg.m_dSinrLteOrg = nVal;
		if(nVal != INVALID)	//系统无效值判断
			m_dSinrLte = nVal * m_SignalPar.m_dSinrPar;
		else
			m_dSinrLte = INVALID;

		//CQI
		nVal = getLteSignalInfoRefectEx(signalInfo, LteCqi);
		m_signalOrg.m_nCqiLteOrg = nVal;
		if(nVal != INVALID)	//系统无效值判断
			m_nCqiLte = (int)( nVal * m_SignalPar.m_dCqiPar);
		else
			m_nCqiLte = INVALID;
	
		//Log.i(TAG, "RSRP:" + m_dRsrp + " RSRQ:" + m_dRsrq +" Rssi:" + m_dRssiLte + " SINR:" + m_dSinrLte + " CQI:" + m_nCqiLte );
		//--------------------------------------------------------------------
		// Rx3G
		nVal = signalInfo.getEvdoDbm();
		m_signalOrg.m_dRx3GOrg = nVal;
		if(nVal != -120) //系统无效值判断
			m_dRx3G = nVal * m_SignalPar.m_dRxPar3G;
		else
			m_dRx3G = INVALID;
		
		//Ecio3G
		nVal = signalInfo.getEvdoEcio();
		m_signalOrg.m_dEcio3GOrg = nVal;
		if(nVal != -1)	//系统无效值判断
			m_dEcio3G = nVal * m_SignalPar.m_dEcioPar3G;
		else
			m_dEcio3G = INVALID;
		
		//snr
		nVal = signalInfo.getEvdoSnr();
		m_signalOrg.m_dSnr3GOrg = nVal;
		if(nVal != -1)	//系统无效值判断
			m_dSnr3G = nVal * m_SignalPar.m_dSnrPar; //[0,8]
		else 
			m_dSnr3G = INVALID;

		// Rx2G
		nVal = signalInfo.getCdmaDbm();
		m_signalOrg.m_dRx2GOrg = nVal;
		if(nVal != -120)	//系统无效值判断
			m_dRx2G = nVal * m_SignalPar.m_dRxPar2G;
		else
			nVal = INVALID;
		
		//Ecio2G
		nVal = signalInfo.getCdmaEcio();
		m_signalOrg.m_dEcio2GOrg = nVal;
		if(nVal != 	-160)	//系统无效值判断
			m_dEcio2G = nVal * m_SignalPar.m_dEcioPar2G;
		else
			m_dEcio2G = INVALID;
		//Log.i(TAG,"Rx3g:" + m_dRx3G + " ECIO3g:" + m_dEcio3G + " Snr:" + m_dSnr3G + " Rx2g:" + m_dEcio2G + " ECIO2g:" + m_dEcio2G);
		//--------------------------------------------------------------
		// Gsm
		nVal = signalInfo.getGsmSignalStrength();
		m_signalOrg.m_dRxGsmOrg = nVal;
		if(nVal != 99)	//系统无效值判断
			m_dRxGsm = (int)((nVal * 2 -113) * m_SignalPar.m_dRxParGsm);
		else
			m_dRxGsm = INVALID;
		
		//终端经纬度
		m_dLonDev = dLon;
		m_dLatDev = dLat;
	}
	
	//获取当前网络类型-->小区信息变更时更新
	public void getNetWorkType()
	{
		m_nCurNetWorkType = PhoneDataProvider.getNetworkType(m_contextApp);	//当前网络类型
		m_strCurNetWorkType = PhoneDataProvider.getActNetworkName(m_contextApp);
	}
	
	/**
	 * 功能:获取所有网络的基站详细信息
	 * 参数:
	 * 返回值:
	 * 说明:
	 */
/*	public boolean updateAllBtsInfo()
	{
		BtsBasicInfo btsInfo = null;
		
		//获取LTE
		if (m_bLteChanged) //有更新
		{
			//有效才获取
			//if (IsValidID(m_strCi) == true && IsValidID(m_strMcc) == true && IsValidID(m_strMnc) == true)
			{
				btsInfo = WebUtil.updateBtsInfo(m_strCi, m_strMcc, m_strMnc,m_contextApp);
				m_bLteChanged = false;

				if (btsInfo != null)
				{
					m_strDevTypeLte = btsInfo.m_strDevType;
					m_strCityLte = btsInfo.m_strCityName;
					m_strBtsNameLte = btsInfo.m_strBtsName;
					m_strBtsIDLte = btsInfo.m_strBtsNo;
					m_strCellIdLte = btsInfo.m_strCellId;

					m_dLonLte = btsInfo.m_dLon;
					m_dLatLte = btsInfo.m_dLat;
				}
			}
		}
		
		//----------------------------------------------------------------------------------------------
		//获取CDMA
		if (m_bCdmaChanged)
		{
			//if (IsValidID(m_strCID) == true && IsValidID(m_strSID) == true && IsValidID(m_strNID) == true)
			{
				btsInfo = WebUtil.updateBtsInfo(m_strCID, m_strSID, m_strNID,m_contextApp);
				m_bCdmaChanged = false; //修改标志

				if (btsInfo != null)
				{
					m_strDevTypeCdma = btsInfo.m_strDevType;
					m_strCityCdma = btsInfo.m_strCityName;
					m_strBSCNo = btsInfo.m_strBscNo;
					m_strBtsNameCdma = btsInfo.m_strBtsName;
					m_strBtsIDCdma = btsInfo.m_strBtsNo;
					m_strCellIdCdma = btsInfo.m_strCellId;
					m_strPN = btsInfo.m_strPN;

					m_dLonCdma = btsInfo.m_dLon;
					m_dLatCdma = btsInfo.m_dLat;
				}
			}
		}
		return true;
	}
*/	//-----------------------------------------------------------------------------
	/**
	 * 功能: 通过反射成员变量获取LTE信号信息
	 * 参数:	signalInfo: 当前信号
	 * 		strFunName: 函数名
	 * 返回值:
	 * 说明:
	 */
	private int getLteSignalInfoRefectEx(SignalStrength signalInfo,String strParName)
	{
		Field[] fieldAll = SignalStrength.class.getDeclaredFields();	//提取所有成员变量
		Field curField;
		int nRet = Integer.MAX_VALUE;
		String strName;
		for (int i = 0; i < fieldAll.length; i++)
		{
			curField = fieldAll[i];
			strName = curField.getName().toLowerCase(); //小写变量名
			if (strName.contains(strParName))
			{
				curField.setAccessible(true);	//修改可读权限
				try
				{
					if (!(curField.get(signalInfo) instanceof Integer)) //返回值判断
						continue;

					nRet = ((Integer) curField.get(signalInfo));
					break;
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		return nRet;
	}
	//----------------------------解码获取小区信号信息---------------------------------
	
	//------------------------判断各网络信息是否有效-----------------------------------
	/**
	 * 功能: 判断指定的网络信息是否有效
	 * 参数:	nType: 网络类型,0:LTE, 1: EVDO, 2:1x,  3:GSM
	 * 返回值:
	 * 说明:
	 */
	public boolean IsValidInfo(int nType)
	{
		boolean bVidalSignal = IsValidSignal(nType);	//信号信息是否有效
		boolean bVidalBts = IsValidBts(nType);		//小区信息是否有效
		
		switch (nType)
		{
		case 0:
		case 1:
		case 2:
			if(!bVidalSignal && !bVidalBts)	//两个都无效
				return false;
			break;
		case 3: //GSM需要有小区信息才显示
			return bVidalBts;
		default:
			break;
		}
		return true;
	}

	/**
	 * 功能:判断指定网络的信号值是否有效
	 * 参数:	nType: 网络类型,0:LTE, 1: EVDO, 2:1x,  3:GSM
	 * 返回值:
	 * 说明:
	 */
	public boolean IsValidSignal(int nType)
	{
		boolean bRet = false;
		switch (nType)
		{
		case 0:	//LTE
			if(m_dRsrp == -1 && m_dRsrq == -1) //低版本的手机会出现rsrp和rsrq同时为-1的情况.
				return false;
			
			bRet = IsValidRSRP(m_dRsrp);
			break;
		case 1:	//do
			bRet = IsValidRx(m_dRx3G);
			break;
		case 2:	//1x
			bRet = IsValidRx(m_dRx2G);
			break;
		case 3:	//GSM
			bRet = IsValidRx(m_dRxGsm);
		default:
			break;
		}
		return bRet;
	}
	
	/**
	 * 功能:判断指定网络的小区值是否有效
	 * 参数:	nType: 网络类型,0:LTE, 1: EVDO, 2:1x,  3:GSM
	 * 返回值:
	 * 说明:
	 */
	public boolean IsValidBts(int nType)
	{
		boolean bRet = false;
		switch (nType)
		{
		case 0:	//LTE
			bRet = IsValidID(m_strCi);
			break;
		case 1: //do
		case 2:	//1x
			bRet = IsValidID(m_strCID);
			break;
		case 3:	//GSM
			bRet = IsValidID(m_strCIDGsm);
			break;
		default:
			break;
		}
		return bRet;
	}
	
	/**
	 * 功能:判断Rx Power是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:Rx3G和Rx2G的有效值判断;有效范围段为(-120,0);0,-1,-0.1为无效值
	 */
	public static boolean IsValidRx(double dVal)
	{
		if(dVal <= -120 || dVal >= 0)
			return false;
		
		return true;
	}
	
	/**
	 * 功能: 获取Rx Power有效字符串
	 * 参数:
	 * 返回值:
	 * 说明:无效的返回空字符串,保留两位小数点
	 */
	public static String getRxString(double dVal)
	{
		if(!IsValidRx(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断Ecio是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:Ecio3G和ecio2G的有效值判断;有效范围段为[-31.5,0];
	 */
	public static boolean IsValidEcio(double dVal)
	{
		if(dVal < -31.5 || dVal > 0)
			return false;
		
		return true;
	}
	
	//返回ecio有效字符串
	public static String getEcioString(double dVal)
	{
		if(!IsValidEcio(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断sinr是否有效
	 * 参数: dVal:
	 * 返回值:
	 * 说明:LTE的RSSNR
	 */
	public static boolean IsValidSinr(double dVal)
	{
		if(dVal < -50 || dVal > 50 )
			return false;
		
		return true;
	}
	
	//获取SINR有效字符串
	public static String getSinrString(double dVal)
	{
		if(!IsValidSinr(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断SNR是否有效
	 * 参数: dVal:
	 * 返回值:
	 * 说明:CDMA的SNR
	 */
	public static boolean IsValidSnr(double dVal)
	{
		if(dVal < -50 || dVal > 50 )
			return false;
		
		return true;
	}
	
	//获取SINR有效字符串
	public static String getSnrString(double dVal,double dRx)
	{
		//特殊情况
		if(!IsValidRx(dRx) && dVal == -1.0)
			return "";
		
		if(!IsValidSnr(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断RSRP是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:有效范围段为(-140,0);
	 */
	public static boolean IsValidRSRP(double dVal)
	{
		if(dVal <= -140 || dVal >= 0)
			return false;
		
		return true;
	}
	
	//获取RSRP有效字符串
	public static String getRsrpString(double dVal)
	{
		if(!IsValidRSRP(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断RSRQ是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:有效范围段为(-20,0);
	 */
	public static boolean IsValidRSRQ(double dVal)
	{
		if(dVal <= -20 || dVal >= 0)
			return false;
		
		return true;
	}
	
	//获取RSRQ有效字符串
	public static String getRsrqString(double dVal)
	{
		if(!IsValidRSRQ(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能: 判断Rssi是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:有效范围段为(-120,0);0,-1,-0.1为无效值
	 */
	public static boolean IsValidRssi(double dVal)
	{
		if(dVal <= -120 || dVal >= 0)
			return false;
		
		return true;
	}
	
	//获取RSSI有效字符串
	public static String getRssiString(double dVal)
	{
		if(!IsValidRssi(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能: 判断CQI是否有效
	 * 参数dVal:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:有效范围段为(-120,0);0,-1,-0.1为无效值
	 */
	public static boolean IsValidCqi(double dVal)
	{
		if(dVal <= -120 || dVal >= 0)
			return false;
		
		return true;
	}
	
	//获取RSSI有效字符串
	public static String getCqiString(double dVal)
	{
		if(!IsValidCqi(dVal))
			return "";
		
		return CGlobal.floatFormatString(dVal, 2);
	}
	
	/**
	 * 功能:判断ID是否有效
	 * 参数:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:基站ID,NID等
	 */
	public static boolean IsValidID(String strId)
	{
		if(strId.length() == 0)
			return false;
		
		int nId = -1; //初始无效
		try
		{
			nId = Integer.parseInt(strId);
		}
		catch (Exception e)
		{
			return false;
		}
		
		if(nId == Integer.MAX_VALUE || nId <= 0)
			return false;
		
		return true;
	}
		
	/**
	 * 功能:判断ID是否有效
	 * 参数:
	 * 返回值: true: 有效
	 * 		 false: 无效
	 * 说明:基站ID,NID等
	 */
	public static String getIDString(int nId)
	{
		if(nId == Integer.MAX_VALUE || nId <= 0)
			return "";
		
		return nId + "";
	}
	//==================================================================
	//获取LTE运营商
	public String getLteOperator()
	{
		String strOperator = "";
		if (!m_strMnc.isEmpty())
		{
			int nMnc = -1;
			try
			{
				nMnc = Integer.parseInt(m_strMnc);
			}
			catch (Exception e)
			{
				return "";
			}
			
			switch (nMnc)
			{
			case 0:
			case 2:
			case 7:
				strOperator = "中国移动";
				break;
			case 3:
			case 5:
			case 11:
				strOperator = "中国电信";
				break;
			case 1:
			case 6:
				strOperator = "中国联通";
			default:
				strOperator = "其他";
				break;
			}
		}
		return strOperator;
	}
	
	/**
	 * 功能: 获取LTE频段号
	 * 参数:nCi: CI
	 * 返回值:
	 * 说明:CI,28bit,第六个半字节为频段号
	 */
	private double getFrequenceLte(String strCi)
	{
		double dFreq = -1;
		if(!IsValidID(strCi))	//有效值判断
			return dFreq;
		
		int nCi = Integer.parseInt(strCi);
		nCi = nCi & 0XF0;
		nCi = nCi >> 4;
		switch (nCi)
		{
		case 0:	
			dFreq = 2.1;
			break;
		case 3:
			dFreq = 1.8;
			break;
		case 6:
			dFreq = 2.6;
			break;
		default:
			break;
		}
		return dFreq;
	}
	//==================================================================
	/**
	 * 功能: 获取LTE RSSI
	 * 参数:
	 * 返回值:
	 * 说明: 通过rsrp和rsrq换算
	 */
	private static double getLteRssi(double dRsrp,double dRsrq,SignalStrengthPar signalPar) 
	{
		//String[] strVals = signalStrength.toString().split(" ");
		//return Integer.parseInt(strVals[8]);
		double dRssi = Integer.MAX_VALUE;
		if(dRsrq <= 0)
			dRssi = 10 * Math.log10(100) + dRsrp - dRsrq;
		
		return CGlobal.floatFormat(dRssi,2) * signalPar.m_dRssiPar;
	}

	/**
	 * 功能: 获取LTE RSRP
	 * 参数: signalStrength: 信号信息
	 * 		signalPar: 信号参数
	 * 返回值:
	 * 说明:通过解析SignalStrength字符串获取
	 */
	public static int getLteRsrp(SignalStrength signalStrength,SignalStrengthPar signalPar)
	{
		String[] strVals = signalStrength.toString().split(" ");
		
		int nRsrp = Integer.MAX_VALUE;
		if(strVals.length < 10)	//有效性判断
			return nRsrp;
		
		try
		{
			nRsrp = Integer.parseInt(strVals[9]);
		}
		catch (Exception e)
		{
		}
		
		return (int)(nRsrp * signalPar.m_dRsrpPar);
	}

	/**
	 * 功能: 获取LTE RSRQ
	 * 参数: signalStrength: 信号信息
	 * 		signalPar: 信号参数
	 * 返回值:
	 * 说明:通过解析SignalStrength字符串获取
	 */	
	private static int getLteRsrq(SignalStrength signalStrength,SignalStrengthPar signalPar) 
	{
		String[] strVals = signalStrength.toString().split(" ");
		
		int nRsrq = Integer.MAX_VALUE;
		if(strVals.length < 11)	//有效性判断s
			return nRsrq;
		try
		{
			nRsrq = Integer.parseInt(strVals[10]);
		}
		catch (Exception e)
		{
		}
		
		return (int)(nRsrq * signalPar.m_dRsrqPar);
	}
	
	/**
	 * 功能: 获取LTE SINR
	 * 参数: signalStrength: 信号信息
	 * 		signalPar: 信号参数
	 * 返回值:
	 * 说明:通过解析SignalStrength字符串获取
	 */
	public static double getRssnr(SignalStrength signalStrength,SignalStrengthPar signalPar)
	{
		String[] strVals = signalStrength.toString().split(" ");
			
		double dSinr = Integer.MAX_VALUE;
		if(strVals.length < 12)	//有效性判断
			return dSinr;
		
		try
		{
			dSinr = Integer.parseInt(strVals[11]);
		}
		catch (Exception e)
		{
		}
		return CGlobal.floatFormat(dSinr,2) * signalPar.m_dSinrPar;
	}

	/**
	 * 功能: 获取LTE CQI
	 * 参数: signalStrength: 信号信息
	 * 		signalPar: 信号参数
	 * 返回值:
	 * 说明:通过解析SignalStrength字符串获取
	 */
	private static int getLteCqi(SignalStrength signalStrength,SignalStrengthPar signalPar)
	{
		String[] strVals = signalStrength.toString().split(" ");
		int nCqi = Integer.MAX_VALUE;
		
		//有效性判断
		if(strVals.length < 13)
			return nCqi;
		
		try
		{
			nCqi = Integer.parseInt(strVals[12]);
		}
		catch (Exception e)
		{
		}
		
		return (int)(nCqi * signalPar.m_dCqiPar);
	}
	
	//========================================================================
	
	/**
	 * 功能:获取LTE信号强度等级
	 * 参数: dSignal: 信号强度
	 * 返回值:等级
	 * 说明:
	 */
	public static int getSignalGradeLTE(double dSignal)
	{
		int nGrade = 0;
		if (dSignal >= -80) // green
			nGrade = 1;
		else if (dSignal >= -95 && dSignal < -80) // blue
			nGrade = 2;
		else if (dSignal >= -105 && dSignal < -95) // yellow
			nGrade = 3;
		else if (dSignal >= -115 && dSignal < -105) // red
			nGrade = 4;
		else if (dSignal > -150 && dSignal < -115) // black
			nGrade = 5;
		else// null
			nGrade = 0;
		
		return nGrade;
	}
	
	/**
	 * 功能:获取CDMA信号强度等级
	 * 参数: dSignal: 信号强度
	 * 返回值:等级
	 * 说明:
	 */
	public static int getSignalGradeCDMA(double dSignal)
	{
		int nGrade = 0;
		if (dSignal >= -65) // green
			nGrade = 1;
		else if (dSignal >= -80 && dSignal < -65) // blue
			nGrade = 2;
		else if (dSignal >= -95 && dSignal < -80) // yellow
			nGrade = 3;
		else if (dSignal >= -105 && dSignal < -95) // red
			nGrade = 4;
		else if (dSignal > -115 && dSignal < -105) // black
			nGrade = 5;
		else// null
			nGrade = 0;
		
		return nGrade;
	}
	
	//返回当前连接基站信息
/*	public BtsBasicInfo getCurBtsInfo()
	{
		BtsBasicInfo btsInfo = null;
		switch (m_nCurNetWorkType) //当前连接网络类型
		{
		case TelephonyManager.NETWORK_TYPE_1xRTT:
		case TelephonyManager.NETWORK_TYPE_CDMA:
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
		case TelephonyManager.NETWORK_TYPE_EVDO_B: //CDMA
			btsInfo = getBtsCDMA();
			break;
		case TelephonyManager.NETWORK_TYPE_LTE:  //LTE
			btsInfo = getBtsLte();
			break;
		default:
			break;
		}
		return btsInfo;
	}
	
	//获取LTE小区信息
	public BtsBasicInfo getBtsLte()
	{
		BtsBasicInfo btsInfo = new BtsBasicInfo();
		
		btsInfo.m_strDevType = m_strDevTypeLte;
		btsInfo.m_strCityName = m_strCityLte;
		btsInfo.m_strBscNo = m_strBSCNo;
		btsInfo.m_strBtsName = m_strBtsNameLte;
		btsInfo.m_strBtsNo = m_strBtsIDLte;
		btsInfo.m_strSID = m_strMcc;
		btsInfo.m_strNID = m_strMnc;
		btsInfo.m_strCID = m_strCi;
		btsInfo.m_strCellId = m_strCellIdLte;
		btsInfo.m_dLon = m_dLonLte;
		btsInfo.m_dLat = m_dLatLte;
		
		return btsInfo;
	}
	
	//获取CDMA小区信息
	public BtsBasicInfo getBtsCDMA()
	{
		BtsBasicInfo btsInfo = new BtsBasicInfo();
		
		btsInfo.m_strDevType = m_strDevTypeCdma;
		btsInfo.m_strCityName = m_strCityCdma;
		btsInfo.m_strBscNo = m_strBSCNo;
		btsInfo.m_strBtsName = m_strBtsNameCdma;
		btsInfo.m_strBtsNo = m_strBtsIDCdma;
		btsInfo.m_strSID = m_strSID;
		btsInfo.m_strNID = m_strNID;
		btsInfo.m_strCID = m_strCID;
		btsInfo.m_strCellId = m_strCellIdCdma;
		btsInfo.m_strPN = m_strPN;
		btsInfo.m_dLon = m_dLonCdma;
		btsInfo.m_dLat = m_dLatCdma;
		
		return btsInfo;
	}
	*/
	/**
	 * 功能: 获取当前连接小区的关键信息
	 * 参数:
	 * 返回值:
	 * 说明:	CDMA: SID_NID_CID
	 * 		LTE: MCC_MNC_CI
	 */
/*	public String getCurCellKey()
	{
		String strRet = "";
		BtsBasicInfo info = getCurBtsInfo();
		if(info != null)
			strRet = info.m_strSID + "_" + info.m_strNID + "_" + info.m_strCID;
		
		return strRet;
	}
	*/
	/**
	 * 功能: 获取CDMA吸取的关键信息
	 * 参数:
	 * 返回值:SID_NID_CID
	 * 说明: 替换LTE的小区信息,如翼热点选择所需
	 */
/*	public String getCurCellKeyCDMA()
	{
		String strRet = "";
		BtsBasicInfo info = getBtsCDMA();
		if(info != null)
			strRet = info.m_strSID + "_" + info.m_strNID + "_" + info.m_strCID;
		
		return strRet;	
	}
*/	
	//判断经纬度是否有效
	private boolean IsValidLonLat()
	{
		if(m_dLonDev != 0 && m_dLatDev != 0)
			return true;
		else 
			return false;
	}
	
	/**
	 * 功能:	更新经纬度
	 * 参数:
	 * 返回值:
	 * 说明: 及时更新经纬度信息,防止经纬度更新了,而已有信号集合的经纬度没更新
	 */
	public void UpdateLocInfo(double dLon,double dLat)
	{
		if(!IsValidLonLat())
		{
			m_dLonDev = dLon;
			m_dLatDev = dLat;
		}
	}
	//-------------------------------------------------------

	/**
	 * 功能: 获取小区信号原始值
	 * 参数: nSrc: 数据来源;0:本机获取,1:解码
	 * 返回值:
	 * 说明:
	 */
	public void setSignalLog(int nSrc)
	{
		if(m_SignalPar.m_nErrorRep > 0)	//汇报控制
			SignalLog.addSignalLog(m_signalOrg, nSrc);	//保存
	}
	
	//上传信号日志
	public void uploadSignalLog()
	{
		if(m_SignalPar.m_nErrorRep == 0)
			return;

		//开线程上传
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				SignalLog.uploadSignalLog();	//上传信号日志
			}
		}).start();
	}
	
	//===================================================
	//KQI数据
	public JSONArray getJsonVal(JSONArray jsonArr)
	{
		if(jsonArr == null)
			jsonArr = new JSONArray();
		
		//当前网络
		jsonArr.put(m_strCurNetWorkType);
		jsonArr.put(m_strSSID);
		
		//CDMA
		jsonArr.put(m_strSID);
		jsonArr.put(m_strNID);
		jsonArr.put(m_strCID);
		/*if(IsValidInfo(1)) //do
			jsonArr.put(getRxString(m_dRx3G));
		else
			jsonArr.put("");*/
		
		if(IsValidInfo(2)) //1x
			jsonArr.put(TelStrengthInfo.getRxString(m_dRx2G));
		else
			jsonArr.put("");
		
		//-----------------------------------
		//LTE
		jsonArr.put(m_strCi);
		jsonArr.put(m_strPci);
		jsonArr.put(m_strTac);
		
		if(IsValidInfo(0))
		{
			jsonArr.put(getRsrpString(m_dRsrp));
			jsonArr.put(getSinrString(m_dSinrLte));
		}
		else	//无效
		{
			jsonArr.put("");
			jsonArr.put("");
		}
		
		return jsonArr;
	}
}

/**
 * 功能: 通过反射函数获取LTE信号信息
 * 参数:	signalInfo: 当前信号
 * 		strFunName: 函数名
 * 返回值:
 * 说明:
 */
/*	private int getLteSignalInfoRefect(SignalStrength signalInfo,String strFunName)
{
	Method[] arrMethods = SignalStrength.class.getMethods();	//提取所有函数
	Field[] fields = SignalStrength.class.getDeclaredFields();
	Field f1;
	try
	{
		f1 = SignalStrength.class.getField("mLteRssnr");
	}
	catch (NoSuchFieldException e1)
	{
		e1.printStackTrace();
	}
	Method curMethod;
	int nRet = Integer.MAX_VALUE;
	String strMethodName;
	for (int i = 0; i < arrMethods.length; i++)
	{
		curMethod = arrMethods[i];
		strMethodName = curMethod.getName(); //函数名
		if (strMethodName.equals(strFunName))
		{
			curMethod.getParameterTypes();
			try
			{
				if (!(curMethod.invoke(signalInfo, null) instanceof Integer)) //返回值判断
					continue;

				nRet = ((Integer) curMethod.invoke(signalInfo, null)).intValue();
				//Log.v("getLTEinfo", "methods:" + curMethod.getName() + ":" + nRet);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
			catch (InvocationTargetException e)
			{
				e.printStackTrace();
			}
		}
	}
	return nRet;
}
*/
