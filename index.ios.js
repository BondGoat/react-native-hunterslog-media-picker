'use strict';

/*<CameraRollPicker
  callback={this._getSelectedImages}
  groupTypes : The group where the photos will be fetched, one of 'Album', 'All', 'Event', 'Faces', 'Library', 'PhotoStream' and 'SavedPhotos'. (Default: SavedPhotos)
  assetType : The asset type, one of 'Photos', 'Videos' or 'All'. (Default: Photos)
  selected : Already be selected images array. (Default: [])
  maximum : Maximum number of selected images. (Default: 15)
  imagesPerRow : Number of images per row. (Default: 3)
  imageMargin : Margin size of one image. (Default: 5)
  containerWidth : Width of camer roll picker container. (Default: device width)
  selectedMarker : Custom selected image marker component. (Default: checkmark).
  backgroundColor : Set background color. (Default: white).
/>*/

import React, {Component} from 'react';
import {
  CameraRoll,
  View,
  Image,
  StyleSheet,
} from 'react-native';
import CameraRollPicker from './ios/camerarollpicker';

export default class MediaHunterslogPicker extends Component {

  constructor(props) {
    super(props);
    this.state = {
      is_spinner_visible: false,
      selectedImages: this.props.selectedImages,
    }
  }

  componentWillReceiveProps(nextProps) {
    this.setState({selectedImages: nextProps.selectedImages});
  }

  _getSelectedImages(currentImage) {
    this.props.onSelectedImages(currentImage);
  }

  _goToCamera() {
    this.props.onGoToCamera();
  }

  render() {
    return (
      <View style={{flex: 1}}>
        <CameraRollPicker
          groupTypes='All'
          assetType='All'
          selected={this.props.selectedImages}
          maximum={10}
          imagesPerRow={3}
          imageMargin={2}
          selectedMarker={
            <View style={{flex: 1}}>
              <Image style={styles.marker} source={require('./img/ico_checked.png')}/>
            </View>
          }
          onGoToCamera={this._goToCamera.bind(this)}
          onSelectedImages={this._getSelectedImages.bind(this)}
        />

      </View>
    )
  }
}

var styles = StyleSheet.create({
  marker: {
    position: 'absolute',
    bottom: 5,
    backgroundColor: 'transparent',
    width: 20,
    height: 20,
    right: 5
  }
});
