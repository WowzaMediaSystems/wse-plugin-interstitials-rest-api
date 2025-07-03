package com.wowza.wms.plugin.interstitialsrestapi.module;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.*;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.plugin.interstitialsrestapi.LiveStreamPacketizerListener;
import com.wowza.wms.server.ReleaseInfo;

public class ModuleInterstitialsRestApi extends ModuleBase
{
	private static final Class<ModuleInterstitialsRestApi> CLASS = ModuleInterstitialsRestApi.class;

	public static final String MODULE_NAME = CLASS.getSimpleName();
	public static final String MODULE_VERSION = ReleaseInfo.getVersion();

	private WMSLogger logger;

	public void onAppStart(IApplicationInstance appInstance)
	{
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
		logger.info(MODULE_NAME + ".onAppStart: [" + appInstance.getContextStr() + "] " + ReleaseInfo.getProject() + " version: " + MODULE_VERSION + " build: " + ReleaseInfo.getBuildNumber());

		appInstance.addLiveStreamPacketizerListener(new LiveStreamPacketizerListener(appInstance));
	}

	public void onAppStop(IApplicationInstance appInstance)
	{
		logger.info(MODULE_NAME + ".onAppStop: [" + appInstance.getContextStr() + "]");
	}
}
