package tnmClassifier.bridge78;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import tnmClassifier.BaseClassifier;
import tnmClassifier.ClassifierEnvironment.ChangeMode;
import tnmClassifier.utils.CSVTools;
import tnmClassifier.utils.IDataReader;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 *
 * @author Susanne Zabka Juli-August 2017, updates: Oliver Brunner
 * Use pancreas bridge ontology to classify data as TNM7, TNM8e and TNM8n
 * from CSV-table with column headers as listed below
 * Update Susanne Zabka, 14-Jun-2018, adaptation to new ontology structure (...PancreasStructureClassifiedByMalignancy, 
*  AnatomicalStructureAdjacentToPancreas, new variable and method for tumorOrganExpression, renaming of tumorOfOrgan to organClassified )
 * Update SZ 23-Mar-2019: neuer Ausdruck ...PancreasStructureAssessedForMalignancy"
 */
public class Pancreas extends BaseClassifier {
	private String tumorTypeColumnHeader;
	private String tumorOrganColumnHeader;
	private String tumorTorNorMColumnHeader;
	private String tumorAssessedPrimaryTumorColumnHeader;
	private String tumorEvidencePrimaryTumorColumnHeader;
	private String tumorConfinementColumnHeader;
	private String tumorSizeColumnHeader;
	private String tumorInvasiveInSoftTissueColumnHeader;
	private String tumorInvasiveInTrunkOrMesArtSupColumnHeader;
	private String tumorInvasiveInComHepArtColumnHeader;
	private String tumorInvasiveInAnyVesselColumnHeader;
	private String tumorInvasiveInBileDuctDuodenumColumnHeader;
	private String tumorInvasiveInSerosaOtherColumnHeader;
	private String tumorMetaLKColumnHeader;
	private String tumorDistMetaColumnHeader;
	private String tumorDistMetaOrganColumnHeader;

	private Set<OWLClassExpression> res;
	private Map<String, OWLNamedIndividual> individuals;
	private OWLClass representationUnitTNM7;
	private OWLClass representationUnitTNM8;

	public Pancreas(String inputDataPath) throws FileNotFoundException, IOException {
		super(inputDataPath);
		this.location = "pancreas";
		this.version = "bridge_7_8";

		this.res = new HashSet<>();
		this.individuals = new HashMap<String, OWLNamedIndividual>();

		this.tumorTypeColumnHeader = "Tumortype";
		this.tumorOrganColumnHeader = "OrganClassified";
		this.tumorTorNorMColumnHeader = "T or N or M";
		this.tumorAssessedPrimaryTumorColumnHeader = "NoAssessment _PrimT";
		this.tumorEvidencePrimaryTumorColumnHeader = "NoEvidence _PrimT";
		this.tumorConfinementColumnHeader = "Confinement";
		this.tumorSizeColumnHeader = "Size";
		this.tumorInvasiveInSoftTissueColumnHeader = "InvasiveInPeripancreaticSoftTissue";
		this.tumorInvasiveInTrunkOrMesArtSupColumnHeader = "InvasiveInCeliacTrunkOrSuperiorMesentericArtery";
		this.tumorInvasiveInComHepArtColumnHeader = "InvasiveInCommonHepaticArtery";
		this.tumorInvasiveInAnyVesselColumnHeader = "InvasiveInCeliacTrunkOrSuperiorMesentericArteryOrCommonHepaticArtery";
		this.tumorInvasiveInBileDuctDuodenumColumnHeader = "InvasiveInBileDuctOrDuodeum";
		this.tumorInvasiveInSerosaOtherColumnHeader = "InvasiveInSerosaOrOtherOrgan";
		this.tumorMetaLKColumnHeader = "Meta LymphNodes";
		this.tumorDistMetaColumnHeader = "Distant Meta";
		this.tumorDistMetaOrganColumnHeader = "Dist Meta Org";
	}

	@Override
	public void setUp() throws OWLOntologyCreationException {
		this.env.addTNMOAsBase("TNMO")
				.addOntology("bridge_7_8", "pancreasbridge", "TNMO").addOntology("7", "pancreas", "TNMO")
				.addOntology("8", "pancreas_exocrine", "TNMO").addOntology("8", "pancreas_neuroendocrine", "TNMO");

		this.representationUnitTNM7 = this.env.getDataFactory().getOWLClass(
				IRI.create(getPancreas7Iri() + "RepresentationalUnitInPancreasTNM7ClinicalClassification"));
		this.representationUnitTNM8 = this.env.getDataFactory().getOWLClass(IRI
				.create(getBridgeIri() + "RepresentationalUnitInUndefinedPancreasLocationClinicalTNM8Classification"));
	}

	public Map<String, List<String>>[] run()
			throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		OWLDataFactory datafactory = this.env.getDataFactory();

		OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();
		ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
		OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

		Configuration configuration = new Configuration();
		configuration.ignoreUnsupportedDatatypes = true;

		String[] nextLine;

		int i = 0;
		IDataReader dataReader = new CSVTools(inputDataPath);
		Map<String, List<String>>[] result = new HashMap[dataReader.countLines(inputDataPath)];

		while ((nextLine = dataReader.nextLine()) != null) {
			if (nextLine != null && i >= 0) {
				result[i] = new HashMap<String, List<String>>();
				result[i].put("Pancreas7", new ArrayList<String>());

				System.out.println(
						"----------------- Nr. " + i + " ------------------------------------------------------------");
				System.out.println("Tumorart (erwartet):     " + nextLine[dataReader.getIndex("Tumortype")]);
				System.out.println("TNM7 (erwartet):         " + nextLine[dataReader.getIndex("TNM7")]);
				System.out.println("TNM8 (erwartet):         " + nextLine[dataReader.getIndex("TNM8undef")]);

				this.res.clear();
				this.individuals.clear();

				this.buildClassExpressions(nextLine);
				this.createIndividuals(i);

				this.env.save("TNMO");

				OWLReasoner reasoner = reasonerFactory.createReasoner(this.env.getOntology("TNMO"));
//AAAAAAAAAAAAAAAAA___________****************************______________________--------
				System.out.println("");
				System.out.print("res:        " + res);
				System.out.println("");


				//__________**************************************_________________________-


				System.out.println("run reasoner: ");

				NodeSet<OWLClass> typesSetindividual = reasoner.getTypes(this.individuals.get("individual"), true);

				// TODO: Throw this away
				for (OWLClass k : typesSetindividual.getFlattened()) {
					System.out.print("Individual (ermittelt):  " + k.getIRI().getFragment());
					if (nextLine[dataReader.getIndex("Tumortype")].equals(k.getIRI().getFragment())) {
						System.out.println("   ---KORREKT!---");
					} else {
						System.out.println("");
					}

				}

				NodeSet<OWLClass> typesSet7 = reasoner.getTypes(this.individuals.get("tnm7"), true);
				Set<OWLClass> resultSet7 = typesSet7.getFlattened();
				// TODO: Throw this away as well
				for (OWLClass k : typesSet7.getFlattened()) {
					if (k == representationUnitTNM7) {
						resultSet7.remove(i);
					} else {
						System.out.print("TNM7 (ermittelt):        " + k.getIRI().getFragment());
						if (nextLine[dataReader.getIndex("TNM7")].equals(k.getIRI().getFragment())) {
							System.out.println("   ---KORREKT!---");
						} else {
							System.out.println("");
						}
					}
				}

				if (!(resultSet7.size() == 1) && !(typesSet7.isSingleton())) {
					for (OWLClass k : typesSet7.getFlattened()) {
						if (!reasoner.getSubClasses(k, true).isBottomSingleton()) {
							resultSet7.remove(k);
						}
						if (resultSet7.isEmpty()) {
							resultSet7 = typesSet7.getFlattened();
							NodeSet<OWLClass> checkSuperClasses = reasoner.getSuperClasses(k, false);

							for (OWLClass j : checkSuperClasses.getFlattened()) {
								if (k == j) {
									resultSet7.remove(i);
								}
							}
						}
					}
				}
				NodeSet<OWLClass> typesSet8 = reasoner.getTypes(this.individuals.get("tnm8"), true);
				Set<OWLClass> resultSet8 = typesSet8.getFlattened();
				for (OWLClass k : typesSet8.getFlattened()) {
					if (k == this.representationUnitTNM8) {
						resultSet8.remove(k);
					} else {
						System.out.print("TNM8 (ermittelt):        " + k.getIRI().getFragment());
						if (nextLine[dataReader.getIndex("TNM8undef")].equals(k.getIRI().getFragment())) {
							System.out.println("   ---KORREKT!---");
						} else {
							System.out.println("");
						}
					}
				}
				reasoner.dispose();
			}
			this.env.tearDown(ChangeMode.TEMPORARY);
			i++;
		}
		return new Map[0];
	}

	private void createIndividuals(int id) throws OWLOntologyStorageException {
		OWLDataFactory datafactory = this.env.getDataFactory();

		this.individuals.put("individual",
				createAndReturnIndividual("testTumor_" + String.valueOf(id), datafactory, res, "TNMO"));
		this.individuals.put("tnm7", addTumorIsRepresentedbyTNMversion(representationUnitTNM7,
				this.individuals.get("individual"), datafactory, id, "7"));
		this.individuals.put("tnm8", addTumorIsRepresentedbyTNMversion(representationUnitTNM8,
				this.individuals.get("individual"), datafactory, id, "8"));
	}

	private void buildClassExpressions(String[] nextLine) {
		String torNorM = nextLine[dataReader.getIndex(tumorTorNorMColumnHeader)];
		switch (torNorM) {
		case "T":
			this.buildTClassExpressions(nextLine);
			break;
		case "N":
			this.buildNClassExpression(nextLine);
			break;
		case "M":
			this.buildMClassExpression(nextLine);
			break;
		}
	}

	private void buildMClassExpression(String[] nextLine) {
		String organClassified = nextLine[dataReader.getIndex(tumorOrganColumnHeader)];
		String ontologyID = getOntologyID(organClassified);
		String tumorOrganExpression = getTumorOrganExpression(organClassified);
		String distMeta = nextLine[dataReader.getIndex(tumorDistMetaColumnHeader)];
		String distMetaOrg = nextLine[dataReader.getIndex(tumorDistMetaOrganColumnHeader)];

		OWLClassExpression distantMetaForOrgan = getTumorAggregatMetaOrganPart(this.env.getDataFactory(), organClassified);
		res.add(distantMetaForOrgan);
		if (distMeta.equals("no"))
			res.add(this.notHasPart("DistantMetastasisOf" + tumorOrganExpression, ontologyID));
		if (distMeta.equals("yes")) {
			if (distMetaOrg.equals("") || distMetaOrg.equals(null)) {
				res.add(this.hasPart("DistantMetastasisOf" + tumorOrganExpression, ontologyID));
			} else {
				if (distMetaOrg.equals("Liver")) {
					res.add(this.hasPart("LiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
					res.add(this.notHasPart("OtherThanLiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
				}
				if (distMetaOrg.equals("OtherNotLiver")) {
					res.add(this.notHasPart("LiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
					res.add(this.hasPart("OtherThanLiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
				}
				if (distMetaOrg.equals("LiverAndOther")) {
					res.add(this.hasPart("LiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
					res.add(this.hasPart("OtherThanLiverMetastasisOfNeuroendocrinePancreasTumor", ontologyID));
				}
			}
		}
	}

	private void buildNClassExpression(String[] nextLine) {
		String organClassified = nextLine[dataReader.getIndex(tumorOrganColumnHeader)];
		String tumorOrganExpression = getTumorOrganExpression(organClassified);
		String ontologyID = getOntologyID(organClassified);
		String metaLK = nextLine[dataReader.getIndex(tumorMetaLKColumnHeader)];

		OWLClassExpression metaslymphnodeForOrgan = getTumorAggregatLKOrganPart(this.env.getDataFactory(),
				organClassified);
		res.add(metaslymphnodeForOrgan);
		if (metaLK.equals("Notassessed")) {
			res.add(this.notAssessed());
		} else {
			if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy"))
				res.add(this.notNotAssessed());
			if (metaLK.equals("0")) {
				res.add(this.notHasPart("MetastaticRegionalLymphNodeOf" + tumorOrganExpression, ontologyID));
			} else {
				if (metaLK.equals(">0")) {
					res.add(hasPart("MetastaticRegionalLymphNodeOf" + tumorOrganExpression, ontologyID));
				}
				if (metaLK.equals("1<3")) {
					res.add(this.hasPartWithQuality("MetastaticRegionalLymphNodeOf" + tumorOrganExpression, "Cardinality1to3",
							ontologyID));
				}
				if (metaLK.equals(">4")) {
					res.add(this.hasPartWithQuality("MetastaticRegionalLymphNodeOf" + tumorOrganExpression,
							"Cardinality4orMore", ontologyID));
				}
			}
		}
	}

	private void buildTClassExpressions(String[] nextLine) {
		String organClassified = nextLine[dataReader.getIndex(tumorOrganColumnHeader)];
		String organ = getOrgan(organClassified);
		String noEvidence = nextLine[dataReader.getIndex(tumorEvidencePrimaryTumorColumnHeader)];
		String confinement = nextLine[dataReader.getIndex(tumorConfinementColumnHeader)];
		String size = nextLine[dataReader.getIndex(tumorSizeColumnHeader)];
		String softTissue = nextLine[dataReader.getIndex(tumorInvasiveInSoftTissueColumnHeader)];
		String trunkMesArtSup = nextLine[dataReader.getIndex(tumorInvasiveInTrunkOrMesArtSupColumnHeader)];
		String comHepArt = nextLine[dataReader.getIndex(tumorInvasiveInComHepArtColumnHeader)];
		String bileDuodenum = nextLine[dataReader.getIndex(tumorInvasiveInBileDuctDuodenumColumnHeader)];
		String serosaOther = nextLine[dataReader.getIndex(tumorInvasiveInSerosaOtherColumnHeader)];
		String vessels = nextLine[dataReader.getIndex(tumorInvasiveInAnyVesselColumnHeader)];

		OWLClassExpression organPart = getOrganPart(this.env.getDataFactory(),
				nextLine[dataReader.getIndex(tumorOrganColumnHeader)]);
		res.add(organPart);
		if (nextLine[dataReader.getIndex(tumorAssessedPrimaryTumorColumnHeader)].equals("NoAssessment"))
			res.add(this.notAssessed());
		if (noEvidence.equals("NoEvidence"))
			res.add(this.addQuality(noEvidence));
		if (!confinement.equals(""))
			res.add(this.addQuality(confinement));
		if (!size.equals(""))
			res.add(this.addQuality(size));
		// invasive in Organs
		List<String> invasiveInOrganList = new ArrayList<String>();
		if (softTissue.equals("yes")) {
			invasiveInOrganList.add("PeripancreaticSoftTissue");
			res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
		} else {
			if (softTissue.equals("unknown")) {
				invasiveInOrganList.add(organ);
				invasiveInOrganList.add("PeripancreaticSoftTissue");
				res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
			}
		}
		if (trunkMesArtSup.equals("yes")) {
			invasiveInOrganList.add("CeliacTrunk");
			invasiveInOrganList.add("SuperiorMesentericArtery");
			res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
		}
		if (trunkMesArtSup.equals("no")) {
			invasiveInOrganList.add("CeliacTrunk");
			invasiveInOrganList.add("SuperiorMesentericArtery");
			res.add(this.notHasPartIsIncludedIn(invasiveInOrganList));
		}
		if (comHepArt.equals("yes")) {
			invasiveInOrganList.add("CommonHepaticArtery");
			res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
		}
		if (vessels.equals("yes")) {
			invasiveInOrganList.add("CeliacTrunk");
			invasiveInOrganList.add("SuperiorMesentericArtery");
			invasiveInOrganList.add("CommonHepaticArtery");
			res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
		}
		if (bileDuodenum.equals("yes")) {
			invasiveInOrganList.add("BileDuct");
			invasiveInOrganList.add("Duodenum");
			res.add(this.hasPartIsIncludedIn(invasiveInOrganList));
		}
		if (serosaOther.equals("yes")) {
			List<String> possibleOrgans = new ArrayList<String>();
			possibleOrgans.add("Serosa");

			List<String> organlistExcludingOrgans = new ArrayList<String>();
			organlistExcludingOrgans.add("AnatomicalStructureAdjacentToPancreas");

			List<String> andNotorganlist = new ArrayList<String>();
			andNotorganlist.add("BileDuct");
			andNotorganlist.add("Duodenum");
			andNotorganlist.add("PeripancreaticSoftTissue");

			res.add(this.hasPartIncludedInSpecialOrganListWithExceptions(possibleOrgans, organlistExcludingOrgans,
					andNotorganlist));
		}
	}

	public String getOrgan(String organClassified) {
		String organ = "Pancreas";
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
			organ = "Pancreas";
		else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
			organ = "ExocrinePancreas";
		else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")|| organClassified.equals("NeuroendocrinePancreasStructureNotAssessedForMalignancy"))
			organ = "SetOfPancreaticIslets";
		return organ;
	}
	
	public String getTumorOrganExpression(String organClassified) {
		String tumorOrganExpression  = " ";
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
			tumorOrganExpression = "PancreasTumor";
		else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
			tumorOrganExpression = "ExocrinePancreasTumor";
		else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")|| organClassified.equals("NeuroendocrinePancreasStructureNotAssessedForMalignancy"))
			tumorOrganExpression = "NeuroendocrinePancreasTumor";
		return tumorOrganExpression;
	}
	

	public String getOntologyID(String organClassified) {
		String ontologyID = "main";
		// if (organClassified.equals("PancreasTumor"))
		// ontologyID = "Pancreas7";
		// else if (organClassified.equals("ExocrinePancreasTumor"))
		// ontologyID = "Pancreas8e";
		// else if (organClassified.equals("NeuroendocrinePancreasTumor"))
		// ontologyID = "Pancreas8n";
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
			ontologyID = "pancreas7";
		else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
			ontologyID = "pancreas_exocrine_8";
		else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")|| organClassified.equals("NeuroendocrinePancreasStructureNotAssessedForMalignancy"))
			ontologyID = "pancreas_neuroendocrine_8";
		return ontologyID;
	}
//neuer Versuch
	@Override
	protected OWLClassExpression hasPart(String part, String ontologyId) {
		OWLDataFactory factory = this.env.getDataFactory();
		OWLObjectProperty hasPart = factory.getOWLObjectProperty(IRI.create(this.env.getBioTopLight2Iri() + "hasPart"));
		OWLClassExpression partOwlClass = null;
		if (ontologyId.equals("pancreas7"))
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas7Iri() + part));
		else if (ontologyId.equals("pancreas_exocrine_8")) {
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas8exoIri() + part));
		} else if (ontologyId.equals("pancreas_neuroendocrine_8")) {
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas8neIri() + part));
		}
		OWLClassExpression andhasPartsome = factory.getOWLObjectSomeValuesFrom(hasPart, partOwlClass);
		return andhasPartsome;
	}
	//neuer Versuch
		@Override
	protected OWLClassExpression hasPartWithQuality(String part, String regionValue, String ontologyId) { 
		OWLDataFactory factory = this.env.getDataFactory();
		OWLObjectProperty isBearerOf = factory
				.getOWLObjectProperty(IRI.create(this.env.getBioTopLight2Iri() + "isBearerOf"));
		OWLObjectProperty projectsOnto = factory
				.getOWLObjectProperty(IRI.create(this.env.getBioTopLight2Iri() + "projectsOnto"));
		// SomeQuality
		OWLClassExpression TumorQualityValuesRegion = factory
				.getOWLClass(IRI.create(this.env.getTNMOIri() + regionValue));
		OWLClassExpression onlyTumorQuality = factory.getOWLObjectAllValuesFrom(projectsOnto, TumorQualityValuesRegion);
		// val.add(onlyTumorQuality);
		OWLClassExpression qualityPart = factory.getOWLObjectSomeValuesFrom(isBearerOf, onlyTumorQuality);

		OWLObjectProperty hasPart = factory
				.getOWLObjectProperty(IRI.create(this.env.getBioTopLight2Iri() + "hasPart"));
		OWLClassExpression partOwlClass = null;
		if (ontologyId.equals("pancreas7"))
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas7Iri() + part));
		else if (ontologyId.equals("pancreas_exocrine_8")) {
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas8exoIri() + part));
		} else if (ontologyId.equals("pancreas_neuroendocrine_8")) {
			partOwlClass = factory.getOWLClass(IRI.create(getPancreas8neIri() + part));
		}

		OWLClassExpression partwithquality = factory.getOWLObjectIntersectionOf(partOwlClass,qualityPart);
		OWLClassExpression andhasParts = factory.getOWLObjectSomeValuesFrom(hasPart, partwithquality);
		return andhasParts;
	}


	public OWLClassExpression getOrganPart(OWLDataFactory datafactory, String organClassified) {
		OWLClassExpression organpart = null;
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
			organpart = datafactory.getOWLClass(IRI.create(getPancreas7Iri() + organClassified));

		else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
			organpart = datafactory.getOWLClass(IRI.create(getPancreas8exoIri() + organClassified));

		else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")|| organClassified.equals("NeuroendocrinePancreasStructureNotAssessedForMalignancy"))
			organpart = datafactory.getOWLClass(IRI.create(getPancreas8neIri() + organClassified));

		return organpart;
	}

	public OWLClassExpression getTumorAggregatLKOrganPart(OWLDataFactory datafactory, String organClassified) {
		OWLClassExpression tumorAggLK = null;
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
	{
	tumorAggLK = datafactory.getOWLClass(
					IRI.create(getPancreas7Iri() + "PancreasTumorAggregateAsRelatedToMetastaticRegionalLymphNodes"));
		} 
			else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
{			tumorAggLK = datafactory.getOWLClass(IRI.create(
					getPancreas8exoIri() + "ExocrinePancreasTumorAggregateAsRelatedToMetastaticRegionalLymphNodes"));

		} 		else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")|| organClassified.equals("NeuroendocrinePancreasStructureNotAssessedForMalignancy"))
 {
			tumorAggLK = datafactory.getOWLClass(IRI.create(getPancreas8neIri()
					+ "NeuroendocrinePancreasTumorAggregateAsRelatedToMetastaticRegionalLymphNodes"));
		}
		return tumorAggLK;
	}

	public OWLClassExpression getTumorAggregatMetaOrganPart(OWLDataFactory datafactory, String organClassified) {
		OWLClassExpression tumorAggMeta = null;
		if (organClassified.equals("PancreasStructureAssessedForMalignancy") || organClassified.equals("PancreasStructureNotAssessedForMalignancy"))
 {
			tumorAggMeta = datafactory
					.getOWLClass(IRI.create(getPancreas7Iri() + "PancreasTumorAggregateAsRelatedToDistantMetastasis"));
		} 		else if (organClassified.equals("ExocrinePancreasStructureAssessedForMalignancy") || organClassified.equals("ExocrinePancreasStructureNotAssessedForMalignancy"))
 {
			tumorAggMeta = datafactory.getOWLClass(
					IRI.create(getPancreas8exoIri() + "ExocrinePancreasTumorAggregateAsRelatedToDistantMetastasis"));

		} else if (organClassified.equals("NeuroendocrinePancreasStructureAssessedForMalignancy")) {
			tumorAggMeta = datafactory.getOWLClass(IRI
					.create(getPancreas8neIri() + "NeuroendocrinePancreasTumorAggregateAsRelatedToDistantMetastasis"));
		}
		return tumorAggMeta;
	}

	public OWLNamedIndividual addTumorIsRepresentedbyTNMversion(OWLClass representationUnitTNM,
			OWLIndividual individual, OWLDataFactory datafactory, int i, String tnmversion) {
		OWLNamedIndividual tnm = datafactory
				.getOWLNamedIndividual(IRI.create(getBridgeIri() + i + "_TNM" + tnmversion));
		OWLObjectProperty isRepresentedBy = datafactory
				.getOWLObjectProperty(IRI.create(this.env.getBioTopLight2Iri() + "isRepresentedBy"));
		OWLObjectPropertyAssertionAxiom isRepresentedByTNMproperty = datafactory
				.getOWLObjectPropertyAssertionAxiom(isRepresentedBy, individual, tnm);
		OWLClassAssertionAxiom addTNMRepresentationUnit = datafactory.getOWLClassAssertionAxiom(representationUnitTNM,
				tnm);

		this.env.addAxiom("TNMO", addTNMRepresentationUnit, ChangeMode.TEMPORARY).addAxiom("TNMO",
				isRepresentedByTNMproperty, ChangeMode.TEMPORARY);
		return tnm;
	}

	public String getBridgeIri() {
		return "http://purl.org/tnmo/Bridge2_Pancreas7to8.owl".concat("#");
	}

	public String getPancreas7Iri() {
		return "http://purl.org/tnmo/TNM-O_Pancreas_7.owl".concat("#");
	}

	public String getPancreas8exoIri() {
		return "http://purl.org/tnmo/TNM-O_Pancreas_8exocrine.owl".concat("#");
	}

	public String getPancreas8neIri() {
		return "http://purl.org/tnmo/TNM-O_Pancreas_8neuroendocrine.owl".concat("#");
	}
}