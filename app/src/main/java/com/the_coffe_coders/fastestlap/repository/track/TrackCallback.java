package com.the_coffe_coders.fastestlap.repository.track;

import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Track;

public interface TrackCallback {
    void onTrackLoaded(Track track);

    void onError(Exception exception);
}
