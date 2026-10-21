// ds2 - ReXGlue Recompiled Project
//
// Customize your app by overriding virtual hooks from rex::ReXApp.

#pragma once

#include <rex/rex_app.h>
#include <rex/ui/overlay/debug_overlay.h>

#include <fstream>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>

#if defined(__ANDROID__)
#include <adrenotools/driver.h>
#include <android/log.h>
#include <dlfcn.h>
#define LOG_TAG "DS2_RECOMP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...)
#define LOGE(...)
#endif

static std::unordered_map<std::string, std::string> LoadLauncherConfig() {
  std::unordered_map<std::string, std::string> config;
#if defined(__ANDROID__)
  const char* config_path = "/data/data/com.fromsoftware.darksouls2/files/launcher_config.txt";
  std::ifstream file(config_path);
  if (file.is_open()) {
    std::string line;
    while (std::getline(file, line)) {
      size_t eq = line.find('=');
      if (eq != std::string::npos) {
        std::string key = line.substr(0, eq);
        std::string val = line.substr(eq + 1);
        if (!val.empty() && val.back() == '\r') {
          val.pop_back();
        }
        config[key] = val;
      }
    }
    LOGI("Loaded launcher configuration successfully from %s", config_path);
  } else {
    LOGI("launcher_config.txt not found, using default candidate paths.");
  }
#endif
  return config;
}

class Ds2App : public rex::ReXApp {
 public:
  using rex::ReXApp::ReXApp;

  static std::unique_ptr<rex::ui::WindowedApp> Create(
      rex::ui::WindowedAppContext& ctx) {
    return std::unique_ptr<Ds2App>(new Ds2App(ctx, "ds2",
        PPCImageConfig));
  }

  // Hook executed before guest runtime setup
  void OnPreSetup(rex::RuntimeConfig& config) override {
    config.gpu_plugin = "xenos";
#if defined(__ANDROID__)
    LOGI("Dark Souls II (Android ARM64): Initializing AdrenoTools & Hardware Hooks...");

    auto cfg = LoadLauncherConfig();
    bool turbo = (cfg.find("turbo") == cfg.end() || cfg["turbo"] == "1");
    if (turbo) {
      adrenotools_set_turbo(true);
      LOGI("AdrenoTools: Turbo clocks forced successfully for Adreno 710!");
    }

    bool turnip_enabled = (cfg["turnip_enabled"] == "1");
    std::string turnip_driver = cfg["turnip_driver"];
    if (turnip_enabled && !turnip_driver.empty() && std::filesystem::exists(turnip_driver)) {
      LOGI("AdrenoTools: Attempting to hook custom Turnip driver: %s", turnip_driver.c_str());
      std::filesystem::path p(turnip_driver);
      std::string dir = p.parent_path().string();
      std::string name = p.filename().string();

      std::string hook_dir = cfg["native_lib_dir"];
      if (hook_dir.empty()) {
        hook_dir = dir;
      }
      LOGI("AdrenoTools: Loading custom Turnip driver with hook dir: %s, driver dir: %s, name: %s",
           hook_dir.c_str(), dir.c_str(), name.c_str());

      void* mapping_handle = nullptr;
      void* handle = adrenotools_open_libvulkan(
          RTLD_NOW,
          ADRENOTOOLS_DRIVER_CUSTOM,
          nullptr,
          hook_dir.c_str(),
          dir.c_str(),
          name.c_str(),
          nullptr,
          &mapping_handle
      );
      if (handle) {
        LOGI("AdrenoTools: Custom Turnip driver successfully hooked at %p", handle);
      } else {
        LOGE("AdrenoTools: Failed to hook custom Turnip driver, falling back to system driver.");
      }
    } else {
      LOGI("AdrenoTools: Using Qualcomm system Vulkan driver.");
    }
#endif
  }

  void OnConfigurePaths(rex::PathConfig& paths) override {
#if defined(__ANDROID__)
    auto cfg = LoadLauncherConfig();
    std::string selected = cfg["game_path"];
    if (!selected.empty() && std::filesystem::exists(selected)) {
      paths.game_data_root = selected;
      paths.user_data_root = std::filesystem::path(selected) / "user";
      paths.cache_root = std::filesystem::path(selected) / "cache";
      paths.config_path = std::filesystem::path(selected) / "ds2.toml";
      std::error_code ec;
      std::filesystem::create_directories(paths.user_data_root, ec);
      std::filesystem::create_directories(paths.cache_root, ec);
      LOGI("Dark Souls II: Using launcher-configured game path: %s", selected.c_str());
      return;
    }

    const std::vector<std::filesystem::path> candidates = {
        "/sdcard/DarkSouls2",
        "/storage/emulated/0/DarkSouls2",
        "/sdcard/Download/DarkSouls2",
        "/storage/emulated/0/Download/DarkSouls2",
        "/data/data/com.fromsoftware.darksouls2/files"
    };
    for (const auto& candidate : candidates) {
      if (std::filesystem::exists(candidate / "GameData1.bdt") ||
          std::filesystem::exists(candidate / "default.xex") ||
          std::filesystem::exists(candidate / "Default.xex") ||
          std::filesystem::exists(candidate / "DEFAULT.XEX")) {
        paths.game_data_root = candidate;
        paths.user_data_root = candidate / "user";
        paths.cache_root = candidate / "cache";
        paths.config_path = candidate / "ds2.toml";
        std::error_code ec;
        std::filesystem::create_directories(paths.user_data_root, ec);
        std::filesystem::create_directories(paths.cache_root, ec);
        LOGI("Found Dark Souls II game assets at fallback: %s", candidate.c_str());
        return;
      }
    }
    LOGE("Dark Souls II: No game assets found in any candidate path!");
#endif
  }

  void OnLoadXexImage(std::string& xex_image) override {
    if (std::filesystem::exists(game_data_root_ / "Default.xex")) {
      xex_image = "game:\\Default.xex";
    } else if (std::filesystem::exists(game_data_root_ / "DEFAULT.XEX")) {
      xex_image = "game:\\DEFAULT.XEX";
    } else {
      xex_image = "game:\\default.xex";
    }
#if defined(__ANDROID__)
    LOGI("Selected XEX entrypoint image: %s", xex_image.c_str());
#endif
  }

  void OnPostSetup() override {
#if defined(__ANDROID__)
    auto cfg = LoadLauncherConfig();
    bool show_fps = (cfg.find("show_fps") == cfg.end() || cfg["show_fps"] == "1");
    if (show_fps && imgui_drawer_ && !debug_overlay_) {
      LOGI("Enabling Debug & FPS Overlay HUD...");
      debug_overlay_ = std::make_unique<rex::ui::DebugOverlayDialog>(imgui_drawer_.get(), frame_stats_provider_);
    }
#endif
  }
};
