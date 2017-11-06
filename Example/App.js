import React from 'react-native'

const {
  StyleSheet,
  Text,
  View,
  PixelRatio,
  TouchableOpacity,
  Image,
  NativeModules: { FileChooserManager },
} = React

export default class App extends React.Component {
  state = {
    file: undefined,
  }

  selectFileTapped() {
    const options = {
      title: 'File Chooser',
      chooseFileButtonTitle: 'Choose File...',
    }

    FileChooserManager.showFileChooser(options, response => {
      console.log('Response = ', response)

      if (response.didCancel) {
        console.log('User cancelled photo chooser')
      } else if (response.error) {
        console.log('ImageChooserManager Error: ', response.error)
      } else if (response.customButton) {
        console.log('User tapped custom button: ', response.customButton)
      } else {
        this.setState({
          file: response,
        })
      }
    })
  }

  render() {
    return (
      <View style={styles.container}>
        <TouchableOpacity
          style={styles.button}
          onPress={this.selectFileTapped.bind(this)}
        >
          <Text>Choose file...</Text>
        </TouchableOpacity>
        <Text style={styles.fileInfo}>{JSON.stringify(this.state.file)}</Text>
      </View>
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  button: {
    borderColor: '#9B9B9B',
    borderWidth: 1 / PixelRatio.get(),
    margin: 5,
    padding: 5,
  },
  fileInfo: {
    borderColor: '#9B9B9B',
    borderWidth: 1 / PixelRatio.get(),
    margin: 5,
    padding: 5,
  },
})

React.AppRegistry.registerComponent('Example', () => Example)
