package com.cmcc.ics.rcs2.siphademo;

import java.io.Serializable;

/**
 * @Type test
 * @Desc test
 * @author hpai
 * @date 2018年03月09日 16:47
 * @version
 */
public class SessionCache implements Serializable {

    private String sessionId;

    private String transactionId;

    private int currentState;

    private String contents;

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setTransactionId(String transactionId) {
        this.sessionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public String getContents() {
        return contents;
    }
}
/**
 * Revision history
 * -------------------------------------------------------------------------
 *
 * Date Author Note
 * -------------------------------------------------------------------------
 * 2018年03月09日 16:47 hpai create
 */
