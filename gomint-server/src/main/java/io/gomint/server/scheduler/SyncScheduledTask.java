/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.scheduler;

import io.gomint.scheduler.Task;
import io.gomint.util.CompleteHandler;
import io.gomint.util.ExceptionHandler;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author geNAZt
 * @version 1.0
 */
@ToString( of = { "task", "period", "nextExecution" } )
public class SyncScheduledTask implements Task, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger( SyncScheduledTask.class );
    @Getter private final Runnable task;
    private long period;          // -1 means no reschedule
    @Getter private long nextExecution; // -1 is cancelled
    private ExceptionHandler exceptionHandler;
    private List<CompleteHandler> completeHandlerList;
    private SyncTaskManager manager;

    /**
     * Constructs a new SyncScheduledTask. It needs to be executed via a normal {@link java.util.concurrent.ExecutorService}
     *
     * @param manager which schedules this task
     * @param task    The runnable which should be executed
     * @param delay   Amount of time units to wait until the invocation of this execution
     * @param period  Amount of time units for the delay after execution to run the runnable again
     * @param unit    of time
     */
    public SyncScheduledTask( SyncTaskManager manager, Runnable task, long delay, long period, TimeUnit unit ) {
        this.task = task;
        this.period = ( period >= 0 ) ? unit.toMillis( period ) : -1;
        this.nextExecution = ( delay >= 0 ) ? System.currentTimeMillis() + unit.toMillis( delay ) : -1;
        this.manager = manager;
    }

    @Override
    public void run() {
        // CHECKSTYLE:OFF
        try {
            this.task.run();
        } catch ( Exception e ) {
            if ( this.exceptionHandler != null ) {
                if ( !this.exceptionHandler.onException( e ) ) {
                    this.cancel();
                }
            } else {
                LOGGER.warn( "Error in executing task: ", e );
            }
        }
        // CHECKSTYLE:ON

        if ( this.period > 0 ) {
            this.nextExecution = System.currentTimeMillis() + this.period;
        } else {
            this.cancel();
        }
    }

    @Override
    public void cancel() {
        this.period = -1;
        this.nextExecution = -1;
        this.fireCompleteHandlers();
        this.manager.removeTask( this );
    }

    @Override
    public void onException( ExceptionHandler exceptionHandler ) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void onComplete( CompleteHandler completeHandler ) {
        if ( this.completeHandlerList == null ) {
            this.completeHandlerList = new ArrayList<>();
        }

        this.completeHandlerList.add( completeHandler );
    }

    private void fireCompleteHandlers() {
        if ( this.completeHandlerList != null ) {
            for ( CompleteHandler completeHandler : this.completeHandlerList ) {
                completeHandler.onComplete();
            }

            this.completeHandlerList = null;
        }
    }

}
