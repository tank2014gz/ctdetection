package com.wellcell.inet.Task;

import android.content.Context;
import android.widget.Toast;

import com.wellcell.MainFrag.SettingFragment.SettingPar;
import com.wellcell.inet.Common.CGlobal;
import com.wellcell.inet.Task.AbsTask.TaskType;
import com.wellcell.inet.Task.TaskPar.FtpPar;
import com.wellcell.inet.Task.TaskPar.PingPar;
import com.wellcell.inet.Task.TaskPar.VideoPar;
import com.wellcell.inet.Task.TaskPar.WebPar;
import com.wellcell.inet.Task.Ftp.FtpTestTask;
import com.wellcell.inet.Task.Ping.PingTestTask;
import com.wellcell.inet.Task.Video.VideoTask;
import com.wellcell.inet.Task.Web.WebObject;
import com.wellcell.inet.Task.Web.WebTestTaskEx;
import com.wellcell.inet.TaskList.TaskInfo;

//生成业务测试线程
public class BuildTaskThread implements Runnable
{
	private Context m_context;
	
	private TaskInfo m_taskInfo;
	private String m_strJsonPar;		//配置参数
	private boolean m_bAutoTest = true;	//是否是自动测试
	private boolean m_bAnteTest = true;	//是否天馈测量
	private boolean m_bBtsInfo = true;	//是否获取基站详细信息

	public AbsTask m_task;
	public WebObject m_webObj = null;
	public boolean m_bFinish = false;	//创建线程完毕

	/**
	 * 功能: 创建任务
	 * 参数: taskID: 业务ID
	 * 		taskInfo: 业务信息
	 * 		jsonPar: 配置参数
	 * 		hotName: 热点名称
	 * 		bAuto: 是否自动测试
	 * 返回值:
	 * 说明:
	 */
	public BuildTaskThread(Context context, TaskInfo taskInfo, String jsonPar,
			boolean bAuto,boolean bAnteTest,boolean bBtsInfo)
	{
		this.m_taskInfo = taskInfo;
		this.m_strJsonPar = jsonPar;
		this.m_context = context;
		this.m_bAutoTest = bAuto;
		this.m_bAnteTest = bAnteTest;
		this.m_bBtsInfo = bBtsInfo;
	}

	@Override
	public void run()
	{
		TaskType taskType = TaskType.valueOf(m_taskInfo.m_strTaskType);
		try//解析配置参数并创建对应的任务
		{
			switch (taskType)
			{
			case PING:
				PingPar pingPar = new PingPar(m_strJsonPar);
				m_task = new PingTestTask(m_context,pingPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				break;
			case WEBSITE:	//网页
				WebPar webPar = new WebPar(m_strJsonPar);
				//m_task = new WebTestTask(m_context, webPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest);
				m_task = new WebTestTaskEx(m_context, webPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				//m_webPar = new WebPar(i);
				m_webObj = new WebObject(m_context, webPar, null, 4);
				break;
			/*case IM:
				WeiboPar weiboPar = new WeiboPar(m_strJsonPar);
				m_task = new SinaWeiboTask(m_context, WeiboAuthHelper.readAccessToken(m_context), 
						weiboPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				break;
			case TencentWeibo:
				weiboPar = new WeiboPar(m_strJsonPar);
				m_task = new TencentWeiboTask(m_context,weiboPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				break;*/
			case FTP:
				FtpPar ftpPar = new FtpPar(m_strJsonPar);
				//获取本地配置
				SettingPar settingPar = CGlobal.getSettingPar(m_context);
				if(settingPar != null)
					ftpPar.m_nThreadNum = settingPar.m_nThreadCount;
				
				m_task = new FtpTestTask(m_context,ftpPar, m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				break;
			/*case DIAL:	//语音
				DialPar dialPar = new DialPar(m_strJsonPar);
				m_task = new DialTestTask(m_context, dialPar,m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo);
				break;*/
			case VIDEO:	//视频
				VideoPar videoPar = new VideoPar(m_strJsonPar);
				m_task = new VideoTask(m_context, videoPar,m_bAutoTest,m_taskInfo.m_strTaskID,m_bAnteTest,m_bBtsInfo,null);
				break;
			default:
				Toast.makeText(m_context, "不支持该任务类型", Toast.LENGTH_SHORT).show();
				break;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			m_bFinish = true;
		}
	}
}
