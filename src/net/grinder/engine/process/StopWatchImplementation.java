// Copyright (C) 2005 Philip Aston
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

package net.grinder.engine.process;


/**
 * Implementation of {@link StopWatch}.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class StopWatchImplementation implements StopWatch {

  private long m_time;
  private long m_startTime = -1;

  public void start() {
    if (m_startTime != -1) {
      throw new StopWatchRunningException("Already running");
    }

    m_startTime = System.currentTimeMillis();
  }

  public void stop() {
    if (m_startTime == -1) {
      throw new StopWatchNotRunningException("Not running");
    }

    m_time = m_time + System.currentTimeMillis() - m_startTime;
    m_startTime = -1;
  }

  public void reset() throws StopWatchRunningException {
    if (m_startTime != -1) {
      throw new StopWatchRunningException("Still running");
    }

    m_time = 0;
  }

  public long getTime() {
    if (m_startTime != -1) {
      throw new StopWatchRunningException("Still running");
    }

    return m_time;
  }

  public void add(StopWatch watch) {
    m_time += watch.getTime();
  }
}
