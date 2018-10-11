package org.dreamscale.flow.activity


class InMemoryMessageLogger implements MessageLogger {

    List<Object> messages = []

    void flush() {
        messages.clear()
    }

    @Override
    void writeMessage(Object message) {
        messages.add(message)
    }

}
