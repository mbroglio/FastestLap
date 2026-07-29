package com.the_coffe_coders.fastestlap.ui.live.fragment;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.adapter.f1.RaceControlRecyclerAdapter;
import com.the_coffe_coders.fastestlap.domain.Result;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.RaceControlMessage;
import com.the_coffe_coders.fastestlap.domain.f1.livetiming.TeamRadioMessage;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModel;
import com.the_coffe_coders.fastestlap.ui.live.viewmodel.LiveViewModelFactory;

import java.util.List;

/**
 * Fragment che visualizza in una {@link RecyclerView} la lista mista di
 * messaggi Race Control e Team Radio dell'ultima sessione OpenF1.
 *
 * <p>Il {@link LiveViewModel} è legato al ciclo di vita dell'Activity
 * ({@code requireActivity()}) così da essere condiviso con eventuali altri
 * Fragment della stessa Activity senza effettuare chiamate di rete duplicate.</p>
 */
public class RaceControlFragment extends Fragment {

    private static final String TAG = "RaceControlFragment";

    private LiveViewModel liveViewModel;
    private RaceControlRecyclerAdapter adapter;

    public RaceControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_race_control, container, false);

        // ── RecyclerView setup ────────────────────────────────────
        RecyclerView recyclerView = view.findViewById(R.id.race_control_recycler_view);
        adapter = new RaceControlRecyclerAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // ── ViewModel (scoped to Activity so it survives tab switches) ──
        liveViewModel = new ViewModelProvider(
                requireActivity(),
                new LiveViewModelFactory(requireActivity().getApplication())
        ).get(LiveViewModel.class);

        observeRaceControl();
        observeTeamRadio();

        return view;
    }

    // ─────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────

    private void observeRaceControl() {
        liveViewModel.getRaceControlMessages().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                Log.d(TAG, "[RaceControl] Loading…");
                return;
            }
            if (result instanceof Result.RaceControlSuccess) {
                List<RaceControlMessage> messages = ((Result.RaceControlSuccess) result).getData();
                Log.d(TAG, "[RaceControl] Received " + messages.size() + " messages");
                adapter.submitRaceControlMessages(messages);
            } else if (result instanceof Result.Error) {
                Log.e(TAG, "[RaceControl] Error: " + result.getError());
            }
        });
    }

    private void observeTeamRadio() {
        liveViewModel.getTeamRadioMessages().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Loading) {
                Log.d(TAG, "[TeamRadio] Loading…");
                return;
            }
            if (result instanceof Result.TeamRadioSuccess) {
                List<TeamRadioMessage> messages = ((Result.TeamRadioSuccess) result).getData();
                Log.d(TAG, "[TeamRadio] Received " + messages.size() + " messages");
                adapter.submitTeamRadioMessages(messages);
            } else if (result instanceof Result.Error) {
                Log.e(TAG, "[TeamRadio] Error: " + result.getError());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle – orientation lock
    // ─────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}