SRC_URI = " \
    git://github.com/bluez/bluez.git;branch=master;protocol=https;destsuffix=bluez-${PV} \
    file://init \
    file://run-ptest \
"

SRCREV = "9f5adb00c7c1d3fcfa0afeaf53633eac90b5a927"

PACKAGECONFIG:append = " obex-profiles"
RDEPENDS:${PN}:append = " ${PN}-obex"

SRC_URI:remove:rpi = " \
    file://0001-bcm43xx-Add-bcm43xx-3wire-variant.patch \
    file://0002-bcm43xx-The-UART-speed-must-be-reset-after-the-firmw.patch \
    file://0003-Increase-firmware-load-timeout-to-30s.patch \
    file://0004-Move-the-hciattach-firmware-into-lib-firmware.patch \
"

do_install:append() {
    sed -i '/^ExecStart=.*bluetoothd$/ s/$/ -E/' \
        ${D}${systemd_system_unitdir}/bluetooth.service
}
