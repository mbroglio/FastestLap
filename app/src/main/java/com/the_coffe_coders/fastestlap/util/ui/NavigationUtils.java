package com.the_coffe_coders.fastestlap.util.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.the_coffe_coders.fastestlap.R;
import com.the_coffe_coders.fastestlap.domain.f1.grand_prix.Race;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResult;
import com.the_coffe_coders.fastestlap.domain.junior.result.JuniorResultElement;
import com.the_coffe_coders.fastestlap.ui.bio.ConstructorBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.DriverBioActivity;
import com.the_coffe_coders.fastestlap.ui.bio.TrackBioActivity;
import com.the_coffe_coders.fastestlap.ui.event.EventActivity;
import com.the_coffe_coders.fastestlap.ui.event.PastEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.UpcomingEventsActivity;
import com.the_coffe_coders.fastestlap.ui.event.fragment.QualifyingResultsFragment;
import com.the_coffe_coders.fastestlap.ui.event.fragment.RaceAndSprintResultsFragment;
import com.the_coffe_coders.fastestlap.ui.home.HomePageActivity;
import com.the_coffe_coders.fastestlap.ui.junior.Formula2Activity;
import com.the_coffe_coders.fastestlap.ui.junior.Formula3Activity;
import com.the_coffe_coders.fastestlap.ui.junior.JuniorResultsActivity;
import com.the_coffe_coders.fastestlap.ui.junior.fragment.JuniorDialogFragment;
import com.the_coffe_coders.fastestlap.ui.profile.LoginFragment;
import com.the_coffe_coders.fastestlap.ui.standing.ConstructorsStandingActivity;
import com.the_coffe_coders.fastestlap.ui.standing.DriversStandingActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.WelcomeActivity;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.ForgotPasswordFragment;
import com.the_coffe_coders.fastestlap.ui.welcome.fragment.SignUpFragment;
import com.the_coffe_coders.fastestlap.util.Constants;

public class NavigationUtils {
    public static void navigateToHomePage(Context context) {
        Intent intent = new Intent(context, HomePageActivity.class);
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

    public static void navigateToJuniorPage(Context context, int categoryType) {
        Intent intent;
        switch (categoryType){
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

    public static void navigateToJuniorResultsPage(Context context, int categoryType) {
        Intent intent = new Intent(context, JuniorResultsActivity.class);
        intent.putExtra("CATEGORY_TYPE", categoryType);
        context.startActivity(intent);
    }


    public static void showEntryListDialog(FragmentManager fragmentManager, int categoryType) {
        showJuniorDialog(fragmentManager, categoryType, 0, null);
    }

    public static void showCalendarDialog(FragmentManager fragmentManager, int categoryType) {
        showJuniorDialog(fragmentManager, categoryType, 1, null);
    }

    public static void showDriversStandingDialog(FragmentManager fragmentManager, int categoryType) {
        showJuniorDialog(fragmentManager, categoryType, 2, null);
    }

    public static void showConstructorsStandingDialog(FragmentManager fragmentManager, int categoryType) {
        showJuniorDialog(fragmentManager, categoryType, 3, null);
    }

    public static void showFullResultsDialog(JuniorResult result, FragmentManager fragmentManager, int categoryType, int raceType) {
        showJuniorDialog(fragmentManager, categoryType, raceType, result);
        //raceType=0 -> Feature; raceType=1 -> Sprint
    }


    private static void showJuniorDialog(FragmentManager fragmentManager, int categoryType, int content, JuniorResult juniorResult){
        JuniorDialogFragment juniorDialogFragment = new JuniorDialogFragment();
        Bundle args = new Bundle();
        args.putInt("CATEGORY_TYPE", categoryType);
        args.putInt("CONTENT", content);
        args.putParcelable("JUNIOR_RESULT", juniorResult);
        juniorDialogFragment.setArguments(args);
        juniorDialogFragment.show(fragmentManager, "JuniorDialogFragment");
    }

    public static void navigateToWelcomePage(Context context) {
        Intent intent = new Intent(context, WelcomeActivity.class);
        context.startActivity(intent);
    }

    public static void showRaceResultsDialog(FragmentManager fragmentManager, Race race, int sessionType) {
        switch (sessionType) {
            case 0:
                RaceAndSprintResultsFragment raceAndSprintResultsFragment = new RaceAndSprintResultsFragment();
                Bundle args = new Bundle();
                args.putParcelable("RACE", race);
                raceAndSprintResultsFragment.setArguments(args);
                raceAndSprintResultsFragment.show(fragmentManager, "RaceResultsFragment");
                break;
            case 1:
                QualifyingResultsFragment qualifyingResultsFragment = new QualifyingResultsFragment();
                Bundle qualifyingArgs = new Bundle();
                qualifyingArgs.putParcelable("RACE", race);
                qualifyingResultsFragment.setArguments(qualifyingArgs);
                qualifyingResultsFragment.show(fragmentManager, "QualifyingResultsFragment");
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
