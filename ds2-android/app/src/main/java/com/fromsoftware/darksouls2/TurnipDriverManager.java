package com.fromsoftware.darksouls2;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TurnipDriverManager {
    private static final String TAG = "TurnipDriverManager";
    public static final String SYSTEM_DRIVER_ID = "SYSTEM_QUALCOMM";

    public static class DriverItem {
        public String id;
        public String displayName;
        public String soPath;
        public boolean isSystem;

        public DriverItem(String id, String displayName, String soPath, boolean isSystem) {
            this.id = id;
            this.displayName = displayName;
            this.soPath = soPath;
            this.isSystem = isSystem;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final File driversRootDir;

    public TurnipDriverManager(Context context) {
        this.driversRootDir = new File(context.getFilesDir(), "drivers");
        if (!driversRootDir.exists()) {
            driversRootDir.mkdirs();
        }
    }

    public List<DriverItem> getAvailableDrivers() {
        List<DriverItem> list = new ArrayList<>();
        // Driver padrão do sistema sempre em primeiro
        list.add(new DriverItem(SYSTEM_DRIVER_ID, "Driver do Sistema (Qualcomm Adreno Vulkan)", "", true));

        File[] subDirs = driversRootDir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File dir : subDirs) {
                File mainSo = findMainSo(dir);
                if (mainSo != null && mainSo.exists()) {
                    String displayName = readDisplayName(dir);
                    list.add(new DriverItem(dir.getName(), displayName, mainSo.getAbsolutePath(), false));
                }
            }
        }
        return list;
    }

    private File findMainSo(File dir) {
        File f1 = new File(dir, "libvulkan_freedreno.so");
        if (f1.exists()) return f1;
        File f2 = new File(dir, "vulkan.adreno.so");
        if (f2.exists()) return f2;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".so"));
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }

    private String readDisplayName(File dir) {
        File meta = new File(dir, "driver_info.txt");
        if (meta.exists()) {
            try (Scanner scanner = new Scanner(meta)) {
                if (scanner.hasNextLine()) {
                    return scanner.nextLine().trim();
                }
            } catch (Exception ignored) {}
        }
        return dir.getName();
    }

    public DriverItem installDriver(Context context, Uri uri, String customName) throws Exception {
        String cleanName = customName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleanName.isEmpty()) {
            cleanName = "Turnip_" + System.currentTimeMillis();
        }

        File targetDir = new File(driversRootDir, cleanName);
        if (targetDir.exists()) {
            cleanName += "_" + (System.currentTimeMillis() % 1000);
            targetDir = new File(driversRootDir, cleanName);
        }
        targetDir.mkdirs();

        int extractedFiles = 0;
        try (InputStream testIs = context.getContentResolver().openInputStream(uri)) {
            boolean isZip = false;
            try (ZipInputStream zisTest = new ZipInputStream(testIs)) {
                if (zisTest.getNextEntry() != null) {
                    isZip = true;
                }
            } catch (Exception ignored) {}

            if (isZip) {
                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     ZipInputStream zis = new ZipInputStream(is)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = new File(entry.getName()).getName();
                        if (name.endsWith(".so") || name.endsWith(".json")) {
                            File dest = new File(targetDir, name);
                            try (FileOutputStream fos = new FileOutputStream(dest)) {
                                byte[] buf = new byte[8192];
                                int len;
                                while ((len = zis.read(buf)) != -1) {
                                    fos.write(buf, 0, len);
                                }
                            }
                            dest.setReadable(true, false);
                            dest.setExecutable(true, false);
                            extractedFiles++;
                        }
                    }
                }
            } else {
                // Arquivo .so direto
                File dest = new File(targetDir, "libvulkan_freedreno.so");
                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     FileOutputStream fos = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                dest.setReadable(true, false);
                dest.setExecutable(true, false);
                extractedFiles++;
            }
        }

        if (extractedFiles == 0) {
            targetDir.delete();
            throw new Exception("Nenhuma biblioteca de driver (.so) encontrada no arquivo selecionado.");
        }

        // Salvar nome de exibição
        File meta = new File(targetDir, "driver_info.txt");
        try (FileWriter fw = new FileWriter(meta)) {
            fw.write(customName + "\n");
        }

        File mainSo = findMainSo(targetDir);
        String soPath = (mainSo != null) ? mainSo.getAbsolutePath() : "";
        return new DriverItem(targetDir.getName(), customName, soPath, false);
    }

    public boolean deleteDriver(String driverId) {
        if (SYSTEM_DRIVER_ID.equals(driverId)) {
            return false;
        }
        File targetDir = new File(driversRootDir, driverId);
        if (targetDir.exists() && targetDir.isDirectory()) {
            File[] files = targetDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            return targetDir.delete();
        }
        return false;
    }
}
