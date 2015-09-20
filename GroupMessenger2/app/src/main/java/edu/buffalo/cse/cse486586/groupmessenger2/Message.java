package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Created by rahul on 3/6/15.
 */
public class Message implements Serializable/*, Delayed */{
    // Sender's seq
    private int seq;
    private String portNum;
    // Sender's process ID
    private int senderProcessId;
    // process ID who is proposing seq
    private int proposalProcessId;
    // proposed seq by proposalProcessId
    private double proposedSeq;
    private double finalSeq;
    // Message
    private String msg;
    private boolean isDeliverable;

    private String type;

   // private long startTime;

    public Message(int seq , int processId , String portNum , boolean isDeliverable){
        this.seq = seq;
        this.senderProcessId = processId;
        this.portNum = portNum;
        this.isDeliverable = isDeliverable;
        //this.startTime = System.currentTimeMillis() + 10;
        this.type = "m";
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getSenderProcessId() {
        return senderProcessId;
    }

    public void setSenderProcessId(int senderProcessId) {
        this.senderProcessId = senderProcessId;
    }

    public int getProposalProcessId() {
        return proposalProcessId;
    }

    public void setProposalProcessId(int proposalProcessId) {
        this.proposalProcessId = proposalProcessId;
    }

    public double getProposedSeq() {
        return proposedSeq;
    }

    public void setProposedSeq(double proposedSeq) {
        this.proposedSeq = proposedSeq;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setDeliverable(boolean isDeliverable) {
        this.isDeliverable = isDeliverable;
    }

    public double getFinalSeq() {
        return finalSeq;
    }

    public void setFinalSeq(double finalSeq) {
        this.finalSeq = finalSeq;
    }

    public String getPortNum() {
        return portNum;
    }

    public void setPortNum(String portNum) {
        this.portNum = portNum;
    }

    /*@Override
    public int compareTo(Delayed obj) {
        if( this.startTime < ((Message)obj).startTime )
            return -1;
        if( this.startTime > ((Message)obj).startTime )
            return 1;

        return 0;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long timeDef = startTime - System.currentTimeMillis();
        return timeDef;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime + 2000;
    }*/

    public Message( String port){
        this.portNum = port;
        this.type = "HB";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
