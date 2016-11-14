package com.github.tornaia.sync.e2e;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.*;

public class DirectoryAssertUtils {

    private Path expectedRoot;

    private DirectoryAssertUtils() {
    }

    // TODO this class has a poor design
    public static void assertDirectoriesEquality(Path expectedRoot, Path actualRoot) {
        DirectoryAssertUtils directoryAssertUtils = new DirectoryAssertUtils();
        directoryAssertUtils.expectedRoot = expectedRoot;
        directoryAssertUtils.assertRecursively(expectedRoot, actualRoot);
    }

    /**
     * Asserts that two directories are recursively equal. If they are not, an {@link AssertionError} is thrown with the
     * given message.<br/>
     * There will be a binary comparison of all files under expected with all files under actual. File attributes will
     * not be considered.<br/>
     * Missing or additional files are considered an error.<br/>
     *
     * @param expected Path expected directory
     * @param actual   Path actual directory
     */
    public void assertRecursively(Path expected, Path actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        Path absoluteExpected = expected.toAbsolutePath();
        Path absoluteActual = actual.toAbsolutePath();
        try {
            Files.walkFileTree(expected, new FileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path actualDir, BasicFileAttributes attrs) throws IOException {
                    Path relativeExpectedDir = absoluteExpected.relativize(actualDir.toAbsolutePath());
                    Path expectedDir = absoluteActual.resolve(relativeExpectedDir);

                    if (!Files.exists(expectedDir)) {
                        fail(String.format("Directory not found. Expected \'%s\', actual \'%s\'.", expectedDir.toFile().getAbsolutePath(), actualDir.toFile().getAbsolutePath()));
                    }

                    File expectedDirAsFile = expectedDir.toFile();
                    String expectedAbsPath = expectedDirAsFile.getAbsolutePath();
                    File actualDirAsFile = absoluteExpected.resolve(relativeExpectedDir).toFile();
                    String actualAbsPath = actualDirAsFile.getAbsolutePath();
                    String sizeErrorMessage = String.format("Directory size mismatch. Expected \'%s\', actual \'%s\'", expectedDir, actualAbsPath);
                    assertEquals(sizeErrorMessage, new File(expectedAbsPath).list().length, new File(actualAbsPath).list().length);

                    if (actualDir.equals(expectedRoot)) {
                        return FileVisitResult.CONTINUE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path expectedFile, BasicFileAttributes attrs) throws IOException {
                    Path relativeExpectedFile = absoluteExpected.relativize(expectedFile.toAbsolutePath());
                    Path actualFile = absoluteActual.resolve(relativeExpectedFile);

                    if (!Files.exists(actualFile)) {
                        fail(String.format("File \'%s\' missing in target.", expectedFile.getFileName()));
                    }

                    File expectedFileAsFile = relativeExpectedFile.toFile().getAbsoluteFile();
                    String expectedFileAbsolutePath = expectedFileAsFile.getAbsolutePath();
                    File actualFileAsFile = actualFile.toFile();
                    String actualFileAbsolutePath = actualFileAsFile.getAbsolutePath();
                    byte[] expectedFileContent = Files.readAllBytes(expectedFile);
                    byte[] actualFileContent = Files.readAllBytes(actualFile);
                    long expectedFileSize = expectedFileContent.length;
                    long actualFileSize = actualFileContent.length;

                    assertEquals(String.format("File size of \'%s\' and \'%s\' differ. ", expectedFileAbsolutePath, actualFileAbsolutePath), expectedFileSize, actualFileSize);
                    assertArrayEquals(String.format("File content of \'%s\' and \'%s\' differ. ", expectedFileAbsolutePath, actualFileAbsolutePath), expectedFileContent, actualFileContent);

                    long expectedCreatedDateTime = getCreationTime(expectedFile);
                    long actualCreatedDateTime = getCreationTime(actualFile);
                    String creationErrorMessage = String.format("File creation datetime mismatch: \'%s\'. Expected \'%s\', actual \'%s\'", relativeExpectedFile, expectedCreatedDateTime, actualCreatedDateTime);
                    assertEquals(creationErrorMessage, expectedCreatedDateTime, actualCreatedDateTime);

                    long expectedModifiedDateTime = getLastModifiedTime(expectedFile);
                    long actualModifiedDateTime = getLastModifiedTime(actualFile);
                    String modifiedErrorMessage = String.format("File modified datetime mismatch: \'%s\'. Expected \'%s\', actual \'%s\'", relativeExpectedFile, expectedModifiedDateTime, actualModifiedDateTime);
                    assertEquals(modifiedErrorMessage, expectedModifiedDateTime, actualModifiedDateTime);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    fail(exc.getMessage());
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private static long getLastModifiedTime(Path path) throws IOException {
        return ((FileTime) Files.getAttribute(path, "basic:lastModifiedTime")).toMillis();
    }

    private static long getCreationTime(Path path) throws IOException {
        return ((FileTime) Files.getAttribute(path, "basic:creationTime")).toMillis();
    }
}