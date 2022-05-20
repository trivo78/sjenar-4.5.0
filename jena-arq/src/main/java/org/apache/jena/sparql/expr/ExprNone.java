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

package org.apache.jena.sparql.expr;

import org.apache.jena.atlas.lib.InternalErrorException ;
import org.apache.jena.sparql.engine.binding.Binding ;
import org.apache.jena.sparql.function.FunctionEnv ;
import org.apache.jena.sparql.graph.NodeTransform ;

/** Marker, used in place of a null.
 *  This may be tested for using {@code ==} */

//public /*package*/ class ExprNone extends ExprVar { // extends ExprNode {
//    
//    /*package*/ static Expr NONE0 = new ExprNone() ; 
//    
//    private ExprNone() { super("") ; }
//}

public class ExprNone extends ExprNode {
    
    /*package*/ static Expr NONE0 = new ExprNone() ;
    private ExprNone() {}
    
    @Override public void visit(ExprVisitor visitor) { visitor.visit(this); }

    @Override public NodeValue eval(Binding binding, FunctionEnv env) {
        throw new InternalErrorException("Attempt to eval ExprNone") ;
    }

    @Override
    public int hashCode() {
        return -999999 ;
    }

    @Override
    public boolean equals(Expr other, boolean bySyntax) {
        return other == this ;
    }

    @Override
    public Expr copySubstitute(Binding binding) {
        return this ;
    }

    @Override
    public Expr applyNodeTransform(NodeTransform transform) {
        return this ;
    }
}
