package com.the_coffe_coders.fastestlap.ui.junior.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarRepository;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListRepository;

public class JuniorCategoryViewModel extends ViewModel {
    private final JuniorCalendarRepository juniorCalendarRepository;
    private final JuniorEntryListRepository juniorEntryListRepository;

    public JuniorCategoryViewModel(JuniorCalendarRepository juniorCalendarRepository, JuniorEntryListRepository juniorEntryListRepository) {
        this.juniorCalendarRepository = juniorCalendarRepository;
        this.juniorEntryListRepository = juniorEntryListRepository;
    }

    public MutableLiveData<Result> getCalendar(int series) {
        switch (series) {
            case 0:
                return juniorCalendarRepository.getCalendar("f2");
            case 1:
                return juniorCalendarRepository.getCalendar("f3");
            default:
                return null;
        }
    }

    public MutableLiveData<Result> getEntryList(int series) {
        switch (series) {
            case 0:
                return juniorEntryListRepository.getEntryList("f2");
            case 1:
                return juniorEntryListRepository.getEntryList("f3");
            default:
                return null;
        }
    }
}
