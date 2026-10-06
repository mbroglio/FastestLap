package com.the_coffe_coders.fastestlap.util.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.f1.result.RaceResultFastestLap;
import com.the_coffe_coders.fastestlap.domain.f1.result.Stint;
import com.the_coffe_coders.fastestlap.domain.junior.result.FeatureRace;
import com.the_coffe_coders.fastestlap.domain.junior.result.SprintRace;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.PastEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.RaceAndSprintResultsActivity;
import com.the_coffe_coders.fastestlap.ui.event.UpcomingEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.fragment.QualifyingResultsFragment;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.junior.Formula2Activity;
import com.the_coffe_coders.fastestlap.ui.junior.Formula3Activity;
import com.the_coffe_coders.fastestlap.ui.junior.fragment.JuniorFullResultsDialogFragment;
import com.the_coffe_coders.fastestlap.ui.profile.LoginFragment;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.ForgotPasswordFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.SignUpFragment;
import com.the_coffe_coders.fastestlap.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class NavigationUtils {
    public static void navigateToHomePage(Context context) {
        navigateToHomePage(context, null);
    }

    public static void navigateToHomePage(Context context, Bundle extras) {
        Intent intent = new Intent(context, HomePageActivity.class);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }


    public static void navigateToBioPage(Context context, String id, int bioType) {
        Intent intent;

        switch (bioType) {
            case 0:
                intent = new Intent(context, ConstructorBioActivity.class);
                intent.putExtra("TEAM_ID", id);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, DriverBioActivity.class);
                intent.putExtra("DRIVER_ID", id);
                context.startActivity(intent);
                break;
            case 2:
                intent = new Intent(context, TrackBioActivity.class);
                String circuitId = id.split("&")[0];
                String grandPrixName = id.split("&")[1];

                intent.putExtra("CIRCUIT_ID", circuitId);
                intent.putExtra("GRAND_PRIX_NAME", grandPrixName);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToStandingsPage(Context context, String id, int standingsType) {
        Intent intent;

        switch (standingsType) {
            case 0:
                intent = new Intent(context, ConstructorsStandingActivity.class);
                intent.putExtra("TEAM_ID", id);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, DriversStandingActivity.class);
                intent.putExtra("DRIVER_ID", id);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToEventsListPage(Context context, int eventType) {
        Intent intent;

        switch (eventType) {
            case 0:
                intent = new Intent(context, UpcomingEventsActivity.class);
                context.startActivity(intent);
                break;
            case 1:
                intent = new Intent(context, PastEventsActivity.class);
                context.startActivity(intent);
                break;
        }
    }

    public static void navigateToEventPage(Context context, String circuitId) {
        Intent intent = new Intent(context, EventActivity.class);
        intent.putExtra("CIRCUIT_ID", circuitId);
        context.startActivity(intent);
    }

    public static void navigateToLivePage(Context context) {
        navigateToLivePage(context, null, null);
    }

    public static void navigateToLivePage(Context context, String eventTitle) {
        navigateToLivePage(context, eventTitle, null);
    }

    public static void navigateToLivePage(Context context, String eventTitle, String totalLaps) {
        navigateToLivePage(context, eventTitle, totalLaps, null, null);
    }

    public static void navigateToLivePage(Context context, String eventTitle, String totalLaps, String circuitId, String circuitImageUrl) {
        navigateToLivePage(context, eventTitle, totalLaps, circuitId, circuitImageUrl, false, null);
    }

    public static void navigateToLivePage(Context context, String eventTitle, String totalLaps, String circuitId, String circuitImageUrl, boolean isLive, String sessionName) {
        Intent intent = new Intent(context, com.the_coffe_coders.fastestlap.ui.live.LiveActivity.class);
        if (eventTitle != null) {
            intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_EVENT_TITLE, eventTitle);
        }
        if (totalLaps != null) {
            intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_TOTAL_LAPS, totalLaps);
        }
        if (circuitId != null) {
            intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_CIRCUIT_ID, circuitId);
        }
        if (circuitImageUrl != null) {
            intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_CIRCUIT_IMAGE, circuitImageUrl);
        }
        intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_IS_LIVE, isLive);
        if (sessionName != null) {
            intent.putExtra(com.the_coffe_coders.fastestlap.ui.live.LiveActivity.EXTRA_SESSION_NAME, sessionName);
        }
        context.startActivity(intent);
    }

    public static void navigateToJuniorPage(Context context, int categoryType) {
        Intent intent;
        switch (categoryType) {
            case 0:
                intent = new Intent(context, Formula2Activity.class);
                break;
            case 1:
                intent = new Intent(context, Formula3Activity.class);
                break;
            default:
                return;
        }
        intent.putExtra("CATEGORY_TYPE", categoryType);
        context.startActivity(intent);
    }

    public static void navigateToFormula2Home(NavController navController, int categoryType) {
        navigateToJuniorHome(navController, categoryType);
    }

    public static void navigateToFormula3Home(NavController navController, int categoryType) {
        navigateToJuniorHome(navController, categoryType);
    }

    private static void navigateToJuniorHome(NavController navController, int categoryType) {
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.f2HomeFragment, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.f3HomeFragment, args);
        }
    }

    public static void navigateToJuniorResultsPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_juniorResults_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_juniorResults_formula3, args);
        }
    }

    public static void navigateToJuniorCarBioPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_carBio_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_carBio_formula3, args);
        }
    }

    public static void navigateToJuniorEntryListPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_juniorEntryList_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_juniorEntryList_formula3, args);
        }
    }

    public static void navigateToJuniorCalendarPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_juniorCalendar_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_juniorCalendar_formula3, args);
        }
    }

    public static void navigateToJuniorDriverStandingsPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_juniorDriverStandings_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_juniorDriverStandings_formula3, args);
        }
    }

    public static void navigateToJuniorConstructorStandingsPage(View view, int categoryType) {
        NavController navController = Navigation.findNavController(view);
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        if (categoryType == 0) {
            navController.navigate(R.id.to_juniorConstructorStandings_formula2, args);
        } else if (categoryType == 1) {
            navController.navigate(R.id.to_juniorConstructorStandings_formula3, args);
        }
    }

    public static void showFullResultsDialogFeature(String circuit, FeatureRace featureRace, FragmentManager fragmentManager, int categoryType, int raceType) {
        JuniorFullResultsDialogFragment dialog = new JuniorFullResultsDialogFragment();
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        args.putInt("RACE_TYPE", raceType);
        args.putParcelable("JUNIOR_FEATURE_RACE", featureRace);
        args.putString("CIRCUIT", circuit);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "JuniorFullResultsDialogFragment");
    }

    public static void showFullResultsDialogSprint(String circuit, SprintRace sprintRace, FragmentManager fragmentManager, int categoryType, int raceType) {
        JuniorFullResultsDialogFragment dialog = new JuniorFullResultsDialogFragment();
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        args.putInt("RACE_TYPE", raceType);
        args.putParcelable("JUNIOR_SPRINT_RACE", sprintRace);
        args.putString("CIRCUIT", circuit);
        dialog.setArguments(args);
        dialog.show(fragmentManager, "JuniorFullResultsDialogFragment");
    }

    public static void navigateToWelcomePage(Context context) {
        Intent intent = new Intent(context, WelcomeActivity.class);
        context.startActivity(intent);
    }

    public static void showRaceResults(Context context, Race race, int sessionType, List<Stint> stints, RaceResultFastestLap fastestLap) {
        switch (sessionType) {
            case 0:
                Intent intent = new Intent(context, RaceAndSprintResultsActivity.class);
                intent.putExtra("RACE", race);
                intent.putExtra("FASTEST_LAP", fastestLap);
                if (stints != null) {
                    intent.putParcelableArrayListExtra("STINTS", new ArrayList<>(stints));
                }
                context.startActivity(intent);
                break;
            case 1:
                if (context instanceof androidx.fragment.app.FragmentActivity) {
                    FragmentManager fragmentManager = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();
                    QualifyingResultsFragment qualifyingResultsFragment = new QualifyingResultsFragment();
                    Bundle qualifyingArgs = new Bundle();
                    qualifyingArgs.putParcelable("RACE", race);
                    qualifyingResultsFragment.setArguments(qualifyingArgs);
                    qualifyingResultsFragment.show(fragmentManager, "QualifyingResultsFragment");
                }
                break;
        }
    }

    public static void showProfileManageDialogs(FragmentManager fragmentManager, int dialogType, String additionalInfo) {
        switch (dialogType) {
            case 0:
                SignUpFragment signUpFragment = new SignUpFragment();
                signUpFragment.show(fragmentManager, "SignUpFragment");
                break;
            case 1:
                ForgotPasswordFragment forgotPasswordFragment = new ForgotPasswordFragment();
                forgotPasswordFragment.show(fragmentManager, "ForgotPasswordFragment");
                break;
            case 2:
                LoginFragment loginFragment = new LoginFragment();
                Bundle args = new Bundle();
                args.putString("email", additionalInfo);
                loginFragment.setArguments(args);
                loginFragment.show(fragmentManager, "LoginFragment");
                break;
        }
    }

    public static void openLocation(Context context, String latitude, String longitude) {
        String uri = String.format(Constants.GOOGLE_MAPS_ACCESS, latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_map_app_found, Toast.LENGTH_SHORT).show();
            Log.e("UIUtils", "No map app found to open location");
        }
    }

    public static void navigateToWeatherPage(Context context, String locality, String latitude, String longitude, String sessionKey, String zoneId) {
        navigateToWeatherPage(context, locality, latitude, longitude, sessionKey, null, null, zoneId, false);
    }

    public static void navigateToWeatherPage(Context context, String locality, String latitude, String longitude, String sessionKey, String startDate, String endDate, String zoneId) {
        navigateToWeatherPage(context, locality, latitude, longitude, sessionKey, startDate, endDate, zoneId, false);
    }

    public static void navigateToWeatherPage(Context context, String locality, String latitude, String longitude, String sessionKey, String startDate, String endDate, String zoneId, boolean isSessionInProgress) {
        Intent intent = new Intent(context, com.the_coffe_coders.fastestlap.ui.weather.WeatherActivity.class);
        if (locality != null) intent.putExtra("LOCALITY", locality);
        if (latitude != null) intent.putExtra("LATITUDE", latitude);
        if (longitude != null) intent.putExtra("LONGITUDE", longitude);
        if (sessionKey != null) intent.putExtra("SESSION_KEY", sessionKey);
        if (startDate != null) intent.putExtra("START_DATE", startDate);
        if (endDate != null) intent.putExtra("END_DATE", endDate);
        if (zoneId != null) intent.putExtra("ZONE_ID", zoneId);
        intent.putExtra("IS_SESSION_IN_PROGRESS", isSessionInProgress);
        context.startActivity(intent);
    }

    public static void openGoogleWeather(Context context, String locality) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(Constants.WEATHER_ACCESS_PACKAGE);
        if (intent != null) {
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(SearchManager.QUERY, context.getString(R.string.weather, locality));
            context.startActivity(intent);
        } else {
            openWeatherInBrowser(context, locality);
        }
    }

    private static void openWeatherInBrowser(Context context, String locality) {
        String uri = String.format(Constants.GOOGLE_WEATHER_ACCESS, Uri.encode(locality));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        context.startActivity(intent);
    }
}
