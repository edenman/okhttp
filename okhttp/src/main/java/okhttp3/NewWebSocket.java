/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okhttp3;

import okio.ByteString;

/**
 * A non-blocking interface to a websocket. Use the {@link NewWebSocket.Factory factory} to create
 * instances; usually this is {@link OkHttpClient}.
 *
 * <h3>Websocket Lifecycle</h3>
 *
 * Upon normal operation each websocket progresses through a sequence of states:
 *
 * <ul>
 *   <li><strong>Connecting:</strong> the initial state of each websocket. Messages may be enqueued
 *       but they won't be transmitted until the websocket is open.
 *   <li><strong>Open:</strong> the websocket has been accepted by the remote peer and is fully
 *       operational. Messages in either direction are enqueued for immediate transmission.
 *   <li><strong>Closing:</strong> one of the peers on the websocket has initiated a graceful
 *       shutdown. The websocket will continue to transmit already-enqueued messages but will refuse
 *       to enqueue new ones.
 *   <li><strong>Closed:</strong> the websocket has transmitted all of its messages and has received
 *       all messages from the peer.
 * </ul>
 *
 * Websockets may fail due to HTTP upgrade problems, connectivity problems, or if either peer
 * chooses to short-circuit the graceful shutdown process:
 *
 * <ul>
 *   <li><strong>Canceled:</strong> the websocket connection failed. Messages that were successfully
 *       enqueued by either peer may not have been transmitted to the other.
 * </ul>
 *
 * Note that the state progression is independent for each peer. Arriving at a gracefully-closed
 * state indicates that a peer has sent all of its outgoing messages and received all of its
 * incoming messages. But it does not guarantee that the other peer will successfully receive all of
 * its incoming messages.
 */
public interface NewWebSocket {
  /** Returns the original request that initiated this websocket. */
  Request request();

  /**
   * Returns the size in bytes of all messages enqueued to be transmitted to the server. This
   * doesn't include framing overhead. It also doesn't include any bytes buffered by the operating
   * system or network intermediaries. This method returns 0 if no messages are waiting
   * in the queue. If may return a nonzero value after the websocket has been canceled; this
   * indicates that enqueued messages were not transmitted.
   */
  long queueSize();

  /**
   * Attempts to enqueue {@code text} to be UTF-8 encoded and sent as a the data of a text (type
   * {@code 0x1}) message.
   *
   * <p>This method returns true if the message was enqueued. Messages that would overflow the
   * outgoing message buffer will be rejected and trigger a {@link #close graceful shutdown} of
   * this websocket. This method returns false in that case, and in any other case where this
   * websocket is closing, closed, or canceled.
   *
   * <p>This method returns immediately.
   */
  boolean send(String text);

  /**
   * Attempts to enqueue {@code bytes} to be sent as a the data of a binary (type {@code 0x2})
   * message.
   *
   * <p>This method returns true if the message was enqueued. Messages that would overflow the
   * outgoing message buffer will be rejected and trigger a {@link #close graceful shutdown} of
   * this websocket. This method returns false in that case, and in any other case where this
   * websocket is closing, closed, or canceled.
   *
   * <p>This method returns immediately.
   */
  boolean send(ByteString bytes);

  /**
   * Attempts to initiate a graceful shutdown of this websocket. Any already-enqueued messages will
   * be transmitted before the close message is sent but subsequent calls to {@link #send} will
   * return false and their messages will not be enqueued.
   *
   * <p>This returns true if a graceful shutdown was initiated by this call. It returns false and if
   * a graceful shutdown was already underway or if the websocket is already closed or canceled.
   *
   * @param code Status code as defined by <a
   * href="http://tools.ietf.org/html/rfc6455#section-7.4">Section 7.4 of RFC 6455</a> or {@code 0}.
   * @param reason Reason for shutting down or {@code null}.
   */
  boolean close(int code, String reason);

  /**
   * Immediately and violently release resources held by this websocket, discarding any enqueued
   * messages. This does nothing if the websocket has already been closed or canceled.
   */
  void cancel();

  interface Factory {
    NewWebSocket newWebSocket(Request request, Listener listener);
  }

  abstract class Listener {
    /**
     * Invoked when a websocket has been accepted by the remote peer and may begin transmitting
     * messages.
     */
    public void onOpen(NewWebSocket websocket, Response response) {
    }

    /** Invoked when a text (type {@code 0x1}) message has been received. */
    public void onMessage(NewWebSocket websocket, String text) {
    }

    /** Invoked when a binary (type {@code 0x2}) message has been received. */
    public void onMessage(NewWebSocket websocket, ByteString bytes) {
    }

    /** Invoked when the peer has indicated that no more incoming messages will be transmitted. */
    public void onClosing(NewWebSocket websocket, int code, String reason) {
    }

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the
     * connection has been successfully released. No further calls to this listener will be made.
     */
    public void onClosed(NewWebSocket websocket, int code, String reason) {
    }

    /**
     * Invoked when a websocket has been violently closed. Both outgoing and incoming messages may
     * have been lost. No further calls to this listener will be made.
     */
    public void onFailure(NewWebSocket websocket, Throwable t, Response response) {
    }
  }
}
