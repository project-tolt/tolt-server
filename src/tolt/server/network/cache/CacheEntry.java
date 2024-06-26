
package tolt.server.network.cache;

import java.io.OutputStream;
import javax.net.ssl.SSLSocket;

public class CacheEntry {

    public SSLSocket socket;
    public OutputStream stream;

    public int getId () { return socket.hashCode(); }

    public String getName ()
        { return socket.getRemoteSocketAddress().toString(); }

    public CacheEntry (SSLSocket socket) { try {

        this.socket = socket;
        this.stream = socket.getOutputStream();

    } catch (Exception e) {} }

    public void close () { try {

        socket.close();

    } catch (Exception e) {} }
}
