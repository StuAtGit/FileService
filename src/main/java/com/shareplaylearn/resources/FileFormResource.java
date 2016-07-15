/**
 * Copyright 2016 Stuart Smith
 *
 * This files is part of a program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.shareplaylearn.resources;

import com.amazonaws.AmazonClientException;
import com.shareplaylearn.UserItemManager;
import com.shareplaylearn.exceptions.Exceptions;
import com.shareplaylearn.exceptions.InternalErrorException;
import com.shareplaylearn.exceptions.QuotaExceededException;
import com.shareplaylearn.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

import static org.eclipse.jetty.http.HttpStatus.Code.*;

/**
 * Created by stu on 4/24/16.
 */
public class FileFormResource {

    protected static Logger log = LoggerFactory.getLogger(FileFormResource.class);

    private static String getFormString( Request req, String fieldName ) throws IOException, ServletException {
        if( fieldName == null ) {
            log.warn("Tried to retrieve a form string that was null.");
            return null;
        }
        Part part = req.raw().getPart(fieldName);
        if( part == null ) {
            log.warn("Part for field name: " + fieldName + " was null.");
            return null;
        }
        return IOUtils.toString( part.getInputStream() );
    }

    public static String handleFormPost(Request req, Response res) {
        try {
            //https://github.com/perwendel/spark/issues/26#issuecomment-95077039
            if (req.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
                MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
                req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
            }

            Part filePart = req.raw().getPart("file");
            String submittedFilename = filePart.getSubmittedFileName();
            InputStream file = filePart.getInputStream();
            String userId = getFormString(req,"user_id");
            String userName = getFormString(req,"user_name");
            String accessToken = getFormString(req,"access_token");
            String requestedFilename = getFormString(req,"filename");
            int contentLength = req.contentLength();
            String contentType = req.contentType();

            return uploadFile( res, file, submittedFilename, userId, userName, accessToken,
                    requestedFilename, contentLength, contentType);
        } catch (IOException | ServletException | InternalErrorException e) {
            log.error(Exceptions.asString(e));
            res.status(INTERNAL_SERVER_ERROR.getCode());
            res.body(e.getMessage());
            return res.body();
        } catch ( Throwable t ) {
            log.error(Exceptions.asString(t));
            res.status(INTERNAL_SERVER_ERROR.getCode());
            res.body(t.getMessage());
            return res.body();
        }
    }

    public static String uploadFile (
            Response res,
            InputStream file, String submittedFilename,
            String userId, String userName,
            String accessToken, String requestedFilename,
            int contentLength, String contentType
    ) throws IOException, InternalErrorException {

        if (accessToken == null || accessToken.trim().length() == 0) {
            res.status(BAD_REQUEST.getCode());
            res.body("No access token given.");
            return res.body();
        } else {
            if(!FileService.tokenValidator.isValid(accessToken)) {
                res.status(UNAUTHORIZED.getCode());
                res.body(UNAUTHORIZED.toString());
                return res.body();
            }
        }

        if (file == null) {
            res.status(BAD_REQUEST.getCode());
            res.body("No file given");
            return res.body();
        }
        String filename = requestedFilename;
        if (filename == null || filename.trim().length() == 0) {
            filename = submittedFilename;
            if (filename == null || filename.trim().length() == 0) {
                res.status(BAD_REQUEST.getCode());
                res.body("Could not determine filename, requested filename: " + requestedFilename
                        + " submitted filename " +
                        " " + submittedFilename );
                return res.body();
            }
        }
        if (userId == null || userId.trim().length() == 0) {
            res.status(BAD_REQUEST.getCode());
            res.body("No user id given.");
            return res.body();
        }
        if(userName == null || userId.trim().length() == 0) {
            res.status(BAD_REQUEST.getCode());
            res.body("No user name given.");
            return res.body();
        }
        if( contentLength <= 0 ) {
            res.status(BAD_REQUEST.getCode());
            res.body("Content length was invalid: " + contentLength);
            return res.body();
        }

        //Do we need this? What it is a good for?
        //I think we may have tried to use this with the S3 API ?
        if( contentType == null || contentType.trim().length() == 0 ) {
            res.status(BAD_REQUEST.getCode());
            res.body("Content type was null or empty.");
            return res.body();
        }

        UserItemManager userItemManager = new UserItemManager( userName, userId );
        byte[] fileBuffer = org.apache.commons.io.IOUtils.toByteArray(file);
        try {
            userItemManager.addItem( filename, fileBuffer );
            res.status(CREATED.getCode());
            res.body(CREATED.toString());
            return res.body();
        } catch (QuotaExceededException e) {
            res.status( INSUFFICIENT_STORAGE.getCode() );
            res.body( Exceptions.asString(e) );
            return res.body();
        } catch (AmazonClientException e) {
            res.status(INTERNAL_SERVER_ERROR.getCode());
            res.body(Exceptions.asString(e));
            return res.body();
        }
    }

}
