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

package org.apache.jena.sparql.resultset;

import java.util.*;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetRewindable;
import org.apache.jena.sparql.util.EqualityTest;
import org.apache.jena.sparql.util.Iso;
import org.apache.jena.sparql.util.NodeIsomorphismMap;
import org.apache.jena.sparql.util.NodeUtils;

/** Comparison of ResultSets.
 *  Note that reading ResultSets is destructive so consider using {@link ResultSetRewindable}
 *  from {@link ResultSetFactory#makeRewindable}
 */

public class ResultSetCompare
{
    /* This is from the DAWG test suite.
     * Result set 1:
     *   ---------------
     *   | x    | y    |
     *   ===============
     *   | _:b0 | _:b1 |
     *   | _:b2 | _:b3 |
     *   | _:b1 | _:b0 |
     *   ---------------
     * Result set 2:
     *   ---------------
     *   | x    | y    |
     *   ===============
     *   | _:b1 | _:b0 |
     *   | _:b3 | _:b2 |
     *   | _:b2 | _:b3 |
     *   ---------------
     */

//    private static String[] rs1$ = {
//        "(resultset (?x ?y)",
//        "   (row (?x _:b0) (?y _:b1))",
//        "   (row (?x _:b2) (?y _:b3))",
//        "   (row (?x _:b1) (?y _:b0))",
//        ")"};
//    private static String[] rs2$ = {
//        "(resultset (?x ?y)",
//        "   (row (?x _:c1) (?y _:c0))",
//        "   (row (?x _:c3) (?y _:c2))",
//        "   (row (?x _:c2) (?y _:c3))",
//        ")"};
//

    // Limitations:
    // This code does not do compare/isomorphism combined with value testing.
    // It drops to graph isomorphism, which is term based.

    /** Compare two result sets for equivalence.  Equivalence means:
     * A row rs1 has one matching row in rs2, and vice versa.
     * A row is only matched once.
     * Rows match if they have the same variables with the same values.
     * bNodes must map to a consistent other bNodes.  Value comparisons of nodes.
     *
     * Destructive - rs1 and rs2 are both read, possibly to exhaustion.
     * @param rs1
     * @param rs2
     * @return true if they are equivalent
     */

    public static boolean equalsByValue(ResultSet rs1, ResultSet rs2) {
        return equalsByValue(RowSet.adapt(rs1), RowSet.adapt(rs2));
    }

    /** See {@link #equalsByValue(ResultSet, ResultSet)} */
    public static boolean equalsByValue(RowSet rs1, RowSet rs2) {
        if ( ! compareHeader(rs1, rs2) ) return false;

        RowSetRewindable rs1a = rs1.rewindable();
        RowSetRewindable rs2a = rs2.rewindable();

        if ( equivalent(convert(rs1a), convert(rs2a), new BNodeIso(NodeUtils.sameValue)) )
            return true;
        rs1a.reset();
        rs2a.reset();
        return isomorphic(rs1a, rs2a);
    }

    public static boolean equalsByTest(Collection<Binding> b1, Collection<Binding> b2, EqualityTest match)
    {
        List<Binding> rows1 = new ArrayList<>(b1);
        List<Binding> rows2 = new ArrayList<>(b2);
        return equivalent(rows1, rows2, match);
    }


    /** compare two result sets for equivalence.  Equivalence means:
     * A row rs1 has one matching row in rs2, and vice versa.
     * A row is only matched once.
     * Rows match if they have the same variables with the same values,
     * bNodes must map to a consistent other bNodes.
     * Term comparisons of nodes.
     *
     * Destructive - rs1 and rs2 are both read, possibly to exhaustion.
     * @param rs1
     * @param rs2
     * @return true if they are equivalent
     */

    public static boolean equalsByTerm(ResultSet rs1, ResultSet rs2) {
        return equalsByTerm(RowSet.adapt(rs1), RowSet.adapt(rs2));
    }

    /** See {@link #equalsByTerm(ResultSet, ResultSet)} */
    public static boolean equalsByTerm(RowSet rs1, RowSet rs2) {
        if ( ! compareHeader(rs1, rs2) )
            return false;

        RowSetRewindable rs1a = rs1.rewindable();
        RowSetRewindable rs2a = rs2.rewindable();
        // Aligned rows
        if ( equivalent(convert(rs1a), convert(rs2a), new BNodeIso(NodeUtils.sameNode)) )
            return true;
        rs1a.reset();
        rs2a.reset();
        return isomorphic(rs1a, rs2a);
    }

    /** Compare two result sets for equivalence.  Equivalence means:
     * Each row in rs1 matches the same index row in rs2.
     * Rows match if they have the same variables with the same values,
     * bNodes must map to a consistent other bNodes.
     * Value comparisons of nodes.
     *
     * Destructive - rs1 and rs2 are both read, possibly to exhaustion.
     * @param rs1
     * @param rs2
     * @return true if they are equivalent
     */
    public static boolean equalsByValueAndOrder(ResultSet rs1, ResultSet rs2) {
        return equalsByValueAndOrder(RowSet.adapt(rs1), RowSet.adapt(rs2));
    }

    /** See {@link #equalsByValueAndOrder(ResultSet, ResultSet)} */
    public static boolean equalsByValueAndOrder(RowSet rs1, RowSet rs2) {
        if ( ! compareHeader(rs1, rs2) )
            return false;
        return equivalentByOrder(convert(rs1) , convert(rs2), new BNodeIso(NodeUtils.sameValue));
    }


    /** compare two result sets for equivalence.  Equivalence means:
     * Each row in rs1 matches the same index row in rs2.
     * Rows match if they have the same variables with the same values,
     * bNodes must map to a consistent other bNodes.
     * RDF term comparisons of nodes.
     *
     * Destructive - rs1 and rs2 are both read, possibly to exhaustion.
     * @param rs1
     * @param rs2
     * @return true if they are equivalent
     */
    public static boolean equalsByTermAndOrder(ResultSet rs1, ResultSet rs2) {
        return equalsByTermAndOrder(RowSet.adapt(rs1), RowSet.adapt(rs2));
    }

    /** See {@link #equalsByTermAndOrder(ResultSet, ResultSet)} */
    public static boolean equalsByTermAndOrder(RowSet rs1, RowSet rs2) {
        if ( ! compareHeader(rs1, rs2) )
            return false;
        return equivalentByOrder(convert(rs1) , convert(rs2), new BNodeIso(NodeUtils.sameNode));
    }

    /** compare two result sets for exact equality equivalence.
     * Exact equalitymeans:
     * Each row in rs1 matches the same index row in rs2.
     * Rows match if they have the same variables with the same values,
     * bNodes must have same labels
     *
     * Destructive - rs1 and rs2 are both read, possibly to exhaustion.
     * @param rs1
     * @param rs2
     * @return true if they are equivalent
     */
    public static boolean equalsExact(ResultSet rs1, ResultSet rs2) {
        return equalsExact(RowSet.adapt(rs1), RowSet.adapt(rs2));
    }

    /** See {@link #equalsExact(ResultSet, ResultSet)} */
    public static boolean equalsExact(RowSet rs1, RowSet rs2) {
        if ( !compareHeader(rs1, rs2) )
            return false;
        return equivalentByOrder(convert(rs1) , convert(rs2), NodeUtils.sameNode);
    }

    /** Compare two result sets for bNode isomorphism equivalence.
     * Only does RDF term comparison.
     */
    public static boolean isomorphic(ResultSet rs1, ResultSet rs2) {
        Model m1 = RDFOutput.encodeAsModel(rs1);
        Model m2 = RDFOutput.encodeAsModel(rs2);
        return m1.isIsomorphicWith(m2);
    }

    /** See {@link #isomorphic(ResultSet, ResultSet)} */
    public static boolean isomorphic(RowSet rs1, RowSet rs2) {
        return isomorphic(ResultSet.adapt(rs1), ResultSet.adapt(rs2));
    }

    /** Compare two bindings, use the node equality test provided */
    static public boolean equal(Binding bind1, Binding bind2, EqualityTest test) {
        if ( bind1 == bind2 )
            return true;
        if ( bind1.size() != bind2.size() )
            return false;
        // They are the same size so containment is enough.
        if ( ! containedIn(bind1, bind2, test) ) return false;
        return true;
    }

    static private boolean compareHeader(RowSet rs1, RowSet rs2) {
        if ( rs1 == null && rs2 == null )
            return true;
        if ( rs1 == null )
            return false;
        if ( rs2 == null )
            return false;
        Set<Var> names1 = Set.copyOf(rs1.getResultVars());
        Set<Var> names2 = Set.copyOf(rs2.getResultVars());
        return names1.equals(names2);
    }

    static private List<Binding> convert(RowSet rs) {
        return Iter.iter(rs).toList();
    }

    static private boolean equivalent(Collection<Binding> rows1, Collection<Binding> rows2, EqualityTest match) {
        if ( rows1.size() != rows2.size() )
            return false;
        for ( Binding row1 : rows1 ) {
            // find in rows2.
            Binding matched = null;
            for ( Binding row2 : rows2 ) {
                // NEED BACKTRACKING
                if ( equal(row1, row2, match) ) {
                    matched = row2;
                    break;
                }
            }

            if ( matched == null )
                return false;
            // Remove matching.
            rows2.remove(matched);
        }
        return true;
    }

    static private boolean equivalentByOrder(List<Binding> rows1, List<Binding> rows2, EqualityTest match) {
        if ( rows1.size() != rows2.size() )
             return false;

        Iterator<Binding> iter1 = rows1.iterator();
        Iterator<Binding> iter2 = rows2.iterator();

        while ( iter1.hasNext() )
        {
            // Does not need backtracking because rows must
            // align and so must variables in a row.
            Binding row1 = iter1.next();
            Binding row2 = iter2.next();
            if ( !equal(row1, row2, match) )
                return false;
        }
        return true;
    }

    // Is bind1 contained in bind2?  For every (var,value) in bind1, is it in bind2?
    // Maybe more in bind2.
    private static boolean containedIn(Binding bind1, Binding bind2, EqualityTest test) {
        // There are about 100 ways to do this!
        Iterator<Var> iter1 =  bind1.vars();

        while(iter1.hasNext()) {
            Var v = iter1.next();
            Node n1 = bind1.get(v);
            Node n2 = bind2.get(v);
            if ( n2 == null )
                // v bound in bind1 and not in bind2.
                return false;
            if ( ! test.equal(n1, n2) )
                return false;
        }
        return true;
    }

    public static class BNodeIso implements EqualityTest
    {
        private NodeIsomorphismMap mapping;
        private EqualityTest literalTest;

        public BNodeIso(EqualityTest literalTest) {
            this.mapping = new NodeIsomorphismMap();
            this.literalTest = literalTest;
        }

        @Override
        public boolean equal(Node n1, Node n2) {
            if ( n1 == null && n2 == null )
                return true;
            if ( n1 == null )
                return false;
            if ( n2 == null )
                return false;

            if ( n1.isURI() && n2.isURI() )
                return n1.equals(n2);

            if ( n1.isLiteral() && n2.isLiteral() )
                return literalTest.equal(n1, n2);

            if ( n1.isBlank() && n2.isBlank() )
                return Iso.nodeIso(n1, n2, mapping);

            if ( n1.isVariable() && n2.isVariable() )
                return mapping.makeIsomorphic(n1, n2);

            if ( n1.isNodeTriple() && n2.isNodeTriple() ) {
                Triple t1 = n1.getTriple();
                Triple t2 = n2.getTriple();
                return  Iso.tripleIso(t1, t2, mapping);
            }

            return false;
        }
    }
}
