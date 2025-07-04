package model;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class ApkControl {

    private Context sContext;

    public ApkControl(Context mContext){

        sContext = mContext;
    }

    public int uninstall_app(String pkgname){

        int rtn = 0;
        Log.d(TAGS, "01");
        PackageManager packageManager = sContext.getPackageManager();
        Intent intent = new Intent();
        intent.putExtra("key", "value");
        PendingIntent pendingIntent = PendingIntent.getActivity(sContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        IntentSender intentSender = pendingIntent.getIntentSender();
        packageManager.getPackageInstaller().uninstall(pkgname ,  intentSender) ;
        Log.d(TAGS, "02");
        boolean isInstalled = true;
        do {
            isInstalled = isAppInstalled(pkgname);
            if (isInstalled) {
                // 应用已安装
                Log.d(TAGS, "App已安裝");
            } else {
                // 应用未安装
                Log.d(TAGS, "App未安裝");
            }
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                //Todo 加return
                return -1;
            }
        }while(isInstalled != false);

        return rtn;
    }

    public int install_app(String pkgname, String AppPath){

        int rtn = 0;

        PackageInstaller packageInstaller = sContext.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(pkgname);
        try {
            Method allowDowngrade = PackageInstaller.SessionParams.class.getMethod("setAllowDowngrade", boolean.class);
            allowDowngrade.setAccessible(true);
            allowDowngrade.invoke(params, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        OutputStream os = null;
        InputStream is = null;
        try {
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            os = session.openWrite(pkgname, 0, -1);
            is = new FileInputStream(AppPath);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            session.fsync(os);
            os.close();
            os = null;
            is.close();
            is = null;
            session.commit(PendingIntent.getBroadcast(sContext, sessionId,
                    new Intent(Intent.ACTION_MAIN), PendingIntent.FLAG_IMMUTABLE).getIntentSender());
        } catch (Exception e) {
            e.printStackTrace();
            rtn = -1;

        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    rtn = -2;
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    rtn = -3;
                }
            }
        }

        return rtn;
    }
    public boolean isAppInstalled(String packageName) {
        try {
            sContext.getPackageManager().getPackageInfo(packageName, 0);
            return true; // 如果找到应用程序包名，说明应用已安装
        } catch (PackageManager.NameNotFoundException e) {
            return false; // 如果找不到应用程序包名，说明应用未安装
        }
    }

    public String GetApkPkgName(String AppPath){

        PackageManager pm = sContext.getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(AppPath, 0);
        if (packageInfo != null) {
            return packageInfo.packageName;
        }
        return null;

    }

    static String TAGS = "## [KO] ApkControl";
}

