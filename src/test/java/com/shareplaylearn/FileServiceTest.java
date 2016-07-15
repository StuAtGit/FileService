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


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shareplaylearn.exceptions.InternalErrorException;
import com.shareplaylearn.resources.FileResourceMethods;
import com.shareplaylearn.resources.FileListResource;
import com.shareplaylearn.resources.FileFormResource;
import com.shareplaylearn.models.ItemSchema;
import com.shareplaylearn.models.UserItem;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import spark.Response;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.Code.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * PowerMock runner is mucking up a bunch of classes, even with this:
 * @PowerMockIgnore( {"javax.management.*",
 * "sun.security.*", "com.amazonaws.*", "javax.xml.*"})
 */
public class FileServiceTest
{
    private TokenValidator tokenValidator;
    private static String submittedName = "TestUpload.txt.tmp";
    private static String userId = "TestId";
    private static String userName = "TestUser";
    private static String accessToken = "TestToken";
    private static String requestedFilename = "TestUpload.txt";
    private static String testUploadDir = "testUploads";
    private static String nonExistentFilename = "THisFileDoesNotExist123123.txt";

    private static byte[] testFileBytes;
    private static String testFileContents;

    public FileServiceTest( ) throws IOException {
        tokenValidator = mock( TokenValidator.class );
        testFileBytes = Files.readAllBytes( FileSystems.getDefault().getPath(
                testUploadDir + File.separator +
                requestedFilename) );
        testFileContents = new String( testFileBytes, StandardCharsets.UTF_8);
    }

    /**
     * @throws IOException
     * @throws InternalErrorException
     */
    @Test
    public void testUpload() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(true);
        Response uploadResponse = mock(Response.class);
        Path path = FileSystems.getDefault().getPath("testUploads/TestUpload.txt");
        InputStream testFile = Files.newInputStream(path);

        int contentLength = (int)Files.size(path);
        String contentType = "application/text";

        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        FileFormResource.uploadFile( uploadResponse, testFile, submittedName,
                userId, userName, accessToken, requestedFilename, contentLength,
                contentType );
        verify(uploadResponse).status(CREATED.getCode());
        verify(uploadResponse).body((String) arg.capture());
        System.out.println(arg.getValue().toString());

    }

    @Test
    public void testGetFileList() throws IOException, InternalErrorException {
        testUpload();
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(true);
        Response fileListResponse = mock(Response.class);
        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        //returned response will be a null, because it's a method on a mock
        FileListResource.getFileList(userName, userId, accessToken, fileListResponse);
        verify(fileListResponse).status(OK.getCode());
        verify(fileListResponse).body((String) arg.capture());
        Gson gson = new Gson();
        Type type = new TypeToken< List<UserItem> >(){}.getType();
        List<UserItem> userItemList = gson.fromJson((String) arg.getValue(),type);
        boolean found = false;
        for( UserItem item : userItemList ) {
            if( item.getPreferredLocation().itemName.equals(requestedFilename) ) {
                found = true;
            }
        }
        System.out.println(arg.getValue());
        assertTrue(found);
    }

    @Test
    public void testGetFile() throws IOException, InternalErrorException {
        testUpload();
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(true);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(byte[].class);
        String response = FileResourceMethods.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                requestedFilename, null, fileResponse);
        verify(out).write((byte[]) arg.capture());
        verify(mockRaw).setContentType("application/octect-stream");
        Arrays.equals( testFileBytes, (byte[]) arg.getValue());
    }

    @Test
    public void testGetNonExistentFile() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(true);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(byte[].class);
        String response = FileResourceMethods.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                nonExistentFilename, null, fileResponse);
        verify(out,never()).write((byte[]) arg.capture());
        verify(fileResponse).status(NOT_FOUND.getCode());
    }

    @Test
    public void testGetFileDenied() throws IOException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(false);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        String response = FileResourceMethods.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                requestedFilename, null, fileResponse);

        verify(fileResponse).status(UNAUTHORIZED.getCode());
        verify(fileResponse).body(UNAUTHORIZED.toString());
    }

    @Test
    public void testPostFileDenied() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(false);
        Response uploadResponse = mock(Response.class);
        Path path = FileSystems.getDefault().getPath("testUploads/TestUpload.txt");
        InputStream testFile = Files.newInputStream(path);

        int contentLength = (int)Files.size(path);
        String contentType = "application/text";

        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        FileFormResource.uploadFile( uploadResponse, testFile, submittedName,
                userId, userName, accessToken, requestedFilename, contentLength,
                contentType );
        verify(uploadResponse).status(UNAUTHORIZED.getCode());
        verify(uploadResponse).body(UNAUTHORIZED.toString());
    }

    @Test
    public void testGetFilelistDenied() throws IOException {
        /**
         * TODO: factor out these tests a little...
         */
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(anyString()) ).thenReturn(false);
        Response fileListResponse = mock(Response.class);
        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        //returned response will be a null, because it's a method on a mock
        FileListResource.getFileList(userName, userId, accessToken, fileListResponse);
        verify(fileListResponse).status(UNAUTHORIZED.getCode());
        verify(fileListResponse).body(UNAUTHORIZED.toString());
    }
}
