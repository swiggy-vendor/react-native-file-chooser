# react-native-file-chooser
A React Native module that allows you to use native UI to select a file from the device library
Based on [react-native-image-chooser](https://github.com/marcshilling/react-native-image-chooser)

- Supports choosing files from Google Drive

## Install

### iOS
This component does not currently work on iOS, instead use [react-native-document-chooser](https://github.com/Elyx0/react-native-document-chooser)

### Android
1. `npm install react-native-file-chooser --save`

```gradle
// file: android/settings.gradle
...

include ':react-native-file-chooser'
project(':react-native-file-chooser').projectDir = new File(settingsDir, '../node_modules/react-native-file-chooser/android')
```
```gradle
// file: android/app/build.gradle
...

dependencies {
    ...
    compile project(':react-native-file-chooser')
}
```
```xml
<!-- file: android/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.myApp">

    <uses-permission android:name="android.permission.INTERNET" />

    <!-- add following permissions -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <!-- -->
    ...
```
```java
// file: MainApplication.java
...

import com.filechooser.FileChooserPackage; // import package

public class MainApplication extends Application implements ReactApplication {

   /**
   * A list of packages used by the app. If the app uses additional views
   * or modules besides the default ones, add more packages here.
   */
    @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new FileChooserPackage() // Add package
        );
    }
...
}

```
## Usage
1. In your React Native javascript code, bring in the native module:

  ```javascript
import { NativeModules } from 'react-native'
const FilePicker = NativeModules.FileChooser
  ```
2. Use it like so:

  When you want to display the chooser:
  ```javascript

  FilePicker.show(null, (response) => {
    console.log('Response = ', response);

    if (response.didCancel) {
      console.log('User cancelled file chooser');
    }
    else if (response.error) {
      console.log('FileChooserManager Error: ', response.error);
    }
    else {
      this.setState({
        file: response
      });
    }
  });
  ```
