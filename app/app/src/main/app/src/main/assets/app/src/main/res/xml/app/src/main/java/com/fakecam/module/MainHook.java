package com.fakecam.module;

import android.hardware.Camera;
import android.os.Environment;
import org.xmlpull.v1.XmlPullParser;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "FakeCamHook";
    private final Set<String> targetPackages = new HashSet<>();

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (targetPackages.isEmpty()) loadConfig();
        if (!targetPackages.contains(lpparam.packageName)) return;
        XposedBridge.log(TAG + ": Hooking camera in " + lpparam.packageName);
        XposedBridge.hookAllMethods(Camera.class, "takePicture", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(null);
                Camera.PictureCallback cb = (Camera.PictureCallback) param.args[2];
                if (cb != null) {
                    File fake = new File(Environment.getExternalStorageDirectory(),
                                         "DCIM/FakeCamera/img.jpg");
                    if (fake.exists()) {
                        byte[] data = readBytes(fake);
                        cb.onPictureTaken(data, (Camera) param.thisObject);
                        XposedBridge.log(TAG + ": Delivered fake image");
                    } else {
                        XposedBridge.log(TAG + ": Fake image not found");
                    }
                }
            }
        });
    }

    private void loadConfig() {
        try {
            XmlPullParser p = android.util.Xml.newPullParser();
            p.setInput(getClass().getClassLoader()
                .getResourceAsStream("res/xml/config.xml"), "utf-8");
            int e;
            while ((e = p.next()) != XmlPullParser.END_DOCUMENT) {
                if (e == XmlPullParser.START_TAG && "package".equals(p.getName())) {
                    String name = p.getAttributeValue(null, "name");
                    targetPackages.add(name);
                }
            }
        } catch (Exception ignored) {}
    }

    private byte[] readBytes(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] buf = new byte[(int) file.length()];
        fis.read(buf);
        fis.close();
        return buf;
    }
}
