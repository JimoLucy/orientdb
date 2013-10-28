/*
 * Copyright 2010-2012 Luca Garulli (l.garulli(at)orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.orient.core.db.record.ridset.sbtree;

import java.util.concurrent.ConcurrentMap;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.orientechnologies.common.serialization.types.OBooleanSerializer;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.sbtreebonsai.local.OBonsaiBucketPointer;
import com.orientechnologies.orient.core.index.sbtreebonsai.local.OSBTreeBonsai;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.orientechnologies.orient.core.storage.impl.local.OStorageLocalAbstract;

/**
 * @author <a href="mailto:enisher@gmail.com">Artem Orobets</a>
 */
public class OSBTreeCollectionManager {
  private static final int                                                                 CACHE_SIZE        = OGlobalConfiguration.SBTREE_COLLECTION_MANAGER_CACHE_SIZE
                                                                                                                 .getValueAsInteger();
  public static final String                                                               DEFAULT_EXTENSION = ".rbt";

  private static final String                                                              FILE_NAME         = "ridset";

  private final ConcurrentMap<OBonsaiBucketPointer, OSBTreeBonsai<OIdentifiable, Boolean>> cache;

  public OSBTreeCollectionManager() {
    this.cache = new ConcurrentLinkedHashMap.Builder<OBonsaiBucketPointer, OSBTreeBonsai<OIdentifiable, Boolean>>()
        .maximumWeightedCapacity(CACHE_SIZE).build();
  }

  public OSBTreeBonsai<OIdentifiable, Boolean> createSBTree() {
    final OSBTreeBonsai<OIdentifiable, Boolean> tree = new OSBTreeBonsai<OIdentifiable, Boolean>(DEFAULT_EXTENSION, true);

    tree.create(FILE_NAME, OLinkSerializer.INSTANCE, OBooleanSerializer.INSTANCE,
        (OStorageLocalAbstract) ODatabaseRecordThreadLocal.INSTANCE.get().getStorage());

    final OSBTreeBonsai<OIdentifiable, Boolean> oldTree = cache.put(tree.getRootBucketPointer(), tree);
    assert oldTree == null;

    return tree;
  }

  public OSBTreeBonsai<OIdentifiable, Boolean> loadSBTree(OBonsaiBucketPointer rootIndex) {
    OSBTreeBonsai<OIdentifiable, Boolean> tree = cache.get(rootIndex);
    if (tree != null)
      return tree;

    tree = new OSBTreeBonsai<OIdentifiable, Boolean>(DEFAULT_EXTENSION, true);
    tree.load(FILE_NAME, rootIndex, (OStorageLocalAbstract) ODatabaseRecordThreadLocal.INSTANCE.get().getStorage());

    cache.putIfAbsent(tree.getRootBucketPointer(), tree);
    tree = cache.get(rootIndex);

    return tree;
  }

  public void remove(OBonsaiBucketPointer rootIndex) {
    final OSBTreeBonsai<OIdentifiable, Boolean> tree = cache.remove(rootIndex);
    if (tree != null)
      tree.delete();
  }

  public void shutdown() {
    cache.clear();
  }
}
