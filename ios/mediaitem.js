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

var tempDuration = [];
var isFirstTime = true;
var mDisplayedMediaCount = 0;
var loadMoreActivationTimeOut = null;
const DISPLAY_MORE_MEDIA_STEP_COUNT = 20
const MAX_INIT_DISPLAYED_MEDIA_COUNT = 20;
var mFetchMediaItemCount = MAX_INIT_DISPLAYED_MEDIA_COUNT;
class MediaItem extends Component {
  constructor(props) {
    super(props);
    this.state = {
      item: this.props.item,
      isChecked: this.props.item.isChecked,
      imageSize: this.props.imageSize,
      imageMargin: this.props.imageMargin,
      selectedMarker: this.props.selectedMarker,
      mediaType: this.props.mediaType,
      selectedItemCount: this.props.selectedItemCount,
      maxPhoto: this.props.maxPhoto,
      maxVideo: this.props.maxVideo
    };
  }

  componentWillReceiveProps(nextProps){
    this.setState({
      mediaType: nextProps.mediaType,
      selectedItemCount: nextProps.selectedItemCount
    });
  }

  _selectImage(item) {
    console.log("_selectImage: " + item.realUrl);
    console.log("this.state.mediaType: " + this.state.mediaType);
    console.log("this.state.selectedItemCount: " + this.state.selectedItemCount);
    var mediaType = 2; // Photo or Video;
    if(item){
      if(item.type.includes('Photo')){
        mediaType = 0; // Photo
      } else {
        mediaType = 1; // Video
      }

      if((mediaType == this.props.mediaType || this.props.mediaType == 2) &&
         ((mediaType == 0 && this.props.selectedItemCount < this.state.maxPhoto) ||
          (mediaType == 1 && this.props.selectedItemCount < this.state.maxVideo) ||
          this.props.mediaType == 2)){
        item.isChecked = !item.isChecked;
        this.setState({
          isChecked: !this.state.isChecked
        }, () => {
          this.props.onSelectedImages(item);
        });
      } else {
        this.props.onSelectedImages(item);
      }
    }
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
            { (this.state.isChecked) ? marker : null }
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