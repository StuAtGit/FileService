package com.shareplaylearn.fileservice;

import com.shareplaylearn.fileservice.resources.ItemForm;

import static spark.Spark.get;
import static spark.Spark.post;

public class FileService
{
    public static void main( String[] args )
    {
        get( "/api/status", (req,res) -> {
            res.status(200);
            return "OK";
        });

        post( "/api/file/form", (req,res) -> ItemForm.handleFormPost(req, res) );
    }
}
