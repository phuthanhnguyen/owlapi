/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.change;/*
* Copyright (C) 2007, University of Manchester
*
* Modifications to the initial code base are copyright of their
* respective authors, or their employers as appropriate.  Authorship
* of the modifications may be determined from the ChangeLog placed at
* the end of this file.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.

* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.

* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asList;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.RemoveAxiom;

/**
 * Given a set of ontologies, this composite change will convert all property
 * assertion axioms whose subject is a 'punned' individual (i.e. an individual
 * that shares a name with a class), removes these axioms and replaces them with
 * annotations on the class that shares the same name as the individual. For
 * example for a class A and an individual A, the data property assertion
 * hasX(A, "Val") would be converted to an entity annotation on the class A with
 * an annotation URI of "hasX" and a value of "Val".<br>
 * This composite change supports refactoring an ontology where punning was used
 * to simulate annotations on a class rather than using actual annotations on a
 * class.
 * 
 * @author Matthew Horridge, The University Of Manchester, Bio-Health
 *         Informatics Group
 * @since 2.1.0
 */
public class ConvertPropertyAssertionsToAnnotations extends
        AbstractCompositeOntologyChange {

    private static final long serialVersionUID = 40000L;
    @Nonnull
    private final Set<OWLOntology> ontologies;

    /**
     * Instantiates a new convert property assertions to annotations.
     * 
     * @param dataFactory
     *        factory to use
     * @param ontologies
     *        ontologies to change
     */
    public ConvertPropertyAssertionsToAnnotations(
            @Nonnull OWLDataFactory dataFactory,
            @Nonnull Set<OWLOntology> ontologies) {
        super(dataFactory);
        this.ontologies = checkNotNull(ontologies, "ontologies cannot be null");
        generateChanges();
    }

    private Stream<OWLOntology> ontologies() {
        return ontologies.stream();
    }

    /**
     * Gets the punned individuals.
     * 
     * @param individuals
     *        the individuals
     * @return the punned individuals
     */
    @Nonnull
    private Collection<OWLNamedIndividual> getPunnedIndividuals(
            @Nonnull Stream<OWLNamedIndividual> individuals) {
        return asList(individuals.filter(i -> ontologies().anyMatch(
                o -> o.containsClassInSignature(i.getIRI()))));
    }

    private void generateChanges() {
        Stream<OWLNamedIndividual> inds = ontologies().flatMap(
                o -> o.individualsInSignature());
        getPunnedIndividuals(inds).forEach(ind -> convertToAnnotations(ind));
    }

    private void remove(Stream<? extends OWLAxiom> c, OWLOntology o) {
        c.forEach(ax -> addChange(new RemoveAxiom(o, ax)));
    }

    private void remove(@Nonnull OWLDataProperty prop) {
        ontologies().forEach(o -> {
            remove(o.declarationAxioms(prop), o);
            remove(o.axioms(prop), o);
        });
    }

    private void remove(@Nonnull OWLNamedIndividual ind) {
        ontologies().forEach(o -> {
            remove(o.declarationAxioms(ind), o);
            remove(o.classAssertionAxioms(ind), o);
        });
    }

    private void convertToAnnotations(@Nonnull OWLNamedIndividual ind) {
        ontologies.forEach(ont -> {
            ont.dataPropertyAssertionAxioms(ind)
                    .filter(ax -> !ax.getProperty().isAnonymous())
                    .forEach(
                            ax -> {
                                addChange(new RemoveAxiom(ont, ax));
                                addChange(new AddAxiom(ont,
                                        convertToAnnotation(ind, ax)));
                                remove(ax.getProperty().asOWLDataProperty());
                            });
        });
        remove(ind);
    }

    @Nonnull
    private OWLAnnotationAssertionAxiom convertToAnnotation(
            @Nonnull OWLNamedIndividual ind,
            @Nonnull OWLDataPropertyAssertionAxiom ax) {
        OWLAnnotation anno = df.getOWLAnnotation(df.getOWLAnnotationProperty(ax
                .getProperty().asOWLDataProperty()), ax.getObject());
        return df.getOWLAnnotationAssertionAxiom(ind.getIRI(), anno);
    }
}
