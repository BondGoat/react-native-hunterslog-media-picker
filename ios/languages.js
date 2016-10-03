/*
 * This variable is used as language initiation
 * Used for multi language support
 */

module.exports = {
  app_language: 'en',

  en: {
    txt_add: 'ADD',
    txt_choose_media: 'CHOOSE MEDIA',
    txt_ok: 'OK',

    txt_warn_no_media: 'Please choose at least one picture or video',
    txt_max_video: 'The maximum number of video is #V',
    txt_max_photo: 'The maximum number of pictures are #P',
    txt_one_type: 'Only 1 type of media is allowed in a Post'
  },
  se: {

  },

  getAppLanguage() {
    switch(this.app_language) {
      case 'en': return this.en;
      case 'se': return this.se;
    }
  }

};
