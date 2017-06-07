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
} from 'react-native';
import Bar from 'react-native-bar-collapsible';
import _ from "lodash";

var tempDuration = [];
var isFirstTime = true;
var mDisplayedMediaCount = 0;
var loadMoreActivationTimeOut = null;
const DISPLAY_MORE_MEDIA_STEP_COUNT = 20
const MAX_INIT_DISPLAYED_MEDIA_COUNT = 20;
var mFetchMediaItemCount = MAX_INIT_DISPLAYED_MEDIA_COUNT;
class CameraRollPicker extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isLoading: false,
      images: [],
      sortedImages: [],
      selected: this.props.selected,
      lastCursor: null,
      loadingMore: false,
      noMore: false,
      dataSource: new ListView.DataSource({rowHasChanged: (r1, r2) => r1 !== r2}),
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
    console.log("CameraRollPicker componentWillReceiveProps");
    this.setState({
      isLoading: false,
      selected: nextProps.selected,
    });

    InteractionManager.runAfterInteractions(() => {
      this._fetch();
      });
    }

  _onEndReached() {
    console.log("_onEndReached");
    if (!this.state.noMore) {
       mFetchMediaItemCount += DISPLAY_MORE_MEDIA_STEP_COUNT;
       this.fetch();
    }
  }

  fetch() {
    if (!this.state.loadingMore) {
      this.setState({loadingMore: true}, () => { this._fetch(); });
    }
  }

  _fetch() {
    console.log("_fetch");
    console.log("clear old fetch timeout");
    InteractionManager.runAfterInteractions(() => {
      console.log("current fetch count = " + mFetchMediaItemCount);
      var {groupTypes, assetType} = this.props;
    
      var fetchParams = {
        first: mFetchMediaItemCount,
        groupTypes: groupTypes,
        assetType: assetType,
      };

      if (Platform.OS === "android") {
        // not supported in android
        delete fetchParams.groupTypes;
      }

      if (this.state.lastCursor) {
        fetchParams.after = this.state.lastCursor;
      }

      CameraRoll.getPhotos(fetchParams).then((data) => this._appendImages(data), (e) => console.log(e));
    });
  }

  NumberToTextMonth(numMonth){
    var result = "ERROR";
    if(numMonth != null && numMonth != "", numMonth != undefined){
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
    }
    return result;
  }

  organizeMediaItems(mMediaList, selected){
    console.log("organizeMediaItems");
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
        console.log("mMediaList.length = " + mMediaList.length);
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
                tmpMediaList = {
                  id: null,
                  mediaList: [],
                  IsExpanded: false
                };
              }
              tmpMediaList = {
                id: null,
                mediaList: [],
                IsExpanded: false
              };
              tmpMediaList.id = currentItemCreatedTime;
              tmpMediaItem = {
                id: i,
                type: mMediaList[i].node.type,
                duration: (!_.isEmpty(mMediaList[i].node.duration) ? mMediaList[i].node.duration : 0),
                realUrl: mMediaList[i].node.image.uri,
                location: {
                  latitude: ((!_.isEmpty(mMediaList[i].node.location) && !_.isEmpty(mMediaList[i].node.location.latitude)) ? mMediaList[i].node.location.latitude : null ),
                  longitude: ((!_.isEmpty(mMediaList[i].node.location) && !_.isEmpty(mMediaList[i].node.location.longitude)) ? mMediaList[i].node.location.longitude : null ),
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
                  }
                }
              }
              tmpMediaList.mediaList.push(tmpMediaItem);
            } else {
              tmpMediaItem = {
                id: i,
                type: mMediaList[i].node.type,
                duration: (!_.isEmpty(mMediaList[i].node.duration) ? mMediaList[i].node.duration : 0),
                realUrl: mMediaList[i].node.image.uri,
                location: {
                  latitude: mMediaList[i].node.location.latitude,
                  longitude: mMediaList[i].node.location.longitude,
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
                  }
                }
              }
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
            mSortByDateMedialist.push(tmpMediaList);
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
    var newState = {
      loadingMore: false,
    };

    if (!data.page_info.has_next_page) {
      newState.noMore = true;
    }

    if (assets.length > 0) {
      newState.lastCursor = data.page_info.end_cursor;

      var listData = this.state.images.concat(assets);

      var mSortByDateMedialist = this.dividedByRowData(listData, this.state.selected);
      newState.images = listData;      
      newState.sortedImages = mSortByDateMedialist;
      newState.dataSource = this.state.dataSource.cloneWithRows(mSortByDateMedialist);
    } else {
      var mSortByDateMedialist = this.dividedByRowData(this.state.images, this.state.selected);
      newState.sortedImages = mSortByDateMedialist;
      newState.dataSource = this.state.dataSource.cloneWithRows(mSortByDateMedialist);
    }
    this.setState(newState);
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
          scrollRenderAheadDistance={scrollRenderAheadDistance}
          initialListSize={initialListSize}
          pageSize={pageSize}
          removeClippedSubviews={removeClippedSubviews}
          renderFooter={this._renderFooterSpinner.bind(this)}
          onEndReached={this._onEndReached.bind(this)}
          dataSource={this.state.dataSource}
          renderRow={rowData => this._renderRow(rowData)}
        />
      </View>
    );
  }

  _gotoCamera() {
    this.props.onGoToCamera();
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
    var strTitle = rowData.id + " - " + numberOfItems + " item";
    if(numberOfItems > 1){
      strTitle = strTitle + "s";
    }
    var isShowOnStart = false;
    if(mDisplayedMediaCount <= MAX_INIT_DISPLAYED_MEDIA_COUNT){
      isShowOnStart = true;
      mDisplayedMediaCount += numberOfItems;
    }
    if(!isShowOnStart){
      for(var i=0;i<rowData.mediaList.length; i++){
        if(rowData.mediaList[i].isChecked){
          isShowOnStart = true;
          break;
        }
      }
    }
    return(
        <Bar
          style={{backgroundColor: '#e87600'}}
          title={strTitle}
          collapsible={true}
          showOnStart={isShowOnStart}
          iconCollapsed='chevron-up'
          iconOpened='chevron-down'
          >
          {this._renderListRowContent(rowData.mediaList)}
        </Bar>
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

    var marker = selectedMarker ? selectedMarker :
      <Image
        style={[styles.marker, {width: 25, height: 25, right: imageMargin + 5},]}
        source={require('../img/circle-check.png')}
      />;

    return (
	  <View>
		  <TouchableOpacity
			key={item.id}
			style={{marginBottom: imageMargin, marginRight: imageMargin}}
			onPress={event => this._selectImage(item)}>
			<Image
			  source={{uri: item.realUrl}}
			  style={{height: this._imageSize, width: this._imageSize, margin: imageMargin}} >
			  {this._renderPlayIcon(item.type)}
			  { (item.isChecked) ? marker : null }
			</Image>
		  </TouchableOpacity>
      </View>
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

  _selectImage(item) {
    this.setState({isLoading: true}, () => {
      this.props.onSelectedImages(item);
    });
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

  _arrayObjectIndexOf(array, value) {
    var index = -1;
    for (var i = 0; i < array.length; i++) {
      if(!_.isEmpty(array[i])){
        if (!_.isEmpty(array[i].image) && array[i].image.uri.localeCompare(value) == 0) {
        return i;
        } else if (!_.isEmpty(array[i].realUrl) && array[i].realUrl.localeCompare(value) == 0) {
          return i;
        }
      }
    }
    return index;
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
  initialListSize: 1,
  pageSize: 3,
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
