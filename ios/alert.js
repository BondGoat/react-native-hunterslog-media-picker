/*
 * Custom Alert dialog, only used for HuntersLog
 * Capable of several types of Dialog:
 1. Single Dialog with 1 message
 2. Single Dialog with 1 message, 1 confirm Button
 3. Single Dialog with 1 message, 2 confirm Buttons
 * Icons and Title are optional, can be turn enable or disable
 * Usage:
 <AlertDialog
  visible={this.state.is_alert_visible}
  title='TITLE'
  message='MESSAGE'
  buttonPositiveTxt='YES'
  buttonNegativeTxt='NO'
  onPositivePressed={() => callback1()}
  onNegativePressed={() => callback2()} />
 */
import React, { Component } from 'react';
import {
  StyleSheet,
  View,
  Modal,
  Text,
  Image,
  Platform
} from 'react-native';
import Button from 'react-native-button';

export default class AlertDialog extends Component {

  constructor(props) {
    super(props);
  }

  static defaultProps = {
    visible: false,
    title: '',
    message: '',
    buttonPositiveTxt: '',
    buttonNegativeTxt: '',
    iconImg: ''
  }

  onPositivePressed() {
    this.props.onPositivePressed();
  }

  onNegativePressed() {
    this.props.onNegativePressed();
  }

  renderIcon() {
    if(this.props.iconImg != '')
      return (<Image source={this.props.iconImg} style={styles.icon} />);
    return <View />;
  }

  renderTitle() {
    if(this.props.title != '')
      return (
        <View style={styles.title_area}>
          <Text style={styles.white_text}>{this.props.title}</Text>
        </View>
      );
    return <View />;
  }

  renderAlert() {
    if (!this.props.visible)
      return (
        <View />
      );

    if(this.props.buttonNegativeTxt == '' && this.props.buttonPositiveTxt == '') {
      return (
        <Modal onRequestClose={() => this.onNegativePressed()} visible={this.props.visible} transparent>
          {this.renderAlertWithAMessage()}
        </Modal>
      );
    }

    if(this.props.buttonNegativeTxt == '' && this.props.buttonPositiveTxt != '') {
      return (
        <Modal onRequestClose={() => this.onNegativePressed()} visible={this.props.visible} transparent>
          {this.renderAlertWithMessageAButton()}
        </Modal>
      );
    }

    return (
      <Modal onRequestClose={() => this.onNegativePressed()} visible={this.props.visible} transparent>
        {this.renderAlertWithMessageButtons()}
      </Modal>
    );

  }

  renderAlertWithAMessage() {
    return (
      <View style={styles.container}>
        <View style={styles.dialog}>
          <View style={[styles.dialog_container, (Platform.OS === 'android') ? {backgroundColor: '#00000055', padding: 3} : {}]}>
            {this.renderTitle()}
            <View style={styles.message_area}>
              {this.renderIcon()}
              <View style={styles.message_text_container}>
                <Text style={styles.message_text}>{this.props.message}</Text>
              </View>
            </View>
          </View>
        </View>
      </View>
    );
  }

  renderAlertWithMessageAButton() {
    return (
      <View style={styles.container}>
        <View style={styles.dialog}>
          <View style={[styles.dialog_container, (Platform.OS === 'android') ? {backgroundColor: '#00000055', padding: 3} : {}]}>
            {this.renderTitle()}
            <View style={styles.message_area}>
              {this.renderIcon()}
              <View style={styles.message_text_container}>
                <Text style={styles.message_text}>{this.props.message}</Text>
              </View>
            </View>
            <View style={styles.button_area}>
              <Button
                containerStyle={styles.button_positive}
                style={styles.white_text}
                onPress={this.onPositivePressed.bind(this)}>
                {this.props.buttonPositiveTxt}
              </Button>
            </View>
          </View>
        </View>
      </View>
    );
  }

  renderAlertWithMessageButtons() {
    return (
      <View style={styles.container}>
        <View style={styles.dialog}>
          <View style={[styles.dialog_container, (Platform.OS === 'android') ? {backgroundColor: '#00000055', padding: 3} : {}]}>
            {this.renderTitle()}
            <View style={styles.message_area}>
              {this.renderIcon()}
              <View style={styles.message_text_container}>
                <Text style={styles.message_text}>{this.props.message}</Text>
              </View>
            </View>
            <View style={styles.button_area}>
              <Button
                containerStyle={styles.button_positive}
                style={styles.white_text}
                onPress={this.onPositivePressed.bind(this)}>
                {this.props.buttonPositiveTxt}
              </Button>
              <Button
                containerStyle={styles.button_negative}
                style={styles.white_text}
                onPress={this.onNegativePressed.bind(this)}>
                {this.props.buttonNegativeTxt}
              </Button>
            </View>
          </View>
        </View>
      </View>
    );
  }

  render() {
    return this.renderAlert();
  }

};

var styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0
  },
  dialog: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    left: 0,
    right: 0,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000000AA'
  },
  dialog_container: {
    flexDirection: 'column',
    width: 300,
    height: 200,
    shadowColor: '#000000',
    shadowRadius: 10,
  },
  message_area: {
    flex: 3,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: 'white',
  },
  message_text_container: {
    flex: 15,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 10
  },
  message_text: {
    fontSize: 16,
    textAlign: 'center',
    color: 'black',
  },
  button_area: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  button_positive: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#e87600',
    width: (Platform.OS === 'android') ? 145 : 150,
    overflow:'hidden',
  },
  button_negative: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ef9f4c',
    width: (Platform.OS === 'android') ? 145 : 150,
    overflow:'hidden',
  },
  white_text: {
    color: 'white'
  },
  icon: {
    flex: 1,
    width: 30,
    height: 30,
    resizeMode: 'cover',
    margin: 10
  },
  title_area: {
    flex: 1.5,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#e87600',
  }
});
