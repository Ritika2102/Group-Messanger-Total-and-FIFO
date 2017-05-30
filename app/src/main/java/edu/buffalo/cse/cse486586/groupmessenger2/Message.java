package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Created by Fnu on 3/15/2017.
 */

class Message implements Comparable<Message> {
    float pr;
    String msg;
    boolean isdecided;
    int id;

    public Message(float pr, String msg, int id) {
        this.pr = pr;
        this.msg = msg;
        this.id = id;

    }
    public Message(float pr, String msg,boolean isdecided) {
        this.pr = pr;
        this.msg = msg;
        this.isdecided=isdecided;

    }

    @Override
    public int compareTo(Message m) {
        if(pr>m.pr){
            return 1;
        }else if(pr<m.pr){
            return -1;
        }else{
            return 0;
        }
    }
}

