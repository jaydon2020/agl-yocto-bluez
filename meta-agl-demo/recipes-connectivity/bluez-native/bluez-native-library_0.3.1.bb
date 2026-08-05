SUMMARY = "Native BlueZ bindings for Dart"
HOMEPAGE = "https://github.com/jwinarske/bluez_native"

LICENSE = "Apache-2.0 & BSD-3-Clause & LGPL-2.1-or-later"
LIC_FILES_CHKSUM = " \
    file://LICENSE;md5=86d3f3a95c324c9479bd8986968f4327 \
    file://${UNPACKDIR}/LICENSE-DART;md5=29b4ad63b1f1509efea6629404336393 \
    file://native/third_party/sdbus-cpp/COPYING;md5=1803fa9c2c3ce8cb06b4861d75310742 \
    file://native/third_party/sdbus-cpp/COPYING-LGPL-Exception;md5=263c8172d75b98e3e2962dc32bd5c3eb \
"

DEPENDS += " \
    compiler-rt \
    libcxx \
    systemd \
"

SRC_URI = " \
    gitsm://github.com/jwinarske/bluez_native.git;protocol=https;nobranch=1 \
    file://LICENSE-DART \
"
SRCREV = "f5a7e9df63b2c1952375c436172c037241847328"

OECMAKE_SOURCEPATH = "${S}/native"

TOOLCHAIN = "clang"
TOOLCHAIN_NATIVE = "clang"
TC_CXX_RUNTIME = "llvm"
PREFERRED_PROVIDER_llvm = "clang"
PREFERRED_PROVIDER_llvm-native = "clang-native"
PREFERRED_PROVIDER_libgcc = "compiler-rt"
LIBCPLUSPLUS = "-stdlib=libc++"
CXXFLAGS:append = " ${LIBCPLUSPLUS}"

inherit cmake pkgconfig

EXTRA_OECMAKE = "-DBUILD_TESTING=OFF -DBLUEZ_HOOK_BUILD=ON"

do_install() {
    install -Dm 0755 ${B}/libbluez_nc.so ${D}${libdir}/libbluez_nc.so
}

FILES_SOLIBSDEV = ""
INSANE_SKIP:${PN} += "dev-so"
FILES:${PN} = "${libdir}/libbluez_nc.so"
