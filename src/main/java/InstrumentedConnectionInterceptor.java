import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.frame.FrameHeaderCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InstrumentedConnectionInterceptor implements DuplexConnectionInterceptor {

  @Override
  public DuplexConnection apply(Type type, DuplexConnection connection) {
    return new InstrumentedConnection(type, connection);
  }

  private static class InstrumentedConnection implements DuplexConnection {
    private final Type connectionType;
    private final DuplexConnection connection;

    public InstrumentedConnection(Type connectionType, DuplexConnection connection) {
      this.connectionType = connectionType;
      this.connection = connection;
    }

    @Override
    public Mono<Void> send(Publisher<ByteBuf> frames) {
      return connection.send(Flux.from(frames) //
          .doOnNext(frame -> recordFrame("out", frame, connectionType)));
    }

    @Override
    public Mono<Void> sendOne(ByteBuf frame) {
      recordFrame("in", frame, connectionType);
      return connection.sendOne(frame);
    }

    @Override
    public Flux<ByteBuf> receive() {
      return Flux.from(connection.receive()) //
          .doOnNext(frame -> recordFrame("in", frame, connectionType));
    }

    @Override
    public double availability() {
      return connection.availability();
    }

    @Override
    public Mono<Void> onClose() {
      return connection.onClose();
    }

    @Override
    public ByteBufAllocator alloc() {
      return connection.alloc();
    }

    @Override
    public void dispose() {
      connection.dispose();
    }

    @Override
    public boolean isDisposed() {
      return connection.isDisposed();
    }

    private void recordFrame(String flux, ByteBuf frame, Type connectionType) {
      FrameType frameType = FrameHeaderCodec.frameType(frame);
      System.out.printf("%s%s:%s%n", "in".equals(flux) ? "->" : "<-", connectionType, frameType);
    }
  }
}
