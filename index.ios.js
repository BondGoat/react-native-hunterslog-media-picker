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
  NativeModules
} from 'react-native';
import CameraRollPicker from './ios/camerarollpicker';
import CustomHeader from './ios/customheader';
import AlertDialog from './ios/alert';
import _ from 'lodash';

var Strings = require('./ios/languages');

export default class MediaHunterslogPicker extends Component {

  constructor(props) {
    super(props);
    this.state = {
      selectedImages: [],
      selectedConvertImages: [],
      isShowAlert: false,
      alertMessage: ''
    }
  }

  componentWillMount() {
    this._convertImageNode(this.props.selectedImages);
  }

  _createResizedImage(path, width, height, format, quality, rotation = 0, outputPath = null) {
    if (format !== 'JPEG' && format !== 'PNG') {
      throw new Error('Only JPEG and PNG format are supported by createResizedImage');
    }

    return new Promise((resolve, reject) => {
      NativeModules.ImageResizer.createResizedImage(path, width, height, format, quality, outputPath, (err, resizedPath) => {
        if (err) {
          return reject(err);
        }

        resolve(resizedPath);
      });
    });
  }

  _getSelectedImages(imageNodes, currentImage) {
    this.setState({selectedImages: imageNodes});

  }

  _onCloseGallery() {
    this.props.navigator.pop();
  }

  _createSubmitItem(node, url) {
    var listData = _.clone(this.state.selectedConvertImages);

    var item = {
      Id : listData.length,
      ThumbUrl : url,
      Url : url,
      RealUrl : node.image.uri,
      Location : {Lat : node.location.latitude, Lng : node.location.longitude},
      isTrophyAlbum : false
    }

    listData.push(item);

    console.log("ARR: " + JSON.stringify(listData));
    this.setState({selectedConvertImages: listData});

    this._fetch(++index);
  }

  _fetch(index) {

    if (index < this.state.selectedImages.length) {
      var node = this.state.selectedImages[index];

      var scaleW = node.image.width, scaleH = node.image.height;
      if (node.type.includes('Photo')) {
        if (scaleW > 700) {
          scaleW = 700;
          scaleH = (scaleW * node.image.height) / node.image.width;
          this._createResizedImage(node.image.uri, scaleW, scaleH, 'JPEG', 100, null)
            .then((resizedImageUri) => {
              var url = resizedImageUri;

              this._createSubmitItem(node, url);

            }).catch((err) => {
              console.log(err);
            });
        }
      } else {
        this._createSubmitItem(node, url);
      }
    }
  }

  _onSubmitMedia() {
    if (this.state.selectedImages.length == 0) {
      this.setState({isShowAlert: true, alertMessage: Strings.getAppLanguage().txt_warn_no_media});
    } else {

      this._fetch(0);

      // this.props.submitMedia(selectedMedia);
    }
  }

  _showAlert(message) {
    this.setState({isShowAlert: true, alertMessage: message});
  }

  _convertImageNode() {
    var listData = [];
    var selectedMedia = _.clone(this.state.selectedImages);

    for (var i = 0; i < selectedMedia.length; i++) {
      var item = {
        timestamp: 0,
        group_name: '',
        type: (selectedMedia[i].Url.includes('mov') || selectedMedia[i].Url.includes('mp4') || selectedMedia[i].Url.includes('m4v')) ? "ALAssetTypeVideo" : "ALAssetTypePhoto",
        image:{
          isStored:true,
          height:0,
          uri: selectedMedia[i].RealUrl,
          width:0
        },
        location:{
          speed:0,
          latitude:selectedMedia[i].Location.Lat,
          longitude:selectedMedia[i].Location.Lng,
          heading:0,
          altitude:0
        }
      }
      listData.push(item);
    }

    this.setState({selectedImages: listData});
  }

  _renderAlert() {
    return (
        <AlertDialog
         visible={this.state.isShowAlert}
         message={this.state.alertMessage}
         buttonPositiveTxt={Strings.getAppLanguage().txt_ok}
         onPositivePressed={() => this.setState({isShowAlert: false, alertMessage: ''})}/>
    );
  }

  render() {
    return (
      <View style={{flex: 1}}>
        <CustomHeader
          headerText={Strings.getAppLanguage().txt_choose_media}
          imgIconLeft={require('./img/ico_back.png')}
          onIconLeftPressed={() => {
            this._onCloseGallery();
          }}
          subTextRight={Strings.getAppLanguage().txt_add}
          onIconRightPressed={() => this._onSubmitMedia()}
        />

        <CameraRollPicker
          headerText={this.props.headerText}
          callback={this._getSelectedImages.bind(this)}
          groupTypes='All'
          assetType='All'
          selected={this.state.selectedImages}
          maximum={10}
          imagesPerRow={3}
          imageMargin={2}
          selectedMarker={
            <View style={{flex: 1}}>
              <Image style={styles.marker} source={require('./img/ico_checked.png')}/>
            </View>
          }
          showAlert={this._showAlert.bind(this)}
          max_video={this.props.max_video}
          max_photo={this.props.max_photo}
          max_duration={this.props.max_duration}
        />

        {this._renderAlert()}
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
