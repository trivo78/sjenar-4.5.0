/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.dboe.trans.bplustree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.jena.atlas.lib.ByteBufferLib;
import org.apache.jena.dboe.base.buffer.RecordBuffer;
import org.apache.jena.dboe.base.recordbuffer.RecordBufferPage;
import org.apache.jena.dboe.base.recordbuffer.RecordBufferPageMgr;

/**
 * Tools for working with B+Trees and datastructures associated with them, rather directly.
 * Development support, not for use in released code.
 */
class BPlusTreeTools {
    /*public*/private static void binDump(String filename) {
        try(RandomAccessFile fh = new RandomAccessFile(filename, "r")) {
            ByteBuffer bb = ByteBuffer.allocate(8*1024);
            FileChannel ch = fh.getChannel();
            int idx = 0;
            while(true) {
                int x = ch.read(bb);
                if ( x < 0 )
                    break;
                ByteBufferLib.print(bb);
                bb.clear();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*public*/private static void bpt_scan_record_buffer(RecordBufferPageMgr recordPageMgr, boolean verbose) {
        System.out.print("[Scan Records] start\n");
        int idx = 0;
        int n = 0;
        int total = 0;
        if ( verbose ) System.out.printf("recordPageMgr = %s\n", recordPageMgr);

        // Blocks in scan order
        try {
            while ( idx >= 0 ) {
                if ( verbose ) System.out.printf("idx = %d\n", idx);
                RecordBufferPage page = recordPageMgr.getRead(idx);
                if ( verbose ) System.out.printf("%04d :: id=%04d -> link=%04d [count=%d, max=%d]\n", n, page.getId(), page.getLink(), page.getCount(), page.getMaxSize());
                RecordBuffer rb = page.getRecordBuffer();
                if ( verbose ) System.out.printf("     :: %d %d\n", rb.getSize(), rb.maxSize() );
                total += rb.size();
                idx = page.getLink();
                n++;
                recordPageMgr.release(page);
            }
        } catch (Exception ex) {
            System.out.println("Exception: "+ex);
        }

        System.out.printf("[Scan Records] Count = %d in %d blocks (avg: %.2f)\n", total, n, ((float)total)/n);
    }

    /*public*/private static void bpt_scan_nodes(BPlusTree bpt, boolean verbose) {
        System.out.print("[Scan Nodes] start\n");
        BPTreeNodeMgr nodeMgr = new BPTreeNodeMgr(bpt, bpt.getNodeManager().getBlockMgr());

        int idx = 0;
        int n = 0;
        int total = 0;
        if ( verbose ) System.out.printf("BPTreeNodeMgr = %s\n", nodeMgr);

        // Blocks in file order
        try {
            while ( idx >= 0 ) {
                if ( verbose ) System.out.printf("idx = %d\n", idx);

                BPTreeNode node = nodeMgr.getRead(idx, 0);
                if ( node == null )
                    break;
                System.out.println(node);

                //            if ( verbose )
                //                System.out.printf("%04d :: id=%04d -> link=%04d [count=%d, max=%d]\n", n, page.getId(), page.getLink(), page.getCount(), page.getMaxSize());
                n++;
                idx ++;
                nodeMgr.release(node);
            }
        } catch (Exception ex) {
            System.out.println("Exception: "+ex);
            ex.printStackTrace();
        }

        System.out.printf("[Scan Nodes] Count = %d in %d blocks (avg: %.2f)\n", total, n, ((float)total)/n);
    }

}
