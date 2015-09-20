package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by rahul on 3/7/15.
 */
public class PriorityComparator implements Comparator<Message> {
    @Override
    public int compare(Message lhs, Message rhs) {
        if (lhs.getFinalSeq() < rhs.getFinalSeq())
        {
            return -1;
        }
        if (lhs.getFinalSeq() > rhs.getFinalSeq())
        {
            return 1;
        }
        if( !lhs.isDeliverable() ){
            return 1;
        }
        return 0;
    }
}
