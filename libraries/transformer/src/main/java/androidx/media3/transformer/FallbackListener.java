/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media3.transformer;

import static androidx.media3.common.util.Assertions.checkState;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.ListenerSet;
import androidx.media3.common.util.Util;

/**
 * Listener for fallback {@link TransformationRequest TransformationRequests} from the audio and
 * video renderers.
 */
/* package */ final class FallbackListener {

  private final MediaItem mediaItem;
  private final TransformationRequest originalTransformationRequest;
  private final ListenerSet<Transformer.Listener> transformerListeners;

  private TransformationRequest fallbackTransformationRequest;
  private int trackCount;

  /**
   * Creates a new instance.
   *
   * @param mediaItem The {@link MediaItem} to transform.
   * @param transformerListeners The {@linkplain Transformer.Listener listeners} to forward events
   *     to.
   * @param originalTransformationRequest The original {@link TransformationRequest}.
   */
  public FallbackListener(
      MediaItem mediaItem,
      ListenerSet<Transformer.Listener> transformerListeners,
      TransformationRequest originalTransformationRequest) {
    this.mediaItem = mediaItem;
    this.transformerListeners = transformerListeners;
    this.originalTransformationRequest = originalTransformationRequest;
    this.fallbackTransformationRequest = originalTransformationRequest;
  }

  /**
   * Registers an output track.
   *
   * <p>All tracks must be registered before a transformation request is {@linkplain
   * #onTransformationRequestFinalized(TransformationRequest) finalized}.
   */
  public void registerTrack() {
    trackCount++;
  }

  /**
   * Updates the {@link TransformationRequest}, if fallback is applied.
   *
   * <p>Should be called with the final {@link TransformationRequest} for each track, after any
   * track-specific fallback changes have been applied.
   *
   * <p>Fallback is applied if the finalized {@code TransformationRequest} is different from the
   * original {@code TransformationRequest}. If fallback is applied, calls {@link
   * Transformer.Listener#onFallbackApplied(MediaItem, TransformationRequest,
   * TransformationRequest)} once this method has been called for each track.
   *
   * @param transformationRequest The final {@link TransformationRequest} for a track.
   * @throws IllegalStateException If called for more tracks than registered using {@link
   *     #registerTrack()}.
   */
  public void onTransformationRequestFinalized(TransformationRequest transformationRequest) {
    checkState(trackCount-- > 0);

    TransformationRequest.Builder fallbackRequestBuilder =
        fallbackTransformationRequest.buildUpon();
    if (!Util.areEqual(
        transformationRequest.audioMimeType, originalTransformationRequest.audioMimeType)) {
      fallbackRequestBuilder.setAudioMimeType(transformationRequest.audioMimeType);
    }
    if (!Util.areEqual(
        transformationRequest.videoMimeType, originalTransformationRequest.videoMimeType)) {
      fallbackRequestBuilder.setVideoMimeType(transformationRequest.videoMimeType);
    }
    if (transformationRequest.outputHeight != originalTransformationRequest.outputHeight) {
      fallbackRequestBuilder.setResolution(transformationRequest.outputHeight);
    }
    if (transformationRequest.enableHdrEditing != originalTransformationRequest.enableHdrEditing) {
      fallbackRequestBuilder.experimental_setEnableHdrEditing(
          transformationRequest.enableHdrEditing);
    }
    if (transformationRequest.enableRequestSdrToneMapping
        != originalTransformationRequest.enableRequestSdrToneMapping) {
      fallbackRequestBuilder.setEnableRequestSdrToneMapping(
          transformationRequest.enableRequestSdrToneMapping);
    }
    fallbackTransformationRequest = fallbackRequestBuilder.build();

    if (trackCount == 0 && !originalTransformationRequest.equals(fallbackTransformationRequest)) {
      transformerListeners.queueEvent(
          /* eventFlag= */ C.INDEX_UNSET,
          listener ->
              listener.onFallbackApplied(
                  mediaItem, originalTransformationRequest, fallbackTransformationRequest));
      transformerListeners.flushEvents();
    }
  }
}
