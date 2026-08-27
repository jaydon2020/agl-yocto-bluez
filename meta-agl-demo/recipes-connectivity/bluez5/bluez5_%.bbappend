SRC_URI = " \
    git://github.com/bluez/bluez.git;branch=master;protocol=https \
    file://init \
    file://run-ptest \
"

SRCREV = "9f5adb00c7c1d3fcfa0afeaf53633eac90b5a927"
S = "${WORKDIR}/git"

PACKAGECONFIG:append = " obex-profiles"
RDEPENDS:${PN}:append = " ${PN}-obex"

do_install:append() {
    sed -i '/^ExecStart=.*bluetoothd$/ s/$/ -E/' \
        ${D}${systemd_system_unitdir}/bluetooth.service
}
