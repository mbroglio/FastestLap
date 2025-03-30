package com.the_coffe_coders.fastestlap.repository.track;

import com.the_coffe_coders.fastestlap.domain.grand_prix.Track;

public interface TrackCallback {
    public void onSuccess(Track track);
    public void onFailure(Exception exception);
}
