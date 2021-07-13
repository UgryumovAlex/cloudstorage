package com.geekbrains.cloudstorage.cloudserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StorageLogic {

    private final Path rootPath;  //корневой каталог на сервере, предоставленный подключившемуся пользователю
    private Path currentPath;  //текущий каталог, в котором находится пользователь

    public StorageLogic(Path rootPath) throws IOException {
        this.rootPath = rootPath;
        this.currentPath = rootPath;

        checkUserDirectory();
    }

    private void checkUserDirectory() throws IOException {
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectory(rootPath);
            } catch (IOException e) {
                throw new IOException("USER_DIRECTORY_ERROR");
            }
        }
    }

    public Path getUserPath() {
        return rootPath.relativize(currentPath);
    }

    public String getFilesList() {
        String[] files = new File(currentPath.toString()).list();
        StringBuilder sb = new StringBuilder();
        if (files != null && files.length > 0) {
            for (String file : files) {
                if (Files.isDirectory(Paths.get(currentPath.toString() + File.separator + file))) {
                    sb.append(file.toUpperCase()).append("\\..");
                } else {
                    sb.append(file);
                }
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    public String createDirectory(String newDir) throws IOException {
        Path newDirectory = Paths.get(currentPath.toString() + File.separator + newDir);
        if (Files.exists(newDirectory)) {
            throw new FileAlreadyExistsException("DIRECTORY_EXISTS_ALREADY"); //потомок IOEXCEPTION
        } else {
            Files.createDirectory(newDirectory);
        }
        return "DIRECTORY_CREATED";
    }

    public String createFile(String newFileName) throws IOException {
        Path newFile = Paths.get(currentPath.toString() + File.separator + newFileName);
        if (Files.exists(newFile)) {
            throw new FileAlreadyExistsException("FILE_EXISTS_ALREADY"); //потомок IOEXCEPTION

        } else {
            Files.createFile(newFile);
        }
        return "FILE_CREATED";
    }

    public void changeDirectory(String newDir) throws IllegalArgumentException {
        if ("~".equals(newDir)) {
            currentPath = rootPath;
        } else if ("..".equals(newDir)) {
            if (!currentPath.equals(rootPath)) {
                currentPath = currentPath.getParent();
            }
        } else {
            Path newPath = Paths.get(currentPath.toString() + File.separator + newDir);
            if (!Files.exists(newPath)) {
                throw new IllegalArgumentException("UNKNOWN_PATH");
            } else {
                currentPath = newPath;
            }
        }
    }

    public String removeFileOrDirectory(String pathToRemove) throws IOException {
        Path deletePath = Paths.get(currentPath.toString() + File.separator + pathToRemove);
        if (!Files.exists(deletePath)) {
            throw new NoSuchFileException("DO_NOT_EXIST");
        } else {
            try {
                Files.delete(deletePath); //В этом месте, если пытаемся удалить непустой каталог,
                                          // сгенерится DirectoryNotEmptyException
            } catch (DirectoryNotEmptyException e) {
                return "directory is not empty, delete anyway? Y/N\r\n";
            }
        }
        return "DELETED";
    }

    public void deleteNotEmptyDirectory(String directory) throws IOException {
        String dir = currentPath.toString() + File.separator + directory;
        deleteDirectory(Paths.get(dir));
    }

    private void deleteDirectory(Path directory) throws  IOException {
        String[] files = new File(directory.toString()).list();
        if (files != null && files.length > 0) {
            for (String file : files) {
                Path fileToDelete = Paths.get(directory + File.separator + file);
                if (Files.isDirectory(fileToDelete)) {
                    deleteDirectory(fileToDelete);
                } else {
                    try {
                        Files.delete(fileToDelete);
                    } catch (IOException e) {
                        throw new IOException("ERROR_DELETING_FILE");
                    }
                }
            }
        }
        try {
            Files.delete(directory);
        } catch (IOException e) {
            throw new IOException("ERROR_DELETING_DIRECTORY");
        }
    }

    public void copy (String source, String destination) throws IllegalArgumentException, IOException {
        Path sourcePath = Paths.get(currentPath.toString() + File.separator + source);
        Path destinationPath = Paths.get(currentPath.toString() + File.separator + destination);
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("SOURCE_NOT_FOUND");
        } else {
            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
