package com.shareplaylearn.fileservice;

import com.shareplaylearn.fileservice.resources.FormResource;

import static spark.Spark.*;
/**
 * Hello world!
 *
 */
public class FileService
{
    public static void main( String[] args )
    {
        get( "/api/status", (req,res) -> {
            res.status(200);
            return "OK";
        });

        post( "/api/file/form", (req,res) -> FormResource.handleFormPost(req, res) );
    }
}
