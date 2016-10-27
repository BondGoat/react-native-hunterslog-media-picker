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

RCTResponseSenderBlock callback, callbackPlayback;

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

RCT_EXPORT_METHOD(trimVideoFromStartTime:(CGFloat)startTime toEndTime:(CGFloat)endTime withContentURL:(NSURL*)contenturl andCompletionHandler:(RCTResponseSenderBlock)result)
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    
    NSString *trimmedFilename =  [contenturl lastPathComponent];
    
    trimmedFilename = [trimmedFilename stringByReplacingOccurrencesOfString:@".MOV" withString:@"_trimmed.mp4"];
    
    NSString *tempPath = [documentsDirectory stringByAppendingFormat:@"/%@", trimmedFilename];
    BOOL videoExists = [[NSFileManager defaultManager] fileExistsAtPath:tempPath];
    NSError * errorRef;
    if (videoExists) {
        [[NSFileManager defaultManager] removeItemAtPath:tempPath error:&errorRef];
        if (errorRef) {
            
        }
    }
    
    AVAsset *asset = [[AVURLAsset alloc] initWithURL:contenturl options:nil];
    AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]initWithAsset:asset presetName:AVAssetExportPresetHighestQuality];
    
    CMTime start = CMTimeMakeWithSeconds(startTime, 1);
    CMTime duration = CMTimeMakeWithSeconds((endTime - startTime), 1);
    CMTimeRange range = CMTimeRangeMake(start, duration);
    
    exportSession.timeRange = range;
    exportSession.outputURL = [NSURL fileURLWithPath:tempPath];
    exportSession.outputFileType = AVFileTypeMPEG4;
    
    [exportSession exportAsynchronouslyWithCompletionHandler:^(void)
     {         
         result(@[[NSNull null],tempPath]);
     }];
}

RCT_EXPORT_METHOD(generateThumbnail:(NSURL *)filepath width:(NSInteger)width height:(NSInteger)height timeStep:(NSInteger)picsCount
                  callback:(RCTResponseSenderBlock)callback){
    NSMutableArray * imagesArr = [[NSMutableArray alloc] init];
    
    float THUMBNAIL_FREQUENCY = 1;
    CGSize thumbnailSize = CGSizeMake(width, height);
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSError *error;
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *documentsDirectory = [paths objectAtIndex:0];
    
    NSString *txtPath = [documentsDirectory stringByAppendingPathComponent:@"video.MOV"];
    
    if ([fileManager fileExistsAtPath:txtPath] == YES) {
        [fileManager removeItemAtPath:txtPath error:&error];
    }
    
    [fileManager copyItemAtPath:filepath toPath:txtPath error:&error];
    
    AVURLAsset *asset=[[AVURLAsset alloc] initWithURL:filepath options:nil];
    AVAssetImageGenerator *generator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
    generator.appliesPreferredTrackTransform=TRUE;
    
    generator.requestedTimeToleranceAfter = kCMTimeZero;
    generator.requestedTimeToleranceBefore = kCMTimeZero;
    
    if ([self isRetina]){
        generator.maximumSize = CGSizeMake(thumbnailSize.width*[[UIScreen mainScreen]scale], thumbnailSize.height*[[UIScreen mainScreen]scale]);
    } else {
        generator.maximumSize = CGSizeMake(thumbnailSize.width, thumbnailSize.height);
    }
    
    CMTime actualTime;
    
    int64_t i = 0;
    
    while(i<picsCount){
        CMTime timeFrame = CMTimeMake(i, THUMBNAIL_FREQUENCY);
        CGImageRef halfWayImage = [generator copyCGImageAtTime:timeFrame actualTime:&actualTime error:&error];
        UIImage *generatedImage;
        if ([self isRetina]){
            generatedImage = [[UIImage alloc] initWithCGImage:halfWayImage scale:[[UIScreen mainScreen]scale] orientation:UIImageOrientationUp];
        } else {
            generatedImage = [[UIImage alloc] initWithCGImage:halfWayImage];
        }
        
        NSData *data = UIImagePNGRepresentation(generatedImage);
        NSString *str = @"data:image/jpg;base64,";
        NSString *base = [data base64EncodedStringWithOptions:NSDataBase64EncodingEndLineWithLineFeed];
        
        str = [str stringByAppendingString:base];
        
        [imagesArr addObject:str];
        CGImageRelease(halfWayImage);
        i = i+1;
    }
    if(imagesArr) {
        callback(@[[NSNull null],imagesArr]);
    } else {
        callback(@[@"Error", @[] ]);
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
}


- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    
    [picker dismissViewControllerAnimated:YES completion:NULL];
    
}

-(BOOL)isRetina{
    return ([[UIScreen mainScreen] respondsToSelector:@selector(displayLinkWithTarget:selector:)] &&
            ([UIScreen mainScreen].scale > 1.0));
}


@end
