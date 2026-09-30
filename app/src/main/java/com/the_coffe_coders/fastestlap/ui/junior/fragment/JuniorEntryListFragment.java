package com.the_coffe_coders.fastestlap.ui.junior.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorEntryListRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.standings.JuniorEntryList;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorEntryListFragment extends Fragment {
    private static final String TAG = "JuniorEntryListFragment";

    private View view;
    private int categoryType = 0; // 0: F2, 1: F3
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout entryListLayout;
    private RecyclerView entryListRecyclerView;
    private TextView contentNotAvailableLayout;
    private LoadingScreen loadingScreen;

    public JuniorEntryListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryType = getArguments().getInt("CATEGORY_TYPE", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_junior_entry_list, container, false);
        setupFragment();
        return view;
    }

    private void setupFragment() {
        entryListLayout = view.findViewById(R.id.entry_list_layout);
        UIUtils.applyWindowInsets(entryListLayout);

        loadingScreen = new LoadingScreen(view, getContext(), null, entryListLayout);
        loadingScreen.showLoadingScreen(false);

        entryListRecyclerView = view.findViewById(R.id.entry_list_recycler_view);
        contentNotAvailableLayout = view.findViewById(R.id.content_not_available_layout);

        juniorCategoryViewModel = new ViewModelProvider(
                this,
                new JuniorCategoryViewModelFactory(requireActivity().getApplication())
        ).get(JuniorCategoryViewModel.class);

        entryListLayout.setOnRefreshListener(() -> {
            if (juniorCategoryViewModel != null) {
                juniorCategoryViewModel.refreshEntryList(categoryType);
            }
            fetchEntryList();
        });

        fetchEntryList();
    }

    private void fetchEntryList() {
        MutableLiveData<Result> entryListLiveData = juniorCategoryViewModel.getEntryList(categoryType);
        if (entryListLiveData == null) {
            showContentNotAvailable();
            return;
        }

        entryListLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }

            if (entryListLayout != null) {
                entryListLayout.setRefreshing(false);
            }

            if (result != null && result.isSuccess()) {
                JuniorEntryList entryList = ((Result.JuniorEntryListSuccess) result).getData();
                if (entryList != null && entryList.getTeams() != null && !entryList.getTeams().isEmpty()) {
                    showContent();
                    entryListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorEntryListRecyclerAdapter adapter = new JuniorEntryListRecyclerAdapter(
                            requireContext(), entryList, categoryType, loadingScreen);
                    entryListRecyclerView.setAdapter(adapter);
                } else {
                    Log.w(TAG, "Entry list is null or empty");
                    showContentNotAvailable();
                }
            } else {
                Log.e(TAG, "Entry list fetch failed");
                showContentNotAvailable();
            }
        });
    }

    private void showContent() {
        if (entryListRecyclerView != null) {
            entryListRecyclerView.setVisibility(View.VISIBLE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.GONE);
        }
    }

    private void showContentNotAvailable() {
        if (entryListRecyclerView != null) {
            entryListRecyclerView.setVisibility(View.GONE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.VISIBLE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}
