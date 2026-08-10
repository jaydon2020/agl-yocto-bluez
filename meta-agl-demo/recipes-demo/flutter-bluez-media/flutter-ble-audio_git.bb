SUMMARY = "Flutter BlueZ media controller example"
DESCRIPTION = "Example application for controlling Bluetooth media through bluez_media_native"
HOMEPAGE = "https://github.com/jaydon2020/bluez_media_native"
SECTION = "graphics"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4be81725e05bc258e9c398895cb112e1"

SRC_URI = "gitsm://github.com/jaydon2020/bluez_media_native.git;branch=main;protocol=https"
SRCREV = "aec1fa3da6eac496134a6ee2fb61826b7281dd0f"

DEPENDS += " \
    compiler-rt \
    libcxx \
    lld-native \
    ninja-native \
    systemd \
"

TOOLCHAIN = "clang"
TOOLCHAIN_NATIVE = "clang"
TC_CXX_RUNTIME = "llvm"
PREFERRED_PROVIDER_llvm = "clang"
PREFERRED_PROVIDER_llvm-native = "clang-native"
PREFERRED_PROVIDER_libgcc = "compiler-rt"
CXXFLAGS:append = " -stdlib=libc++"

OECMAKE_SOURCEPATH = "${S}/native"

inherit cmake flutter-app pkgconfig agl-app

EXTRA_OECMAKE = "-DBUILD_TESTING=OFF"

PUBSPEC_APPNAME = "flutter_ble_audio"
FLUTTER_APPLICATION_PATH = "example/flutter_ble_audio"
PUBSPEC_IGNORE_LOCKFILE = "1"

FLUTTER_TARGET_PLATFORM = "linux-x64"
FLUTTER_TARGET_PLATFORM:aarch64 = "linux-arm64"
FLUTTER_BUILD_ARGS = "bundle -v --target-platform=${FLUTTER_TARGET_PLATFORM}"

AGL_APP_TEMPLATE = "agl-app-flutter"
AGL_APP_NAME = "Bluetooth Media"
AGL_APP_ID = "flutter_ble_audio"

python do_compile:prepend() {
    import os

    hook = os.path.join(d.getVar("S"), "hook", "build.dart")
    if os.path.exists(hook):
        os.remove(hook)
}

python do_cmake_compile() {
    bb.build.exec_func('cmake_do_compile', d)
}

addtask cmake_compile after do_compile before do_install
do_cmake_compile[dirs] = "${B}"

do_install:append() {
    for runtime_mode in ${FLUTTER_APP_RUNTIME_MODES}; do
        app_libdir="${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/$runtime_mode/lib"
        if [ -d "$app_libdir" ]; then
            install -m 0755 ${B}/libbluez_media_native.so "$app_libdir/"
        fi
    done
}

FILES:${PN}-dbg += "${FLUTTER_INSTALL_DIR}/*/*/lib/.debug/libbluez_media_native.so"
INSANE_SKIP:${PN}-dbg += "libdir"
