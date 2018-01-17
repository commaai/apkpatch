import java.io.*;
import java.util.*;
import java.util.TimeZone;

import com.tencent.tinker.build.patch.Runner;
import com.tencent.tinker.build.patch.InputParam;

import ai.comma.tinkerhax.UpgradePatch;
import ai.comma.tinkerhax.PatchResult;

public class ApkPatch {
  public static void main(String args[]) {
    try {

      if (args.length < 3) {
        System.err.println("usage: apkpatch gen|apply old.apk new.apk patch.apk");
        return;
      }

      TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

      if (args[0].equals("gen")) {


        ArrayList<String> resPat = new ArrayList<String>();
        // resPat.add("res/*");
        resPat.add("res/*/*.xml");
        resPat.add("assets/*");
        resPat.add("resources.arsc");
        resPat.add("AndroidManifest.xml");

        ArrayList<String> libPat = new ArrayList<String>();
        libPat.add("lib/*/*.so");

        ArrayList<String> dexPat = new ArrayList<String>();
        dexPat.add("classes*.dex");

        InputParam.Builder builder = new InputParam.Builder();
        builder.setOldApk(args[1])
               .setNewApk(args[2])
               .setOutBuilder("tmp/")
               .setDexFilePattern(dexPat)
               .setDexMode("raw")
               .setSoFilePattern(libPat)
               .setResourceFilePattern(resPat)
               .setResourceLargeModSize(100)
               .setResourceIgnoreChangePattern(new ArrayList<String>())
               .setDexLoaderPattern(new ArrayList<String>())
               .setDexIgnoreWarningLoaderPattern(new ArrayList<String>())
               .setConfigFields(new HashMap<String, String>())
               .setIgnoreWarning(true)
               ;
        InputParam ip = builder.create();

        Runner.gradleRun(ip);
      } else if (args[0].equals("apply")) {
        PatchResult pr = new PatchResult();
        UpgradePatch up = new UpgradePatch();
        boolean r = up.tryPatch(args[1], args[3], args[2], pr);
        System.err.println("result: " + r);
      } else {
        System.err.println("usage: apkpatch gen|apply old.apk new.apk patch.apk");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}