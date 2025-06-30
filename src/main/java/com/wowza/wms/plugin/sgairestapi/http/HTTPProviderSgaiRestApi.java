/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.sgairestapi.http;

import com.fasterxml.jackson.core.*;
import com.wowza.util.SystemUtils;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.HTTPProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.httpstreamer.cupertinostreaming.livestreampacketizer.LiveStreamPacketizerCupertino;
import com.wowza.wms.logging.*;
import com.wowza.wms.plugin.sgairestapi.ReleaseInfo;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.vhost.IVHost;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import com.fasterxml.jackson.databind.*;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public class HTTPProviderSgaiRestApi extends HTTPProvider2Base
{
	public static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public FastDateFormat fastDateFormat;
	public int adNumber = 0;
	Map<String,Timer> cancelDateRange = new HashMap<String, Timer>();
	static WMSLogger log = null;

	public HTTPProviderSgaiRestApi()
	{
		log = WMSLoggerFactory.getLogger(HTTPProviderSgaiRestApi.class);
		log.info("Creating HTTPProvider SgaiRestApi v"+ ReleaseInfo.getVersion());
		fastDateFormat = FastDateFormat.getInstance(DATEFORMAT, SystemUtils.gmtTimeZone, Locale.US);
	}
	//
//	@Override
//	public void addCORSHeaders(IHTTPResponse ihttpResponse) {
//		ihttpResponse.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, HEAD, DELETE");
//		super.addCORSHeaders(ihttpResponse);
//		this
//	}
	@Override
	public void init()
	{
		log.info("init");
		super.init();
	}
	@Override
	public boolean doHTTPAuthentication(IVHost var1, IHTTPRequest var2, IHTTPResponse var3)
	{
		log.info("doHTTPAuthentication");
		return super.doHTTPAuthentication(var1,var2,var3);
	}

	@Override
	public boolean canHandle(String var1) {
		boolean retVal = super.canHandle(var1);
		log.info("canHandle:" + retVal);
		return retVal;
	}


	@Override
	public void onHTTPRequest(IVHost ivHost, IHTTPRequest ihttpRequest, IHTTPResponse ihttpResponse) {

		log.info("Starting onHTTPRequest");
		String appName = "unknown";
		String streamName = "unknown";
		String[] splits = ihttpRequest.getRequestURL().split("/");
		for (int idx = 0; idx < splits.length; idx++)
		{
			if (splits[idx].equals("applications"))
			{
				appName = splits[idx + 1];
			}
		}

		for (int idx = 0; idx < splits.length; idx++)
		{
			if (splits[idx].equals("streams"))
			{
				streamName = splits[idx + 1];
			}
		}

		if (ihttpRequest.getMethod().equals("DELETE") || ihttpRequest.getRequestURL().endsWith("/delete"))
		{
			deleteDateRange(ivHost,appName,streamName);
			sendResponse(ihttpResponse, "removed", 204);
		}
		else
		{
			try
			{
				String body = new String(ihttpRequest.getMsgBytes());
				DataRangeData drd = new DataRangeData(body);
				addDateRange(ivHost, appName, streamName, drd);
				sendResponse(ihttpResponse, "ad id:" + drd.id + " to start at:" + fastDateFormat.format(drd.startDate), 200);
			}
			catch (Exception e)
			{
				sendResponse(ihttpResponse, "Error:" + e.getMessage(), 500);
			}
		}
		// curl -X POST "http://localhost:1935/v1/sgai/applications/simu-live/streams/myStream -d '{ "id":"foo" }'

	}

	public void deleteDateRange(IVHost ivHost, String appName, String streamName)
	{
		ILiveStreamPacketizer liveStreamPacketizer = getLiveStreamPacketizer(ivHost,appName,streamName);
		((LiveStreamPacketizerCupertino)liveStreamPacketizer).getUserManifestHeaders().removeHeader("EXT-X-DATERANGE");
	}

	public void addDateRange(IVHost ivHost, String appName, String streamName, DataRangeData drd)
	{
		ILiveStreamPacketizer liveStreamPacketizer = getLiveStreamPacketizer(ivHost,appName,streamName);

		String dateRange = "";
		dateRange = "ID=\"" + drd.id + "\",";
		dateRange += "CLASS=\"com.apple.hls.interstitial\",";
		dateRange += "START-DATE=\"" + fastDateFormat.format(drd.startDate) + "\",";
		dateRange += "DURATION=" + String.format("%.3f",drd.duration) + ",";
		if(drd.assetList != null) {
			dateRange += "X-ASSET-LIST=\"" + drd.assetList + "\",";
		}
		if(drd.assetUri != null) {
			dateRange += "X-ASSET-URI=\"" + drd.assetUri + "\",";
		}
		dateRange += "X-RESUME-OFFSET=" + drd.resumeOffset + ",";
		dateRange += "X-RESTRICT=\"" + drd.restrict + "\"";

		((LiveStreamPacketizerCupertino)liveStreamPacketizer).getUserManifestHeaders().removeHeader("EXT-X-DATERANGE");
		((LiveStreamPacketizerCupertino)liveStreamPacketizer).getUserManifestHeaders().addHeader("EXT-X-DATERANGE",dateRange);

		synchronized(cancelDateRange)
		{
			String key = appName + ":" + streamName;
			if (cancelDateRange.get(key) != null)
			{
				cancelDateRange.get(key).cancel();
			}
			CancelDateRangeThread cancelThread = new CancelDateRangeThread((LiveStreamPacketizerCupertino)liveStreamPacketizer);
			Timer timer = new Timer();
			Date now = new Date();

			long delay = drd.startDate.getTime() - now.getTime() + (10*1000); //30 second pad
			//timer.schedule(cancelThread, delay);
			cancelDateRange.put(key,timer);
		}
	}
	private ILiveStreamPacketizer getLiveStreamPacketizer(IVHost ivHost, String appName, String streamName)
	{
		IApplication app = ivHost.getApplication(appName);
		if(app == null)
		{
			throw new IllegalArgumentException("Application not found:" + appName);
		}
		IApplicationInstance appInstance = app.getAppInstance("_definst_");

		ILiveStreamPacketizer liveStreamPacketizer = appInstance.getStreams().getLiveStreamPacketizer(streamName, "cupertinostreamingpacketizer", false);
		if(liveStreamPacketizer == null)
		{
			throw new IllegalArgumentException("Stream not found:" + streamName);
		}
		return liveStreamPacketizer;
	}
	private void sendResponse(IHTTPResponse resp, String msg, int httpCode)
	{
		//resp.setHeader("Content-Type", "application/json");
		msg = msg + "\r\n";
		StringBuffer ret = new StringBuffer();
		ret.append(msg);

		try
		{
			OutputStream out = resp.getOutputStream();
			byte[] outBytes = ret.toString().getBytes();
			out.write(outBytes);
			resp.setResponseCode(httpCode);
		}
		catch (Exception e)
		{
			resp.setResponseCode(500);
		}

	}
	public class DataRangeData
	{
		public String id = null;
		public Date startDate;
		public double duration;
		public String assetUri = null;
		public String assetList = null;
		public int resumeOffset;
		public String restrict;

		public DataRangeData(String json) throws  Exception
		{
			JsonNode actualObj = null;

			ObjectMapper mapper = new ObjectMapper();
			actualObj = mapper.readTree(json);
			JsonNode obj;

			obj	= actualObj.get("id");
			if (obj != null)
			{
				adNumber = adNumber + 1;
				this.id = obj.textValue() + "-" + adNumber;
			}
			else
			{
				this.id = "ad1";
			}

			Date now = new Date();

			obj = actualObj.get("start_date");
			if (obj != null)
			{
				if(obj.textValue().startsWith("+"))
				{
					now.setTime(now.getTime() + (Integer.parseInt(obj.textValue().substring(1)) * 1000L));
					startDate = now;
				}
				else
				{
					startDate = fastDateFormat.parse(obj.textValue());
				}
			}
			else
			{
				now.setTime(now.getTime());
				startDate = now;
			}

			obj	= actualObj.get("duration");
			if (obj != null)
			{
				this.duration = obj.doubleValue();
			}
			else
			{
				this.duration = 30.0;
			}

			obj	= actualObj.get("asset_list");
			if (obj != null)
			{
				this.assetList = obj.textValue();
			}

			obj	= actualObj.get("asset_uri");
			if (obj != null)
			{
				this.assetUri = obj.textValue();
			}

			obj	= actualObj.get("resume_offset");
			if (obj != null)
			{
				this.resumeOffset = obj.intValue();
			}
			else
			{
				this.resumeOffset = 0;
			}

			obj	= actualObj.get("restrict");
			if (obj != null)
			{
				this.restrict = obj.textValue();
			}
			else
			{
				this.restrict = "SKIP,JUMP";
			}
		}
	}
	public class CancelDateRangeThread extends TimerTask
	{
		private LiveStreamPacketizerCupertino packetizer;

		public CancelDateRangeThread(LiveStreamPacketizerCupertino packetizer)
		{
			this.packetizer = packetizer;
		}

		@Override
		public void run()
		{
			((LiveStreamPacketizerCupertino)packetizer).getUserManifestHeaders().removeHeader("EXT-X-DATERANGE");
		}
	}
}
//
//aws route53 change-resource-record-sets \
//        --hosted-zone-id Z3O1ERC4N4MG3B \
//        --change-batch '
//        {
//        "Comment": "Testing creating a record set"
//        ,"Changes": [{
//        "Action"              : "UPSERT"
//        ,"ResourceRecordSet"  : {
//        "Name"              : "demo.entrypoint.cloud.wowza.com"
//        ,"Type"             : "A"
//        ,"TTL"              : 120
//        ,"ResourceRecords"  : [{
//        "Value"         : "13.56.194.199"
//        }]
//        }
//        }]
//        }
//        '


//