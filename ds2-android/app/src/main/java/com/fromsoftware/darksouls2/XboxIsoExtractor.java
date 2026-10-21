package com.fromsoftware.darksouls2;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class XboxIsoExtractor {
    private static final String TAG = "XboxIsoExtractor";
    private static final int SECTOR_SIZE = 2048;
    private static final byte[] XDVDFS_MAGIC = "MICROSOFT*XBOX*MEDIA".getBytes(StandardCharsets.US_ASCII);

    @FunctionalInterface
    public interface ProgressUpdater {
        void onProgress(String currentFile, long currentBytes, long totalBytes, int percent);
    }

    public interface ExtractCallback {
        void onProgress(String currentFile, long currentBytes, long totalBytes, int percent);
        void onSuccess(File destDir);
        void onError(String message);
    }

    private static class IsoEntry {
        boolean isDirectory;
        long sector;
        long size;
        String name;
        File targetFile;

        IsoEntry(boolean isDir, long sector, long size, String name, File targetFile) {
            this.isDirectory = isDir;
            this.sector = sector;
            this.size = size;
            this.name = name;
            this.targetFile = targetFile;
        }
    }

    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    public void cancel() {
        isCancelled.set(true);
    }

    public void extractIsoAsync(Context context, Uri isoUri, File destDir, ExtractCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                extractIso(context, isoUri, destDir, (file, current, total, percent) -> {
                    mainHandler.post(() -> callback.onProgress(file, current, total, percent));
                });
                mainHandler.post(() -> callback.onSuccess(destDir));
            } catch (Exception e) {
                Log.e(TAG, "Erro na extração da ISO", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Erro desconhecido ao extrair ISO"));
            }
        }).start();
    }

    private void extractIso(Context context, Uri isoUri, File destDir, ProgressUpdater progressCallback) throws Exception {
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(isoUri, "r");
        if (pfd == null) {
            throw new IOException("Não foi possível acessar o arquivo da ISO.");
        }

        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
             FileChannel channel = fis.getChannel()) {

            long fileSize = channel.size();
            long partitionOffset = findXdvdfsPartition(channel, fileSize);

            if (partitionOffset < 0) {
                throw new IOException("Estrutura Xbox 360 (XDVDFS) não encontrada na ISO. Verifique se o arquivo é uma ISO original de Xbox 360.");
            }

            Log.i(TAG, "Partição XDVDFS encontrada no offset: " + partitionOffset);

            // Ler o descritor de volume raiz (2048 bytes)
            ByteBuffer headerBuf = ByteBuffer.allocate(SECTOR_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            channel.position(partitionOffset);
            channel.read(headerBuf);
            headerBuf.flip();

            headerBuf.position(0x14);
            long rootDirSector = headerBuf.getInt() & 0xFFFFFFFFL;
            long rootDirSize = headerBuf.getInt() & 0xFFFFFFFFL;

            Log.i(TAG, "RootDirSector: " + rootDirSector + ", RootDirSize: " + rootDirSize);

            if (!destDir.exists()) {
                destDir.mkdirs();
            }

            // Primeiro: mapear todos os arquivos para saber o tamanho total
            Queue<IsoEntry> allFiles = new ArrayDeque<>();
            long[] totalBytesHolder = new long[]{0};

            readDirectory(channel, partitionOffset, rootDirSector, rootDirSize, destDir, allFiles, totalBytesHolder);

            long totalBytes = totalBytesHolder[0];
            Log.i(TAG, "Total de arquivos para extrair: " + allFiles.size() + " (" + (totalBytes / (1024 * 1024)) + " MB)");

            long extractedBytes = 0;
            byte[] buffer = new byte[64 * 1024]; // 64KB buffer

            for (IsoEntry entry : allFiles) {
                if (isCancelled.get()) {
                    throw new IOException("Extração cancelada pelo usuário.");
                }

                if (entry.isDirectory) {
                    entry.targetFile.mkdirs();
                    continue;
                }

                File parent = entry.targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                long fileOffset = partitionOffset + (entry.sector * SECTOR_SIZE);
                channel.position(fileOffset);

                long remaining = entry.size;
                try (FileOutputStream fos = new FileOutputStream(entry.targetFile)) {
                    while (remaining > 0 && !isCancelled.get()) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, toRead);
                        int read = channel.read(bb);
                        if (read <= 0) break;
                        fos.write(buffer, 0, read);
                        remaining -= read;
                        extractedBytes += read;

                        int percent = (totalBytes > 0) ? (int) ((extractedBytes * 100) / totalBytes) : 0;
                        progressCallback.onProgress(entry.name, extractedBytes, totalBytes, percent);
                    }
                }
            }

            Log.i(TAG, "Extração de ISO concluída com sucesso em " + destDir.getAbsolutePath());
        } finally {
            pfd.close();
        }
    }

    private void readDirectory(FileChannel channel, long partitionOffset, long dirSector, long dirSize,
                               File currentDir, Queue<IsoEntry> fileQueue, long[] totalBytesHolder) throws IOException {
        if (dirSize <= 0) return;

        ByteBuffer dirBuf = ByteBuffer.allocate((int) dirSize).order(ByteOrder.LITTLE_ENDIAN);
        channel.position(partitionOffset + (dirSector * SECTOR_SIZE));
        channel.read(dirBuf);
        dirBuf.flip();

        parseDirectoryTree(channel, partitionOffset, dirBuf, 0, currentDir, fileQueue, totalBytesHolder);
    }

    private void parseDirectoryTree(FileChannel channel, long partitionOffset, ByteBuffer dirBuf, int entryOffset,
                                    File currentDir, Queue<IsoEntry> fileQueue, long[] totalBytesHolder) throws IOException {
        if (entryOffset < 0 || entryOffset + 14 > dirBuf.capacity()) {
            return;
        }

        dirBuf.position(entryOffset);
        int leftSubtree = dirBuf.getShort() & 0xFFFF;
        int rightSubtree = dirBuf.getShort() & 0xFFFF;
        long sector = dirBuf.getInt() & 0xFFFFFFFFL;
        long size = dirBuf.getInt() & 0xFFFFFFFFL;
        int attributes = dirBuf.get() & 0xFF;
        int nameLen = dirBuf.get() & 0xFF;

        if (nameLen > 0 && entryOffset + 14 + nameLen <= dirBuf.capacity()) {
            byte[] nameBytes = new byte[nameLen];
            dirBuf.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            boolean isDir = (attributes & 0x10) != 0;
            File target = new File(currentDir, name);

            if (isDir) {
                fileQueue.add(new IsoEntry(true, sector, size, name, target));
                // Carregar subdiretório
                readDirectory(channel, partitionOffset, sector, size, target, fileQueue, totalBytesHolder);
            } else {
                fileQueue.add(new IsoEntry(false, sector, size, name, target));
                totalBytesHolder[0] += size;
            }
        }

        // Visitar nós da árvore binária (offset em unidades de 4 bytes)
        if (leftSubtree != 0 && leftSubtree != 0xFFFF) {
            parseDirectoryTree(channel, partitionOffset, dirBuf, leftSubtree * 4, currentDir, fileQueue, totalBytesHolder);
        }
        if (rightSubtree != 0 && rightSubtree != 0xFFFF) {
            parseDirectoryTree(channel, partitionOffset, dirBuf, rightSubtree * 4, currentDir, fileQueue, totalBytesHolder);
        }
    }

    private long findXdvdfsPartition(FileChannel channel, long fileSize) throws IOException {
        // Offsets conhecidos para partições Xbox 360
        long[] candidates = new long[]{
            0L,
            0x10000L,           // 64 KB
            0x1040000L,         // ~16.25 MB (XGD3 - Setor 0x2080 * 2048)
            0x1048000L,         // ~16.28 MB (XGD3 - Setor 0x2090 * 2048)
            0xFD90000L          // ~253.56 MB (XGD2 - Setor 0x1FB20 * 2048)
        };

        ByteBuffer magicBuf = ByteBuffer.allocate(XDVDFS_MAGIC.length);

        for (long off : candidates) {
            if (off + XDVDFS_MAGIC.length <= fileSize) {
                magicBuf.clear();
                channel.position(off);
                channel.read(magicBuf);
                if (Arrays.equals(magicBuf.array(), XDVDFS_MAGIC)) {
                    return off;
                }
            }
        }

        // Se não encontrou nos offsets fixos, varre os primeiros 300 MB a cada setor
        long scanLimit = Math.min(fileSize, 300L * 1024 * 1024);
        ByteBuffer sectorBuf = ByteBuffer.allocate(64 * SECTOR_SIZE); // Lê 64 setores por vez

        for (long pos = 0; pos < scanLimit; pos += (64 * SECTOR_SIZE)) {
            sectorBuf.clear();
            channel.position(pos);
            int read = channel.read(sectorBuf);
            if (read <= 0) break;

            for (int s = 0; s < read; s += SECTOR_SIZE) {
                if (s + XDVDFS_MAGIC.length <= read) {
                    boolean match = true;
                    for (int m = 0; m < XDVDFS_MAGIC.length; m++) {
                        if (sectorBuf.get(s + m) != XDVDFS_MAGIC[m]) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return pos + s;
                    }
                }
            }
        }

        return -1;
    }
}
