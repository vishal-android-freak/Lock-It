package vaf.vishal.lockit;

import android.app.AndroidAppHelper;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by vishal on 13/5/16.
 */
public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("android")) {
            final Class<?> shutDownClass = XposedHelpers.findClass("com.android.server.power.ShutdownThread", loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(shutDownClass, "shutdown", "android.content.Context", "boolean", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }

                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context context = AndroidAppHelper.currentApplication().createPackageContext("vaf.vishal.lockit", Context.CONTEXT_IGNORE_SECURITY);
                    Resources res = context.getResources();
                    int dialogLayout = res.getIdentifier("dialog_layout", "layout", "vaf.vishal.lockit");
                    int style = res.getIdentifier("App", "style", "vaf.vishal.lockit");
                    final Dialog dialog = new Dialog(context, style);
                    dialog.setContentView(dialogLayout);
                    dialog.setCancelable(false);

                    EditText password = (EditText) dialog.findViewById(R.id.password_text);
                    Button switchOff = (Button) dialog.findViewById(R.id.switch_off);
                    switchOff.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context uiContext = (Context) XposedHelpers.callStaticMethod(shutDownClass, "getUiContext", (Context)param.args[0]);
                            XposedHelpers.setStaticBooleanField(shutDownClass, "mReboot", false);
                            XposedHelpers.setStaticBooleanField(shutDownClass, "mRebootSafeMode", false);
                            XposedHelpers.callStaticMethod(shutDownClass, "shutdownInner", uiContext, param.args[1]);
                        }
                    });

                    Button cancel = (Button) dialog.findViewById(R.id.cancel);
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
        }
    }
}