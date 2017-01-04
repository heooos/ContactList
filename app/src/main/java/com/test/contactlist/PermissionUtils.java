package com.test.contactlist;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Created by zh on 2017/1/4.
 */

public class PermissionUtils {

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean requestPermission(Context context, String permission) {
        return (Build.VERSION.SDK_INT >= 23) && (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }
}
