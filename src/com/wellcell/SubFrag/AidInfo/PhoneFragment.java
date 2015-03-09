package com.wellcell.SubFrag.AidInfo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.achartengine.ChartFactory;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wellcell.ctdetection.R;
import com.wellcell.inet.DataProvider.PhoneDataProvider;

public class PhoneFragment extends Fragment implements OnStateListener
{
	private PropertyChecker mChecker;
	private TextView m_tvCpuUsage, textIdle, m_tvMemFree, m_tvMemUsed, textSDfree, textSDused;
	private ProgressBar progressCpu, progressMemory, progressSD;
	private FrameLayout cpuFrame;
	private PolylineChart mChart;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_phone, null);
		// initial data
		// CPU
		m_tvCpuUsage = (TextView) view.findViewById(R.id.busy);
		textIdle = (TextView) view.findViewById(R.id.idle);
		progressCpu = (ProgressBar) view.findViewById(R.id.cpu_progress);
		cpuFrame = (FrameLayout) view.findViewById(R.id.cpu_runtime);
		mChart = new PolylineChart(getActivity());
		// memory
		m_tvMemUsed = (TextView) view.findViewById(R.id.ram_used);
		m_tvMemFree = (TextView) view.findViewById(R.id.ram_free);
		progressMemory = (ProgressBar) view.findViewById(R.id.ram_progress);
		// SD
		textSDused = (TextView) view.findViewById(R.id.storage_used);
		textSDfree = (TextView) view.findViewById(R.id.storage_free);
		progressSD = (ProgressBar) view.findViewById(R.id.storage_progress);

		// general
		((TextView) view.findViewById(R.id.model_value)).setText(android.os.Build.MODEL);
		((TextView) view.findViewById(R.id.version_value)).setText(android.os.Build.VERSION.RELEASE);
		((TextView) view.findViewById(R.id.imsi_value)).setText(PhoneDataProvider.getIMSI(getActivity()));
		((TextView) view.findViewById(R.id.imei_value)).setText(PhoneDataProvider.getIMEI(getActivity()));

		((TextView) view.findViewById(R.id.base_band_value)).setText(PhoneDataProvider.getBaseBand()); //基带
		((TextView) view.findViewById(R.id.kernel_value)).setText(PhoneDataProvider.getKernelVersion());//内核
		((TextView) view.findViewById(R.id.internal_value)).setText(PhoneDataProvider.getRomVersion());//内部

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mChecker = new PropertyChecker(this).setContext(getActivity());
		mChecker.execute();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		mChecker.stop();
	}

	@Override
	public void onReceiveState(Long[] properties)
	{
		// CPU
		int used = Math.round(properties[0]);
		if (used < 0)
			return;
		m_tvCpuUsage.setText("已用" + used + "%");
		textIdle.setText("空闲" + (100 - used) + "%");
		progressCpu.setProgress(used);
		// SD card
		textSDused.setText("已用" + (properties[2] - properties[1]) + "MB");
		textSDfree.setText("空闲" + properties[1] + "MB");
		if (properties[2] > 0)
			progressSD.setProgress(Math.round(100 * (properties[2] - properties[1]) / properties[2]));
		cpuFrame.removeAllViews();
		cpuFrame.addView(mChart.create(used));
		// memory
		m_tvMemUsed.setText("已用" + (properties[4] - properties[3]) + "MB");
		m_tvMemFree.setText("空闲" + properties[3] + "MB");
		if (properties[4] > 0)
			progressMemory.setProgress(Math.round(100 * (properties[4] - properties[3]) / properties[4]));
	}

}

class PropertyChecker extends AsyncTask<Void, Long, Void>
{

	private boolean checking = true;
	public static final String TAG = "Checker";
	private static final int INTERVAL = 1024;
	OnStateListener listener;
	private Context context;

	public PropertyChecker(OnStateListener listener)
	{
		this.listener = listener;
	}

	public void stop()
	{
		checking = false;
	}

	public PropertyChecker setContext(Context ctx)
	{
		context = ctx;
		return this;
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		CpuDetail current, old = null;
		long freeSD = 0, totalSD = 0, totalMemory = 0, freeMemory = 0;
		current = calculate();
		while (checking)
		{
			// check CPU
			if (current != null)
				old = current;
			current = calculate();
			if (old == null || current == null)
				continue;

			long ratio = 0;
			if ((current.total - old.total) > 0)
				ratio = 100 * ((current.total - old.total) - (current.idle - old.idle)) / (current.total - old.total);

			// check SD card
			StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
			long freeTmp = 0, totalTmp = 0;
			try
			{
				freeTmp = ((long) sf.getAvailableBlocks() * (long) sf.getBlockSize()) / 1024 / 1024;
				totalTmp = (sf.getBlockCount() * (long) sf.getBlockSize()) / 1024 / 1024;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (freeTmp != 0 && totalTmp != 0)
				{
					freeSD = freeTmp;
					totalSD = totalTmp;
				}
			}

			// check memory
			String[] totalInfos = null;
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1024);
				String load = reader.readLine();
				totalInfos = load.split(" ");
				reader.close();
			}
			catch (IOException ex)
			{
				Log.e(TAG, "IOException" + ex.toString());
			}

			totalTmp = 0;
			freeTmp = 0;
			try
			{
				for (String str : totalInfos)
				{
					if (str.contains("M") || str.contentEquals("") || str.contains("k"))
						continue;
					else
					{
						totalTmp = Long.parseLong(str) / 1024;
						break;
					}
				}
				ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
				MemoryInfo mi = new MemoryInfo();
				am.getMemoryInfo(mi);
				freeTmp = mi.availMem/1024/1024;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				Log.i(TAG, "ArrayIndexOutOfBoundsException" + e.toString());
			}
			finally
			{
				if (freeTmp != 0 && totalTmp != 0)
				{
					freeMemory = freeTmp;
					totalMemory = totalTmp;
				}
			}
			// return property results
			publishProgress(ratio, freeSD, totalSD, freeMemory, totalMemory);
			try
			{
				Thread.sleep(INTERVAL);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		Log.d(TAG, "Exit checking!");
		return null;
	}

	protected void onProgressUpdate(Long... progress)
	{
		listener.onReceiveState(progress);
	}

	private class CpuDetail
	{
		long total;
		long idle;

		public CpuDetail(long total, long idle)
		{
			this.total = total;
			this.idle = idle;
		}
	}

	private CpuDetail calculate()
	{
		String[] cpuInfos = null;
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")), 1024);
			String load = reader.readLine();
			reader.close();
			cpuInfos = load.split(" ");
		}
		catch (IOException ex)
		{
			Log.e(TAG, "IOException" + ex.toString());
			return null;
		}
		long totalCpu = 0, idle = 0;
		try
		{
			totalCpu = Long.parseLong(cpuInfos[2]) + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4]) + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5]) + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
			idle = Long.parseLong(cpuInfos[5]);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			Log.i(TAG, "ArrayIndexOutOfBoundsException" + e.toString());
			return null;
		}
		return new CpuDetail(totalCpu, idle);
	}

}

interface OnStateListener
{
	public void onReceiveState(Long[] progress);
}

class PolylineChart
{
	private Context context;
	private ArrayList<Integer> ratios;
	private static final int MAX_SIZE = 32;
	private XYMultipleSeriesRenderer renderer;
	private XYMultipleSeriesDataset dataset;
	private XYSeries series;

	public PolylineChart(Context context)
	{
		this.context = context;
		ratios = new ArrayList<Integer>();
		// 1, 构造显示用渲染图
		renderer = new XYMultipleSeriesRenderer();
		renderer.clearYTextLabels();
		renderer.setMarginsColor(Color.WHITE);
		renderer.setMargins(new int[] { 0, 16, 0, 8 });
		renderer.setShowLegend(false);
		renderer.setShowGrid(true);
		renderer.setRange(new double[] { 0, 32, 0, 100 });
		renderer.clearXTextLabels();
		renderer.clearYTextLabels();
		renderer.setShowGridY(false);
		// 2,进行显示
		dataset = new XYMultipleSeriesDataset();
		// 2.1, 构建数据
		series = new XYSeries(null);
		// 需要绘制的点放进dataset中
		dataset.addSeries(series);
		// 3, 对点的绘制进行设置
		XYSeriesRenderer xyRenderer = new XYSeriesRenderer();
		// 3.1设置颜色
		xyRenderer.setColor(Color.BLACK);
		xyRenderer.setFillPoints(false);
		// 3.2设置点的样式
		// xyRenderer.setPointStyle(PointStyle.SQUARE);
		xyRenderer.addFillOutsideLine(new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BELOW));
		// 3.3, 将要绘制的点添加到坐标绘制中
		renderer.addSeriesRenderer(xyRenderer);

	}

	public View create(int ratio)
	{
		ratios.add(ratio);
		if (ratios.size() > MAX_SIZE)
			ratios.remove(0);

		series.clear();
		int xCoordinate = MAX_SIZE, index = ratios.size();
		while (index > 0)
			series.add(--xCoordinate, ratios.get(--index));

		return ChartFactory.getLineChartView(context, dataset, renderer);
	}
}