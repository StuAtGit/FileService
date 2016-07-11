package com.shareplaylearn.fileservice.resources;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shareplaylearn.UserItemManager;
import com.shareplaylearn.exceptions.Exceptions;
import com.shareplaylearn.fileservice.FileService;
import com.shareplaylearn.models.UserItem;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.Code.INTERNAL_SERVER_ERROR;
import static org.eclipse.jetty.http.HttpStatus.Code.OK;
import static org.eclipse.jetty.http.HttpStatus.Code.UNAUTHORIZED;

/**
 * Created by stu on 7/10/16.
 * A per-user list of files they have uploaded.
 */
public class FileList {
    public static String getFileList(Request req, Response res) throws IOException {
        String userName = req.params("userName");
        String userId = req.params("userId");
        String accessToken = req.headers(FileService.AUTHENTICATION_HEADER);
        if( accessToken.startsWith("Bearer") ) {
            String[] tokenFields = accessToken.split(" ");
            if( tokenFields.length > 1 ) {
                accessToken = tokenFields[1];
            }
        }
        return getFileList(userName, userId, accessToken, res);
    }

    /**
     * This is split out for easier unit testing
     * @param userName
     * @param userId
     * @param accessToken
     * @param res
     * @return
     * @throws IOException
     */
    public static String getFileList(String userName,
                                String userId,
                                String accessToken,
                                Response res) throws IOException {
        if( !FileService.tokenValidator.isValid(accessToken) ) {
            res.body(UNAUTHORIZED.toString());
            res.status(UNAUTHORIZED.getCode());
            return res.body();
        }
        res.status(OK.getCode());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        UserItemManager userItemManager = new UserItemManager( userName, userId );
        List<UserItem> fileList;
        try {
            fileList = userItemManager.getItemList();
        } catch ( AmazonClientException e ) {
            res.status(INTERNAL_SERVER_ERROR.getCode());
            res.body(Exceptions.asString(e));
            return res.body();
        }
        res.body( gson.toJson(fileList) );
        return res.body();
    }
}
