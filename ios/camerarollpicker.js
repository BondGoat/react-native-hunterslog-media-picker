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
} from 'react-native';

var tempDuration = [];

class CameraRollPicker extends Component {
  constructor(props) {
    super(props);

    this.state = {
      images: [],
      selected: this.props.selected,
      lastCursor: null,
      loadingMore: false,
      noMore: false,
      dataSource: new ListView.DataSource({rowHasChanged: (r1, r2) => r1 !== r2}),
    };
  }

  componentWillMount() {
    var {width} = Dimensions.get('window');
    var {imageMargin, imagesPerRow, containerWidth} = this.props;

    if(typeof containerWidth != "undefined") {
      width = containerWidth;
    }
    this._imageSize = (width - (imagesPerRow + 1) * imageMargin) / imagesPerRow;

    this.fetch();
  }

  componentWillReceiveProps(nextProps) {
    this.setState({
      selected: nextProps.selected,
      dataSource: this.state.dataSource.cloneWithRows(
        this._nEveryRow(this.state.images, this.props.imagesPerRow)
      ),
    });
  }

  fetch() {
    if (!this.state.loadingMore) {
      this.setState({loadingMore: true}, () => { this._fetch(); });
    }
  }

  _fetch() {
    var {groupTypes, assetType} = this.props;

    var fetchParams = {
      first: 1000,
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

    CameraRoll.getPhotos(fetchParams)
      .then((data) => this._appendImages(data), (e) => console.log(e));
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
      var item = {
        node : null
      };
      // Add fake item to create Camera icon at the top
      listData.splice(0,0,item);

      var selectedImages = this.state.selected;
      for(var i = 0; i < selectedImages.length; i++) {
        if (selectedImages[i].image.uri.indexOf('http') >= 0 ||
            selectedImages[i].image.uri.indexOf('https') >= 0) {
          var item = {node: selectedImages[i]};
          listData.splice(1,0,item);
        }
      }

      newState.images = listData;
      newState.dataSource = this.state.dataSource.cloneWithRows(
        this._nEveryRow(newState.images, this.props.imagesPerRow)
      );
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
        style={[styles.wrapper, {padding: imageMargin, paddingRight: 0, backgroundColor: backgroundColor},]}>
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
          renderRow={rowData => this._renderRow(rowData)} />
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

  _renderImage(item) {
    var {selectedMarker, imageMargin} = this.props;

    var marker = selectedMarker ? selectedMarker :
      <Image
        style={[styles.marker, {width: 25, height: 25, right: imageMargin + 5},]}
        source={require('../img/circle-check.png')}
      />;

    if (null === item.node) {
      return (
        <TouchableOpacity
          key={"FIRST"}
          style={{marginBottom: imageMargin, marginRight: imageMargin}}
          onPress={() => this._gotoCamera()}>
          <Image
            source={require('../img/ico_media_capture.png')}
            style={{height: this._imageSize, width: this._imageSize}} >
          </Image>
        </TouchableOpacity>
      );
    }

    return (
      <TouchableOpacity
        key={item.node.image.uri}
        style={{marginBottom: imageMargin, marginRight: imageMargin}}
        onPress={event => this._selectImage(item.node)}>
        <Image
          source={{uri: item.node.image.uri}}
          style={{height: this._imageSize, width: this._imageSize}} >
          {this._renderPlayIcon(item.node.type)}
          { (this._arrayObjectIndexOf(this.state.selected, item.node.image.uri) >= 0) ? marker : null }
        </Image>
      </TouchableOpacity>
    );
  }

  _renderRow(rowData) {
    var items = rowData.map((item) => {
      if (item === null) {
        return null;
      }
      return this._renderImage(item);
    });

    return (
      <View style={styles.row}>
        {items}
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
    }
    return null;
  }

  _onEndReached() {
    if (!this.state.noMore) {
      this.fetch();
    }
  }

  _selectImage(imageNode) {
    var index = this._arrayDurationIndexOf(imageNode.image.uri);
    if (index >= 0) {
      imageNode.duration = tempDuration[index].duration;
    }
    this.props.onSelectedImages(imageNode);
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
      while (temp.length !== n) {
        temp.push(null);
      }
      result.push(temp);
    }

    return result;
  }

  _arrayObjectIndexOf(array, value) {
    var index = -1;
    for (var i = 0; i < array.length; i++) {
      if (array[i] != null && array[i].image.uri == value) {
        return i;
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
