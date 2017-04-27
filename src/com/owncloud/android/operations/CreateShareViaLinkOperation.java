/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2017 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

/**
 * Creates a new public share for a given file
 */


import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

public class CreateShareViaLinkOperation extends SyncOperation {

    private String mPath;
    private String mName;
    private String mPassword;
    private long mExpirationDateInMillis;
    private Boolean mPublicUpload;

    /**
     * Constructor
     *
     * @param path Full path of the file/folder being shared. Mandatory argument
     *             Only available for public link shares
     */
    public CreateShareViaLinkOperation(
            String path
    ) {

        mPath = path;
        mName = null;
        mPassword = null;
        mExpirationDateInMillis = 0;
        mPublicUpload = null;
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }

    /**
     * Set name to update in public link.
     *
     * @param name Name to set to the public link.
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * Set password to update in public link.
     *
     * @param password Password to set to the public link.
     */
    public void setPassword(String password) {
        this.mPassword = password;
    }

    /**
     * Set expiration date to update in Share resource.
     *
     * @param expirationDateInMillis    Expiration date to set to the public link.
     */
    public void setExpirationDateInMillis(long expirationDateInMillis) {
        this.mExpirationDateInMillis = expirationDateInMillis;
    }

    /**
     * Enable upload permissions to update in Share resource.
     *
     * @param publicUpload Upload Permission to set to the public link.
     */
    public void setPublicUpload(Boolean publicUpload) {
        this.mPublicUpload = publicUpload;
    }

    public String getName() {
        return mName;
    }

    public Boolean getPublicUpload() {
        return mPublicUpload;
    }

    public long getExpirationDateInMillis() {
        return mExpirationDateInMillis;
    }

    public String getPath() {
        return mPath;
    }

    public String getPassword() {
        return mPassword;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        // Check if the share link already exists
        RemoteOperation operation = new GetRemoteSharesForFileOperation(mPath, false, false);
        RemoteOperationResult result = operation.execute(client);

        // TO DO - Check using capabilities
        // Create public link if doesn't exist yet
//        boolean publicShareExists = false;
//        if (result.isSuccess()) {
//            OCShare share;
//            for (int i = 0; i < result.getData().size(); i++) {
//                share = (OCShare) result.getData().get(i);
//                if (ShareType.PUBLIC_LINK.equals(share.getShareType())) {
//                    publicShareExists = true;
//                    break;
//                }
//            }
//        }

//        if (!publicShareExists) {
            CreateRemoteShareOperation createOp = new CreateRemoteShareOperation(
                    mPath,
                    ShareType.PUBLIC_LINK,
                    "",
                    mPublicUpload,
                    mPassword,
                    OCShare.DEFAULT_PERMISSION
            );
            createOp.setGetShareDetails(true);
            createOp.setName(mName);
            createOp.setExpirationDate(mExpirationDateInMillis);
            result = createOp.execute(client);
//        }

        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                Object item = result.getData().get(0);
                if (item instanceof OCShare) {
                    updateData((OCShare) item);
                } else {
                    ArrayList<Object> data = result.getData();
                    result = new RemoteOperationResult(
                            RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
                    );
                    result.setData(data);
                }
            } else {
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
            }
        }

        return result;
    }

    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }

        getStorageManager().saveShare(share);

        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file != null) {
            file.setSharedViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

}
