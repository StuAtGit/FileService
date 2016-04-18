package com.shareplaylearn;

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
    }
}
