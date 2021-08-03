package com.geekbrains.cloudstorage.common;

import java.io.File;
import java.io.Serializable;

/**
 * Класс используется для передачи информации о загружаемом или скачиваемом файле в обработчик FileProcessHandler
 * */
public class FileProcess implements Serializable {

    private File file;
    private long size;
    private String processDirection;

    public FileProcess(File file, long size, String processDirection) {
        this.file = file;
        this.size = size;
        this.processDirection = processDirection;
    }

    public File getFile() {
        return file;
    }

    public long getSize() {
        return size;
    }

    public String getProcessDirection() {
        return processDirection;
    }
}
