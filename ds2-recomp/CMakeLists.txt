# ds2 - ReXGlue Recompiled Project
#
# SDK boilerplate lives in generated/rexglue.cmake.

cmake_minimum_required(VERSION 3.25)
project(ds2 LANGUAGES CXX C)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(ANDROID)
    # Add libadrenotools for custom Turnip driver loading and Turbo clock control on Adreno 710
    set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)
    add_subdirectory("C:/Users/Alesson/.gemini/antigravity/scratch/libadrenotools" adrenotools_build)
endif()

include(generated/rexglue.cmake)

# Sources
set(DS2_SOURCES
    src/main.cpp
)

if(ANDROID)
    add_library(ds2 SHARED ${DS2_SOURCES})
    target_include_directories(ds2 PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/../rexglue-sdk/thirdparty/imgui")
    target_link_libraries(ds2 PRIVATE adrenotools android log)
    target_link_options(ds2 PRIVATE -Wl,-z,noexecstack)
elseif(WIN32)
    add_executable(ds2 WIN32 ${DS2_SOURCES})
    target_link_libraries(ds2 PRIVATE imgui SDL3::SDL3)
else()
    add_executable(ds2 ${DS2_SOURCES})
    target_link_libraries(ds2 PRIVATE imgui SDL3::SDL3)
endif()

rexglue_setup_target(ds2)
