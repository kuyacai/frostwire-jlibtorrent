package com.frostwire.jlibtorrent;

import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.Alerts;
import com.frostwire.jlibtorrent.swig.alert;
import com.frostwire.jlibtorrent.swig.alert_ptr_vector;
import com.frostwire.jlibtorrent.swig.session;
import com.frostwire.jlibtorrent.swig.settings_pack;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author gubatron
 * @author aldenml
 */
public final class SessionManager {

    private static final long ALERTS_LOOP_WAIT_MILLIS = 1000;

    private final String interfaces;
    private final int retries;
    private final boolean logging;

    private final ReentrantLock sync;

    private session session;
    private Thread alertsLoop;

    public SessionManager(String interfaces, int retries, boolean logging) {
        this.interfaces = interfaces;
        this.retries = retries;
        this.logging = logging;

        this.sync = new ReentrantLock();
    }

    public SessionManager() {
        this("0.0.0.0:6881,[::]:6881", 10, false);
    }

    public void start() {
        sync.lock();

        try {
            if (session != null) {
                return;
            }

            session = createSession(interfaces, retries, logging);

            startAlertsLoop();

            for (Pair p : defaultDhtRouters()) {
                session.add_dht_router(p.to_string_int_pair());
            }

        } finally {
            sync.unlock();
        }
    }

    private void startAlertsLoop() {
        alertsLoop = new Thread(new AlertsLoop(), "SessionManager-alertsLoop");
        alertsLoop.setDaemon(true);
        alertsLoop.start();
    }

    private session createSession(String interfaces, int retries, boolean logging) {
        settings_pack sp = new settings_pack();

        sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(), interfaces);
        sp.set_int(settings_pack.int_types.max_retry_port_bind.swigValue(), retries);

        int alert_mask = alert.category_t.all_categories.swigValue();
        if (!logging) {
            int log_mask = alert.category_t.session_log_notification.swigValue() |
                    alert.category_t.torrent_log_notification.swigValue() |
                    alert.category_t.peer_log_notification.swigValue() |
                    alert.category_t.dht_log_notification.swigValue() |
                    alert.category_t.port_mapping_log_notification.swigValue() |
                    alert.category_t.picker_log_notification.swigValue();
            alert_mask = alert_mask & ~log_mask;
        }

        sp.set_int(settings_pack.int_types.alert_mask.swigValue(), alert_mask);

        return new session(sp);
    }

    private static LinkedList<Pair> defaultDhtRouters() {
        LinkedList<Pair> list = new LinkedList();

        list.add(new Pair("router.bittorrent.com", 6881));
        list.add(new Pair("dht.transmissionbt.com", 6881));

        // for DHT IPv6
        list.add(new Pair("outer.silotis.us", 6881));

        return list;
    }

    private final class AlertsLoop implements Runnable {

        @Override
        public void run() {
            alert_ptr_vector v = new alert_ptr_vector();

            while (session != null) {
                alert ptr = session.wait_for_alert_ms(ALERTS_LOOP_WAIT_MILLIS);

                if (ptr != null && session != null) {
                    session.pop_alerts(v);
                    int size = (int) v.size();
                    for (int i = 0; i < size; i++) {
                        alert a = v.get(i);

                        Alert alert = Alerts.cast(a);
                        System.out.println(alert);
                    }
                }
            }
        }
    }
}
