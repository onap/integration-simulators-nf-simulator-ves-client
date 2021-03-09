package org.onap.pnfsimulator.integration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;

public class TestUtils {

    private TestUtils() {}

    public static final String PNF_SIMULATOR_DB = "pnf_simulator";
    public static final String COMMON_EVENT_HEADER = "commonEventHeader";
    public static final String PNF_SIMULATOR_DB_PSWD = "zXcVbN123!";
    public static final String PNF_SIMULATOR_DB_USER = "pnf_simulator_user";
    public static final String PATCHED = "patched";
    public static final String SINGLE_EVENT_URL = "http://0.0.0.0:5000/simulator/event";

    public static Document findSourceNameInMongoDB() throws UnknownHostException {
        MongoCredential credential = MongoCredential
            .createCredential(PNF_SIMULATOR_DB_USER, PNF_SIMULATOR_DB, PNF_SIMULATOR_DB_PSWD.toCharArray());
        MongoClient mongoClient = new MongoClient(new ServerAddress(Inet4Address.getLocalHost(), 27017),
            credential, MongoClientOptions.builder().build());
        MongoDatabase pnfSimulatorDb = mongoClient.getDatabase(PNF_SIMULATOR_DB);
        MongoCollection<Document> table = pnfSimulatorDb.getCollection("eventData");
        Document searchQuery = new Document();
        searchQuery.put(PATCHED, new Document("$regex", ".*" + "HistoricalEvent" + ".*"));
        FindIterable<Document> findOfPatched = table.find(searchQuery);
        Document dbObject = null;
        MongoCursor<Document> cursor = findOfPatched.iterator();
        if (cursor.hasNext()) {
            dbObject = cursor.next();
        }
        return dbObject;
    }

    public static String getCurrentIpAddress() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
            .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
            .filter(ip -> ip instanceof Inet4Address)
            .map(e -> (Inet4Address) e)
            .findFirst()
            .orElseThrow(RuntimeException::new)
            .getHostAddress();
    }
}
