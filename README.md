# Wowza Streaming Engine HLS interstitials REST API

The **HLS Interstitials REST API** module for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to add HLS Interstitials with a REST API to a live video feed by adding the #EXT-DATE-RANGE tag to the HLS manifest.

For more details about HLS interstitials, see [Getting Started with HLS Interstitials](https://developer.apple.com/streaming/GettingStartedWithHLSInterstitials.pdf).

## Prerequisites

* Wowza Streaming Engine™ 4.9.4 or later is required
* Java 21
* Gradle (to build)

## Build instructions

1. Clone this repository to your local filesystem.
1. Update the `wseLibDir` variable in the `gradle.properties` file to point to the local Wowza Streaming Engine `lib` folder.
1. Run `./gradlew build` to build the jar file.

## Install

1. Copy `wse-plugin-cloud-interstitials-rest-api-x.x.x.jar` into the `lib` directory.
1. Add the HTTPProvider to `VHost.xml`:

```xml
<HTTPProvider>
    <BaseClass>com.wowza.wms.plugin.interstitialsrestapi.http.HTTPProviderInterstitialsRestApi</BaseClass>
    <RequestFilters>v1/interstitials/*</RequestFilters>
    <AuthenticationMethod>none</AuthenticationMethod>
</HTTPProvider>
```

1. Add the following property to `VHost.xml`:

```xml
<Property>
    <Name>optionsCORSHeadersAddMain</Name>
    <Value>Access-Control-Allow-Methods:DELETE</Value>
    <Type>String</Type>
</Property>
```

1. Add the following module to the Application.xml:

```xml
<Module>
    <Name>ModuleInterstitialsRestApi</Name>
    <Description>ModuleInterstitialsRestApi</Description>
    <Class>com.wowza.wms.plugin.interstitialsrestapi.module.ModuleInterstitialsRestApi</Class>
</Module>
```

1. Add the following property to the **HTTPStreamer > Properties** block in the Application.xml file:

```xml
<Property>
    <Name>cupertinoEnableProgramDateTime</Name>
    <Value>true</Value>
    <Type>Boolean</Type>
</Property>
```

## API details

### API pattern

* `/v1/interstitials/applications/{appName}/streams/{streamName}`

### API supported methods

* `POST`
* `DELETE`

### Metadata

A JSON object can be passed into the video stream using the properties outlined in the following table.

#### Properties

| Property        | Description                                                              |
| :-------------- | :----------------------------------------------------------------------- |
| `id`            | Specify and identifier to use for the ad.                                |
| `start_date`    | Define an absolute start date in ISO8601 format, or +seconds from now.   |
| `duration`      | Specify duration for the ad.                                             |
| `asset_list`    | Define a URL for an assets list.                                         |
| `asset_uri`     | Define a URL for a single asset.                                         |
| `resume_offset` | Specify seconds to offset resume.                                        |
| `restrict`      | SKIP,JUMP                                                                |

## Examples and demo

After building the module, start Wowza Streaming Engine and Wowza Streaming Engine Manager using the docker-compose.yaml file in this repository. It includes a pre-configured Wowza Streaming Engine instance and sample **live** and **simu-live** applications.

1. Run the following command:

```bash
docker compose up
```

1. Insert an HLS interstitial for the **simu-live** application with a 10 second ad break, five seconds from now:

```shell
curl -X POST  -H "Content-Type: application/json"  -d '{
  "id": "ad1",
  "start_date": "+5",
  "duration": 10.0,
  "asset_uri": "https://wv-cdn-00-00.flowplayer.com/7bb18344-08f9-4c1e-84a7-80c1007aa99b/cmaf/6089d839-d699-424b-b914-445152e25115/playlist.m3u8"  
  }' http://localhost/v1/interstitials/applications/simu-live/streams/myStream
```

1. To test playback, go to:

```text
https://hlsjs.video-dev.org/demo/?src=https://wse-trial.wowza.com/simu-live/myStream/playlist.m3u8
```

1. To view the HLS interstitial in the HLS manifest, run:

```bash
curl http://localhost/simu-live/myStream/chunklist_w2003968828.m3u8
```

## HLS output example

An HLS output example looks similar to:

```text
#EXTM3U 
#EXT-X-VERSION:3 
#EXT-X-TARGETDURATION:4 
#EXT-X-MEDIA-SEQUENCE:60897 
#EXT-X-DISCONTINUITY-SEQUENCE:0 
#EXT-X-PROGRAM-DATE-TIME:2025-02-13T17:03:19.368Z
#EXT-X-DATERANGE:ID="ad1-5",CLASS="com.apple.hls.interstitial",START-DATE="2025-06-30T20:14:26.497Z",DURATION=10.000,X-ASSET-URI="https://wv-cdn-00-00.flowplayer.com/7bb18344-08f9-4c1e-84a7-80c1007aa99b/cmaf/6089d839-d699-424b-b914-445152e25115/playlist.m3u8",X-RESUME-OFFSET=0,X-RESTRICT="SKIP,JUMP" 
#EXTINF:4.0, 
media_10.ts
#EXTINF:4.0, 
media_11.ts 
#EXTINF:4.0, 
media_12.ts
```
