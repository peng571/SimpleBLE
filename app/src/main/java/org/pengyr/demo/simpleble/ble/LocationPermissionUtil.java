package org.pengyr.demo.simpleble.ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.pengyr.demo.simpleble.R;

import static android.os.Build.VERSION_CODES.M;

/**
 * Android SDK >= 6.0 need to enable location when using bluetooth
 */
@TargetApi(M)
public class LocationPermissionUtil {

    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    public static final int ENABLE_REQUEST_COARSE_LOCATION = 3;

    @TargetApi(23)
    protected static boolean isLocatePermissionEnable23(Context context) {
        int perm = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(22)
    protected static boolean isLocationPermissionEnable22(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isLocationPermissionEnable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isLocatePermissionEnable23(context);
        } else {
            return isLocationPermissionEnable22(context);
        }
    }


    public static boolean isLocationEnable(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public static boolean checkLocationPermission(final Context context) {
        if (!(context instanceof Activity)) return false;
        if (isLocationPermissionEnable(context)) return true;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.location_enable_question)
                .setPositiveButton(R.string.ok, (DialogInterface dialog, int which) -> {
                    ActivityCompat.requestPermissions((Activity) context,
                                                      new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                                      PERMISSION_REQUEST_COARSE_LOCATION);
                    dialog.dismiss();
                })
                .setOnCancelListener((DialogInterface dialog) -> ((Activity) context).finish())
                .show();
        return false;
    }

    public static boolean checkLocationEnable(final Context context) {
        if (!(context instanceof Activity)) return false;
        if (isLocationEnable(context)) return true;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.location_allow_question)
                .setPositiveButton(R.string.ok, (DialogInterface dialog, int which) -> {
                    ((Activity) context).startActivityForResult(
                            new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                            ENABLE_REQUEST_COARSE_LOCATION);
                    dialog.dismiss();
                })
                .setOnCancelListener((DialogInterface dialog) -> ((Activity) context).finish())
                .show();
        return false;
    }

    public static boolean setLocationEnable(Context context) {
        if (!(context instanceof Activity)) return false;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return true;
        }
        ((Activity) context).startActivityForResult(
                new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                ENABLE_REQUEST_COARSE_LOCATION);
        return false;
    }
}
