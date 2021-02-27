import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Main {
  public static void main(String[] args) throws InterruptedException {
    var server = RSocketServer //
        .create((setup, sendingSocket) -> Mono.just(new RSocket() {
          @Override
          public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
            return Flux.from(payloads)
                .limitRate(5, 3)
                .map(msg -> {
                  try {
                    Thread.sleep(1000L);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  return DefaultPayload.create(msg.getDataUtf8().replace('c', 's'));
                });
          }
        }))
        .interceptors(registry -> {
          registry.forResponder(new InstrumentedSocketInterceptor());
          registry.forConnection(new InstrumentedConnectionInterceptor());
        })
        .bind(WebsocketServerTransport.create(9000))
        .block();
    System.out.println("address " + server.address());
    Thread.currentThread().join();
  }
}
