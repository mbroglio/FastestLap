package com.the_coffe_coders.fastestlap.ui.junior.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.junior.calendar.JuniorCalendarRepository;
import com.the_coffe_coders.fastestlap.repository.junior.entrylist.JuniorEntryListRepository;
import com.the_coffe_coders.fastestlap.repository.junior.result.JuniorResultRepository;
import com.the_coffe_coders.fastestlap.repository.junior.standings.JuniorStandingsRepository;

public class JuniorCategoryViewModel extends ViewModel {
    private final JuniorCalendarRepository juniorCalendarRepository;
    private final JuniorEntryListRepository juniorEntryListRepository;
    private final JuniorResultRepository juniorResultRepository;
    private final JuniorStandingsRepository juniorStandingsRepository;



    public JuniorCategoryViewModel(
            JuniorCalendarRepository juniorCalendarRepository,
            JuniorEntryListRepository juniorEntryListRepository,
            JuniorResultRepository juniorResultRepository,
            JuniorStandingsRepository juniorStandingsRepository) {
        this.juniorCalendarRepository = juniorCalendarRepository;
        this.juniorEntryListRepository = juniorEntryListRepository;
        this.juniorResultRepository = juniorResultRepository;
        this.juniorStandingsRepository = juniorStandingsRepository;
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

    public MutableLiveData<Result> getResults(int series) {
        switch (series) {
            case 0:
                return juniorResultRepository.getResults("f2");
            case 1:
                return juniorResultRepository.getResults("f3");
            default:
                return null;
        }
    }

    public MutableLiveData<Result> getDriverStandings(int series) {
        switch (series) {
            case 0:
                return juniorStandingsRepository.getDriverStandings("f2");
            case 1:
                return juniorStandingsRepository.getDriverStandings("f3");
            default:
                return null;
        }
    }

    public MutableLiveData<Result> getConstructorStandings(int series) {
        switch (series) {
            case 0:
                return juniorStandingsRepository.getConstructorStandings("f2");
            case 1:
                return juniorStandingsRepository.getConstructorStandings("f3");
            default:
                return null;
        }
    }
}
