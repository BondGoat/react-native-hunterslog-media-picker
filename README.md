# react-native-hunterslog-media-picker
React Native Media Picker component, only used in Hunterslog Project. Do not copy or modify in any circumstances.

## Table of contents
- [Install](#install)
  - [iOS](#ios)
  - [Android](#android)
- [Usage](#usage)

## Install

`npm install react-native-hunterslog-media-picker --save`
Also you need to install dependencies plugin if not installed yet:
`npm install react-native-video --save`
`npm install react-native-asset-thumbnail --save`

Use [rnpm](https://github.com/rnpm/rnpm) to automatically complete the installation:  
`rnpm link react-native-hunterslog-media-picker`
`rnpm link react-native-video`
`rnpm link react-native-asset-thumbnail`

or link manually like so:

### iOS
- Libraries need to be added to XCode, [follow direction at here](https://facebook.github.io/react-native/docs/linking-libraries-ios.html):
  - RCTImageResizer in ./ios
  - RNCamera in ./ios
  - RTCCameraRoll in ./node_modules/react-native/Libraries
  - RNAssetThumbnail in node_modules/react-native-asset-thumbnail  
- Insert this to Info.plist
```Info.plist
<key>NSPhotoLibraryUsageDescription</key>
<string>$(PRODUCT_NAME) want to acccess your Camera Library</string>
```
- Use it as a independent component [MediaHunterslogPicker]

### Android
```gradle
// file: android/settings.gradle
...

include ':react-native-hunterslog-media-picker'
project(':react-native-hunterslog-media-picker').projectDir = new File(settingsDir, '../node_modules/react-native-hunterslog-media-picker/android')
```
```gradle
// file: android/app/build.gradle
...

dependencies {
    ...
    compile project(':react-native-hunterslog-media-picker')
}
```
```xml
<!-- file: android/app/src/main/AndroidManifest.xml -->
<manifest ...>

	...
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application...>
    	...
        <activity android:name="com.falco.mediapicker.MediaPickerActivity" android:screenOrientation="portrait"/>
        ...
        <activity android:name="com.falco.mediapicker.PhotoPreviewActivity" />
        <activity android:name="com.falco.mediapicker.VideoPreviewActivity" />

        <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="[package name].fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths"></meta-data>
        </provider>
        ...
    </application>
    ...
```
```java
// file: android/app/src/main/java/com/<...>/MainApplication.java
...

import com.falco.mediapicker.MediaHunterslogPickerPackage; // <-- add this import

public class MainApplication extends Application implements ReactApplication {
    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new MediaHunterslogPickerPackage() // <-- add this line
        );
    }
...
}

```

Create xml folder in ./res, and create a xml file named "file_paths.xml":
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="hunterslog_img" path="Android/data/[package name]/files/Pictures/" />
</paths>
```

## Usage

```javascript
var Platform = require('react-native').Platform;
var MediaPicker = require('react-native-hunterslog-media-picker');

/**
 * The method will launch native module
 * @params {Int} max_photo Max number of photo can be uploaded
 * @params {Int} max_video Max number of video can be uploaded
 * @params {Int} max_video_duration Maximun seconds a video can be captured
 * @params {Object} selectedMedia List of selected media, used in case Edit or go back from Create post after
 * selected some photos, videos
 */
MediaPicker.showMediaPicker(max_photo, max_video, max_video_duration, selectedMedia, (response) => {
// Data is an Array of selected media
  console.log('Response = ', response);

  ...

});
```
```Response Model
[{'Location':{'Lat':10.254,'Lng':101.5458},'Url':'file:///storage/...','ThumbUrl':'base64String'},{'Location':{'Lat':10.254,'Lng':101.5458},'Url':'file:///storage/...','ThumbUrl':'base64String'},...]
```

The format of media file will be 'file:///storage...' for Android, put it in Image tag:
```javascript
<Image source={this.state.avatarSource} style={styles.uploadAvatar} />
```
