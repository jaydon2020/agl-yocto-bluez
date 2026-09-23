SUMMARY = "Flutter BlueZ OBEX phone contacts and messages example"
DESCRIPTION = "AGL demo for browsing phone contacts and messages over BlueZ OBEX"
HOMEPAGE = "https://github.com/jaydon2020/bluez_obex_native"
SECTION = "graphics"

LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=82c2720ee875b422181e228795552d0e"

SRC_URI = " \
    gitsm://github.com/jaydon2020/bluez_obex_native.git;protocol=https;branch=main \
"
SRCREV = "1d46b1f43b4fd1892f0e2173b186d0c8d057e786"

PV = "1.0+git${SRCPV}"

DEPENDS += "systemd"

inherit flutter-app-plugins agl-app

FLUTTER_APPLICATION_PATH = "example/flutter_phone_message"
PUBSPEC_APPNAME = "flutter_phone_message"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_PREBUILD_CMD = "flutter pub get --offline"

AGL_APP_TEMPLATE = "agl-app-flutter"
AGL_APP_ID = "flutter_phone_message"
AGL_APP_NAME = "OBEX Phone Studio"

OBEX_NATIVE_BUILD = "${WORKDIR}/build-bluez-obex-native"

DEBUG_PREFIX_MAP:append = " \
    -ffile-prefix-map=${S}/native=${TARGET_DBGSRC_DIR}/bluez_obex_native \
    -ffile-prefix-map=${OBEX_NATIVE_BUILD}=${TARGET_DBGSRC_DIR}/bluez_obex_native-build \
"

python do_configure_bluez_obex_native() {
    localdata = d.createCopy()
    localdata.setVar("OECMAKE_SOURCEPATH", d.expand("${S}/native"))
    localdata.setVar("B", d.getVar("OBEX_NATIVE_BUILD"))
    localdata.setVar(
        "EXTRA_OECMAKE",
        "-DBUILD_TESTING=OFF -DBLUEZ_HOOK_BUILD=ON",
    )
    bb.build.exec_func("cmake_do_configure", localdata)
}

addtask configure_bluez_obex_native after do_configure before do_compile
do_configure_bluez_obex_native[dirs] = "${OBEX_NATIVE_BUILD}"

# Yocto cross-compiles the native library explicitly below.  Do not let the
# Dart hook attempt a second, host-architecture build during flutter pub get.
python do_compile:prepend() {
    import os

    hook = os.path.join(d.getVar("S"), "hook", "build.dart")
    if os.path.exists(hook):
        os.remove(hook)
}

python do_compile_bluez_obex_native() {
    localdata = d.createCopy()
    localdata.setVar("B", d.getVar("OBEX_NATIVE_BUILD"))
    bb.build.exec_func("cmake_do_compile", localdata)
}

addtask compile_bluez_obex_native after do_compile before do_install
do_compile_bluez_obex_native[dirs] = "${OBEX_NATIVE_BUILD}"

do_install:prepend() {
    native_assets_dir="${S}/${FLUTTER_APPLICATION_PATH}/build/native_assets/linux"
    install -d "$native_assets_dir"
    install -m 0755 \
        ${OBEX_NATIVE_BUILD}/libbluez_obex_native.so \
        "$native_assets_dir/"
}

FILES:${PN}-dbg += "${FLUTTER_INSTALL_DIR}/*/*/lib/.debug/libbluez_obex_native.so"

RDEPENDS:${PN} += "bluez5-obex"
