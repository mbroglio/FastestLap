package com.the_coffe_coders.fastestlap.source.track;

import com.the_coffe_coders.fastestlap.repository.track.TrackCallback;

public interface TrackDataSource {
    void getTrack(String trackId, TrackCallback callback);
}
