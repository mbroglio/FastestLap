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
import com.the_coffe_coders.fastestlap.adapter.junior.JuniorCalendarRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.junior.calendar.JuniorCalendar;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModel;
import com.the_coffe_coders.fastestlap.ui.junior.viewmodel.JuniorCategoryViewModelFactory;
import com.the_coffe_coders.fastestlap.util.ui.LoadingScreen;
import com.the_coffe_coders.fastestlap.util.ui.UIUtils;

public class JuniorCalendarFragment extends Fragment {
    private static final String TAG = "JuniorCalendarFragment";

    private View view;
    private int categoryType = 0; // 0: F2, 1: F3
    private JuniorCategoryViewModel juniorCategoryViewModel;
    private SwipeRefreshLayout calendarLayout;
    private RecyclerView calendarRecyclerView;
    private TextView contentNotAvailableLayout;
    private LoadingScreen loadingScreen;

    public JuniorCalendarFragment() {
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
        view = inflater.inflate(R.layout.fragment_junior_calendar, container, false);
        setupFragment();
        return view;
    }

    private void setupFragment() {
        calendarLayout = view.findViewById(R.id.calendar_layout);
        UIUtils.applyWindowInsets(calendarLayout);

        loadingScreen = new LoadingScreen(view, getContext(), null, calendarLayout);
        loadingScreen.showLoadingScreen(false);

        calendarRecyclerView = view.findViewById(R.id.calendar_recycler_view);
        contentNotAvailableLayout = view.findViewById(R.id.content_not_available_layout);

        juniorCategoryViewModel = new ViewModelProvider(
                this,
                new JuniorCategoryViewModelFactory(requireActivity().getApplication())
        ).get(JuniorCategoryViewModel.class);

        calendarLayout.setOnRefreshListener(() -> {
            if (juniorCategoryViewModel != null) {
                juniorCategoryViewModel.refreshCalendar(categoryType);
            }
            fetchCalendar();
        });

        fetchCalendar();
    }

    private void fetchCalendar() {
        MutableLiveData<Result> calendarLiveData = juniorCategoryViewModel.getCalendar(categoryType);
        if (calendarLiveData == null) {
            showContentNotAvailable();
            return;
        }

        calendarLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                return;
            }

            if (calendarLayout != null) {
                calendarLayout.setRefreshing(false);
            }

            if (result != null && result.isSuccess()) {
                JuniorCalendar calendar = ((Result.JuniorCalendarSuccess) result).getData();
                if (calendar != null && calendar.getEvents() != null && !calendar.getEvents().isEmpty()) {
                    showContent();
                    calendarRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                    JuniorCalendarRecyclerAdapter adapter = new JuniorCalendarRecyclerAdapter(requireContext(), calendar, categoryType);
                    calendarRecyclerView.setAdapter(adapter);
                } else {
                    Log.w(TAG, "Calendar is null or empty");
                    showContentNotAvailable();
                }
            } else {
                Log.e(TAG, "Calendar fetch failed");
                showContentNotAvailable();
            }
        });
    }

    private void showContent() {
        if (calendarRecyclerView != null) {
            calendarRecyclerView.setVisibility(View.VISIBLE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.GONE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }

    private void showContentNotAvailable() {
        if (calendarRecyclerView != null) {
            calendarRecyclerView.setVisibility(View.GONE);
        }
        if (contentNotAvailableLayout != null) {
            contentNotAvailableLayout.setVisibility(View.VISIBLE);
        }
        if (loadingScreen != null) {
            loadingScreen.hideLoadingScreen();
        }
    }
}
