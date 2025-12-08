package com.the_coffe_coders.fastestlap.repository.junior.entrylist;

import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;

public interface JuniorEntryListCallback {
    void onEntryListLoaded(JuniorEntryList entryList);

    void onError(Exception e);
}
