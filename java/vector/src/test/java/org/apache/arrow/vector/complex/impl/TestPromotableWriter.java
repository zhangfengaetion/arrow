/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.arrow.vector.complex.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.DirtyRootAllocator;
import org.apache.arrow.vector.complex.AbstractMapVector;
import org.apache.arrow.vector.complex.MapVector;
import org.apache.arrow.vector.complex.UnionVector;
import org.apache.arrow.vector.holders.UInt4Holder;
import org.apache.arrow.vector.types.MaterializedField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPromotableWriter {
  private final static String EMPTY_SCHEMA_PATH = "";

  private BufferAllocator allocator;

  @Before
  public void init() {
    allocator = new DirtyRootAllocator(Long.MAX_VALUE, (byte) 100);
  }

  @After
  public void terminate() throws Exception {
    allocator.close();
  }

  @Test
  public void testPromoteToUnion() throws Exception {
    final MaterializedField field = MaterializedField.create(EMPTY_SCHEMA_PATH, UInt4Holder.TYPE);

    try (final AbstractMapVector container = new MapVector(field, allocator, null);
         final MapVector v = container.addOrGet("test", MapVector.TYPE, MapVector.class);
         final PromotableWriter writer = new PromotableWriter(v, container)) {

      container.allocateNew();

      writer.start();

      writer.setPosition(0);
      writer.bit("A").writeBit(0);

      writer.setPosition(1);
      writer.bit("A").writeBit(1);

      writer.setPosition(2);
      writer.integer("A").writeInt(10);

      // we don't write anything in 3

      writer.setPosition(4);
      writer.integer("A").writeInt(100);

      writer.end();

      container.getMutator().setValueCount(5);

      final UnionVector uv = v.getChild("A", UnionVector.class);
      final UnionVector.Accessor accessor = uv.getAccessor();

      assertFalse("0 shouldn't be null", accessor.isNull(0));
      assertEquals(false, accessor.getObject(0));

      assertFalse("1 shouldn't be null", accessor.isNull(1));
      assertEquals(true, accessor.getObject(1));

      assertFalse("2 shouldn't be null", accessor.isNull(2));
      assertEquals(10, accessor.getObject(2));

      assertTrue("3 should be null", accessor.isNull(3));

      assertFalse("4 shouldn't be null", accessor.isNull(4));
      assertEquals(100, accessor.getObject(4));
    }
  }
}
