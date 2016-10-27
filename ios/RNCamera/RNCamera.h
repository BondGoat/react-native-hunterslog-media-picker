//
//  RNCamera.h
//  RNCamera
//
//  Created by DC5 Admin (MACMINI032) on 10/11/16.
//  Copyright © 2016 DC5 Admin (MACMINI032). All rights reserved.
//

#import <UIKit/UIKit.h>
#import <MediaPlayer/MediaPlayer.h>
#import <MobileCoreServices/MobileCoreServices.h>
#import <AVFoundation/AVFoundation.h>
#import <AVFoundation/AVAsset.h>
#include <stdlib.h>
#import "RCTBridge.h"
#import "AppDelegate.h"

@interface RNCamera : UIViewController <RCTBridgeModule, UIImagePickerControllerDelegate, UINavigationControllerDelegate>

@end
