package com.geansea.gszip;

import android.support.annotation.NonNull;

import com.geansea.gszip.util.GsZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class GsZip {
    public static boolean unpackToFolder(@NonNull String zipPath, @NonNull String dirPath, @NonNull String password) {
        try {
            GsZipFile zip = GsZipFile.create(zipPath);
            GsZipUtil.check(zip != null, "Cannot open zip: " + zipPath);
            File dir = new File(dirPath);
            GsZipUtil.check(!dir.exists(), "Folder already exist: " + dirPath);
            GsZipUtil.check(dir.mkdirs(), "Make dirs Failed");
            if (zip.needPassword()) {
                GsZipUtil.check(!password.isEmpty(), "Password is empty");
                zip.setPassword(password);
            }
            for (int index = 0; index < zip.size(); ++index) {
                GsZipEntry entry = zip.getEntry(index);
                GsZipUtil.check(entry != null, "Entry is null for index: " + index);
                File file = new File(dir.getPath(), entry.getName());
                if (entry.isFile()) {
                    GsZipUtil.check(!file.exists(), "File already exists");
                    if (!file.getParentFile().exists()) {
                        GsZipUtil.check(file.getParentFile().mkdirs(), "Create parent dirs fail");
                    }
                    GsZipUtil.check(file.createNewFile(), "Create file fail");
                    InputStream entryStream = zip.getInputStream(index);
                    GsZipUtil.check(entryStream != null, "Entry stream is null for index: " + index);
                    OutputStream outStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = entryStream.read(buffer)) > 0) {
                        outStream.write(buffer, 0, count);
                    }
                } else {
                    if (!file.exists()) {
                        GsZipUtil.check(file.mkdirs(), "Create dirs fail");
                    }
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean packFolder(@NonNull String dirPath,
                                     @NonNull String zipPath,
                                     @NonNull String password,
                                     boolean includingSelf) {
        try {
            File dir = new File(dirPath);
            GsZipUtil.check(dir.exists(), "Folder not exist");
            LinkedList<String> paths = new LinkedList<>();
            if (dir.isFile()) {
                GsZipUtil.check(includingSelf, "Should set including self when pack file");
                String path = dir.getName();
                paths.add(path);
                dir = dir.getParentFile();
            } else {
                if (includingSelf) {
                    String path = dir.getName();
                    paths.add(path);
                    collectFilePaths(dir, path, paths);
                    dir = dir.getParentFile();
                } else {
                    collectFilePaths(dir, "", paths);
                }
            }
            GsZipPacker packer = new GsZipPacker();
            for (String path : paths) {
                File file = new File(dir, path);
                GsZipUtil.check(file.exists(), "Path error");
                if (file.isFile()) {
                    packer.addFile(path, file.getAbsolutePath());
                } else {
                    packer.addFolder(path);
                }
            }
            GsZipUtil.check(packer.packTo(zipPath, password), "Pack fail");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean packFile(@NonNull String filePath,
                                   @NonNull String zipPath,
                                   @NonNull String password) {
        return packFolder(filePath, zipPath, password, true);
    }

    private static void collectFilePaths(@NonNull File folder,
                                         @NonNull String baseDir,
                                         @NonNull LinkedList<String> paths) {
        String[] names = folder.list();
        if (names == null || names.length == 0) {
            return;
        }
        for (String name : names) {
            File file = new File(folder, name);
            String path = baseDir.isEmpty() ? name : (baseDir + "/" + name);
            paths.add(path);
            if (file.isDirectory()) {
                collectFilePaths(file, path, paths);
            }
        }
    }
}
