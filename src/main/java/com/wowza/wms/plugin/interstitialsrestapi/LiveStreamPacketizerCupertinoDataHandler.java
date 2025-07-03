/*
 * This code and all components (c) Copyright 2006 - 2025, Wowza Media Systems, LLC.  All rights reserved.
 * This code is licensed pursuant to the Wowza Public License version 1.0, available at www.wowza.com/legal.
 */

package com.wowza.wms.plugin.interstitialsrestapi;

import com.wowza.util.BufferUtils;
import com.wowza.wms.amf.*;
import com.wowza.wms.httpstreamer.cupertinostreaming.livestreampacketizer.*;
import com.wowza.wms.media.mp3.model.idtags.*;
import com.wowza.wms.stream.IMediaStream;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public  class LiveStreamPacketizerCupertinoDataHandler implements
		IHTTPStreamerCupertinoLivePacketizerDataHandler2
{
	protected final LiveStreamPacketizerCupertino liveStreamPacketizer;

	protected DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("GMT")).withLocale(
			Locale.US);

	protected final long[] tcOffsets = {-1, -1, -1, -1};
	protected final long[] streamStartTimes = {-1, -1, -1, -1};
	protected final long[] spliceOutIds = {-1, -1, -1, -1};
	protected final long[] spliceOutTCs = {-1, -1, -1, -1};
	protected final long[] eventIds = {-1, -1, -1, -1};

	protected long streamStartTime = -1;
	protected final IMediaStream stream;

	public LiveStreamPacketizerCupertinoDataHandler(LiveStreamPacketizerCupertino liveStreamPacketizer, IMediaStream stream)
	{
		//super(stream);
		this.stream = stream;
		this.liveStreamPacketizer = liveStreamPacketizer;
	}

	@Override
	public void onFillChunkStart(LiveStreamPacketizerCupertinoChunk chunk)
	{
		// common PDT code for all implementations
		int rendition = chunk.getRendition().getRendition();
		long chunkStartTime = chunk.getStartTimecode();
		if (tcOffsets[rendition - 1] == -1)
		{
			tcOffsets[rendition - 1] = chunkStartTime;
			streamStartTime = stream.getElapsedTime().getDate().getTime();
		}

		String programDateTime = dateTimeFormatter.format(Instant.ofEpochMilli(streamStartTime + (chunkStartTime - tcOffsets[rendition - 1])));
		chunk.setProgramDateTime(programDateTime);

		ID3Frames id3Header = liveStreamPacketizer.getID3FramesHeader(chunk.getRendition());
		if (id3Header != null)
		{
			ID3V2FrameTextInformationUserDefined comment = new ID3V2FrameTextInformationUserDefined();

			comment.setDescription("programDateTime");
			comment.setValue(programDateTime);

			id3Header.clear();
			id3Header.putFrame(comment);
		}
	}

	@Override
	public void onFillChunkEnd(LiveStreamPacketizerCupertinoChunk chunk, long timecode)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onFillChunkDataPacket(LiveStreamPacketizerCupertinoChunk liveStreamPacketizerCupertinoChunk,
			CupertinoPacketHolder cupertinoPacketHolder, AMFPacket amfPacket, ID3Frames id3Frames)
	{

	}

	@Override
	public void onFillChunkMediaPacket(LiveStreamPacketizerCupertinoChunk chunk, CupertinoPacketHolder holder, AMFPacket packet)
	{

	}

	protected String getRawDataAsHexStr(AMFDataObj data)
	{
		String rawData = null;
		String encodedData = data.getString("rawData");
		if (encodedData != null)
		{
			byte[] dataBytes = Base64.getDecoder().decode(encodedData);
			rawData = "0x" + BufferUtils.encodeHexString(dataBytes).toUpperCase();
		}
		return rawData;
	}
}
