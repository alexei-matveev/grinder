// Copyright (C) 2003 Philip Aston
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
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.communication;

import net.grinder.common.GrinderException;


/**
 * Work queue and worker threads.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class Kernel {

  private final ThreadSafeQueue m_workQueue = new ThreadSafeQueue();
  private final ThreadGroup m_threadGroup = new ThreadGroup("Kernel");
  private boolean m_shutdown = false;
  private int m_workerThreads;

  /**
   * Constructor.
   *
   * @param numberOfThreads Number of worker threads to use.
   */
  public Kernel(int numberOfThreads) {

    m_threadGroup.setDaemon(true);

    for (int i = 0; i < numberOfThreads; ++i) {
      new WorkerThread(m_threadGroup, i).start();
    }
  }

  /**
   * Queue some work.
   *
   * @param work The work.
   * @throws ShutdownException If the Kernel has been stopped.
   */
  public void execute(Runnable work) throws ShutdownException {
    if (m_shutdown) {
      throw new ShutdownException("Kernel is stopped");
    }

    try {
      m_workQueue.queue(work);
    }
    catch (ThreadSafeQueue.ShutdownException e) {
      throw new ShutdownException("Kernel is stopped", e);
    }
  }

  /**
   * Shut down this kernel, waiting for work to complete.
   *
   * @throws InterruptedException If our thread is interrupted whilst
   * we are waiting for work to complete.
   */
  public void gracefulShutdown() throws InterruptedException {

    m_shutdown = true;

    // Wait until the queue is empty.
    synchronized (m_workQueue.getMutex()) {
      while (m_workQueue.getSize() > 0) {
        m_workQueue.getMutex().wait();
      }
    }

    // In case a worker thread is waiting in a Runnable.
    m_threadGroup.interrupt();

    // Now wait until all the worker threads have completed.
    synchronized (m_threadGroup) {
      while (m_workerThreads > 0) {
        m_threadGroup.wait();
      }
    }
  }


  /**
   * Shut down this kernel, discarding any outstanding work.
   */
  public void forceShutdown() {

    m_shutdown = true;

    m_workQueue.shutdown();

    // In case a worker thread is waiting in a Runnable.
    m_threadGroup.interrupt();
  }

  private final class WorkerThread extends Thread {

    public WorkerThread(ThreadGroup threadGroup, int workerThreadIndex) {
      super(threadGroup, "Worker thread " + workerThreadIndex);
      setDaemon(true);
      ++m_workerThreads;
    }

    public void run() {

      try {
        while (true) {
          final Runnable runnable =
            (Runnable) m_workQueue.dequeue(!m_shutdown);

          if (runnable == null) {
            // We're shutting down and the queue is empty.
            break;
          }

          runnable.run();
        }
      }
      catch (ThreadSafeQueue.ShutdownException e) {
        // We've been shutdown, exit this thread.
      }
      finally {
        synchronized (m_threadGroup) {
          --m_workerThreads;
          m_threadGroup.notifyAll();
        }
      }
    }
  }

  /**
   * Exception that indicates <code>Kernel</code> has been shutdown.
   * It doesn't extend {@link CommunicationException} because
   * typically callers want to propagate
   * <code>ShutdownException</code>s but handle
   * <code>CommunicationException</code>s locally.
   **/
  static final class ShutdownException extends GrinderException {
    private ShutdownException(String s) {
      super(s);
    }

    private ShutdownException(String s, Exception e) {
      super(s, e);
    }
  }
}
