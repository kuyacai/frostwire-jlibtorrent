package com.frostwire.jlibtorrent.plugins;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.swig.bdecode_node;

import java.util.List;

/**
 * This is the base class for a session plugin. One primary feature
 * is that it is notified of all torrents that are added to the session,
 * and can add its own torrent_plugins.
 *
 * @author gubatron
 * @author aldenml
 */
public interface Plugin {

    boolean handleOperation(Operation op);

    /**
     * this is called by the session every time a new torrent is added.
     * // The ``torrent*`` points to the internal torrent object created
     * // for the new torrent. The ``void*`` is the userdata pointer as
     * // passed in via add_torrent_params.
     * //
     * // If the plugin returns a torrent_plugin instance, it will be added
     * // to the new torrent. Otherwise, return an empty shared_ptr to a
     * // torrent_plugin (the default).
     *
     * @param t
     * @return
     */
    TorrentPlugin newTorrent(TorrentHandle t);

    /**
     * called when plugin is added to a session
     */
    void added(SessionHandle s);

    void registerDhtPlugins(List<Pair<String, DhtPlugin>> plugins);

    /**
     * Called when an alert is posted alerts that are filtered are not posted.
     *
     * @param a
     */
    void onAlert(Alert a);

    /**
     * return true if the add_torrent_params should be added.
     *
     * @param infoHash
     * @param pc
     * @param p
     * @return
     */
    boolean onUnknownTorrent(Sha1Hash infoHash, PeerConnectionHandle pc, AddTorrentParams p);

    /**
     * called once per second.
     */
    void onTick();

    /**
     * called when saving settings state.
     *
     * @param e
     */
    void saveState(Entry e);

    /**
     * called when loading settings state.
     *
     * @param n
     */
    void loadState(bdecode_node n);

    enum Operation {
        NEW_TORRENT,
        REGISTER_DHT_EXTENSIONS,
        ON_ALERT,
        ON_UNKNOWN_TORRENT,
        SAVE_STATE,
        LOAD_STATE
    }
}
