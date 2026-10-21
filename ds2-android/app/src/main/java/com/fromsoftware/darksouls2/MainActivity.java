package com.fromsoftware.darksouls2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "DS2_LAUNCHER";
    private static final int REQ_STORAGE_PERM = 1001;
    private static final int REQ_PICK_FOLDER = 1002;
    private static final int REQ_PICK_ISO = 1003;
    private static final int REQ_PICK_TURNIP = 1004;

    private TextView tvGamePath;
    private TextView tvGameStatus;
    private TextView tvDriverInfo;
    private TextView tvLogSummary;
    private Spinner spnDrivers;
    private Switch swShowFps;
    private Switch swTurbo;
    private Button btnSelectFolder;
    private Button btnInstallIso;
    private Button btnInstallDriver;
    private Button btnDeleteDriver;
    private Button btnViewLogs;
    private Button btnCopyLogs;
    private Button btnPlay;

    private SharedPreferences prefs;
    private String currentGamePath = "/storage/emulated/0/DarkSouls2";
    private TurnipDriverManager driverManager;
    private List<TurnipDriverManager.DriverItem> driverList;
    private TurnipDriverManager.DriverItem currentSelectedDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driverManager = new TurnipDriverManager(this);
        prefs = getSharedPreferences("ds2_launcher_prefs", MODE_PRIVATE);
        currentGamePath = prefs.getString("game_path", "/storage/emulated/0/DarkSouls2");

        initViews();
        checkAndRequestPermissions();
        loadDriverList();
        updateGameStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGameStatus();
        updateLogSummary();
    }

    private void initViews() {
        tvGamePath = findViewById(R.id.tvGamePath);
        tvGameStatus = findViewById(R.id.tvGameStatus);
        tvDriverInfo = findViewById(R.id.tvDriverInfo);
        tvLogSummary = findViewById(R.id.tvLogSummary);
        spnDrivers = findViewById(R.id.spnDrivers);
        swShowFps = findViewById(R.id.swShowFps);
        swTurbo = findViewById(R.id.swTurbo);
        btnSelectFolder = findViewById(R.id.btnSelectFolder);
        btnInstallIso = findViewById(R.id.btnInstallIso);
        btnInstallDriver = findViewById(R.id.btnInstallDriver);
        btnDeleteDriver = findViewById(R.id.btnDeleteDriver);
        btnViewLogs = findViewById(R.id.btnViewLogs);
        btnCopyLogs = findViewById(R.id.btnCopyLogs);
        btnPlay = findViewById(R.id.btnPlay);

        tvGamePath.setText(currentGamePath);
        swShowFps.setChecked(prefs.getBoolean("show_fps", true));
        swTurbo.setChecked(prefs.getBoolean("turbo", true));

        btnSelectFolder.setOnClickListener(v -> pickFolder());
        btnInstallIso.setOnClickListener(v -> pickIsoFile());
        btnInstallDriver.setOnClickListener(v -> pickTurnipFile());
        btnDeleteDriver.setOnClickListener(v -> confirmDeleteDriver());
        btnViewLogs.setOnClickListener(v -> showLogsDialog());
        btnCopyLogs.setOnClickListener(v -> copyLogsToClipboard());
        btnPlay.setOnClickListener(v -> launchGame());

        swShowFps.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("show_fps", isChecked).apply());
        swTurbo.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.edit().putBoolean("turbo", isChecked).apply());

        spnDrivers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (driverList != null && position >= 0 && position < driverList.size()) {
                    currentSelectedDriver = driverList.get(position);
                    prefs.edit().putString("selected_driver_id", currentSelectedDriver.id).apply();
                    updateDriverUi();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDriverList() {
        driverList = driverManager.getAvailableDrivers();
        ArrayAdapter<TurnipDriverManager.DriverItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                driverList
        );
        spnDrivers.setAdapter(adapter);

        String savedDriverId = prefs.getString("selected_driver_id", TurnipDriverManager.SYSTEM_DRIVER_ID);
        int selectedIndex = 0;
        for (int i = 0; i < driverList.size(); i++) {
            if (driverList.get(i).id.equals(savedDriverId)) {
                selectedIndex = i;
                break;
            }
        }
        spnDrivers.setSelection(selectedIndex);
        currentSelectedDriver = driverList.get(selectedIndex);
        updateDriverUi();
    }

    private void updateDriverUi() {
        if (currentSelectedDriver == null) return;

        if (currentSelectedDriver.isSystem) {
            tvDriverInfo.setText("Driver ativo: Qualcomm Adreno Vulkan (Driver Padrão)");
            tvDriverInfo.setTextColor(0xFF60A5FA);
            btnDeleteDriver.setEnabled(false);
            btnDeleteDriver.setAlpha(0.4f);
        } else {
            tvDriverInfo.setText("Driver ativo: " + currentSelectedDriver.displayName + "\nArquivo: " + currentSelectedDriver.soPath);
            tvDriverInfo.setTextColor(0xFF10B981);
            btnDeleteDriver.setEnabled(true);
            btnDeleteDriver.setAlpha(1.0f);
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new AlertDialog.Builder(this)
                    .setTitle("Permissão de Armazenamento")
                    .setMessage("O Dark Souls II necessita de acesso ao armazenamento para ler os dados do jogo (.xex e GameData.bdt) e extrair arquivos ISO.")
                    .setPositiveButton("Conceder Acesso", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQ_STORAGE_PERM);
                        } catch (Exception e) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivityForResult(intent, REQ_STORAGE_PERM);
                        }
                    })
                    .setNegativeButton("Mais Tarde", null)
                    .show();
            }
        }
    }

    private boolean checkXexExists(File dir) {
        return new File(dir, "default.xex").exists() ||
               new File(dir, "Default.xex").exists() ||
               new File(dir, "DEFAULT.XEX").exists();
    }

    private boolean checkBdtExists(File dir) {
        return new File(dir, "GameData1.bdt").exists() ||
               new File(dir, "gamedata1.bdt").exists() ||
               new File(dir, "GAMEDATA1.BDT").exists();
    }

    private void updateGameStatus() {
        tvGamePath.setText(currentGamePath);
        File dir = new File(currentGamePath);

        boolean hasXex = checkXexExists(dir);
        boolean hasBdt = checkBdtExists(dir);

        if (hasXex && hasBdt) {
            tvGameStatus.setText("✔ default.xex e GameData1.bdt detectados com sucesso!");
            tvGameStatus.setTextColor(0xFF10B981); // Verde
        } else if (hasXex) {
            tvGameStatus.setText("⚠ default.xex encontrado, mas GameData1.bdt não está nesta pasta.");
            tvGameStatus.setTextColor(0xFFF59E0B); // Âmbar
        } else {
            tvGameStatus.setText("❌ default.xex não encontrado em " + currentGamePath);
            tvGameStatus.setTextColor(0xFFEF4444); // Vermelho
        }
    }

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_FOLDER);
    }

    private void pickIsoFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_PICK_ISO);
    }

    private void pickTurnipFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_PICK_TURNIP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            if (requestCode == REQ_PICK_FOLDER) {
                String path = resolvePathFromUri(uri);
                if (path != null && !path.isEmpty()) {
                    currentGamePath = path;
                    prefs.edit().putString("game_path", currentGamePath).apply();
                    updateGameStatus();
                }
            } else if (requestCode == REQ_PICK_ISO) {
                startIsoExtraction(uri);
            } else if (requestCode == REQ_PICK_TURNIP) {
                promptTurnipInstall(uri);
            }
        }
    }

    private String resolvePathFromUri(Uri uri) {
        try {
            String docId = null;
            if (DocumentsContract.isTreeUri(uri)) {
                docId = DocumentsContract.getTreeDocumentId(uri);
            } else if (DocumentsContract.isDocumentUri(this, uri)) {
                docId = DocumentsContract.getDocumentId(uri);
            }
            if (docId != null) {
                if (docId.startsWith("primary:")) {
                    return "/storage/emulated/0/" + docId.substring("primary:".length());
                } else if (docId.contains(":")) {
                    String[] parts = docId.split(":", 2);
                    File customPath = new File("/storage/" + parts[0] + "/" + parts[1]);
                    if (customPath.exists()) {
                        return customPath.getAbsolutePath();
                    }
                    return "/storage/emulated/0/" + parts[1];
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Falha ao resolver URI: " + uri, e);
        }
        return uri.getPath();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return (result != null) ? result : "Arquivo";
    }

    private void startIsoExtraction(Uri isoUri) {
        String fileName = getFileNameFromUri(isoUri);
        File destDir = new File(Environment.getExternalStorageDirectory(), "DarkSouls2");

        new AlertDialog.Builder(this)
            .setTitle("Extrair Imagem ISO")
            .setMessage("Deseja extrair os arquivos de '" + fileName + "' para a pasta:\n" + destDir.getAbsolutePath() + "?\n\nIsso pode levar alguns minutos dependendo do tamanho da ISO.")
            .setPositiveButton("Extrair Agora", (dialog, which) -> {
                performIsoExtraction(isoUri, destDir);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void performIsoExtraction(Uri isoUri, File destDir) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Extraindo ISO do Dark Souls II");
        progress.setMessage("Localizando arquivos e partição XDVDFS...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);

        XboxIsoExtractor extractor = new XboxIsoExtractor();
        progress.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancelar", (d, w) -> {
            extractor.cancel();
            Toast.makeText(this, "Extração cancelada.", Toast.LENGTH_SHORT).show();
        });
        progress.show();

        extractor.extractIsoAsync(this, isoUri, destDir, new XboxIsoExtractor.ExtractCallback() {
            @Override
            public void onProgress(String currentFile, long currentBytes, long totalBytes, int percent) {
                progress.setProgress(percent);
                progress.setMessage("Extraindo: " + currentFile + "\n(" + (currentBytes / (1024 * 1024)) + " MB de " + (totalBytes / (1024 * 1024)) + " MB)");
            }

            @Override
            public void onSuccess(File targetDir) {
                progress.dismiss();
                currentGamePath = targetDir.getAbsolutePath();
                prefs.edit().putString("game_path", currentGamePath).apply();
                updateGameStatus();
                Toast.makeText(MainActivity.this, "ISO extraída com sucesso!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                progress.dismiss();
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Erro na Extração da ISO")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
            }
        });
    }

    private void promptTurnipInstall(Uri driverUri) {
        String fileName = getFileNameFromUri(driverUri);
        String defaultName = fileName.replace(".zip", "").replace(".so", "");

        final EditText input = new EditText(this);
        input.setText(defaultName);
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
            .setTitle("Instalar Driver Turnip")
            .setMessage("Informe um nome para identificar este driver:")
            .setView(input)
            .setPositiveButton("Instalar", (dialog, which) -> {
                String chosenName = input.getText().toString().trim();
                if (chosenName.isEmpty()) chosenName = defaultName;
                installDriverInternal(driverUri, chosenName);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void installDriverInternal(Uri driverUri, String driverName) {
        try {
            TurnipDriverManager.DriverItem installed = driverManager.installDriver(this, driverUri, driverName);
            Toast.makeText(this, "Driver '" + installed.displayName + "' instalado com sucesso!", Toast.LENGTH_LONG).show();
            prefs.edit().putString("selected_driver_id", installed.id).apply();
            loadDriverList();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao instalar driver", e);
            new AlertDialog.Builder(this)
                .setTitle("Falha na Instalação do Driver")
                .setMessage("Erro: " + e.getMessage())
                .setPositiveButton("OK", null)
                .show();
        }
    }

    private void confirmDeleteDriver() {
        if (currentSelectedDriver == null || currentSelectedDriver.isSystem) return;

        new AlertDialog.Builder(this)
            .setTitle("Excluir Driver")
            .setMessage("Deseja realmente remover o driver '" + currentSelectedDriver.displayName + "'?")
            .setPositiveButton("Excluir", (d, w) -> {
                driverManager.deleteDriver(currentSelectedDriver.id);
                prefs.edit().putString("selected_driver_id", TurnipDriverManager.SYSTEM_DRIVER_ID).apply();
                loadDriverList();
                Toast.makeText(this, "Driver removido.", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void saveLauncherConfig() {
        try {
            File configFile = new File(getFilesDir(), "launcher_config.txt");
            FileWriter writer = new FileWriter(configFile);
            writer.write("game_path=" + currentGamePath + "\n");
            boolean useTurnip = (currentSelectedDriver != null && !currentSelectedDriver.isSystem);
            writer.write("turnip_enabled=" + (useTurnip ? "1" : "0") + "\n");
            writer.write("turnip_driver=" + (useTurnip ? currentSelectedDriver.soPath : "") + "\n");
            writer.write("native_lib_dir=" + getApplicationInfo().nativeLibraryDir + "\n");
            writer.write("show_fps=" + (swShowFps.isChecked() ? "1" : "0") + "\n");
            writer.write("turbo=" + (swTurbo.isChecked() ? "1" : "0") + "\n");
            writer.close();
            Log.i(TAG, "Configuração do launcher gravada: " + configFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Erro ao gravar launcher_config.txt", e);
        }
    }

    private void launchGame() {
        File dir = new File(currentGamePath);
        if (!checkXexExists(dir)) {
            new AlertDialog.Builder(this)
                .setTitle("Arquivos do Jogo Não Encontrados")
                .setMessage("O arquivo 'default.xex' não foi encontrado na pasta:\n" + currentGamePath + "\n\nSelecione a pasta onde você copiou o Dark Souls 2 ou clique em 'Instalar .ISO' para extrair o jogo.")
                .setPositiveButton("Jogar Mesmo Assim", (d, w) -> startGameActivity())
                .setNegativeButton("Voltar", null)
                .show();
            return;
        }

        startGameActivity();
    }

    private void startGameActivity() {
        saveLauncherConfig();
        Intent intent = new Intent(this, DarkSouls2Activity.class);
        intent.putExtra("game_path", currentGamePath);
        startActivity(intent);
    }

    private void clearLogs() {
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (Exception ignored) {}
    }

    private String readRecentLogs() {
        StringBuilder sb = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v time -t 500");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("DS2") || line.contains("SDL") || line.contains("ReX") || line.contains("vulkan") ||
                    line.contains("DEBUG") || line.contains("libc") || line.contains("fatal") || line.contains("crash") ||
                    line.contains("AndroidRuntime") || line.contains("DarkSouls2Activity")) {
                    sb.append(line).append("\n");
                }
            }
            reader.close();
        } catch (Exception e) {
            sb.append("Erro ao ler logs: ").append(e.getMessage());
        }
        return sb.toString();
    }

    private void updateLogSummary() {
        String logs = readRecentLogs();
        if (logs.length() > 0) {
            String[] lines = logs.split("\n");
            tvLogSummary.setText("Últimas linhas de log capturadas (" + lines.length + " entradas).");
        } else {
            tvLogSummary.setText("Nenhum erro registrado recentemente.");
        }
    }

    private void showLogsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_logs);
        dialog.setTitle("Console de Logs do Jogo");

        TextView tvDialogLogs = dialog.findViewById(R.id.tvLogContent);
        Button btnDialogClose = dialog.findViewById(R.id.btnDialogClose);
        Button btnDialogCopy = dialog.findViewById(R.id.btnDialogCopy);
        Button btnDialogClear = dialog.findViewById(R.id.btnDialogClear);

        String logs = readRecentLogs();
        tvDialogLogs.setText(logs.isEmpty() ? "Nenhum log encontrado." : logs);

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        btnDialogCopy.setOnClickListener(v -> {
            copyLogsToClipboard();
            Toast.makeText(this, "Logs copiados para a área de transferência!", Toast.LENGTH_SHORT).show();
        });
        btnDialogClear.setOnClickListener(v -> {
            clearLogs();
            tvDialogLogs.setText("Buffer de logs limpo com sucesso.\nExecute o jogo para registrar novos logs.");
            updateLogSummary();
            Toast.makeText(this, "Buffer de logs limpo!", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void copyLogsToClipboard() {
        String logs = readRecentLogs();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Dark Souls 2 Logs", logs);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Logs copiados para a área de transferência!", Toast.LENGTH_SHORT).show();
        }
    }
}
