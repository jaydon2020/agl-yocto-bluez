SUMMARY = "PipeWire Bluetooth telephony Flutter example"
DESCRIPTION = "AGL exerciser for PipeWire Bluetooth HFP telephony over D-Bus"
HOMEPAGE = "https://github.com/jaydon2020/pipewire_telephony_native"
SECTION = "graphics"

LICENSE = "CLOSED"

SRC_URI = "gitsm://github.com/jaydon2020/pipewire_telephony_native.git;protocol=https;branch=main"
SRCREV = "22c66389a1b9b50b3e5d2a56592dcc5cf79afe58"

PV = "1.0+git${SRCPV}"

DEPENDS += "systemd"

inherit flutter-app-plugins agl-app

FLUTTER_APPLICATION_PATH = "example/flutter_telephony"
PUBSPEC_APPNAME = "flutter_telephony"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_PREBUILD_CMD = "flutter pub get --offline"

AGL_APP_TEMPLATE = "agl-app-flutter"
AGL_APP_ID = "flutter_telephony"
AGL_APP_NAME = "PipeWire Telephony"

PWT_NATIVE_BUILD = "${WORKDIR}/build-pipewire-telephony-native"

DEBUG_PREFIX_MAP:append = " \
    -ffile-prefix-map=${S}/native=${TARGET_DBGSRC_DIR}/pipewire_telephony_native \
    -ffile-prefix-map=${PWT_NATIVE_BUILD}=${TARGET_DBGSRC_DIR}/pipewire_telephony_native-build \
"

python do_configure_pipewire_telephony_native() {
    localdata = d.createCopy()
    localdata.setVar("OECMAKE_SOURCEPATH", d.expand("${S}/native"))
    localdata.setVar("B", d.getVar("PWT_NATIVE_BUILD"))
    localdata.setVar(
        "EXTRA_OECMAKE",
        "-DBUILD_TESTING=OFF "
        "-DPWT_BUILD_DART_INTEGRATION_TESTS=OFF "
        "-DPWT_REQUIRE_CROSS_COMPILE=ON",
    )
    bb.build.exec_func("cmake_do_configure", localdata)
}

addtask configure_pipewire_telephony_native after do_configure before do_compile
do_configure_pipewire_telephony_native[dirs] = "${PWT_NATIVE_BUILD}"

python do_compile_pipewire_telephony_native() {
    localdata = d.createCopy()
    localdata.setVar("B", d.getVar("PWT_NATIVE_BUILD"))
    bb.build.exec_func("cmake_do_compile", localdata)
}

addtask compile_pipewire_telephony_native after do_compile before do_install
do_compile_pipewire_telephony_native[dirs] = "${PWT_NATIVE_BUILD}"

do_install:prepend() {
    native_assets_dir="${S}/${FLUTTER_APPLICATION_PATH}/build/native_assets/linux"
    install -d "$native_assets_dir"
    install -m 0755 \
        ${PWT_NATIVE_BUILD}/libpipewire_telephony_native.so \
        "$native_assets_dir/"
}

FILES:${PN}-dbg += "${FLUTTER_INSTALL_DIR}/*/*/lib/.debug/libpipewire_telephony_native.so"
