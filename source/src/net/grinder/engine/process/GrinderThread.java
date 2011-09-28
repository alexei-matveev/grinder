// Copyright (C) 2000 Paco Gomez
// Copyright (C) 2000 - 2008 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.process;

import java.io.PrintWriter;

import net.grinder.common.GrinderProperties;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.process.ScriptEngineService.ScriptEngine;
import net.grinder.engine.process.ScriptEngineService.ScriptExecutionException;
import net.grinder.engine.process.ScriptEngineService.WorkerRunnable;
import net.grinder.statistics.StatisticsServices;
import net.grinder.util.Sleeper;


/**
 * The class executed by each thread.
 *
 * @author Paco Gomez
 * @author Philip Aston
 */
class GrinderThread implements java.lang.Runnable {

  private final WorkerThreadSynchronisation m_threadSynchronisation;
  private final ProcessLifeCycleListener m_processLifeCycle;
  private final GrinderProperties m_properties;
  private final Sleeper m_sleeper;
  private final ScriptEngine m_scriptEngine;
  private final ThreadContext m_context;
  private final WorkerRunnable m_workerRunnable;

  /**
   * The constructor.
   */
  public GrinderThread(WorkerThreadSynchronisation threadSynchronisation,
                       ProcessLifeCycleListener processLifeCycle,
                       GrinderProperties properties,
                       Sleeper sleeper,
                       LoggerImplementation loggerImplementation,
                       StatisticsServices statisticsServices,
                       ScriptEngine scriptEngine,
                       int threadID,
                       WorkerRunnable workerRunnable)
    throws EngineException {

    m_threadSynchronisation = threadSynchronisation;
    m_processLifeCycle = processLifeCycle;
    m_properties = properties;
    m_sleeper = sleeper;
    m_scriptEngine = scriptEngine;
    m_workerRunnable = workerRunnable;

    m_context =
      new ThreadContextImplementation(
        properties,
        statisticsServices,
        loggerImplementation.createThreadLogger(threadID),
        loggerImplementation.getFilenameFactory().
        createSubContextFilenameFactory(Integer.toString(threadID)),
        loggerImplementation.getDataWriter());

    // Dispatch the process context callback in the main thread.
    m_processLifeCycle.threadCreated(m_context);

    threadSynchronisation.threadCreated();
  }

  /**
   * The thread's main loop.
   */
  public void run() {
    m_processLifeCycle.threadStarted(m_context);

    final ThreadLogger logger = m_context.getThreadLogger();
    final PrintWriter errorWriter = logger.getErrorLogWriter();

    logger.setCurrentRunNumber(-1);

    // Fire begin thread event before creating the worker runnable to allow
    // plug-ins to do per-thread initialisation required by the script code.
    m_context.fireBeginThreadEvent();

    try {
      final WorkerRunnable scriptThreadRunnable;

      if (m_workerRunnable == null) {
        scriptThreadRunnable = m_scriptEngine.createWorkerRunnable();
      }
      else {
        scriptThreadRunnable = m_workerRunnable;
      }

      final int numberOfRuns = m_properties.getInt("grinder.runs", 1);

      if (numberOfRuns == 0) {
        logger.output("starting, will run forever");
      }
      else {
        logger.output("starting, will do " + numberOfRuns + " run" +
                      (numberOfRuns == 1 ? "" : "s"));
      }

      m_threadSynchronisation.awaitStart();

      m_sleeper.sleepFlat(m_properties.getLong("grinder.initialSleepTime", 0));

      int currentRun;

      for (currentRun = 0;
           numberOfRuns == 0 || currentRun < numberOfRuns;
           currentRun++) {

        logger.setCurrentRunNumber(currentRun);

        m_context.fireBeginRunEvent();

        try {
          scriptThreadRunnable.run();
        }
        catch (ScriptExecutionException e) {
          final Throwable cause = e.getCause();

          if (cause instanceof ShutdownException ||
              cause instanceof Sleeper.ShutdownException) {
            logger.output("shut down");
            break;
          }

          // Sadly PrintWriter only exposes its lock object to subclasses.
          synchronized (errorWriter) {
            logger.error("Aborted run due to " + e.getShortMessage());
            e.printStackTrace(errorWriter);
          }
        }

        m_context.fireEndRunEvent();
      }

      logger.setCurrentRunNumber(-1);

      logger.output("finished " + currentRun +
                    (currentRun == 1 ? " run" : " runs"));

      m_context.fireBeginShutdownEvent();

      try {
        scriptThreadRunnable.shutdown();
      }
      catch (ScriptExecutionException e) {
        // Sadly PrintWriter only exposes its lock object to subclasses.
        synchronized (errorWriter) {
          logger.error(
            "Aborted test runner shut down due to " + e.getShortMessage());
          e.printStackTrace(errorWriter);
        }
      }

      m_context.fireEndThreadEvent();
    }
    catch (ScriptExecutionException e) {
      synchronized (errorWriter) {
        logger.error("Aborting thread due to " + e.getShortMessage());
        e.printStackTrace(errorWriter);
      }
    }
    catch (Exception e) {
      synchronized (errorWriter) {
        logger.error("Aborting thread due to " + e);
        e.printStackTrace(errorWriter);
      }
    }
    finally {
      logger.setCurrentRunNumber(-1);

      m_threadSynchronisation.threadFinished();
    }
  }
}
