FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://0001-unit-test-media-player-add-media-player-tests.patch"

do_install:append() {
    sed -i 's/^#Experimental = false$/Experimental = true/' \
        ${D}${sysconfdir}/bluetooth/main.conf
}
