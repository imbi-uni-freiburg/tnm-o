# tnm-o

## The TNM Ontology

*tnm-o* is the abbreviation for the **TNM Ontology**. The TNM Ontology represents the TNM Classification of Malignant Tumours (TNM) in description logic (OWL). For a detailed report on *tnm-o* see: https://jbiomedsem.biomedcentral.com/articles/10.1186/s13326-016-0106-9. 

Objective of this project is to represent the complete TNM Classification system in description logic and provide formal transformation rules between versions. Based on this representation, data on clinical and pathological findings can be classified automatically by documentation systems and (partially) transformed between TNM versions.

*tnm-o* consists of a stack of hierarchically organized modules:

 * **BioTop** Toplevel Domain Ontology (https://biotopontology.github.io/) defines the foundational classes and relations for *tnm-o* and thus its underlying ontological framework.
 * **TNM-O**: a hub ontology providing common classes and definitions which are used by all organ-specific modules of *tnm-o*.  TNM-O is the hup in which other modules *can be* loaded depending on the application context.
 * **TNM-Anatomy**: an ontology providing definitions for common anatomical entities
 * **Organ specific modules**: representations of TNM definitions according to the organ-specific structure of the TNM classification system for different versions
 * **Transformation rules**: organ-specific, formal rules for the transformation between different versions


### Organ-specific modules 

| represented organ |  v6  |       v7       |         v8         |
| :---------------- | :--: | :------------: | :----------------: |
| Breast            |      | under revision | under construction |
| Colon/ Rectum     |  X   | under revision | under construction |
| Lung              |  X   | under revision | under construction |
| Pancreas          |      |       X        |         X          |
| Liver (HCC)       |      |                |         X          |
| Stomach           |      |                |         X          |


### Contributors

 * Martin Boeker (University of Freiburg)
 * Peter Bronsert (University of Freiburg)
 * Oliver Brunner (University of Freiburg)
 * Rita Faria
 * Fabio Franca
 * Johannes Herrmann
 * Thomas Maulhardt
 * Stefan Schulz (Medical University of Graz)
 * Susanne Zabka (University of Freiburg)

 This repository is under construction.

 Legacy repository of this project: http://purl.org/tnmo/

## How to use the ontologies for one organ and one version of TNM

Start Protégé with the ontology TNM-O (folder TNMO) and import the ontology TNM-O-Anatomy (folder TNMO) and the organ-specific module of the desired TNM-version (folder TNM/TNM7 or TNM/TNM8). 
Test instances are named [organ]test.owl
Example: to use the TNM-ontology of the organ “liver” in TNM-version 8, use the following files in Protégé:
 * TNM-O.owl                                                         (from folder TNM)
 * Import: TNM-O_Anatomy.owl                                         (from folder TNM)
 * Import: liver(HCC).owl                                            (from folder TNM/TNMO8)
 * If you want to add test data, also import liver(HCC)Test.owl      (from folder TNM/TNMO8)
Note: Ensure that only the necessary ontologies are imported. You may need to remove direct and/or indirect imports.

## How to use the ontologies for the re-classification of pancreas cancer in TNM7 and TNM8: 
(see also: http://ceur-ws.org/Vol-2050/ODLS_paper_3.pdf ) 

### a) Re-classification using the semantic web rules language: 
Start Protégé with the ontology TNM-O (folder TNMO) and import all necessary ontologies:
 * TNM-O.owl                            	      (from folder TNM)
 * Import: TNM-O_Anatomy.owl                          (from folder TNM)
 * Import: pancreas.owl                               (from folder TNM/TNMO7)
 * Import: pancreas:_exocrine.owl                     (from folder TNM/TNMO8)
 * Import: pancreas_neuroendocrine.owl                (from folder TNM/TNMO8)
 * Import either of:
    * pancreas_swrl7to8.owl                           (from folder TNM/TNMOBridge_7_8)
    * pancreas_swrl8eto7.owl                          (from folder TNM/TNMOBridge_7_8)
    * pancreas_swrl8nto7.owl                          (from folder TNM/TNMOBridge_7_8) 
If you want to add test data, also import either of pancreas7to8TEST.owl  

### b) Re-classification using bridging classes:
Start Protégé with the ontology TNM-O (folder TNMO) and import all necessary ontologies:
 * TNM-O.owl                            	      (from folder TNM)
 * Import: TNM-O_Anatomy.owl                          (from folder TNM)
 * Import: pancreas.owl                               (from folder TNM/TNMO7)
 * Import: pancreas_exocrine.owl                      (from folder TNM/TNMO8)
 * Import: pancreas_neuroendocrine.owl                (from folder TNM/TNMO8)
 * Import: pancreasbridge.owl                         (from folder TNM/TNMOBridge_7_8)
This version can only be tested using the Classifier software.

## How to use the classifier software (requires a Java runtime environment)
Note: the classifier software currently only works with the pancreas ontologies including the re-classification from TNM7 to TNM8.
CSV-Files with test data are stored  in folder testdata_pancreas_csv.

In order to download and run a demo of the program, execute the following commands:
``` $ git clone https://github.com/imbi-uni-freiburg/tnm-o.git
    $ cd tnm-o/TNMClassifier
    $ ant demo-TNM8-pancreas
```

You can run the software on the command line or in eclipse with the parameters listed below:
 ``` java -jar TNMClassifier.jar  -v 7 -o pancreas -i ./../PancreasTNM7_TestdataClassifier.csv ```

Parameters for individual pancreas ontologies:
 ``` -v 7 -o pancreas -i ./../testdata_pancreas_csv/PancreasTNM7_TestdataClassifier.csv
  -v 8 -o pancreas_exocrine -i ./../testdata_pancreas_csv/PancreasTNM8exo_TestdataClassifier.csv
  -v 8 -o pancreas_neuroendocrine -i ./../testdata_pancreas_csv/PancreasTNM8neuro_TestdataClassifier.csv ``` 

Parameters for the re-classification using SWRL: 
 ``` -v bridge_7_8 -o pancreas_swrl7to8 -i ./../testdata_pancreas_csv/PancreasTNM_TestdataClassifierSWRL7to8.csv 
  -v bridge_7_8 -o pancreas_swrl8eto7 -i ./../testdata_pancreas_csv/PancreasTNM_TestdataClassifierSWRL8e_7.csv 
  -v bridge_7_8 -o pancreas_swrl8nto7 -i ./../testdata_pancreas_csv/PancreasTNM_TestdataClassifierSWRL8n_7.csv ```

Parameters for the re-classification using bridging classes: 
```  -v bridge_7_8 -o pancreasbridge -i ./../testdata_pancreas_csv/PancreasTNM_TestdataClassifierBridge.csv ```
