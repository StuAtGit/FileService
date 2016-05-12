package com.shareplaylearn.fileservice.resources;

import spark.Request;
import spark.Response;
import spark.utils.IOUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by stu on 4/24/16.
 */
public class FormResource {

    private static String getFormString( Request req, String fieldName ) throws IOException, ServletException {
        Part part = req.raw().getPart(fieldName);
        return IOUtils.toString( part.getInputStream() );
    }

    public static String handleFormPost(Request req, Response res) {
        //https://github.com/perwendel/spark/issues/26#issuecomment-95077039
        if (req.raw().getAttribute("org.eclipse.jetty.multipartConfig") == null) {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));
            req.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
            try {
                Part filePart = req.raw().getPart("file");
                String submittedFileName = filePart.getSubmittedFileName();
                InputStream file = filePart.getInputStream();
                String userId = getFormString(req,"user_id");
                String userName = getFormString(req,"user_name");
                String accessToken = getFormString(req,"access_token");
                String requestedFilename = getFormString(req,"filename");
                int contentLength = req.contentLength();
                String contentType = req.contentType();

                if (file == null) {
                    res.status(400);
                    res.body("No file given");
                    return res.body();
                }
                String filename = requestedFilename;
                if (filename == null || filename.trim().length() == 0) {
                    filename = submittedFileName;
                    if (filename == null || filename.trim().length() == 0) {
                        res.status(400);
                        res.body("Could not determine filename, requested filename: " + requestedFilename
                                + " submitted filename " +
                                " " + submittedFileName );
                        return res.body();
                    }
                }
                if (userId == null || userId.trim().length() == 0) {
                    res.status(400);
                    res.body("No user id given.");
                    return res.body();
                }
                if (accessToken == null || accessToken.trim().length() == 0) {
                    res.status(400);
                    res.body("No access token given.");
                    return res.body();
                }
                if(userName == null || userId.trim().length() == 0) {
                    res.status(400);
                    res.body("No user name given.");
                    return res.body();
                }
                if( contentLength <= 0 ) {
                    res.status(400);
                    res.body("Content length was invalid: " + contentLength);
                    return res.body();
                }
                if( contentType == null || contentType.trim().length() == 0 ) {
                    res.status(400);
                    res.body("Content type was null or empty.");
                    return res.body();
                }

                //TODO: check access token
                byte[] fileBuffer = org.apache.commons.io.IOUtils.toByteArray(file);

            } catch (IOException | ServletException e) {
                res.status(500);
                res.body(e.getMessage());
                return res.body();
            }
        }
        String resourceLocation = "";
        res.status(201);
        //returning this will require an async form (or we have to do the html entity, like before).
        res.body( "/api/file/" + resourceLocation );
        return res.body();
    }
}
