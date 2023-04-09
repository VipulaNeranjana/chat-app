package lk.vipula.dep10.shared.model;

import lk.vipula.dep10.shared.enumaration.MessageHeader;

import java.io.Serializable;

public class Message implements Serializable {
    private MessageHeader messageHeader;
    private Object messageBody;
    private static final long serialVersionUID = 15537620666952793L;

    public Message() {
    }

    public Message(MessageHeader messageHeader, Object messageBody) {
        this.messageHeader = messageHeader;
        this.messageBody = messageBody;
    }

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public void setMessageHeader(MessageHeader messageHeader) {
        this.messageHeader = messageHeader;
    }

    public Object getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(Object messageBody) {
        this.messageBody = messageBody;
    }
}
