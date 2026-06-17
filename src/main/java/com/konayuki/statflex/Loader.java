package com.konayuki.statflex;

import java.io.File;
import java.io.FileOutputStream;
import java.security.CodeSource;

public class Loader {
    public static void start() {
        try {
            CodeSource codeSource = Loader.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                throw new IllegalStateException("Unable to resolve statflex jar location.");
            }

            JarLauncher.launch(codeSource.getLocation(), Loader.class.getClassLoader());
            System.out.println("statflex successfully injected!");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void start(byte[] jarBytes) {
        try {
            File tempJar = File.createTempFile("statflex_temp", ".jar");
            tempJar.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tempJar)) {
                fos.write(jarBytes);
            }

            JarLauncher.launch(tempJar.toURI().toURL(), Loader.class.getClassLoader());
            System.out.println("statflex successfully injected!");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
