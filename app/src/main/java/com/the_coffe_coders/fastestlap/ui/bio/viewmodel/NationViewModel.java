package com.the_coffe_coders.fastestlap.ui.bio.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.repository.nation.NationRepository;

public class NationViewModel extends ViewModel {

    private final NationRepository nationRepository;

    public NationViewModel(NationRepository nationRepository) {
        this.nationRepository = nationRepository;
        Log.i("NationViewModel", "NationViewModel created");
    }

    public MutableLiveData<Result> getNation(String nationId) {
        return nationRepository.getNation(nationId);
    }

}
