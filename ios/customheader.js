/*
 * Common header area which can be reused in other Views
 */
import React, { Component } from 'react';
import {
 StyleSheet,
 View,
 Text,
 Image,
 Platform
} from 'react-native';
import Button from 'react-native-button';

export default class CustomHeader extends Component {

  constructor(props) {
    super(props);
    this.state = {
      headerText: this.props.headerText,
      subTextLeft: this.props.subTextLeft,
      subTextRight: this.props.subTextRight,
      imgIconLeft: this.props.imgIconLeft,
      imgIconRight: this.props.imgIconRight
    }
  }

  _onIconLeftPressed() {
    this.props.onIconLeftPressed();
  }

  _onIconRightPressed() {
    this.props.onIconRightPressed();
  }
  _renderRightIcon() {
    if (this.state.imgIconRight) {
      return (
        <Button containerStyle={styles.button_right_icon} onPress={this._onIconRightPressed.bind(this)}>
          <Image style={styles.img} source={this.state.imgIconRight} />
        </Button>
      );
    }
    if (this.state.subTextRight) {
      return (
        <Button
          containerStyle={styles.button_right_icon}
          style={styles.button_text}
          onPress={this._onIconRightPressed.bind(this)}>
          {this.state.subTextRight}
        </Button>
      );
    }

    return (<View />);
  }

  _renderLeftIcon() {
    if (this.state.imgIconLeft) {
      return (
        <Button containerStyle={styles.button_left_icon} onPress={this._onIconLeftPressed.bind(this)}>
          <Image style={styles.img} source={this.state.imgIconLeft} />
        </Button>
      );
    }
    if (this.state.imgIconLeft) {
      return (
        <Button
          containerStyle={styles.button_left_icon}
          style={styles.button_text}
          onPress={this._onIconLeftPressed.bind(this)}>
          {this.state.subTextLeft}
        </Button>
      );
    }

    return (<View />);
  }

  render() {
    return (
      <View style={styles.header}>
        {this._renderLeftIcon()}
        <View style={styles.header_text_container}>
          <Text style={[styles.header_text]}>{this.state.headerText}</Text>
        </View>
        {this._renderRightIcon()}
      </View>
    );
  }
}

var styles = StyleSheet.create({
  header: {
    backgroundColor: '#e87600',
    height: 60,
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10
  },
  header_text_container: {
    marginTop: (Platform.OS == 'ios') ? 10 : 0,
    flex: 15,
    alignItems: 'center',
    justifyContent: 'center',
  },
  header_text: {
    fontSize: 18,
    color: 'white'
  },
  img: {
    flex: 1,
    resizeMode: 'contain',
    width: 15,
    height: 15,
    justifyContent: 'center',
    alignItems: 'center'
  },
  button_left_icon: {
    marginTop: (Platform.OS == 'ios') ? 10 : 0,
    flex: 1,
    width: 30,
    height: 30,
    justifyContent: 'center'
  },
  button_right_icon: {
    marginTop: (Platform.OS == 'ios') ? 10 : 0,
    flex: 1,
    width: 30,
    height: 30,
    justifyContent: 'center'
  },
  button_text: {
    color: 'white',
    fontWeight: 'normal',
    fontSize: 18
  }

});
