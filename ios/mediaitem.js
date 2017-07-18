import React, {Component} from 'react'
import {
  CameraRoll,
  Image,
  Platform,
  StyleSheet,
  View,
  Dimensions,
  TouchableOpacity,
  InteractionManager,
} from 'react-native';
import _ from "lodash";

var Constants = require("./moduleconstants");

class MediaItem extends Component {
  constructor(props) {
    super(props);
    this.state = {
      item: this.props.item,
      imageSize: this.props.imageSize,
      imageMargin: this.props.imageMargin,
      selectedMarker: this.props.selectedMarker,
    };
  }

  componentWillReceiveProps(nextProps){
    this.setState({
      item: nextProps.item,
    });
  }

  _arrayObjectIndexOf(array, value) {
    var index = -1;
    for (var i = 0; i < array.length; i++) {
      var comparedItem;
      if (array[i].image) {
        comparedItem = array[i].image.uri;
      } else {
        comparedItem = array[i].realUrl;
      }
      if (comparedItem == value) {
        return i;
      }
    }
    return index;
  }

  _selectImage(item) {
    var selected = _.clone(item);
    if (selected.isChecked || this._arrayObjectIndexOf(Constants.SELECTED_IMAGES, selected.realUrl) > -1) {
      selected.isChecked = false;
    } else {
      if ((selected.type.includes('Photo') && Constants.photo_count >= Constants.MAX_PHOTO) ||
          (selected.type.includes('Video') && Constants.video_count >= Constants.MAX_VIDEO)) {
        selected.isChecked = false;
      } else {
        selected.isChecked = true;
      }
    }

    this.setState({item: selected}, () => {
      this.props.onSelectedImages(selected)
    });
  }

  _renderPlayIcon(uri) {
    if (uri.includes('Video')) {
      return (
        <View style={{height: this.state.imageSize, width: this.state.imageSize, justifyContent: 'center', alignItems: 'center'}}>
          <Image
            source={require('../img/ico_play_video.png')}
            style={{width: 30, height: 30}}
          />
        </View>
      );
    }

    return <View />;
  }

  render() {
    //console.log("_renderImage item.realUrl: " + item.realUrl);
    //var {selectedMarker, imageMargin} = this.props;

    var marker = this.state.selectedMarker ? this.state.selectedMarker :
      <Image
        style={[styles.marker, {width: 25, height: 25, right: this.state.imageMargin + 5},]}
        source={require('../img/circle-check.png')}
      />;

    return (
      <View>
        <TouchableOpacity
          key={this.state.item.id}
          style={{marginBottom: this.state.imageMargin, marginRight: this.state.imageMargin}}
          onPress={event => this._selectImage(this.state.item)}>
          <Image
            source={{uri: this.state.item.realUrl}}
            style={{height: this.state.imageSize, width: this.state.imageSize, margin: this.state.imageMargin}} >
            {this._renderPlayIcon(this.state.item.type)}
            { (this.state.item && this.state.item.isChecked) ? marker : <View /> }
          </Image>
        </TouchableOpacity>
      </View>
    );
  }

}

const styles = StyleSheet.create({
  wrapper:{
    flex: 1,
  },
  row:{
    flexDirection: 'row',
    flex: 1,
  },
  column: {
    flexDirection: 'column',
    flex: 1
  },
  marker: {
    position: 'absolute',
    top: 5,
    backgroundColor: 'transparent',
  },
})

export default MediaItem;
