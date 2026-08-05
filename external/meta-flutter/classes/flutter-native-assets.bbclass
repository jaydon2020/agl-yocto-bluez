# SPDX-License-Identifier: MIT

# Flutter forwards PATH, but not Yocto's compiler or pkg-config environment, to
# native asset hooks. A CMake shim keeps CMake-based hooks in the target sysroot.
FLUTTER_NATIVE_ASSETS_BINDIR = "${WORKDIR}/flutter-native-assets-bin"
FLUTTER_NATIVE_ASSETS_TOOLCHAIN_FILE = "${WORKDIR}/flutter-native-assets-toolchain.cmake"

PATH:prepend = "${FLUTTER_NATIVE_ASSETS_BINDIR}:"

FLUTTER_NATIVE_ASSETS_DEPENDS ?= " \
    clang-native \
    cmake-native \
    ninja-native \
    pkgconfig-native \
    libcxx \
    lld-native \
"
DEPENDS += "${FLUTTER_NATIVE_ASSETS_DEPENDS}"

TOOLCHAIN = "clang"
TC_CXX_RUNTIME = "llvm"
PREFERRED_PROVIDER_libgcc = "compiler-rt"

FLUTTER_TARGET_PLATFORM ??= "${@{'aarch64': 'linux-arm64', 'x86_64': 'linux-x64'}.get(d.getVar('TARGET_ARCH'), 'linux-' + d.getVar('TARGET_ARCH'))}"
FLUTTER_BUILD_ARGS:append = " --target-platform=${FLUTTER_TARGET_PLATFORM}"

FLUTTER_NATIVE_ASSETS_CFLAGS = "${@' '.join(flag for flag in d.getVar('CFLAGS').split() if flag != '-fcanon-prefix-map')}"
FLUTTER_NATIVE_ASSETS_CXXFLAGS = "${@' '.join(flag for flag in d.getVar('CXXFLAGS').split() if flag != '-fcanon-prefix-map')}"

do_configure:append() {
    install -d ${FLUTTER_NATIVE_ASSETS_BINDIR}

    cat > ${FLUTTER_NATIVE_ASSETS_TOOLCHAIN_FILE} <<EOF
set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR "${TARGET_ARCH}")
set(CMAKE_SYSROOT "${RECIPE_SYSROOT}")

set(CMAKE_C_COMPILER "${STAGING_BINDIR_TOOLCHAIN}/${HOST_PREFIX}clang")
set(CMAKE_CXX_COMPILER "${STAGING_BINDIR_TOOLCHAIN}/${HOST_PREFIX}clang++")
set(CMAKE_AR "${STAGING_BINDIR_TOOLCHAIN}/${HOST_PREFIX}llvm-ar")
set(CMAKE_RANLIB "${STAGING_BINDIR_TOOLCHAIN}/${HOST_PREFIX}llvm-ranlib")

set(CMAKE_C_FLAGS "${HOST_CC_ARCH} ${TOOLCHAIN_OPTIONS} ${FLUTTER_NATIVE_ASSETS_CFLAGS}")
set(CMAKE_CXX_FLAGS "${HOST_CC_ARCH} ${TOOLCHAIN_OPTIONS} ${FLUTTER_NATIVE_ASSETS_CXXFLAGS} -stdlib=libc++")
set(CMAKE_EXE_LINKER_FLAGS "${LDFLAGS} -stdlib=libc++ -fuse-ld=lld")
set(CMAKE_SHARED_LINKER_FLAGS "${LDFLAGS} -stdlib=libc++ -fuse-ld=lld")

set(CMAKE_FIND_ROOT_PATH "${RECIPE_SYSROOT}")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)

set(ENV{PKG_CONFIG_PATH} "")
set(ENV{PKG_CONFIG_SYSROOT_DIR} "${RECIPE_SYSROOT}")
set(ENV{PKG_CONFIG_LIBDIR} "${RECIPE_SYSROOT}${libdir}/pkgconfig:${RECIPE_SYSROOT}${datadir}/pkgconfig")
EOF

    cat > ${FLUTTER_NATIVE_ASSETS_BINDIR}/cmake <<EOF
#!/bin/sh

case "\${1-}" in
    --build|--install|-E|-P|--version|--help)
        exec "${STAGING_BINDIR_NATIVE}/cmake" "\$@"
        ;;
esac

for arg in "\$@"; do
    case "\$arg" in
        -DCMAKE_TOOLCHAIN_FILE=*)
            exec "${STAGING_BINDIR_NATIVE}/cmake" "\$@"
            ;;
    esac
done

exec "${STAGING_BINDIR_NATIVE}/cmake" \
    "-DCMAKE_TOOLCHAIN_FILE=${FLUTTER_NATIVE_ASSETS_TOOLCHAIN_FILE}" "\$@"
EOF
    chmod 0755 ${FLUTTER_NATIVE_ASSETS_BINDIR}/cmake
}

do_install:append() {
    native_assets_dir="${S}/${FLUTTER_APPLICATION_PATH}/build/native_assets/linux"
    if [ -d "$native_assets_dir" ]; then
        for runtime_mode in ${FLUTTER_APP_RUNTIME_MODES}; do
            app_libdir="${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/$runtime_mode/lib"
            if [ -d "$app_libdir" ]; then
                find "$native_assets_dir" -maxdepth 1 -type f -name '*.so' \
                    -exec install -m 0755 {} "$app_libdir/" \;
            fi
        done
    fi
}

INSANE_SKIP:${PN}-dbg += "buildpaths libdir"
