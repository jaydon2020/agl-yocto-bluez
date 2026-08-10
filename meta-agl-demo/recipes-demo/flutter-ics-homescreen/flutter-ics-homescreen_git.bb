SUMMARY = "AGL ICS Flutter Homescreen"
DESCRIPTION = "Demo Flutter homescreen for Automotive Grade Linux by ICS."
HOMEPAGE = "https://gerrit.automotivelinux.org/gerrit/apps/flutter-ics-homescreen"
SECTION = "graphics"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = " \
    file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57 \
    file://../bluez-media-native/LICENSE;md5=4be81725e05bc258e9c398895cb112e1 \
"

SRC_URI = "git://github.com/jaydon2020/flutter-ics-homescreen.git;branch=bluez-media;protocol=https;name=homescreen;destsuffix=${BP} \
           gitsm://github.com/jaydon2020/bluez_media_native.git;branch=main;protocol=https;name=media;destsuffix=bluez-media-native \
           file://ics-homescreen.toml \
           file://flutter-ics-homescreen.service \
           file://flutter-ics-homescreen.env \
           file://kuksa.toml \
           file://flutter-ics-homescreen.token \
           file://radio-presets.toml \
           file://flutter-ics-homescreen.toml.kvm-tradeshow \
           file://kvm.conf \
"
SRCREV_homescreen = "a216ba717289ea0c472c8fa5ca6ebb1f72ca5f0c"
SRCREV_media = "aec1fa3da6eac496134a6ee2fb61826b7281dd0f"
SRCREV_FORMAT = "homescreen_media"

PUBSPEC_APPNAME = "flutter_ics_homescreen"

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
LIBCPLUSPLUS = "-stdlib=libc++"
CXXFLAGS:append = " ${LIBCPLUSPLUS}"

BLUEZ_NATIVE_SOURCE = "${WORKDIR}/bluez_native"
BLUEZ_MEDIA_DART_SOURCE = "${WORKDIR}/bluez_media_native"
BLUEZ_MEDIA_NATIVE_SOURCE = "${UNPACKDIR}/bluez-media-native"
BLUEZ_MEDIA_NATIVE_BUILD = "${WORKDIR}/bluez-media-build"
OECMAKE_SOURCEPATH = "${BLUEZ_NATIVE_SOURCE}/native"
DEBUG_PREFIX_MAP_EXTRA:append = " \
    -ffile-prefix-map=${OECMAKE_SOURCEPATH}=${TARGET_DBGSRC_DIR}/bluez_native \
    -ffile-prefix-map=${BLUEZ_MEDIA_NATIVE_SOURCE}=${TARGET_DBGSRC_DIR}/bluez_media_native \
    -ffile-prefix-map=${BLUEZ_MEDIA_NATIVE_BUILD}=${TARGET_DBGSRC_DIR}/bluez_media_native/build \
    -ffile-prefix-map=${PUB_CACHE}=${TARGET_DBGSRC_DIR}/pub-cache \
"

inherit cmake flutter-app pkgconfig systemd update-alternatives

EXTRA_OECMAKE = "-DBUILD_TESTING=OFF -DBLUEZ_HOOK_BUILD=ON"

FLUTTER_TARGET_PLATFORM = "linux-x64"
FLUTTER_TARGET_PLATFORM:aarch64 = "linux-arm64"
FLUTTER_BUILD_ARGS:append = " --target-platform=${FLUTTER_TARGET_PLATFORM}"

APP_CONFIG = "ics-homescreen.toml"

PUBSPEC_IGNORE_LOCKFILE = "1"

SYSTEMD_SERVICE:${PN} = "flutter-ics-homescreen.service"

python do_prepare_native_packages() {
    import json
    import os
    from urllib.parse import unquote, urlparse

    app_root = os.path.join(d.getVar("S"), d.getVar("FLUTTER_APPLICATION_PATH"))
    package_config_path = os.path.join(app_root, ".dart_tool",
                                       "package_config.json")
    with open(package_config_path, "r") as config_file:
        package_config = json.load(config_file)

    packages = {
        "bluez_native": d.getVar("BLUEZ_NATIVE_SOURCE"),
        "bluez_media_native": d.getVar("BLUEZ_MEDIA_DART_SOURCE"),
    }
    for package_name, source in packages.items():
        package = next((entry for entry in package_config["packages"]
                        if entry["name"] == package_name), None)
        if package is None:
            bb.fatal("{} is missing from Dart package_config.json".format(
                package_name))

        root_uri = package["rootUri"]
        parsed_uri = urlparse(root_uri)
        if parsed_uri.scheme == "file":
            package_root = unquote(parsed_uri.path)
        elif not parsed_uri.scheme:
            package_root = os.path.realpath(os.path.join(
                os.path.dirname(package_config_path), root_uri))
        else:
            bb.fatal("Unsupported {} root URI: {}".format(package_name,
                                                           root_uri))

        if os.path.lexists(source):
            os.unlink(source)
        os.symlink(package_root, source)
}

addtask prepare_native_packages after do_restore_pub_cache before do_configure

python do_configure_bluez_media_native() {
    import os

    localdata = d.createCopy()
    localdata.setVar("B", d.getVar("BLUEZ_MEDIA_NATIVE_BUILD"))
    localdata.setVar("OECMAKE_SOURCEPATH", os.path.join(
        d.getVar("BLUEZ_MEDIA_NATIVE_SOURCE"), "native"))
    localdata.setVar("EXTRA_OECMAKE", "-DBUILD_TESTING=OFF")
    bb.build.exec_func('cmake_do_configure', localdata)
}

addtask configure_bluez_media_native after do_configure before do_compile
do_configure_bluez_media_native[dirs] = "${BLUEZ_MEDIA_NATIVE_BUILD}"

python do_compile:prepend() {
    import os
    for source in (d.getVar("BLUEZ_NATIVE_SOURCE"),
                   d.getVar("BLUEZ_MEDIA_DART_SOURCE")):
        hook = os.path.join(source, "hook", "build.dart")
        if os.path.exists(hook):
            os.remove(hook)
}

python do_cmake_compile() {
    bb.build.exec_func('cmake_do_compile', d)
}

addtask cmake_compile after do_compile before do_install
do_cmake_compile[dirs] = "${B}"

python do_compile_bluez_media_native() {
    localdata = d.createCopy()
    localdata.setVar("B", d.getVar("BLUEZ_MEDIA_NATIVE_BUILD"))
    bb.build.exec_func('cmake_do_compile', localdata)
}

addtask compile_bluez_media_native after do_compile before do_install
do_compile_bluez_media_native[dirs] = "${BLUEZ_MEDIA_NATIVE_BUILD}"

# Disable the background animation on all platforms except the Renesas M3/H3 for now
DISABLE_BG_ANIMATION = "-DDISABLE_BKG_ANIMATION=true"
DISABLE_BG_ANIMATION:rcar-gen3 = ""
APP_AOT_EXTRA:append = " ${DISABLE_BG_ANIMATION}"

# Check for agl-offline-voice-agent feature
ENABLE_VOICE_ASSISTANT = "${@bb.utils.contains('EXTRA_IMAGE_FEATURES', 'agl-offline-voice-agent', '-DENABLE_VOICE_ASSISTANT=true', '-DENABLE_VOICE_ASSISTANT=false', d)}"
APP_AOT_EXTRA:append = " ${ENABLE_VOICE_ASSISTANT}"

do_install:append() {
    install -D -m 0644 ${UNPACKDIR}/${BPN}.service ${D}${systemd_system_unitdir}/${BPN}.service

    install -D -m 0644 ${UNPACKDIR}/kvm.conf ${D}${systemd_system_unitdir}/${BPN}.service.d/kvm.conf

    install -D -m 0644 ${UNPACKDIR}/${APP_CONFIG} ${D}${datadir}/flutter/${BPN}.json

    install -d ${D}${sysconfdir}/xdg/AGL
    install -m 0644 ${UNPACKDIR}/flutter-ics-homescreen.toml.kvm-tradeshow ${D}${sysconfdir}/xdg/AGL/

    # VIS authorization token file for KUKSA.val should ideally not
    # be readable by other users, but currently that's not doable
    # until a packaging/sandboxing/MAC scheme is (re)implemented or
    # something like OAuth is plumbed in as an alternative.
    install -d ${D}${sysconfdir}/xdg/AGL/flutter-ics-homescreen
    install -m 0644 ${UNPACKDIR}/kuksa.toml ${D}${sysconfdir}/xdg/AGL/flutter-ics-homescreen/
    install -m 0644 ${UNPACKDIR}/flutter-ics-homescreen.token ${D}${sysconfdir}/xdg/AGL/flutter-ics-homescreen/
    install -m 0644 ${UNPACKDIR}/radio-presets.toml ${D}${sysconfdir}/xdg/AGL/flutter-ics-homescreen/

    for runtime_mode in ${FLUTTER_APP_RUNTIME_MODES}; do
        app_libdir="${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/$runtime_mode/lib"
        if [ -d "$app_libdir" ]; then
            install -m 0755 ${B}/libbluez_nc.so "$app_libdir/"
            install -m 0755 ${BLUEZ_MEDIA_NATIVE_BUILD}/libbluez_media_native.so "$app_libdir/"
        fi
    done
}

ALTERNATIVE_LINK_NAME[flutter-ics-homescreen.toml] = "${sysconfdir}/xdg/AGL/flutter-ics-homescreen.toml"

FILES:${PN} += "${datadir} ${sysconfdir}/xdg/AGL ${sysconfdir}/default"
FILES:${PN}-dbg += " \
    ${FLUTTER_INSTALL_DIR}/*/*/lib/.debug/libbluez_nc.so \
    ${FLUTTER_INSTALL_DIR}/*/*/lib/.debug/libbluez_media_native.so \
"
INSANE_SKIP:${PN}-dbg += "libdir"

RDEPENDS:${PN} += " \
    flutter-auto \
    agl-flutter-env \
    applaunchd \
"

# KVM tradeshow demo specific configuration:
# - override radio and mediaplayer backend locations
# - systemd override to add network-online.target dependency
PACKAGE_BEFORE_PN += "${PN}-conf-kvm-tradeshow"
FILES:${PN}-conf-kvm-tradeshow += " \
    ${sysconfdir}/xdg/AGL/flutter-ics-homescreen.toml.kvm-tradeshow \
    ${systemd_system_unitdir}/flutter-ics-homescreen.service.d/kvm.conf \
"
RDEPENDS:${PN}-conf-kvm-tradeshow = "${PN}"
RPROVIDES:${PN}-conf-kvm-tradeshow = "flutter-ics-homescreen.toml"
ALTERNATIVE:${PN}-conf-kvm-tradeshow = "flutter-ics-homescreen.toml"
ALTERNATIVE_TARGET_${PN}-conf-kvm-tradeshow = "${sysconfdir}/xdg/AGL/flutter-ics-homescreen.toml.kvm-tradeshow"
ALTERNATIVE_PRIORITY_${PN}-conf-kvm-tradeshow = "11"

do_compile[network] = "1"
