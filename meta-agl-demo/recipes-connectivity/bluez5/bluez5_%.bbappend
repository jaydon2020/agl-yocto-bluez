FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://0001-player-fix-mediaitem1-play-without-scope.patch \
    file://0002-player-answer-pending-request-on-destroy.patch \
    file://0003-player-update-number-of-items-on-scope-change.patch \
    file://0004-player-report-ebusy-from-busy-search.patch \
    file://0005-unit-test-media-player-add-media-player-tests.patch \
"

PACKAGECONFIG:append = " obex-profiles"
RDEPENDS:${PN}:append = " ${PN}-obex"

do_install:append() {
    sed -i '/^ExecStart=.*bluetoothd$/ s/$/ -E/' \
        ${D}${systemd_system_unitdir}/bluetooth.service
}
