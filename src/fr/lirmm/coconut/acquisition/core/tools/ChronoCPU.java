package fr.lirmm.coconut.acquisition.core.tools;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;

public class ChronoCPU extends Chrono{
	public ChronoCPU(String name)
	{
		super(name,false);
	}
	public void start(String serieName)
	{
		current_chronos.put(serieName, getCpuTimeMillis());
	}
	synchronized public void  stop(String serieName)
	{
		Long currentTime=current_chronos.get(serieName);
		if(currentTime!=null) 
			{
			ArrayList<Long>result=results.get(serieName);
			if(result==null)
			{
				result=new ArrayList<Long>();
				results.put(serieName, result);
			}
			result.add( getCpuTimeMillis()-currentTime);
			}
	}
    /** thread CPU time in milliseconds. */
    private long getCpuTimeMillis ()
    {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.isCurrentThreadCpuTimeSupported() ? threadMXBean.getCurrentThreadCpuTime()/1000000: 0L;
    }
}
