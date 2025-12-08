package com.the_coffe_coders.fastestlap.ui.junior.viewmodel.f3;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.JuniorEntryList;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarRepository;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListRepository;

public class F3ViewModel extends ViewModel {
    private final MutableLiveData<JuniorCalendar> calendarLiveData = new MutableLiveData<>();
    private final MutableLiveData<JuniorEntryList> entryListLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final JuniorCalendarRepository juniorCalendarRepository;
    private final JuniorEntryListRepository juniorEntryListRepository;


    public F3ViewModel(JuniorCalendarRepository juniorCalendarRepository, JuniorEntryListRepository juniorEntryListRepository) {
        this.juniorCalendarRepository = juniorCalendarRepository;
        this.juniorEntryListRepository = juniorEntryListRepository;
    }

    public MutableLiveData<Result> getCalendar(String series) {
        return juniorCalendarRepository.getCalendar(series);
    }

    public MutableLiveData<Result> getEntryList(String series) {
        return juniorEntryListRepository.getEntryList(series);
    }


}
