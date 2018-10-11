package org.dreamscale.flow.activity;


public interface MessageLogger {

    void flush();

    void writeMessage(Object message);

}
