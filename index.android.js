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

  showMediaPlayer(max_photo, max_video, max_video_duration, callback) {
    return MediaHunterslogPicker.showMediaPlayer(max_photo, max_video, max_video_duration, callback);
  }
}
