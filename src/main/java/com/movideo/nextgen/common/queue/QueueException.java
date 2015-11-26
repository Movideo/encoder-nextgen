package com.movideo.nextgen.common.queue;

import com.movideo.nextgen.common.exception.MovideoException;

@SuppressWarnings("serial")
public class QueueException extends MovideoException {
   
    public QueueException(int status, String message, Throwable t) {
   	super(status, message, t);
       }
}
