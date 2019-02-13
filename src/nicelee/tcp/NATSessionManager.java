package nicelee.tcp;

import java.util.HashMap;
import java.util.Map.Entry;

import nicelee.dns.util.CommonMethods;

public class NATSessionManager {
	static final int MAX_SESSION_COUNT = 60;
    static final long SESSION_TIMEOUT_NS = 60 * 1000000000L;
    static final HashMap<Integer, NATSession> Sessions = new HashMap<>();
    static final HashMap<NATSession, Integer> UDPNATSessions = new HashMap<>();

    public static Integer getPort(NATSession socket) {
    	return UDPNATSessions.get(socket);
    }
    
    public static NATSession getSession(int portKey) {
    	NATSession session = Sessions.get(portKey);
        if (session!=null) {
            session.LastNanoTime = System.nanoTime();
        }
        return Sessions.get(portKey);
    }

    public static int getSessionCount() {
        return Sessions.size();
    }

    static void clearExpiredSessions() {
        long now = System.nanoTime();
        for (Entry<Integer, NATSession> entry: Sessions.entrySet()) {
        	NATSession session = entry.getValue();
            if (now - session.LastNanoTime > SESSION_TIMEOUT_NS) {
            	Sessions.remove(entry.getKey());
            	UDPNATSessions.remove(entry.getValue());
            }
        }
    }

    public static NATSession createSession(int portKey, int remoteIP, short remotePort) {
        if (Sessions.size() > MAX_SESSION_COUNT) {
            clearExpiredSessions();//清理过期的会话。
        }

        NATSession session = new NATSession();
        session.LastNanoTime = System.nanoTime();
        session.RemoteIP = remoteIP;
        session.RemotePort = remotePort;

        if (session.RemoteHost == null) {
            session.RemoteHost = CommonMethods.ipIntToString(remoteIP);
        }
        Sessions.put(portKey, session);
        UDPNATSessions.put(session, portKey);
        return session;
    }
}
