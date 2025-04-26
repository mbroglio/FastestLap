package com.the_coffe_coders.fastestlap.repository.track;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

public interface TrackCallback {
    public void onTrackLoaded(Track track);
    public void onError(Exception exception);
}
