package InfrastructureManager;

public class EdgeNode {
    private String id;
    private String ipAddress;
    private boolean connected;

    public EdgeNode() {
        this.id = null;
        this.ipAddress = null;
        this.connected = false;
    }

    public EdgeNode(String id, String ip, boolean connected) {
        this.id = id;
        this.ipAddress = ip;
        this.connected = connected;
    }

    public String getId() {
        return id;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return "EdgeNode{" +
                "id='" + id + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", connected=" + connected +
                '}';
    }
}
