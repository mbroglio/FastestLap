package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.nation.FirebaseNationRepository;

public class NationViewModel extends ViewModel {

    private final FirebaseNationRepository nationRepository;

    public NationViewModel(FirebaseNationRepository nationRepository) {
        this.nationRepository = nationRepository;
    }

    public MutableLiveData<Result> getNation(String nationId) {
        return nationRepository.getNation(nationId);
    }

}
