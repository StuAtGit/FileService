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
import com.shareplaylearn.resources.FileResource;
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
import static org.mockito.Matchers.eq;
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
        when( FileService.tokenValidator.isValid(eq(accessToken), eq(userId)) ).thenReturn(true);
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
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(true);
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
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(true);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(byte[].class);
        String response = FileResource.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                requestedFilename, null, fileResponse);
        verify(out).write((byte[]) arg.capture());
        verify(mockRaw).setContentType("application/octect-stream");
        Arrays.equals( testFileBytes, (byte[]) arg.getValue());
        /**
         * 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
         */
    }

    @Test
    public void testGetNonExistentFile() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(true);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(byte[].class);
        String response = FileResource.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                nonExistentFilename, null, fileResponse);
        verify(out,never()).write((byte[]) arg.capture());
        verify(fileResponse).status(NOT_FOUND.getCode());
    }

    @Test
    public void testGetFileDenied() throws IOException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(false);
        Response fileResponse = mock(Response.class);
        HttpServletResponse mockRaw = mock(HttpServletResponse.class);
        ServletOutputStream out = mock(ServletOutputStream.class);
        when(mockRaw.getOutputStream()).thenReturn(out);
        when(fileResponse.raw()).thenReturn(mockRaw);

        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        String response = FileResource.getFile(userName, userId, accessToken, "unknown",
                ItemSchema.PresentationType.ORIGINAL_PRESENTATION_TYPE.toString(),
                requestedFilename, null, fileResponse);

        verify(fileResponse).status(UNAUTHORIZED.getCode());
        verify(fileResponse).body(UNAUTHORIZED.toString());
    }

    @Test
    public void testPostFileDenied() throws IOException, InternalErrorException {
        FileService.tokenValidator = tokenValidator;
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(false);
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
        when( FileService.tokenValidator.isValid(accessToken, userId) ).thenReturn(false);
        Response fileListResponse = mock(Response.class);
        ArgumentCaptor arg = ArgumentCaptor.forClass(String.class);
        //returned response will be a null, because it's a method on a mock
        FileListResource.getFileList(userName, userId, accessToken, fileListResponse);
        verify(fileListResponse).status(UNAUTHORIZED.getCode());
        verify(fileListResponse).body(UNAUTHORIZED.toString());
    }
}
