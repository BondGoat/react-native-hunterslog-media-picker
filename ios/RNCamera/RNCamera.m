//
//  RNCamera.m
//  RNCamera
//
//  Created by DC5 Admin (MACMINI032) on 10/11/16.
//  Copyright © 2016 DC5 Admin (MACMINI032). All rights reserved.
//

#import "RNCamera.h"

@implementation RNCamera

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

RCTResponseSenderBlock callback;

RCT_EXPORT_METHOD(showCamera: (int) maxVideoDuration
                  callback:(RCTResponseSenderBlock)mediaCallback)
{
    callback = mediaCallback;
    
    if ([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        
        UIImagePickerController *picker = [[UIImagePickerController alloc] init];
        picker.delegate = self;
        picker.allowsEditing = YES;
        picker.videoMaximumDuration = maxVideoDuration;
        picker.sourceType = UIImagePickerControllerSourceTypeCamera;
        picker.videoQuality = UIImagePickerControllerQualityTypeMedium;
        picker.mediaTypes = [[NSArray alloc] initWithObjects: (NSString *) kUTTypeMovie, (NSString *)kUTTypeImage, nil];
        
        AppDelegate *delegate = (AppDelegate *)[[UIApplication sharedApplication] delegate];
        dispatch_async(dispatch_get_main_queue(), ^{
            [delegate.window.rootViewController presentViewController:picker animated:YES completion:NULL];
        });
    }
}

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    
    NSString *mediaType = [info valueForKey:UIImagePickerControllerMediaType];
    
    if(CFStringCompare ((__bridge_retained CFStringRef) mediaType, kUTTypeImage, 0) == kCFCompareEqualTo) {
        UIImage *photoTaken = [info objectForKey:@"UIImagePickerControllerOriginalImage"];
        
        //Save Photo to library only if it wasnt already saved i.e. its just been taken
        if (picker.sourceType == UIImagePickerControllerSourceTypeCamera) {
            UIImageWriteToSavedPhotosAlbum(photoTaken, self, @selector(media:didFinishSavingWithError:contextInfo:), nil);
        }
    }
    if (CFStringCompare ((__bridge_retained CFStringRef) mediaType, kUTTypeMovie, 0) == kCFCompareEqualTo) {
        NSString *moviePath = [[info objectForKey:UIImagePickerControllerMediaURL] path];
        if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(moviePath)) {
            UISaveVideoAtPathToSavedPhotosAlbum(moviePath, self,
                                                @selector(media:didFinishSavingWithError:contextInfo:), nil);
        } 
    }
    
    [picker dismissViewControllerAnimated:YES completion:NULL];
    
}

- (void)media:(UIImage *)media didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {

    if (error) {
        callback(@[@"FAIL"]);
    } else {
        callback(@[@"OK"]);
    }
    
    _videoURL = nil;
    _videoController = nil;
}


- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    
    [picker dismissViewControllerAnimated:YES completion:NULL];
    
    _videoURL = nil;
    _videoController = nil;
}

- (void)videoPlayBackDidFinish:(NSNotification *)notification {
    
    // Temporarily do nothing
    
}

@end
