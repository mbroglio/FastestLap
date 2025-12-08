package com.the_coffe_coders.fastestlap.source.junior.entryList;

import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListCallback;

public interface JuniorEntryListDataSource {

    void getJuniorEntryList(String series, JuniorEntryListCallback callback);

}
