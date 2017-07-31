module.exports = {
  SELECTED_IMAGES: [],
  photo_count: 0,
  video_count: 0,
  MAX_PHOTO: 0,
  MAX_VIDEO: 0,
  isCaptureVideo: false,
  mediaItemEvents: [],

  addEvent(id, callback) {
    if (this.isCaptureVideo)
      return;

    // for (var i = 0; i < this.mediaItemEvents.length; i++) {
    //   if (this.mediaItemEvents[i].id == id) {
    //     return;
    //   }
    // }

    this.mediaItemEvents.push({id, callback});
  },

  notifyEvents(selected) {
    for (var i = 0; i < this.mediaItemEvents.length; i++) {
      this.mediaItemEvents[i].callback(selected);
    }
  },

  clearEvents() {
    this.mediaItemEvents = [];
  }
};
