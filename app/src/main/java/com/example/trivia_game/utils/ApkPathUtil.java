package com.example.trivia_game.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApkPathUtil {
    public static String getApkPath() {
        try {
            //get project root directory
            String projectDir = System.getProperty("user.dir");

            //navigate to debug APK location
            Path apkPath = Paths.get(projectDir,
                    "app",
                    "build",
                    "outputs",
                    "apk",
                    "debug",
                    "app-debug.apk");

            File apkFile = apkPath.toFile();
            if (!apkFile.exists()) {
                throw new RuntimeException("APK not found at: " + apkPath);
            }

            return apkFile.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("Failed to locate APK: " + e.getMessage());
        }
    }
}
