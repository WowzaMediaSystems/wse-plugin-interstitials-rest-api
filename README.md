# Wowza SGAI REST API
The **SGAI REST API** module for [Wowza Streaming Engine™ media server software](https://www.wowza.com/products/streaming-engine) enables you to added HLS Interstitials with a rest api

## Prerequisites
* Wowza Streaming Engine™ 4.9.4 or later is required.
* Java 21.

## Build instructions
* Clone repo to local filesystem.
* Update `wseLibDir` variable in the `gradle.properties` file to point to local _Wowza Streaming Engine_ `lib` folder.
* Run `./gradlew build` to build the jar file.

## Install
* Copy `wse-plugin-cloud-sgai-rest-api-x.x.x.jar` into lib directory 
* Add HTTPProvider to `VHost.xml`
```xml

<HTTPProvider>
    <BaseClass>com.wowza.wms.plugin.sgairestapi.http.HTTPProviderSgaiRestApi</BaseClass>
    <RequestFilters>v1/sgai/*</RequestFilters>
    <AuthenticationMethod>none</AuthenticationMethod>
</HTTPProvider>
```  
* Add Property to `VHost.xml`
```xml
<Property>
    <Name>optionsCORSHeadersAddMain</Name>
    <Value>Access-Control-Allow-Methods:DELETE</Value>
    <Type>String</Type>
</Property>
```
* Add module to each application xml:
```xml
<Module>
    <Name>ModuleSgaiRestApi</Name>
    <Description>ModuleSgaiRestApi</Description>
    <Class>com.wowza.wms.plugin.sgairestapi.module.ModuleSgaiRestApi</Class>
</Module>
```
* Add HLS datetime to HTTPStreamer Properties in each application xml
```xml
<Property>
    <Name>cupertinoEnableProgramDateTime</Name>
    <Value>true</Value>
    <Type>Boolean</Type>
</Property>
```

## API
### API patterns is
* `/v1/sgai/applications/{appName}/streams/{streamName}`


### API supports methods/verbs
`POST DELETE`

### Metadata 
A json object can be passed into the video stream 
#### Properties
| Property      | Description                                                   |
|:--------------|:--------------------------------------------------------------|
| id            | ID of the Ad                                                  |
| start_date    | Absolute start date in ISO8601 format, or +<seconds> from now |
| duration      | Duration of the add                                           |
| asset_list    | url for the assets                                            |
| resume_offset | seconds to offset resume                                      |
| restrict      | SKIP,JUMP                                                     |


## curl Examples

```shell
curl -X POST  -H "Content-Type: application/json"  -d '{
  "id": "ad1",
  "start_date": "+10",
  "duration": 30.0,
  "asset_list": "https://myads.example.com/ad1.m3u8"  
  }' http://127.0.0.1:1935/v1/sgai/applications/live/streams/mystream
```

## HLS Output Example
```
#EXTM3U 
#EXT-X-VERSION:3 
#EXT-X-TARGETDURATION:4 
#EXT-X-MEDIA-SEQUENCE:60897 
#EXT-X-DISCONTINUITY-SEQUENCE:0 
#EXT-X-PROGRAM-DATE-TIME:2025-02-13T17:03:19.368Z
#EXT-X-DATERANGE:ID="ad1",CLASS="com.apple.hls.interstitial",START-DATE="2025-02-13T17:03:29.794Z",DURATION=30.000,X-ASSET-LIST="https://myads.example.com/ad1.m3u8" 
#EXTINF:4.0, 
media_10.ts
#EXTINF:4.0, 
media_11.ts 
#EXTINF:4.0, 
media_12.ts
```