import React, {useEffect, useRef, useState} from 'react';
import './App.css';
import {JsonSerializers, RSocketClient} from "rsocket-core";
import RSocketWebSocketClient from "rsocket-websocket-client";
import {ConnectionStatus, ISubscription, Payload, ReactiveSocket} from "rsocket-types";
import {Flowable} from "rsocket-flowable";

async function connect(): Promise<[RSocketWebSocketClient, ReactiveSocket<Message, object>]> {
  console.log("connecting")
  let wsClient = new RSocketWebSocketClient({url: 'ws://localhost:9000'});
  const socketClient = new RSocketClient<Message, object>({
    serializers: JsonSerializers,
    setup: {
      dataMimeType: 'text/plain',
      metadataMimeType: 'text/plain',
      keepAlive: 30_000,
      lifetime: 90_000,
    },
    transport: wsClient,
  });
  const rsocket = await socketClient.connect();
  return [wsClient, rsocket];
}

class MessageInput {
  budget = 0;
  cancelled = false;
  out: Flowable<Payload<Message, object>>;
  sink?: ((m: string) => void);
  setBudget: (b: number) => void;

  constructor(setBudget: (b: number) => void, setError: (value: string) => void) {
    console.log("creating an input")
    this.setBudget = setBudget;
    this.out = new Flowable<Payload<Message, object>>(subscriber => {
      const self = this;
      subscriber.onSubscribe({
        request(n: number): void {
          console.log("requested " + n)
          self.budget += n;
          self.setBudget(self.budget)
          setError("")
        },
        cancel(): void {
          self.cancelled = false;
        }
      });

      this.sink = (msg) => subscriber.onNext({
        data: {msg: msg}
      });
    });
  }

  sendMessage(msg: string) {
    if (!this.budget || !this.sink || this.cancelled) {
      throw new Error("server can't accept messages");
    }
    this.budget--;
    this.setBudget(this.budget)
    this.sink(msg);
  }
}

type Message = Readonly<{
  msg: string;
}>

function App() {
  const [status, setStatus] = useState<ConnectionStatus | undefined>(undefined);
  const [messages, setMessages] = useState<string[]>([]);
  const [budget, setBudget] = useState<number | undefined>(undefined);
  const [currentMessage, setCurrentMessage] = useState<string>("");
  const [error, setError] = useState<string>("");
  const messageInput = useRef<MessageInput | undefined>(undefined);

  useEffect(() => {
    (async function () {
      const [wsClient, rsocket] = await connect();

      wsClient.connectionStatus().subscribe({
        onSubscribe(s) {
          s.request(2147483642);
        },
        onNext(t) {
          setStatus(t);
          if (t.kind === 'CONNECTED') {
            console.log("requesting a channel")
            const input = new MessageInput(setBudget, setError);
            messageInput.current = input;
            rsocket.requestChannel(input.out).subscribe({
              onSubscribe(subscription: ISubscription): void {
                subscription.request(2147483647);
              },
              onError(error: Error): void {
                console.error(error);
                setError(error.message);
              },
              onNext(value: Payload<Message, object>): void {
                const msg = value.data!.msg;
                setMessages(messages => [...messages, msg]);
              },
              onComplete(): void {
              }
            });
          }
        },
        onError(e) {
          console.error(e);
        },
        onComplete() {
          console.info("completed");
        }
      });
    }());
  }, []);

  return (
    <div className="App">
      Status: {JSON.stringify(status)}<br/>
      Budget: {budget ?? ""}<br/>
      {error ? <div style={{color: "red"}}>Error: {error}</div> : <div/>}
      <hr/>
      <input
        type="text"
        value={currentMessage}
        onChange={(input) => setCurrentMessage(input.target.value)}/>
      <input
        type="button"
        value={"Send message"}
        onClick={(e) => {
          try {
            messageInput.current!.sendMessage(currentMessage);
            setCurrentMessage("");
          } catch (error) {
            setError(error.message);
          }
        }}/>
      <hr/>
      <div>
        Messages:
        <ul>
          {messages.map((message, index) =>
            <li key={index}>
              {message}
            </li>)}
        </ul>
      </div>
    </div>
  );
}

export default App;
