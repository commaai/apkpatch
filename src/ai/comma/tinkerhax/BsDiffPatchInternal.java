/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 * Copyright (C) 2017 comma.ai
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

// package com.tencent.tinker.lib.patch;
package ai.comma.tinkerhax;

// import android.content.Context;
// import android.content.pm.ApplicationInfo;
// import android.os.SystemClock;

import com.tencent.tinker.bsdiff.BSPatch;
import com.tencent.tinker.commons.util.StreamUtil;
// import com.tencent.tinker.lib.tinker.Tinker;
// import com.tencent.tinker.lib.util.TinkerLog;
// import com.tencent.tinker.loader.TinkerRuntimeException;
// import com.tencent.tinker.loader.shareutil.ShareBsDiffPatchInfo;
// import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;
// import com.tencent.tinker.loader.shareutil.ShareSecurityCheck;
// import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

// import com.tencent.tinker.ziputils.ziputil.TinkerZipEntry;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipFile;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipOutputStream;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by zhangshaowen on 16/3/21.
 */
public class BsDiffPatchInternal extends BasePatchInternal {
    private static final String TAG = "Tinker.BsDiffPatchInternal";

    protected static boolean tryRecoverLibraryFiles(String apkPath, ShareSecurityCheck checker,
                                                    ZipOutputStream outZip, File patchFile, HashSet<String> added) {

        // if (!manager.isEnabledForNativeLib()) {
        //     System.err.println("patch recover, library is not enabled");
        //     return true;
        // }
        String libMeta = checker.getMetaContentMap().get(SO_META_FILE);

        // if (libMeta == null) {
        //     System.err.println("patch recover, library is not contained");
        //     return true;
        // }
        // long begin = SystemClock.elapsedRealtime();
        boolean result = patchLibraryExtractViaBsDiff(apkPath, outZip, libMeta, patchFile, added);
        // long cost = SystemClock.elapsedRealtime() - begin;
        System.err.println("recover lib result:" + result);
        return result;
    }


    private static boolean patchLibraryExtractViaBsDiff(String apkPath, ZipOutputStream outZip, String meta, File patchFile, HashSet<String> added) {
        // String dir = patchVersionDirectory + "/" + SO_PATH + "/";
        return extractBsDiffInternals(apkPath, outZip, meta, patchFile, TYPE_Library, added);
    }

    private static boolean extractBsDiffInternals(String apkPath, ZipOutputStream outZip, String meta, File patchFile, int type, HashSet<String> added) {
        //parse
        ArrayList<ShareBsDiffPatchInfo> patchList = new ArrayList<>();

        ShareBsDiffPatchInfo.parseDiffPatchInfo(meta, patchList);

        if (patchList.isEmpty()) {
            System.err.println("extract patch list is empty! type:%s:" + ShareTinkerInternals.getTypeString(type));
            return true;
        }

        // File directory = new File(dir);
        // if (!directory.exists()) {
        //     directory.mkdirs();
        // }
        //I think it is better to extract the raw files from apk
        // Tinker manager = Tinker.with(context);
        // ApplicationInfo applicationInfo = context.getApplicationInfo();
        // if (applicationInfo == null) {
        //     // Looks like running on a test Context, so just return without patching.
        //     System.err.println("applicationInfo == null!!!!");
        //     return false;
        // }
        ZipFile apk = null;
        ZipFile patch = null;
        try {
            // String apkPath = applicationInfo.sourceDir;
            apk = new ZipFile(apkPath);
            patch = new ZipFile(patchFile);

            for (ShareBsDiffPatchInfo info : patchList) {
                long start = System.currentTimeMillis();

                final String infoPath = info.path;
                String patchRealPath;
                if (infoPath.equals("")) {
                    patchRealPath = info.name;
                } else {
                    patchRealPath = info.path + "/" + info.name;
                }
                final String fileMd5 = info.md5;
                if (!SharePatchFileUtil.checkIfMd5Valid(fileMd5)) {
                    System.err.println(String.format("meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), info.name, info.md5));
                    // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
                    return false;
                }

                // String middle;
                // middle = info.path + "/" + info.name;
                // File extractedFile = new File(dir + middle);
                File extractedFile = File.createTempFile("bsdiff", ".tmp");

                //check file whether already exist
                // if (extractedFile.exists()) {
                //     if (fileMd5.equals(SharePatchFileUtil.getMD5(extractedFile))) {
                //         //it is ok, just continue
                //         System.err.println("bsdiff file %s is already exist, and md5 match, just continue" + extractedFile.getPath());
                //         continue;
                //     } else {
                //         System.err.println("have a mismatch corrupted dex " + extractedFile.getPath());
                //         extractedFile.delete();
                //     }
                // } else {
                //     extractedFile.getParentFile().mkdirs();
                // }


                String patchFileMd5 = info.patchMd5;
                //it is a new file, just copy
                ZipEntry patchFileEntry = patch.getEntry(patchRealPath);
                ZipEntry outEntry = patchFileEntry;

                if (patchFileEntry == null) {
                    System.err.println("patch entry is null. path:" + patchRealPath);
                    // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type);
                    return false;
                }

                if (patchFileMd5.equals("0")) {
                    if (!extract(patch, patchFileEntry, extractedFile, fileMd5, false)) {
                        System.err.println("Failed to extract file " + extractedFile.getPath());
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type);
                        return false;
                    }
                } else {
                    //we do not check the intermediate files' md5 to save time, use check whether it is 32 length
                    if (!SharePatchFileUtil.checkIfMd5Valid(patchFileMd5)) {
                        System.err.println(String.format("meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), info.name, patchFileMd5));
                        // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
                        return false;
                    }

                    ZipEntry rawApkFileEntry = apk.getEntry(patchRealPath);
                    outEntry = rawApkFileEntry;

                    if (rawApkFileEntry == null) {
                        System.err.println("apk entry is null. path:" + patchRealPath);
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type);
                        return false;
                    }

                    String rawApkCrc = info.rawCrc;

                    //check source crc instead of md5 for faster
                    String rawEntryCrc = String.valueOf(rawApkFileEntry.getCrc());
                    if (!rawEntryCrc.equals(rawApkCrc)) {
                        System.err.println(String.format("apk entry %s crc is not equal, expect crc: %s, got crc: %s", patchRealPath, rawApkCrc, rawEntryCrc));
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type);
                        return false;
                    }
                    InputStream oldStream = null;
                    InputStream newStream = null;
                    try {
                        oldStream = apk.getInputStream(rawApkFileEntry);
                        newStream = patch.getInputStream(patchFileEntry);
                        BSPatch.patchFast(oldStream, newStream, extractedFile);
                    } finally {
                        StreamUtil.closeQuietly(oldStream);
                        StreamUtil.closeQuietly(newStream);
                    }

                    //go go go bsdiff get the
                    if (!SharePatchFileUtil.verifyFileMd5(extractedFile, fileMd5)) {
                        System.err.println("Failed to recover diff file " + extractedFile.getPath());
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, extractedFile, info.name, type);
                        SharePatchFileUtil.safeDeleteFile(extractedFile);
                        return false;
                    }
                    System.err.println(String.format("success recover bsdiff file: %s, use time: %d",
                        extractedFile.getPath(), (System.currentTimeMillis() - start)));
                }


                FileInputStream extractedStream = new FileInputStream(extractedFile);
                outEntry.setSize(extractedFile.length());
                outEntry.setCompressedSize(-1);
                SharePatchFileUtil.extractTinkerEntry(outEntry, extractedStream, outZip);
                added.add(outEntry.getName());
                extractedStream.close();

                // TinkerZipFile patch2 = new TinkerZipFile(patchFile);
                // TinkerZipEntry entry2 = patch2.getEntry(patchRealPath);
                // FileInputStream extractedStream = new FileInputStream(extractedFile);
                // TinkerZipUtil.extractTinkerEntry(entry2, extractedStream, outZip);
                // StreamUtil.closeQuietly(patch2);

                extractedFile.delete();
              
            }

        } catch (Throwable e) {
//            e.printStackTrace();
            throw new RuntimeException("patch " + ShareTinkerInternals.getTypeString(type) + " extract failed (" + e.getMessage() + ").", e);
        } finally {
            // SharePatchFileUtil.closeZip(apk);
            // SharePatchFileUtil.closeZip(patch);
            StreamUtil.closeQuietly(apk);
            StreamUtil.closeQuietly(patch);
        }
        return true;
    }

}
