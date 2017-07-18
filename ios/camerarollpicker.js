import React, {Component} from 'react'
import {
  CameraRoll,
  Image,
  Platform,
  StyleSheet,
  View,
  Text,
  Dimensions,
  TouchableOpacity,
  ListView,
  ActivityIndicator,
  InteractionManager,
  NativeModules
} from 'react-native';
import Bar from 'react-native-bar-collapsible';
import Spinner from 'react-native-loading-spinner-overlay';
import MediaItem from './mediaitem';
import _ from "lodash";

const DISPLAY_MORE_MEDIA_STEP_COUNT = 1000;
const MAX_INIT_DISPLAYED_MEDIA_COUNT = 20;

var tempDuration = [];
var isFirstTime = true;
var mDisplayedMediaCount = 0;
var loadMoreActivationTimeOut = null;
var mFetchMediaItemCount = MAX_INIT_DISPLAYED_MEDIA_COUNT;
var previousAlbum = 'Camera Roll';
var Constants = require("./moduleconstants");

var start = 0, end = 0, max_feed = 10, lazy_data = [];

class CameraRollPicker extends Component {
  constructor(props) {
    super(props);
    this.state = {
      is_spinner_visible: false,
      images: [],
      sortedImages: [],
      selected: this.props.selected,
      lastCursor: null,
      loadingMore: false,
      noMore: true,
      dataSource: new ListView.DataSource({rowHasChanged: (r1, r2) => r1 !== r2}),
      selectedAlbum: this.props.selectedAlbum,
      maxPhoto: this.props.maxPhoto,
      maxVideo: this.props.maxVideo,
      photoCount: this.props.photoCount,
      videoCount: this.props.videoCount,
    };
  }

  componentWillMount() {
    isFirstTime = true;
    mDisplayedMediaCount = 0;

    var {width} = Dimensions.get('window');
    var {imageMargin, imagesPerRow, containerWidth} = this.props;

    if(typeof containerWidth != "undefined") {
      width = containerWidth;
    }
    this._imageSize = ( width - 2 ) / imagesPerRow - (imageMargin * 2);
    this.fetch();
  }

  componentWillReceiveProps(nextProps) {
    mDisplayedMediaCount = 0;
    this.setState({
      selected: nextProps.selected,
      selectedAlbum: nextProps.selectedAlbum,
      maxPhoto: nextProps.maxPhoto,
      maxVideo: nextProps.maxVideo,
      photoCount: nextProps.photoCount,
      videoCount: nextProps.videoCount,
    },() => {
      Constants.SELECTED_IMAGES = this.state.selected;
      Constants.MAX_PHOTO = this.state.maxPhoto;
      Constants.MAX_VIDEO = this.state.maxVideo;
      Constants.photo_count = this.state.photoCount;
      Constants.video_count = this.state.videoCount;

      if (this.state.selectedAlbum != previousAlbum) {
        previousAlbum = this.state.selectedAlbum;
        start = 0; end = 0;
        lazy_data = [];
        setTimeout(() => {
          this.fetch();
        }, 1000);
      }
    });
  }

  _onEndReached() {
    this._lazyLoad();
    // if (!this.state.noMore) {
    //    mFetchMediaItemCount += DISPLAY_MORE_MEDIA_STEP_COUNT;
    //    this.fetch();
    // } else {
    //   this.setState({
    //     loadingMore: false
    //   });
    // }
  }

  fetch() {
    if (!this.state.loadingMore) {
      this.setState({is_spinner_visible: true, loadingMore: true}, () => { this._fetch(); });
    }
  }

  _fetch() {
    InteractionManager.runAfterInteractions(() => {
      // CameraRoll.getPhotos(fetchParams).then((data) => this._appendImages(data), (e) => console.log(e));
      NativeModules.RNCamera.getAlbumPhotos({album: this.state.selectedAlbum}).then((data) => {
        this.setState({loadingMore: false, noMore: true}, () => {
          if (!data || data.edges.length == 0) {
            this.props.onChangeAlbum()
          } else {
            this._appendImages(data);
          }
        });
      }, (e) => console.log(e));
    });
  }

  NumberToTextMonth(numMonth){
    var result = "ERROR";
    switch (numMonth) {
      case 0:
        result = "JAN";
        break;
      case 1:
        result = "FEB";
        break;
      case 2:
        result = "MAR";
        break;
      case 3:
        result = "APR";
        break;
      case 4:
        result = "MAY";
        break;
      case 5:
        result = "JUN";
        break;
      case 6:
        result = "JUL";
        break;
      case 7:
        result = "AUG";
        break;
      case 8:
        result = "SEP";
        break;
      case 9:
        result = "OCT";
        break;
      case 10:
        result = "NOV";
        break;
      case 11:
        result = "DEC";
        break;
    }
    return result;
  }

  organizeMediaItems(mMediaList, selected){
    var mSortByDateMedialist = [];
    var currentItemCreatedTime = null;
    var previousItemCreatedTime = null;
    var previousFile = null;
    var tmpMediaList = {
      id: null,
      mediaList: [],
      IsExpanded: false
    };
    var tmpMediaItem = {
      id: 0,
      type: null,
      duration: 0,
      realUrl: null,
      location: {
        latitude: null,
        longitude: null,
      },
      isChecked: false,
      createdAt: null,
      isTrophyAlbum: false,
      filename: null,
      dimensions: {
        height: 0,
        width: 0
      }
    };
      if(!_.isEmpty(mMediaList) && mMediaList.length > 0){
        //SORT BY DATE
        mMediaList.sort((a,b) => {
          return b.node.timestamp - a.node.timestamp;
        });
        //
        for(var i=0; i<mMediaList.length; i++){
          var tmpCurrentItemCreatedTime = new Date(mMediaList[i].node.timestamp*1000);
          currentItemCreatedTime = this.NumberToTextMonth(tmpCurrentItemCreatedTime.getMonth()) + " " + tmpCurrentItemCreatedTime.getDate() + ", " + tmpCurrentItemCreatedTime.getFullYear();
          if(previousFile == null || previousFile.localeCompare(mMediaList[i].node.image.uri) != 0){
            //new file need to be organized
            if(previousItemCreatedTime == null || previousItemCreatedTime != currentItemCreatedTime){
              previousItemCreatedTime = currentItemCreatedTime;
              if(tmpMediaList.id != null){
                mSortByDateMedialist.push(tmpMediaList);
              }
              tmpMediaList = {
                id: null,
                mediaList: [],
                IsExpanded: false
              };
              tmpMediaList.id = currentItemCreatedTime;
              tmpMediaItem = {
                id: i,
                type: (mMediaList[i].node.type == '2') ? "Video" : "Photo",
                duration: (!_.isEmpty(mMediaList[i].node.duration) ? mMediaList[i].node.duration : 0),
                realUrl: mMediaList[i].node.image.uri,
                location: {
                  latitude: ((mMediaList[i].node.location && mMediaList[i].node.location.latitude > 0) ? mMediaList[i].node.location.latitude : null ),
                  longitude: ((mMediaList[i].node.location && mMediaList[i].node.location.longitude > 0) ? mMediaList[i].node.location.longitude : null ),
                },
                isChecked: false,
                createdAt: mMediaList[i].node.timestamp,
                isTrophyAlbum: mMediaList[i].node.isTrophyAlbum,
                filename: mMediaList[i].node.image.filename,
                dimensions: {
                  height: mMediaList[i].node.image.height,
                  width: mMediaList[i].node.image.width,
                }
              };
              //check pre-selected files
              if(selected != null && selected.length > 0){
                for(var k = 0; k < selected.length; k++){
                  var url = null;
                  if(!_.isEmpty(selected[k].realUrl)){
                    url = selected[k].realUrl;
                  } else if(!_.isEmpty(selected[k].image)){
                    url = selected[k].image.uri;
                  }
                  if(tmpMediaItem.realUrl.localeCompare(url) == 0){
                    tmpMediaItem.isChecked = true;
                    tmpMediaList.IsExpanded = true;
                  }
                }
              }
              //
              tmpMediaList.mediaList.push(tmpMediaItem);
            } else if(previousItemCreatedTime.localeCompare(currentItemCreatedTime) ==0){
              tmpMediaItem = {
                id: i,
                type: (mMediaList[i].node.type == '2') ? "Video" : "Photo",
                duration: (!_.isEmpty(mMediaList[i].node.duration) ? mMediaList[i].node.duration : 0),
                realUrl: mMediaList[i].node.image.uri,
                location: {
                  latitude: ((mMediaList[i].node.location && mMediaList[i].node.location.latitude > 0) ? mMediaList[i].node.location.latitude : null ),
                  longitude: ((mMediaList[i].node.location && mMediaList[i].node.location.longitude > 0) ? mMediaList[i].node.location.longitude : null ),
                },
                isChecked: false,
                createdAt: mMediaList[i].node.timestamp,
                isTrophyAlbum: mMediaList[i].node.isTrophyAlbum,
                filename: mMediaList[i].node.image.filename,
                dimensions: {
                  height: mMediaList[i].node.image.height,
                  width: mMediaList[i].node.image.width,
                }
              };              
              // check pre-selected files
              if(selected != null && selected.length > 0){
                for(var k = 0; k < selected.length; k++){
                  var url = null;
                  if(!_.isEmpty(selected[k].realUrl)){
                    url = selected[k].realUrl;
                  } else if(!_.isEmpty(selected[k].image)){
                    url = selected[k].image.uri;
                  }
                  if(tmpMediaItem.realUrl.localeCompare(url) == 0){
                    tmpMediaItem.isChecked = true;
                    tmpMediaList.IsExpanded = true;
                  }
                }
              }
              //
              tmpMediaList.mediaList.push(tmpMediaItem);
            }
            if(i==mMediaList.length-1){
              previousItemCreatedTime = currentItemCreatedTime;
              mSortByDateMedialist.push(tmpMediaList);
              tmpMediaList = {
                id: null,
                mediaList: [],
                IsExpanded: false
              };
            }
            previousFile = mMediaList[i].node.image.uri;
          } else if(i == (mMediaList.length-1) && tmpMediaList.mediaList.length > 0){
            previousItemCreatedTime = currentItemCreatedTime;
            //mSortByDateMedialist.push(tmpMediaList);
            tmpMediaList = {
              id: null,
              mediaList: [],
              IsExpanded: false
            };
          }
        }
      }
      
      return mSortByDateMedialist;
  }

  dividedByRowData(listData, selected){
    var mSortByDateMedialist = this.organizeMediaItems(listData, selected);
    for(var i=0; i<mSortByDateMedialist.length; i++){
      mSortByDateMedialist[i].mediaList = this._nEveryRow(mSortByDateMedialist[i].mediaList, this.props.imagesPerRow);
    }
    return mSortByDateMedialist;
  }

  _appendImages(data) {
    var assets = data.edges;
    var newState = {is_spinner_visible: false};

    if (!data.page_info.has_next_page) {
      newState.noMore = true;
    }

    if (assets.length > 0) {
      newState.lastCursor = data.page_info.end_cursor;
      var mSortByDateMedialist = this.dividedByRowData(assets, this.state.selected);
      newState.images = _.clone(assets);
      newState.sortedImages = mSortByDateMedialist;
      // newState.dataSource = this.state.dataSource.cloneWithRows(mSortByDateMedialist);
    } else {
      var mSortByDateMedialist = this.dividedByRowData(this.state.images, this.state.selected);
      newState.sortedImages = mSortByDateMedialist;
      // newState.dataSource = this.state.dataSource.cloneWithRows(mSortByDateMedialist);
    }
    InteractionManager.runAfterInteractions(() => {
      this.setState(newState, () => {
        this._lazyLoad();
      });
    });
  }

  _lazyLoad() {
    start = end;
    end = end + max_feed;

    if (end >= this.state.sortedImages.length) {
      end = this.state.sortedImages.length;
    }

    for (var i = start; i < end; i++) {
      var item = _.clone(this.state.sortedImages[i]);
      lazy_data.push(item);
    }

    this.setState({
      dataSource: this.state.dataSource.cloneWithRows(lazy_data)
    });
  }

  _arrayDurationIndexOf(uri) {
    var index = -1;
    for (var i = 0; i < tempDuration.length; i++) {
      if (tempDuration[i].uri === uri) {
        return i;
      }
    }

    return index;
  }

  _onGetVideoDuration(uri, duration) {
    var index = this._arrayDurationIndexOf(uri);

    if (index < 0) {
      var item = {
        uri: uri,
        duration: duration
      }
      tempDuration.push(item);
    }
  }

  render() {
    var {scrollRenderAheadDistance, initialListSize, pageSize, removeClippedSubviews, imageMargin, backgroundColor} = this.props;
    return (
      <View
        style={[styles.wrapper, {paddingBottom: 0, backgroundColor: backgroundColor},]}>
        <View>
          <TouchableOpacity
            style={{marginBottom: imageMargin, marginRight: imageMargin}}
            onPress={() => this._gotoCamera()}>
            <Image
              source={require('../img/ico_media_capture.png')}
              style={{height: this._imageSize, width: this._imageSize}} >
            </Image>
          </TouchableOpacity>
        </View>
        <ListView
          enableEmptySections={true}
          style={{flex: 1,}}
          renderFooter={this._renderFooterSpinner.bind(this)}
          onEndReached={this._onEndReached.bind(this)}
          dataSource={this.state.dataSource}
          renderRow={rowData => this._renderRow(rowData)}
        />
        <Spinner visible={this.state.is_spinner_visible}/>
      </View>
    );
  }

  _gotoCamera() {
    this.props.onGoToCamera();
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

  _renderPlayIcon(uri) {
    if (uri.includes('Video')) {
      return (
        <View style={{height: this._imageSize, width: this._imageSize, justifyContent: 'center', alignItems: 'center'}}>
          <Image
            source={require('../img/ico_play_video.png')}
            style={{width: 30, height: 30}}
          />
        </View>
      );
    }

    return <View />;
  }

  _renderRow(rowData) {
    var numberOfItems = 0;
    for(var i=0; i<rowData.mediaList.length; i++){
      numberOfItems += rowData.mediaList[i].length;
    }
    var isShowOnStart = false;
    if(mDisplayedMediaCount <= MAX_INIT_DISPLAYED_MEDIA_COUNT){
      isShowOnStart = true;
      mDisplayedMediaCount += numberOfItems;
    }
    if(rowData.IsExpanded){
      isShowOnStart = true;
    }
    return(
      <View style={{marginBottom: 1}}>
        <Bar
          style={{backgroundColor: '#e87600'}}
          title={rowData.id}
          collapsible={true}
          showOnStart={isShowOnStart}
          iconCollapsed='chevron-up'
          iconOpened='chevron-down'
          >
          {this._renderListRowContent(rowData.mediaList)}
        </Bar>
      </View>
    );
  }

  _renderListRowContent(mediaList){
    return mediaList.map((item, key) => {
      if (item == null) {
        return <View></View>;
      }
      return (
        <View style={styles.row}>
          {this._renderRowContent(item)}
        </View>
      );
    });
  }

  _renderRowContent(rowDataMediaList){
    return rowDataMediaList.map((item, key) => {
      if (item == null) {
        return <View></View>;
      }
      return this._renderImage(item);
    });
  }

  _renderImage(item) {
    var {selectedMarker, imageMargin} = this.props;
    if (this._arrayObjectIndexOf(this.state.selected, item.realUrl) > -1) {
      item.isChecked = true;
    } else {
      item.isChecked = false;
    }

    return (
      <MediaItem
        item={item}
        imageSize={this._imageSize}
        imageMargin={imageMargin}
        selectedMarker={selectedMarker}
        onSelectedImages={(item) => {this.props.onSelectedImages(item)}}
      />
    );
  }

  _renderFooterSpinner() {
    if (!this.state.noMore) {
      return <ActivityIndicator
                color="black"
                size="large"
                style={{ flex: 1 }}
                />;
    } else {
      return null;
    }
  }

  _nEveryRow(data, n) {
    var result = [],
        temp = [];

    for (var i = 0; i < data.length; ++i) {
      if (i > 0 && i % n === 0) {
        result.push(temp);
        temp = [];
      }
      temp.push(data[i]);
    }

    if (temp.length > 0) {
      result.push(temp);
    }

    return result;
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

CameraRollPicker.propTypes = {
  scrollRenderAheadDistance: React.PropTypes.number,
  initialListSize: React.PropTypes.number,
  pageSize: React.PropTypes.number,
  removeClippedSubviews: React.PropTypes.bool,
  groupTypes: React.PropTypes.oneOf([
    'Album',
    'All',
    'Event',
    'Faces',
    'Library',
    'PhotoStream',
    'SavedPhotos',
  ]),
  maximum: React.PropTypes.number,
  assetType: React.PropTypes.oneOf([
    'Photos',
    'Videos',
    'All',
  ]),
  imagesPerRow: React.PropTypes.number,
  imageMargin: React.PropTypes.number,
  containerWidth: React.PropTypes.number,
  callback: React.PropTypes.func,
  selected: React.PropTypes.array,
  selectedMarker: React.PropTypes.element,
  backgroundColor: React.PropTypes.string,
}

CameraRollPicker.defaultProps = {
  max_video:1,
  max_photo: 10,
  max_duration: 30,
  scrollRenderAheadDistance: 500,
  initialListSize: 20,
  pageSize: 20,
  removeClippedSubviews: true,
  groupTypes: 'SavedPhotos',
  maximum: 15,
  imagesPerRow: 3,
  imageMargin: 5,
  assetType: 'Photos',
  backgroundColor: 'white',
  selected: [],
  callback: function(selectedImages, currentImage) {
    console.log(currentImage);
    console.log(selectedImages);
  },
}

export default CameraRollPicker;
