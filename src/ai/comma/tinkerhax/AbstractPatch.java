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

package ai.comma.tinkerhax;

// import com.tencent.tinker.lib.service.PatchResult;

/**
 * Created by zhangshaowen on 16/3/15.
 */
public abstract class AbstractPatch {

    public abstract boolean tryPatch(String tempPatchPath, PatchResult patchResult);
}