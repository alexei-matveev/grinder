// Copyright (C) 2009 Philip Aston
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

package net.grinder.engine.process.instrumenter.dcr;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.grinder.engine.process.ScriptEngine.Recorder;
import net.grinder.script.NotWrappableTypeException;
import net.grinder.util.weave.Weaver;


/**
 * DCRInstrumenter.
 *
 * @author Philip Aston
 * @version $Revision:$
 */
final class JavaDCRInstrumenter extends DCRInstrumenter {

  /**
   * Constructor for DCRInstrumenter.
   *
   * @param weaver The weaver.
   * @param recorderRegistry The recorder registry.
   */
  public JavaDCRInstrumenter(Weaver weaver, RecorderRegistry recorderRegistry) {
    super(weaver, recorderRegistry);
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return "byte code transforming instrumenter for Java";
  }

  @Override
  protected Object instrument(Object target, Recorder recorder)
    throws NotWrappableTypeException {

    if (target instanceof Class<?>) {
      instrumentClass((Class<?>)target, recorder);
    }
    else {
      instrumentInstance(target, recorder);
    }

    return target;
  }

  private void instrumentClass(Class<?> targetClass, Recorder recorder)
    throws NotWrappableTypeException {

    for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
       instrument(targetClass, constructor, recorder);
    }

    // Instrument the static methods declared by the target class. Ignore
    // any parent class.
    for (Method method : targetClass.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers())) {
        instrument(targetClass, method, recorder);
      }
    }

//    Class<?> c = targetClass;
//
//    do {
//      for (Method method : c.getDeclaredMethods()) {
//        if (Modifier.isStatic(method.getModifiers())) {
//          instrument(c, method, recorder);
//        }
//      }
//
//      c = c.getSuperclass();
//    }
//    while (isInstrumentable(c));
  }

  private void instrumentInstance(Object target, Recorder recorder)
    throws NotWrappableTypeException {

    Class<?> c = target.getClass();

    do {
      for (Method method : c.getDeclaredMethods()) {
        if (!Modifier.isStatic(method.getModifiers())) {
          instrument(target, method, recorder);
        }
      }

      c = c.getSuperclass();
    }
    while (isInstrumentable(c));
  }
}