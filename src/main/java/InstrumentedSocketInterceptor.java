import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;




public class InstrumentedSocketInterceptor implements RSocketInterceptor {
  @Override
  public RSocket apply(RSocket rSocket) {
    return new LoggingRSocket(rSocket);
  }

  public static class LoggingRSocket implements RSocket {
    private final RSocket rSocket;

    public LoggingRSocket(RSocket rSocket) {
      this.rSocket = rSocket;
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
      var in = Flux.from(payloads)
          .doOnSubscribe(s -> System.out.println("subscribed to incoming"))
          .doOnCancel(() -> System.out.println("unsubscribed from incoming"))
          .doOnTerminate(() -> System.out.println("terminate incoming"))
          .doOnError(e -> System.out.println("error incoming: " + e.getClass()))
          .doFinally(signal -> System.out.println("signal incoming " + signal));

      var out = rSocket.requestChannel(in);

      return out
          .doOnSubscribe(s -> System.out.println("subscribed to outgoing"))
          .doOnCancel(() -> System.out.println("unsubscribed from outgoing"))
          .doOnComplete(() -> System.out.println("complete outgoing"))
          .doOnTerminate(() -> System.out.println("terminate outgoing"))
          .doOnError(e -> System.out.println("error outgoing: " + e.getClass()))
          .doFinally(signal -> System.out.println("signal outgoing " + signal));
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      return rSocket.requestResponse(payload);
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      return rSocket.requestStream(payload);
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
      return rSocket.metadataPush(payload);
    }

    @Override
    public double availability() {
      return rSocket.availability();
    }

    @Override
    public void dispose() {
      rSocket.dispose();
    }

    @Override
    public boolean isDisposed() {
      return rSocket.isDisposed();
    }

    @Override
    public Mono<Void> onClose() {
      return rSocket.onClose();
    }
  }
}
