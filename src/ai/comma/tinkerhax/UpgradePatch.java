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

// import com.tencent.tinker.lib.service.PatchResult;
// import com.tencent.tinker.lib.tinker.Tinker;
// import com.tencent.tinker.lib.util.TinkerLog;
// import com.tencent.tinker.loader.shareutil.ShareConstants;
// import com.tencent.tinker.loader.shareutil.SharePatchInfo;
// import com.tencent.tinker.loader.shareutil.ShareSecurityCheck;
// import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import com.tencent.tinker.commons.util.StreamUtil;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipEntry;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipFile;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipOutputStream;
// import com.tencent.tinker.ziputils.ziputil.TinkerZipUtil;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * generate new patch, you can implement your own patch processor class
 * Created by zhangshaowen on 16/3/14.
 */
public class UpgradePatch {
    private static final String TAG = "Tinker.UpgradePatch";

    // @Override
    public boolean tryPatch(String apkPath, String tempPatchPath, String outApkPath, PatchResult patchResult) {
        final File patchFile = new File(tempPatchPath);

        if (!SharePatchFileUtil.isLegalFile(patchFile)) {
            System.err.println("UpgradePatch tryPatch:patch file is not found, just return");
            return false;
        }

        ShareSecurityCheck signatureCheck = new ShareSecurityCheck();
        // int returnCode = ShareTinkerInternals.checkTinkerPackage(context, manager.getTinkerFlags(), patchFile, signatureCheck);
        signatureCheck.verifyPatchMetaSignature(patchFile); // side effects

        String patchMd5 = SharePatchFileUtil.getMD5(patchFile);
        if (patchMd5 == null) {
            System.err.println("UpgradePatch tryPatch:patch md5 is null, just return");
            return false;
        }
        //use md5 as version
        patchResult.patchVersion = patchMd5;

        System.err.println("UpgradePatch tryPatch:patchMd5: " + patchMd5);

        //check ok, we can real recover a new patch
        // final String patchDirectory = manager.getPatchDirectory().getAbsolutePath();
        // final String patchDirectory = "/Volumes/gext/drive/apkpatch/patchdir"; //QQQ

        // File patchInfoLockFile = SharePatchFileUtil.getPatchInfoLockFile(patchDirectory);
        // File patchInfoFile = SharePatchFileUtil.getPatchInfoFile(patchDirectory);

        // SharePatchInfo oldInfo = SharePatchInfo.readAndCheckPropertyWithLock(patchInfoFile, patchInfoLockFile);

        //it is a new patch, so we should not find a exist
        // SharePatchInfo newInfo;

        //already have patch
        // if (oldInfo != null) {
        //     if (oldInfo.oldVersion == null || oldInfo.newVersion == null || oldInfo.oatDir == null) {
        //         TinkerLog.e(TAG, "UpgradePatch tryPatch:onPatchInfoCorrupted");
        //         manager.getPatchReporter().onPatchInfoCorrupted(patchFile, oldInfo.oldVersion, oldInfo.newVersion);
        //         return false;
        //     }

        //     if (!SharePatchFileUtil.checkIfMd5Valid(patchMd5)) {
        //         TinkerLog.e(TAG, "UpgradePatch tryPatch:onPatchVersionCheckFail md5 %s is valid", patchMd5);
        //         manager.getPatchReporter().onPatchVersionCheckFail(patchFile, oldInfo, patchMd5);
        //         return false;
        //     }
        //     // if it is interpret now, use changing flag to wait main process
        //     final String finalOatDir = oldInfo.oatDir.equals(ShareConstants.INTERPRET_DEX_OPTIMIZE_PATH)
        //         ? ShareConstants.CHANING_DEX_OPTIMIZE_PATH : oldInfo.oatDir;
        //     newInfo = new SharePatchInfo(oldInfo.oldVersion, patchMd5, Build.FINGERPRINT, finalOatDir);
        // } else {
        //     newInfo = new SharePatchInfo("", patchMd5, Build.FINGERPRINT, ShareConstants.DEFAULT_DEX_OPTIMIZE_PATH);
        // }

        //it is a new patch, we first delete if there is any files
        //don't delete dir for faster retry
//        SharePatchFileUtil.deleteDir(patchVersionDirectory);
        final String patchName = SharePatchFileUtil.getPatchVersionDirectory(patchMd5);

        // final String patchVersionDirectory = patchDirectory + "/" + patchName;

        // System.err.println("UpgradePatch tryPatch:patchVersionDirectory:%s" + patchVersionDirectory);

        //copy file
//         File destPatchFile = new File(patchVersionDirectory + "/" + SharePatchFileUtil.getPatchVersionFile(patchMd5));

//         try {
//             // check md5 first
//             if (!patchMd5.equals(SharePatchFileUtil.getMD5(destPatchFile))) {
//                 SharePatchFileUtil.copyFileUsingStream(patchFile, destPatchFile);
//                 System.err.println(String.format("UpgradePatch copy patch file, src file: %s size: %d, dest file: %s size:%d", patchFile.getAbsolutePath(), patchFile.length(),
//                     destPatchFile.getAbsolutePath(), destPatchFile.length()));
//             }
//         } catch (IOException e) {
// //            e.printStackTrace();
//             System.err.println(String.format("UpgradePatch tryPatch:copy patch file fail from %s to %s", patchFile.getPath(), destPatchFile.getPath()));
//             // manager.getPatchReporter().onPatchTypeExtractFail(patchFile, destPatchFile, patchFile.getName(), ShareConstants.TYPE_PATCH_FILE);
//             return false;
//         }

        try {
            File outApkFile = new File(outApkPath);
            ZipOutputStream outZip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outApkFile)));

            HashSet<String> added = new HashSet<String>();

            //we use destPatchFile instead of patchFile, because patchFile may be deleted during the patch process
            if (!DexDiffPatchInternal.tryRecoverDexFiles(apkPath, signatureCheck, outZip, patchFile, added)) {
                System.err.println("UpgradePatch tryPatch:new patch recover, try patch dex failed");
                return false;
            }

            if (!BsDiffPatchInternal.tryRecoverLibraryFiles(apkPath, signatureCheck, outZip, patchFile, added)) {
                System.err.println("UpgradePatch tryPatch:new patch recover, try patch library failed");
                return false;
            }

            if (!ResDiffPatchInternal.tryRecoverResourceFiles(apkPath, signatureCheck, outZip, patchFile, added)) {
                System.err.println("UpgradePatch tryPatch:new patch recover, try patch resource failed");
                return false;
            }


            ZipFile oldApk = new ZipFile(apkPath);
            outZip.setComment(oldApk.getComment());

            final Enumeration<? extends ZipEntry> entries = oldApk.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String entryName = zipEntry.getName();
                if (entryName.startsWith("META-INF")) continue;
                if (!added.contains(entryName)) {
                    System.err.println("extra " + entryName);

                    // hack
                    if (outApkPath.toLowerCase().contains("spotify") && entryName.contains("drawable") && entryName.contains("-v4")) {
                        String newName = entryName.replace("-v4", "");
                        SharePatchFileUtil.extractTinkerEntryRename(oldApk, zipEntry, outZip, newName);
                    } else {
                        SharePatchFileUtil.extractTinkerEntry(oldApk, zipEntry, outZip);
                    }
                }
            }

            StreamUtil.closeQuietly(oldApk);
            StreamUtil.closeQuietly(outZip);
        } catch (Throwable e) {
            throw new RuntimeException("fail", e);
        }

        // check dex opt file at last, some phone such as VIVO/OPPO like to change dex2oat to interpreted
        // if (!DexDiffPatchInternal.waitAndCheckDexOptFile(patchFile)) {
        //     System.err.println("UpgradePatch tryPatch:new patch recover, check dex opt file failed");
        //     return false;
        // }

        // if (!SharePatchInfo.rewritePatchInfoFileWithLock(patchInfoFile, newInfo, patchInfoLockFile)) {
        //     TinkerLog.e(TAG, "UpgradePatch tryPatch:new patch recover, rewrite patch info failed");
        //     manager.getPatchReporter().onPatchInfoCorrupted(patchFile, newInfo.oldVersion, newInfo.newVersion);
        //     return false;
        // }

        System.err.println("UpgradePatch tryPatch: done, it is ok");
        return true;
    }

}
