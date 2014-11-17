package org.semanticweb.owlapi.profiles.test;

import static org.junit.Assert.*;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asSet;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;

@SuppressWarnings("javadoc")
public class ProfileBase {

    @Nonnull
    protected String example = "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";
    @Nonnull
    protected String rdf = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" ";
    @Nonnull
    protected String head = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" ";
    @Nonnull
    protected String head2 = "<rdf:RDF xml:base=\"urn:test\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\">";
    @Nonnull
    protected String head3 = "<rdf:RDF xml:base=\"urn:test\" xmlns=\"urn:test#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">";

    private static OWLProfileReport el(OWLOntology in) {
        return new OWL2ELProfile().checkOntology(in);
    }

    private static OWLProfileReport ql(OWLOntology in) {
        return new OWL2QLProfile().checkOntology(in);
    }

    private static OWLProfileReport rl(OWLOntology in) {
        return new OWL2RLProfile().checkOntology(in);
    }

    private static OWLProfileReport dl(OWLOntology in) {
        return new OWL2DLProfile().checkOntology(in);
    }

    boolean in(@Nonnull OWLProfile p, @Nonnull String in) {
        return p.checkOntology(o(in)).isInProfile();
    }

    @Nonnull
    OWLOntology o(@Nonnull String in) {
        try {
            return OWLManager.createOWLOntologyManager()
                    .loadOntologyFromOntologyDocument(
                            new StringDocumentSource(in));
        } catch (OWLOntologyCreationException e) {
            throw new OWLRuntimeException(e);
        }
    }

    void compareOntologies(@Nonnull String in1, @Nonnull String in2) {
        OWLOntology o1 = o(in1);
        OWLOntology o2 = o(in2);
        assertEquals(asSet(o1.axioms()), asSet(o2.axioms()));
    }

    protected void test(@Nonnull String in, boolean el, boolean ql, boolean rl,
            boolean dl) {
        OWLOntology o = o(in);
        assertTrue("empty ontology", o.axioms().count() > 0);
        OWLProfileReport elReport = el(o);
        assertEquals(elReport.toString(), el, elReport.isInProfile());
        OWLProfileReport qlReport = ql(o);
        assertEquals(qlReport.toString(), ql, qlReport.isInProfile());
        OWLProfileReport rlReport = rl(o);
        assertEquals(rlReport.toString(), rl, rlReport.isInProfile());
        OWLProfileReport dlReport = dl(o);
        assertEquals(dlReport.toString(), dl, dlReport.isInProfile());
    }
}
