package modern.challenge;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import jdk.incubator.concurrent.StructuredTaskScope;
import jdk.incubator.concurrent.StructuredTaskScope.ShutdownOnFailure;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tT] [%4$-7s] %5$s %n");
        
        buildTestingTeam();
    }

    public static TestingTeam buildTestingTeam() throws InterruptedException, ExecutionException {
        
        try (ShutdownOnFailure scope = new StructuredTaskScope.ShutdownOnFailure()) {

            Future<String> future1 = scope.fork(() -> fetchTester(1));
            Future<String> future2 = scope.fork(() -> fetchTester(2));
            Future<String> future3 = scope.fork(() -> fetchTester(Integer.MAX_VALUE)); // this will cause the exception

            scope.join();
            scope.throwIfFailed();

            // because we have an exception the following code will not be executed
            return new TestingTeam(future1.resultNow(), future2.resultNow(), future3.resultNow());            
        }
    }
    
    public static String fetchTester(int id) throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest requestGet = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://reqres.in/api/users/" + id))
                .build();

        HttpResponse<String> responseGet = client.send(
                requestGet, HttpResponse.BodyHandlers.ofString());

        if (responseGet.statusCode() == 200) {
            return responseGet.body();
        }

        throw new UserNotFoundException("Code: " + responseGet.statusCode());
    }
}
