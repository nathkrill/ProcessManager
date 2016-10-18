/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Foobar.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

package nl.adaptivity.process.engine;

import net.devrieze.util.Handle;
import net.devrieze.util.Transaction;
import nl.adaptivity.process.ProcessConsts.Engine;
import nl.adaptivity.xml.XmlDeserializer;
import nl.adaptivity.xml.XmlDeserializerFactory;
import nl.adaptivity.xml.XmlException;
import nl.adaptivity.xml.XmlReader;

import javax.xml.namespace.QName;

@XmlDeserializer(HProcessInstance.Factory.class)
public final class HProcessInstance<T extends Transaction> extends XmlHandle<ProcessInstance<T>> {

  public static class Factory implements XmlDeserializerFactory<HProcessInstance> {

    @Override
    public HProcessInstance deserialize(final XmlReader reader) throws XmlException {
      return HProcessInstance.deserialize(reader);
    }
  }

  public static final java.lang.String ELEMENTLOCALNAME = "instanceHandle";
  public static final QName ELEMENTNAME = new QName(Engine.NAMESPACE, ELEMENTLOCALNAME, Engine.NSPREFIX);

  public HProcessInstance() {
    super(-1);
  }

  public HProcessInstance(final Handle<? extends ProcessInstance<T>> handle) {
    super(handle);
  }

  private static HProcessInstance deserialize(final XmlReader in) throws XmlException {
    return nl.adaptivity.xml.XmlUtil.<nl.adaptivity.process.engine.HProcessInstance>deserializeHelper(new HProcessInstance(), in);
  }

  @Override
  public QName getElementName() {
    return ELEMENTNAME;
  }

  @Override
  public boolean equals(final Object obj) {
    return (obj == this) || ((obj instanceof HProcessInstance) && (getHandleValue() == ((HProcessInstance) obj).getHandleValue()));
  }

  @Override
  public int hashCode() {
    return (int) getHandleValue();
  }

}
