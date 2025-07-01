/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.interstitialsrestapi;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.cupertinostreaming.livestreampacketizer.*;
import com.wowza.wms.httpstreamer.mpegdashstreaming.livestreampacketizer.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.livepacketizer.*;
import org.apache.logging.log4j.*;

public class LiveStreamPacketizerListener extends LiveStreamPacketizerActionNotifyBase
{
	private static final Logger log = LogManager.getLogger(LiveStreamPacketizerListener.class);

	private static final Class<LiveStreamPacketizerListener> CLASS = LiveStreamPacketizerListener.class;
	private static final String CLASSNAME = CLASS.getSimpleName();
	private final IApplicationInstance appInstance;
	private final WMSLogger logger;


	public LiveStreamPacketizerListener(IApplicationInstance appInstance)
	{
		this.appInstance = appInstance;
		this.logger = WMSLoggerFactory.getLoggerObj(CLASS, appInstance);
	}

	@Override
	public void onLiveStreamPacketizerInit(ILiveStreamPacketizer liveStreamPacketizer, String streamName)
	{
		try
		{
			IMediaStream stream = appInstance.getStreams().getStream(streamName);
			if (!liveStreamPacketizer.isRepeaterEdge() && liveStreamPacketizer instanceof LiveStreamPacketizerCupertino)
			{
				LiveStreamPacketizerCupertinoDataHandler dataHandler =  new LiveStreamPacketizerCupertinoDataHandler((LiveStreamPacketizerCupertino) liveStreamPacketizer, stream);
				((LiveStreamPacketizerCupertino)liveStreamPacketizer).setDataHandler(dataHandler);
			}
			else if (liveStreamPacketizer instanceof LiveStreamPacketizerMPEGDash)
			{
			// 	((LiveStreamPacketizerMPEGDash)liveStreamPacketizer).setDataHandler(new LiveStreamPacketizerMpegDashDataHandler((LiveStreamPacketizerMPEGDash) liveStreamPacketizer, stream));
			}

		}
		catch (Exception e)
		{
			logger.warn(CLASSNAME + ".onLiveStreamPacketizerCreate cannot set LiveStreamPacketizerDataHandler. " + e.getMessage());
		}
	}
}
