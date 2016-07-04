/**
 * Copyright 2016 Stuart Smith
 *
 * This program is free software: you can redistribute it and/or modify
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
package com.shareplaylearn.fileservice;

import com.shareplaylearn.TokenValidator;
import com.shareplaylearn.fileservice.resources.ItemForm;

import static spark.Spark.get;
import static spark.Spark.post;

public class FileService
{
    public static TokenValidator tokenValidator;

    public static void main( String[] args )
    {
        String validationResource = "https://www.shareplaylearn.com/auth_api/oauthToken_validation";
        int validationCacheSize = 10000;
        int validationCacheTime = 24 * 3600;
        tokenValidator = new TokenValidator( validationResource, validationCacheSize, validationCacheTime );

        get( "/file_api/status", (req,res) -> {
            res.status(200);
            return "OK";
        });

        post( "/file_api/file/form", (req,res) -> ItemForm.handleFormPost(req, res) );
    }
}
