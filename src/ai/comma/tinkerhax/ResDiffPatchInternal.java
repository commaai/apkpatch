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
// import com.tencent.tinker.loader.shareutil.ShareConstants;
// import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;
// import com.tencent.tinker.loader.shareutil.ShareResPatchInfo;
// import com.tencent.tinker.loader.shareutil.ShareSecurityCheck;
// import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;
// import com.tencent.tinker.SharePatchFileUtils.SharePatchFileUtil.TinkerZipEntry;
// import com.tencent.tinker.SharePatchFileUtils.SharePatchFileUtil.TinkerZipFile;
// import com.tencent.tinker.SharePatchFileUtils.SharePatchFileUtil.TinkerZipOutputStream;
// import com.tencent.tinker.SharePatchFileUtils.SharePatchFileUtil.TinkerZipUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by zhangshaowen on 2016/8/8.
 */
public class ResDiffPatchInternal extends BasePatchInternal {

    protected static final String TAG = "Tinker.ResDiffPatchInternal";

    protected static boolean tryRecoverResourceFiles(String apkPath, ShareSecurityCheck checker,
                                                     ZipOutputStream outZip, File patchFile, HashSet<String> added) {

        // if (!manager.isEnabledForResource()) {
        //     System.err.println("patch recover, resource is not enabled");
        //     return true;
        // }
        String resourceMeta = checker.getMetaContentMap().get(RES_META_FILE);

        if (resourceMeta == null || resourceMeta.length() == 0) {
            System.err.println("patch recover, resource is not contained");
            return true;
        }

        // long begin = SystemClock.elapsedRealtime();
        boolean result = patchResourceExtractViaResourceDiff(apkPath, outZip, resourceMeta, patchFile, added);
        // long cost = SystemClock.elapsedRealtime() - begin;
        System.err.println("recover resource result:" + result);
        return result;
    }

    private static boolean patchResourceExtractViaResourceDiff(String apkPath, ZipOutputStream outZip,
                                                               String meta, File patchFile, HashSet<String> added) {
        // String dir = patchVersionDirectory + "/" + ShareConstants.RES_PATH + "/";

        if (!extractResourceDiffInternals(apkPath, outZip, meta, patchFile, TYPE_RESOURCE, added)) {
            System.err.println("patch recover, extractDiffInternals fail");
            return false;
        }
        return true;
    }

    private static boolean extractResourceDiffInternals(String apkPath, ZipOutputStream out, String meta, File patchFile, int type, HashSet<String> added) {
        ShareResPatchInfo resPatchInfo = new ShareResPatchInfo();
        ShareResPatchInfo.parseAllResPatchInfo(meta, resPatchInfo);
        System.err.println(String.format("res dir: %s, meta: %s", out, resPatchInfo.toString()));
        // Tinker manager = Tinker.with(context);

        if (!SharePatchFileUtil.checkIfMd5Valid(resPatchInfo.resArscMd5)) {
            System.err.println(String.format("resource meta file md5 mismatch, type:%s, md5: %s", ShareTinkerInternals.getTypeString(type), resPatchInfo.resArscMd5));
            // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
            return false;
        }
        // File directory = new File(dir);

        // File tempResFileDirectory = new File(directory, "res_temp");

        // File resOutput = new File(directory, ShareConstants.RES_NAME);
        // //check result file whether already exist
        // if (resOutput.exists()) {
        //     if (SharePatchFileUtil.checkResourceArscMd5(resOutput, resPatchInfo.resArscMd5)) {
        //         //it is ok, just continue
        //         System.err.println("resource file %s is already exist, and md5 match, just return true" + resOutput.getPath());
        //         return true;
        //     } else {
        //         System.err.println("have a mismatch corrupted resource " + resOutput.getPath());
        //         resOutput.delete();
        //     }
        // } else {
        //     resOutput.getParentFile().mkdirs();
        // }

        try {
            // ApplicationInfo applicationInfo = context.getApplicationInfo();
            // if (applicationInfo == null) {
            //     //Looks like running on a test Context, so just return without patching.
            //     System.err.println("applicationInfo == null!!!!");
            //     return false;
            // }
            // String apkPath = applicationInfo.sourceDir;

            File tempResFileDirectory = File.createTempFile("res_temp", ".tmp");
            tempResFileDirectory.delete();
            tempResFileDirectory.mkdir();

            if (!checkAndExtractResourceLargeFile(apkPath, tempResFileDirectory, patchFile, resPatchInfo, type)) {
                return false;
            }

            // ZipOutputStream out = null;
            ZipFile oldApk = null;
            ZipFile newApk = null;
            int totalEntryCount = 0;
            try {
                // out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(resOutput)));
                
                oldApk = new ZipFile(apkPath);
                newApk = new ZipFile(patchFile);
                final Enumeration<? extends ZipEntry> entries = oldApk.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry zipEntry = entries.nextElement();
                    if (zipEntry == null) {
                        throw new RuntimeException("zipEntry is null when get from oldApk");
                    }
                    String name = zipEntry.getName();
                    if (name.contains("../")) {
                        continue;
                    }
                    if (ShareResPatchInfo.checkFileInPattern(resPatchInfo.patterns, name)) {
                        //won't contain in add set.
                        if (!resPatchInfo.deleteRes.contains(name)
                            && !resPatchInfo.modRes.contains(name)
                            && !resPatchInfo.largeModRes.contains(name)) {
                            SharePatchFileUtil.extractTinkerEntry(oldApk, zipEntry, out);
                            added.add(zipEntry.getName());
                            totalEntryCount++;
                        }
                    }
                }

                for (String name : resPatchInfo.largeModRes) {
                    ZipEntry largeZipEntry = oldApk.getEntry(name);
                    if (largeZipEntry == null) {
                        System.err.println("large patch entry is null. path:" + name);
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, resOutput, name, type);
                        return false;
                    }
                    ShareResPatchInfo.LargeModeInfo largeModeInfo = resPatchInfo.largeModMap.get(name);
                    SharePatchFileUtil.extractLargeModifyFile(largeZipEntry, largeModeInfo.file, largeModeInfo.crc, out);
                    added.add(largeZipEntry.getName());
                    totalEntryCount++;
                }

                for (String name : resPatchInfo.addRes) {
                    ZipEntry addZipEntry = newApk.getEntry(name);
                    if (addZipEntry == null) {
                        System.err.println("add patch entry is null. path:" + name);
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, resOutput, name, type);
                        return false;
                    }
                    if (resPatchInfo.storeRes.containsKey(name)) {
                        File storeFile = resPatchInfo.storeRes.get(name);
                        SharePatchFileUtil.extractLargeModifyFile(addZipEntry, storeFile, addZipEntry.getCrc(), out);
                        added.add(addZipEntry.getName());
                    } else {
                        SharePatchFileUtil.extractTinkerEntry(newApk, addZipEntry, out);
                        added.add(addZipEntry.getName());
                    }
                    totalEntryCount++;
                }

                for (String name : resPatchInfo.modRes) {
                    ZipEntry modZipEntry = newApk.getEntry(name);
                    if (modZipEntry == null) {
                        System.err.println("mod patch entry is null. path:" + name);
                        // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, resOutput, name, type);
                        return false;
                    }
                    if (resPatchInfo.storeRes.containsKey(name)) {
                        File storeFile = resPatchInfo.storeRes.get(name);
                        SharePatchFileUtil.extractLargeModifyFile(modZipEntry, storeFile, modZipEntry.getCrc(), out);
                        added.add(modZipEntry.getName());
                    } else {
                        SharePatchFileUtil.extractTinkerEntry(newApk, modZipEntry, out);
                        added.add(modZipEntry.getName());
                    }
                    totalEntryCount++;
                }
                // set comment back
                // out.setComment(oldApk.getComment());
            } finally {
                // StreamUtil.closeQuietly(out);
                StreamUtil.closeQuietly(oldApk);
                StreamUtil.closeQuietly(newApk);

                //delete temp files
                SharePatchFileUtil.deleteDir(tempResFileDirectory);
            }

            // boolean result = SharePatchFileUtil.checkResourceArscMd5(resOutput, resPatchInfo.resArscMd5);
            // if (!result) {
            //     System.err.println(String.format("check final new resource file fail path:%s, entry count:%d, size:%d", resOutput.getAbsolutePath(), totalEntryCount, resOutput.length()));
            //     SharePatchFileUtil.safeDeleteFile(resOutput);
            //     // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, resOutput, ShareConstants.RES_NAME, type);
            //     return false;
            // }

            System.err.println(String.format("final new resource : entry count:%d", totalEntryCount));
        } catch (Throwable e) {
//            e.printStackTrace();
            throw new RuntimeException("patch " + ShareTinkerInternals.getTypeString(type) +  " extract failed (" + e.getMessage() + ").", e);
        }
        return true;
    }

    private static boolean checkAndExtractResourceLargeFile(String apkPath, File tempFileDirtory,
                                                            File patchFile, ShareResPatchInfo resPatchInfo, int type) {
        long start = System.currentTimeMillis();
        // Tinker manager = Tinker.with(context);
        ZipFile apkFile = null;
        ZipFile patchZipFile = null;
        try {
            //recover resources.arsc first
            apkFile = new ZipFile(apkPath);
            ZipEntry arscEntry = apkFile.getEntry(ShareConstants.RES_ARSC);
            // File arscFile = new File(directory, ShareConstants.RES_ARSC);
            if (arscEntry == null) {
                System.err.println("resources apk entry is null. path:" + ShareConstants.RES_ARSC);
                // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, arscFile, ShareConstants.RES_ARSC, type);
                return false;
            }
            //use base resources.arsc crc to identify base.apk
            String baseArscCrc = String.valueOf(arscEntry.getCrc());
            if (!baseArscCrc.equals(resPatchInfo.arscBaseCrc)) {
                System.err.println(String.format("resources.arsc's crc is not equal, expect crc: %s, got crc: %s", resPatchInfo.arscBaseCrc, baseArscCrc));
                // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, arscFile, ShareConstants.RES_ARSC, type);
                return false;
            }

            //resource arsc is not changed, just return true
            if (resPatchInfo.largeModRes.isEmpty() && resPatchInfo.storeRes.isEmpty()) {
                System.err.println("no large modify or store resources, just return");
                return true;
            }
            patchZipFile = new ZipFile(patchFile);

            for (String name : resPatchInfo.storeRes.keySet()) {
                long storeStart = System.currentTimeMillis();
                File destCopy = new File(tempFileDirtory, name);
                SharePatchFileUtil.ensureFileDirectory(destCopy);

                ZipEntry patchEntry = patchZipFile.getEntry(name);
                if (patchEntry == null) {
                    System.err.println("store patch entry is null. path:" + name);
                    // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, destCopy, name, type);
                    return false;
                }
                extract(patchZipFile, patchEntry, destCopy, null, false);
                //fast check, only check size
                if (patchEntry.getSize() != destCopy.length()) {
                    System.err.println(String.format("resource meta file size mismatch, type:%s, name: %s, patch size: %d, file size; %d", ShareTinkerInternals.getTypeString(type), name, patchEntry.getSize(), destCopy.length()));
                    // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
                    return false;
                }
                resPatchInfo.storeRes.put(name, destCopy);

                System.err.println(String.format("success recover store file:%s, file size:%d, use time:%d", destCopy.getPath(), destCopy.length(), (System.currentTimeMillis() - storeStart)));
            }
            for (String name : resPatchInfo.largeModRes) {
                long largeStart = System.currentTimeMillis();
                ShareResPatchInfo.LargeModeInfo largeModeInfo = resPatchInfo.largeModMap.get(name);

                if (largeModeInfo == null) {
                    System.err.println(String.format("resource not found largeModeInfo, type:%s, name: %s", ShareTinkerInternals.getTypeString(type), name));
                    // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
                    return false;
                }

                largeModeInfo.file = new File(tempFileDirtory, name);
                SharePatchFileUtil.ensureFileDirectory(largeModeInfo.file);

                //we do not check the intermediate files' md5 to save time, use check whether it is 32 length
                if (!SharePatchFileUtil.checkIfMd5Valid(largeModeInfo.md5)) {
                    System.err.println(String.format("resource meta file md5 mismatch, type:%s, name: %s, md5: %s", ShareTinkerInternals.getTypeString(type), name, largeModeInfo.md5));
                    // manager.getPatchReporter().onPatchPackageCheckFail(patchFile, BasePatchInternal.getMetaCorruptedCode(type));
                    return false;
                }
                ZipEntry patchEntry = patchZipFile.getEntry(name);
                if (patchEntry == null) {
                    System.err.println("large mod patch entry is null. path:" + name);
                    // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, largeModeInfo.file, name, type);
                    return false;
                }

                ZipEntry baseEntry = apkFile.getEntry(name);
                if (baseEntry == null) {
                    System.err.println("resources apk entry is null. path:" + name);
                    // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, largeModeInfo.file, name, type);
                    return false;
                }
                InputStream oldStream = null;
                InputStream newStream = null;
                try {
                    oldStream = apkFile.getInputStream(baseEntry);
                    newStream = patchZipFile.getInputStream(patchEntry);
                    BSPatch.patchFast(oldStream, newStream, largeModeInfo.file);
                } finally {
                    StreamUtil.closeQuietly(oldStream);
                    StreamUtil.closeQuietly(newStream);
                }
                //go go go bsdiff get the
                if (!SharePatchFileUtil.verifyFileMd5(largeModeInfo.file, largeModeInfo.md5)) {
                    System.err.println("Failed to recover large modify file:%s" + largeModeInfo.file.getPath());
                    SharePatchFileUtil.safeDeleteFile(largeModeInfo.file);
                    // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, largeModeInfo.file, name, type);
                    return false;
                }
                System.err.println(String.format("success recover large modify file:%s, file size:%d, use time:%d", largeModeInfo.file.getPath(), largeModeInfo.file.length(), (System.currentTimeMillis() - largeStart)));
            }
            System.err.println("success recover all large modify and store resources use time:%d" + (System.currentTimeMillis() - start));
        } catch (Throwable e) {
//            e.printStackTrace();
            throw new RuntimeException("patch " + ShareTinkerInternals.getTypeString(type) +  " extract failed (" + e.getMessage() + ").", e);
        } finally {
            SharePatchFileUtil.closeZip(apkFile);
            SharePatchFileUtil.closeZip(patchZipFile);
        }
        return true;
    }
}
