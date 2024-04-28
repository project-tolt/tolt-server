
package tolt.server.network.module;

public class Packet {

    public int sender;
    public short id;
    public byte[] data;

    public Packet (int sender, short id, byte[] data) {

        this.sender = sender;
        this.id = id;
        this.data = data;
    }
}
