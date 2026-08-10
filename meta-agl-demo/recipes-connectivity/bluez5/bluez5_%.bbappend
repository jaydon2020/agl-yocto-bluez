FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://obex.service \
    file://org.bluez.obex.service \
"

do_install:append() {
    rm -f ${D}${systemd_user_unitdir}/obex.service
    rm -f ${D}${systemd_user_unitdir}/dbus-org.bluez.obex.service
    rm -f ${D}${datadir}/dbus-1/services/org.bluez.obex.service

    install -D -m 0644 ${UNPACKDIR}/obex.service \
        ${D}${systemd_system_unitdir}/obex.service
    ln -s obex.service \
        ${D}${systemd_system_unitdir}/dbus-org.bluez.obex.service
    install -D -m 0644 ${UNPACKDIR}/org.bluez.obex.service \
        ${D}${datadir}/dbus-1/system-services/org.bluez.obex.service
}

FILES:${PN}-obex += " \
    ${systemd_system_unitdir}/dbus-org.bluez.obex.service \
    ${datadir}/dbus-1/system-services/org.bluez.obex.service \
"
