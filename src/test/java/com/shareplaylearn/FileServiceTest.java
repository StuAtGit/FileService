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
package com.shareplaylearn;


import com.shareplaylearn.exceptions.InternalErrorException;
import com.shareplaylearn.fileservice.FileService;
import com.shareplaylearn.fileservice.resources.ItemForm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.runners.JUnit44RunnerImpl;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import spark.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.eclipse.jetty.http.HttpStatus.Code.CREATED;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * PowerMock runner is mucking up a bunch of classes, even with this:
 * @PowerMockIgnore( {"javax.management.*",
 * "sun.security.*", "com.amazonaws.*", "javax.xml.*"})
 * I still have stuff failing left and right, so just use junit runner
 */
public class FileServiceTest
{
    TokenValidator tokenValidator;
    public FileServiceTest( ) {
        tokenValidator = mock( TokenValidator.class );
    }

    @Test
    public void testUpload() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(true);
        Response uploadResponse = mock(Response.class);
        Path path = FileSystems.getDefault().getPath("testUploads/TestUpload.txt");
        InputStream testFile = Files.newInputStream(path);
        String submittedName = "TestUpload.txt";
        String userId = "TestId";
        String userName = "TestUser";
        String accessToken = "TestToken";
        String requestedFilename = "TestFile.txt.tmp";
        int contentLength = (int)Files.size(path);
        String contentType = "application/text";

        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        System.out.println( ItemForm.uploadFile( uploadResponse, testFile, submittedName,
                userId, userName, accessToken, requestedFilename, contentLength,
                contentType ) );
        verify(uploadResponse).status(CREATED.getCode());
        verify(uploadResponse).body((String) arg.capture());
        System.out.println(arg.getValue().toString());

    }
}
