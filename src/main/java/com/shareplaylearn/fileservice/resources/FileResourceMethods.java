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
package com.shareplaylearn.fileservice.resources;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.shareplaylearn.UserItemManager;
import com.shareplaylearn.exceptions.Exceptions;
import com.shareplaylearn.exceptions.UnsupportedEncodingException;
import com.shareplaylearn.fileservice.FileService;
import com.shareplaylearn.models.ItemSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.eclipse.jetty.http.HttpStatus.Code.*;

/**
 * Created by stu on 7/10/16.
 */
@SuppressWarnings("WeakerAccess")
public class FileResourceMethods {

    public static Logger log = LoggerFactory.getLogger(FileResourceMethods.class);

    public static String getFile(Request req, Response res ) {
        String userName = req.params("userName");
        String userId = req.params("userId");
        String fileType = req.params("fileType");
        String presentationType = req.params("presentationType");
        String filename = req.params("filename");
        String encoding = req.queryParams("encoding");
        if( encoding != null ) {
            encoding = encoding.toUpperCase();
        }
        String accessToken = req.headers(FileService.AUTHENTICATION_HEADER);
        if( accessToken.startsWith("Bearer") ) {
            String[] tokenFields = accessToken.split(" ");
            if( tokenFields.length > 1 ) {
                accessToken = tokenFields[1];
            }
        }

        try {
            return getFile(userName, userId, accessToken, fileType, presentationType,
                    filename, encoding, res);
        } catch (IOException e) {
            res.status(INTERNAL_SERVER_ERROR.getCode());
            res.body(Exceptions.asString(e));
            return res.body();
        }
    }

    //TODO: tests this (for both identity, null, and base64 encoding)
    //TODO: then I think we're ready to deploy, test, and start porting the UI!
    public static String getFile( String userName, String userId, String accessToken,
                           String fileType, String presentationTypeArg,
                           String filename, String encoding,
                           Response res ) throws IOException {

        if( !FileService.tokenValidator.isValid(accessToken) ) {
            res.status(UNAUTHORIZED.getCode());
            res.body(UNAUTHORIZED.toString());
            return res.body();
        }
        UserItemManager userItemManager = new UserItemManager( userName, userId );
        ItemSchema.PresentationType presentationType;
        try {
            presentationType = ItemSchema.PresentationType.fromString(presentationTypeArg);
        } catch (IllegalArgumentException e ) {
            res.status(BAD_REQUEST.getCode());
            res.body("Invalid presentation type: " + presentationTypeArg);
            log.info( Exceptions.asString(e) );
            return res.body();
        }
        try {
            byte[] bytes = userItemManager.getItem( fileType, presentationType,
                    filename, encoding );
            res.status(OK.getCode());
            res.raw().setContentType("application/octect-stream");
            res.raw().getOutputStream().write(bytes);
            return res.body();
        } catch (UnsupportedEncodingException e) {
            res.status(BAD_REQUEST.getCode());
            res.body("Unsupported encoding: " + encoding);
            log.info( Exceptions.asString(e) );
            return res.body();
        } catch(AmazonS3Exception e) {

            //this exception is what we get back for not found, forbidden, etc.
            log.info( e.getMessage() );
            log.info( e.getErrorMessage() );
            Map<String,String> additionalDetails = e.getAdditionalDetails();
            log.info( Arrays.toString(additionalDetails.values().toArray()));
            log.info( e.getStatusCode() + "" );

            res.body( e.getErrorMessage() );
            res.status( e.getStatusCode() );
            return res.body();
        }
    }
}
