package com.konayuki.statflex;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class Loader {
    public static void start(byte[] jarBytes) {
        try {
            File tempJar = File.createTempFile("statflex_temp", ".jar");
            tempJar.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tempJar)) {
                fos.write(jarBytes);
            }

            URLClassLoader loader = new URLClassLoader(
                    new URL[]{tempJar.toURI().toURL()},
                    Loader.class.getClassLoader()
            );

            Class<?> mainClass = loader.loadClass("com.konayuki.statflex.Main");
            Method initMethod = mainClass.getDeclaredMethod("init");
            initMethod.invoke(null);

            System.out.println("StatFlex successfully injected!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}