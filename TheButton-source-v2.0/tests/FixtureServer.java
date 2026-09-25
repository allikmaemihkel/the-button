import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local-only integration fixture. Forward with adb reverse tcp:8765 tcp:8765. */
class FixtureServer {
    public static void main(String[] args) throws Exception {
        byte[] html=Files.readAllBytes(Path.of("tests/fixture.html"));
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",8765),0);
        server.createContext("/",exchange->{exchange.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");exchange.sendResponseHeaders(200,html.length);exchange.getResponseBody().write(html);exchange.close();});
        server.start();System.out.println("Fixture listening on localhost:8765");
    }
}
