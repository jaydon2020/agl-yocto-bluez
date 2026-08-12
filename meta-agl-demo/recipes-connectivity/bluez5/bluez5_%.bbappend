do_install:append() {
    sed -i 's/^#Experimental = false$/Experimental = true/' \
        ${D}${sysconfdir}/bluetooth/main.conf
}
