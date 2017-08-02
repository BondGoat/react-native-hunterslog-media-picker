//
//  RNCamera.m
//  RNCamera
//
//  Created by DC5 Admin (MACMINI032) on 10/11/16.
//  Copyright © 2016 DC5 Admin (MACMINI032). All rights reserved.
//

#import "RNCamera.h"
#import "RCTConvert.h"
#import <RCTUtils.h>
#import <Photos/Photos.h>

@implementation RNCamera

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

RCTResponseSenderBlock callback, callbackPlayback;
BOOL hasNextPage = YES;

RCT_EXPORT_METHOD(showCamera: (int) maxVideoDuration
                  isCaptureVideo:(BOOL) isCaptureVideo
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
        if (isCaptureVideo)
            picker.mediaTypes = [[NSArray alloc] initWithObjects: (NSString *) kUTTypeMovie, (NSString *)kUTTypeImage, nil];
        else
            picker.mediaTypes = [[NSArray alloc] initWithObjects: (NSString *)kUTTypeImage, nil];
        
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
    
    if ([trimmedFilename containsString:@".MOV"])
        trimmedFilename = [trimmedFilename stringByReplacingOccurrencesOfString:@".MOV" withString:@"_trimmed.mp4"];
    else
        trimmedFilename = [NSString stringWithFormat:@"%@%@", trimmedFilename, @"_trimmed.mp4"];
    
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

RCT_EXPORT_METHOD(getAlbumPhotos:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [RNCamera authorize:^(BOOL authorized) {
        if (authorized) {
            NSMutableArray<NSDictionary<NSString *, id> *> *assets = [NSMutableArray new];
            
            PHFetchResult<PHAssetCollection *> *collections =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeSmartAlbum
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            PHFetchResult<PHAssetCollection *> *collectionsUser =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeAlbum
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            PHFetchResult<PHAssetCollection *> *collectionsMoment =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeMoment
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            
            [assets addObjectsFromArray:[self getAllPhotos:collections options:options]];
            [assets addObjectsFromArray:[self getAllPhotos:collectionsUser options:options]];
            [assets addObjectsFromArray:[self getAllPhotos:collectionsMoment options:options]];
            
            NSSortDescriptor *sortDescriptor;
            sortDescriptor = [[NSSortDescriptor alloc] initWithKey:@"timestamp" ascending:NO];
            NSArray *sortedArray = [assets sortedArrayUsingDescriptors:@[sortDescriptor]];
    
            RCTResolvePromise(resolve, sortedArray, hasNextPage);
        } else {
            NSString *errorMessage = @"Access Photos  Permission Denied";
            NSError *error = RCTErrorWithMessage(errorMessage);
            reject(@(error.code), errorMessage, error);
        }
    }];
}

- (NSMutableArray<NSDictionary<NSString *, id> *> *) getAllPhotos:(PHFetchResult<PHAssetCollection *> *)collections options:(NSDictionary *)options
{
    NSMutableArray<NSDictionary<NSString *, id> *> *assets = [NSMutableArray new];
    [collections enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull album, NSUInteger idx, BOOL * _Nonnull stop) {
        
        NSString *selectedAlbum = [RCTConvert NSString:options[@"album"]];
        
        if (album && [selectedAlbum isEqualToString:album.localizedTitle]) {
            [assets addObjectsFromArray:[self getPhotos:album options:options]];
        }
    }];
    return assets;
}

- (NSMutableArray<NSDictionary<NSString *, id> *> *) getPhotos:(PHAssetCollection *)album options:(NSDictionary *)options
{
    NSMutableArray<NSDictionary<NSString *, id> *> *assets = [NSMutableArray new];
    unsigned long start = [RCTConvert NSUInteger:options[@"start"]];
    unsigned long end = [RCTConvert NSUInteger:options[@"end"]];
    bool isCaptureVideo = [RCTConvert NSUInteger:options[@"isCaptureVideo"]];
    
    PHFetchOptions *fetchOptions = [[PHFetchOptions alloc] init];
    fetchOptions.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:NO]];
    
    //    Get all photos
    PHFetchResult *allMediaResult = [PHAsset fetchAssetsInAssetCollection:album options:fetchOptions];
    
    //   Get assets from the PHFetchResult object
    [allMediaResult enumerateObjectsUsingBlock:^(PHAsset *asset, NSUInteger idx, BOOL *stop) {
        if (idx == allMediaResult.count - 1) {
            hasNextPage = NO;
        } else {
            hasNextPage = YES;
        }
        
        if (idx > end) {
            *stop = YES;
        }
        if (!isCaptureVideo && asset.mediaType == PHAssetMediaTypeVideo) {
            return;
        }
        if (asset && idx >= start && idx <= end) {
            CLLocation *loc = asset.location;
            NSDate *date = asset.creationDate;
            
            NSString *uri = [NSString stringWithFormat:@"ph://%@", asset.localIdentifier];
            
            if (asset.mediaType == PHAssetMediaTypeVideo) {
                uri = [NSString stringWithFormat:@"%@%@%@",@"assets-library://asset/asset.MOV?id=", [asset.localIdentifier.pathComponents firstObject], @"&ext=MOV"];
            }
        
            [assets addObject:@{
                                @"node": @{
                                        @"type": @(asset.mediaType),
                                        @"group_name": album.localizedTitle,
                                        @"image": @{
                                                @"uri": uri,
                                                @"filename" : asset.localIdentifier,
                                                @"height": [NSString stringWithFormat:@"%lu", asset.pixelHeight],
                                                @"width": [NSString stringWithFormat:@"%lu", asset.pixelWidth],
                                                @"isStored": @YES,
                                                },
                                        @"timestamp": @(date.timeIntervalSince1970),
                                        @"location": loc ? @{
                                            @"latitude": @(loc.coordinate.latitude),
                                            @"longitude": @(loc.coordinate.longitude),
                                            @"altitude": @(loc.altitude),
                                            @"heading": @(loc.course),
                                            @"speed": @(loc.speed),
                                            } : @{},
                                        }
                                }];
            
        }
    }];
    
    return assets;
}

RCT_EXPORT_METHOD(getAlbumList:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [RNCamera authorize:^(BOOL authorized) {
        if (authorized) {
            PHFetchResult<PHAssetCollection *> *collections =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeSmartAlbum
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            
            PHFetchResult<PHAssetCollection *> *collectionsUser =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeAlbum
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            
            PHFetchResult<PHAssetCollection *> *collectionsMoment =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeMoment
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
            
            if (collections == nil && collectionsUser == nil && collectionsMoment == nil) {
                NSString *errorMessage = @"Cannot access any albums";
                NSError *error = RCTErrorWithMessage(errorMessage);
                reject(@(error.code), errorMessage, error);
            }
            
            NSMutableArray<NSDictionary *> *result = [[NSMutableArray alloc] init];
            
            // Start collecting info
            [result addObjectsFromArray:[self getAlbums:collections options:options]];
            [result addObjectsFromArray:[self getAlbums:collectionsUser options:options]];
            [result addObjectsFromArray:[self getAlbums:collectionsMoment options:options]];
            
            NSSortDescriptor *sortDescriptor;
            sortDescriptor = [[NSSortDescriptor alloc] initWithKey:@"name" ascending:YES];
            NSMutableArray *sortedArray = [[NSMutableArray alloc] init];
            [sortedArray addObjectsFromArray:[result sortedArrayUsingDescriptors:@[sortDescriptor]]];
            
            for (int i = 1; i < sortedArray.count; i++) {
                if ([[sortedArray[i] valueForKey:@"name"] isEqualToString:[sortedArray[i - 1] valueForKey:@"name"]]) {
                    [sortedArray removeObjectAtIndex:i];
                    i--;
                }
            }
    
            resolve(sortedArray);
        } else {
            NSString *errorMessage = @"Access Photos  Permission Denied";
            NSError *error = RCTErrorWithMessage(errorMessage);
            reject(@(error.code), errorMessage, error);
        }
    }];
}

RCT_EXPORT_METHOD(isExistAlbum:(NSString *)albumName
                  callback:(RCTResponseSenderBlock)callback) {
    
    [RNCamera authorize:^(BOOL authorized) {
        if (authorized) {
            PHFetchResult<PHAssetCollection *> *collections =
            [PHAssetCollection fetchAssetCollectionsWithType:PHAssetCollectionTypeSmartAlbum
                                                     subtype:PHAssetCollectionSubtypeAny
                                                     options:nil];
    
            __block BOOL isExist = NO;
            [collections enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                if ([obj.localizedTitle isEqualToString:albumName]) {
                    isExist = YES;
                    *stop = YES;
                }
            }];
            
            if (isExist) {
                callback(@[@"YES"]);
            } else {
                callback(@[@"NO"]);
            }
        } else {
            callback(@[@"NOT AUTHORIZED"]);
        }
    }];
}

- (NSMutableArray<NSDictionary *>*) getAlbums: (PHFetchResult<PHAssetCollection *>*)collections options:(NSDictionary *)options
{
    NSMutableArray<NSDictionary *> *result = [[NSMutableArray alloc] init];
    bool isCaptureVideo = [RCTConvert NSUInteger:options[@"isCaptureVideo"]];
    
    [collections enumerateObjectsUsingBlock:^(PHAssetCollection * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        PHAssetCollectionSubtype type = [obj assetCollectionSubtype];
        if (!isAlbumTypeSupported(type)) {
            return;
        }
        
        if (!isCaptureVideo && [obj.localizedTitle isEqualToString:@"Videos"]) {
            return;
        }
        
        PHFetchOptions *fetchOptions = [[PHFetchOptions alloc] init];
        fetchOptions.sortDescriptors = @[ [NSSortDescriptor sortDescriptorWithKey:@"creationDate" ascending:YES] ];
        
        PHFetchResult *fetchResult = [PHAsset fetchAssetsInAssetCollection:obj options: fetchOptions];
        PHAsset *coverAsset = fetchResult.lastObject;
        
        if (coverAsset) {
            NSDictionary *album = @{@"count": @(fetchResult.count),
                                    @"name": obj.localizedTitle,
                                    // Photos Framework asset scheme ph://
                                    // https://github.com/facebook/react-native/blob/master/Libraries/CameraRoll/RCTPhotoLibraryImageLoader.m
                                    @"cover": [NSString stringWithFormat:@"ph://%@", coverAsset.localIdentifier] };
            [result addObject:album];
        }
    }];
    
    return result;
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

static void RCTResolvePromise(RCTPromiseResolveBlock resolve,
                              NSArray<NSDictionary<NSString *, id> *> *assets,
                              BOOL hasNextPage)
{
    if (!assets.count) {
        resolve(@{
                  @"edges": assets,
                  @"page_info": @{
                          @"has_next_page": @NO,
                          }
                  });
        return;
    }
    resolve(@{
              @"edges": assets,
              @"page_info": @{
                      @"start_cursor": assets[0][@"node"][@"image"][@"uri"],
                      @"end_cursor": assets[assets.count - 1][@"node"][@"image"][@"uri"],
                      @"has_next_page": @(hasNextPage),
                      }
              });
}

static void checkPhotoLibraryConfig()
{
#if RCT_DEV
    if (![[NSBundle mainBundle] objectForInfoDictionaryKey:@"NSPhotoLibraryUsageDescription"]) {
        RCTLogError(@"NSPhotoLibraryUsageDescription key must be present in Info.plist to use camera roll.");
    }
#endif
}

typedef void (^authorizeCompletion)(BOOL);

+ (void)authorize:(authorizeCompletion)completion {
    switch ([PHPhotoLibrary authorizationStatus]) {
        case PHAuthorizationStatusAuthorized: {
            completion(YES);
            break;
        }
        case PHAuthorizationStatusNotDetermined: {
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                [RNCamera authorize:completion];
            }];
            break;
        }
        default: {
            completion(NO);
            break;
        }
    }
}

static BOOL isAlbumTypeSupported(PHAssetCollectionSubtype type) {
    switch (type) {
        case PHAssetCollectionSubtypeSmartAlbumUserLibrary:
        case PHAssetCollectionSubtypeSmartAlbumSelfPortraits:
        case PHAssetCollectionSubtypeSmartAlbumRecentlyAdded:
        case PHAssetCollectionSubtypeSmartAlbumTimelapses:
        case PHAssetCollectionSubtypeSmartAlbumPanoramas:
        case PHAssetCollectionSubtypeSmartAlbumFavorites:
        case PHAssetCollectionSubtypeSmartAlbumScreenshots:
        case PHAssetCollectionSubtypeSmartAlbumBursts:
        case PHAssetCollectionSubtypeAlbumRegular:
        case PHAssetCollectionSubtypeAlbumCloudShared:
        case PHAssetCollectionSubtypeAlbumSyncedEvent:
        case PHAssetCollectionSubtypeAlbumMyPhotoStream:
        case PHAssetCollectionSubtypeAlbumSyncedAlbum:
        case PHAssetCollectionSubtypeAlbumImported:
        case PHAssetCollectionSubtypeAlbumSyncedFaces:
        case PHAssetCollectionSubtypeSmartAlbumVideos:
        case PHAssetCollectionSubtypeSmartAlbumGeneric:
        case PHAssetCollectionSubtypeSmartAlbumAllHidden:
        case PHAssetCollectionSubtypeSmartAlbumLivePhotos:
        case PHAssetCollectionSubtypeSmartAlbumDepthEffect:
        case PHAssetCollectionSubtypeSmartAlbumSlomoVideos:
            return YES;
        default:
            return NO;
    }
}

@end
