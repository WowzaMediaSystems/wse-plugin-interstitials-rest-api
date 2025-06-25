# Ads Inserter
This module provides a rest API to added HLS Interstitials

## Install
* copy `wse-plugin-cloud-ad-inserter-x.x.x.jar` into lib directory 
* add HTTPProvider to `VHost.xml`
```xml

<HTTPProvider>
    <BaseClass>com.wowza.wms.plugin.adinserter.httpprovider.HTTPProviderAdInserter</BaseClass>
    <RequestFilters>v1/ads/*</RequestFilters>
    <AuthenticationMethod>none</AuthenticationMethod>
</HTTPProvider>
```  
* add Property to `VHost.xml`
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
    <Name>moduleAdInsertion</Name>
    <Description>moduleAdInsertion</Description>
    <Class>com.wowza.wms.plugin.adinserter.module.ModuleAdInserter</Class>
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
* `/v1/ads/applications/{appName}/streams/{streamName}`


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
  }' http://127.0.0.1:1935/v1/ads/applications/live/streams/mystream
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