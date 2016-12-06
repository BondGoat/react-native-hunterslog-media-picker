'use-strict'

import React, {
  PropTypes,
  Component
} from 'react';

import {
  View,
  NativeModules,
} from 'react-native';

const { MediaHunterslogPicker } = NativeModules;

module.exports = {
  ...MediaHunterslogPicker,

  showMediaPicker(isCaptureVideo, max_photo, max_video, max_video_duration, selectedList, callback) {
    return MediaHunterslogPicker.showMediaPicker(isCaptureVideo, max_photo, max_video, max_video_duration, selectedList, callback);
  }
}
