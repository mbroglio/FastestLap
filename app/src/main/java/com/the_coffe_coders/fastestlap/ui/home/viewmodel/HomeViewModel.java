package com.the_coffe_coders.fastestlap.ui.home.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.constructor.ConstructorRepository;
import com.the_coffe_coders.fastestlap.repository.driver.DriverRepository;
import com.the_coffe_coders.fastestlap.repository.result.RaceResultRepository;
import com.the_coffe_coders.fastestlap.repository.weeklyrace.RaceRepository;
import com.the_coffe_coders.fastestlap.ui.event.viewmodel.EventViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModel;
import com.the_coffe_coders.fastestlap.ui.standing.viewmodel.DriverStandingsViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ServiceLocator;

public class HomeViewModel extends ViewModel {
    private static final String TAG = EventViewModel.class.getSimpleName();
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final DriverRepository driverRepository;
    private final ConstructorRepository constructorRepository;
    private MutableLiveData<Result> upcomingEventLiveData;
    private MutableLiveData<Result> driver;
    private MutableLiveData<Result> constructor;
    private MutableLiveData<Result> driverStanding;
    private MutableLiveData<Result> constructorStanding;

    public HomeViewModel(RaceRepository raceRepository, RaceResultRepository raceResultRepository, DriverRepository driverRepository, ConstructorRepository constructorRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
        this.driverRepository = driverRepository;
        this.constructorRepository = constructorRepository;
    }

   public MutableLiveData<Result> getLastRace(long lastUpdate) {
      // ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchLastRace(0);
       return raceRepository.fetchLastRace(0);
   }

   public MutableLiveData<Result> getNextRace(long lastUpdate) {
       //ServiceLocator.getInstance().getRaceRepository(getActivity().getApplication(), false).fetchNextRace(0);
       return raceRepository.fetchNextRace(0);
   }

   public MutableLiveData<Result> getDriverStanding(long lastUpdate) {
       return new ViewModelProvider(this, new DriverStandingsViewModelFactory(ServiceLocator.getInstance().getDriverRepository(getActivity().getApplication(), false))).get(DriverStandingsViewModel.class);
   }




}
