/*

Copyright 2012 Daniel Gonzalez Peña, Osvaldo Graña


This file is part of the bicycle Project. 

bicycle Project is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

bicycle Project is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser Public License for more details.

You should have received a copy of the GNU Lesser Public License
along with bicycle Project.  If not, see <http://www.gnu.org/licenses/>.
*/

package es.cnio.bioinfo.bicycle.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import es.cnio.bioinfo.bicycle.Project;
import es.cnio.bioinfo.bicycle.Reference;
import es.cnio.bioinfo.bicycle.Sample;
import es.cnio.bioinfo.bicycle.operations.BowtieAlignment;
import es.cnio.bioinfo.bicycle.operations.BowtieAlignment.Quals;
import es.cnio.bioinfo.bicycle.operations.BowtieAlignment.Strand;
import es.cnio.bioinfo.bicycle.operations.MethylationAnalysis;
import es.cnio.bioinfo.bicycle.operations.ReferenceBisulfitation;
import es.cnio.bioinfo.bicycle.operations.ReferenceBisulfitation.Replacement;
import es.cnio.bioinfo.bicycle.operations.SampleBisulfitation;

public class PairedEndAnalysisTest {

	
	
	private Project prepareProject() throws IOException {
		
		File tempDir = Utils.generateTempDirName("newproject");		
		Project p = Project.buildNewProject(
				tempDir,
				new File(Utils.getReferenceDirectory()), 
				new File(Utils.getPairedEndReadsDirectory()), 
				new File(Utils.getBowtiePath()),
				new File(Utils.getSamtoolsPath()), true, true, "-1.fastq");
		
		ReferenceBisulfitation rb = new ReferenceBisulfitation(p);
		BowtieAlignment ba = new BowtieAlignment(p);
		
		for (Reference ref : p.getReferences()){				
			rb.computeReferenceBisulfitation(Replacement.CT, ref, true);
			rb.computeReferenceBisulfitation(Replacement.GA, ref, true);
			ba.buildBowtieIndex(ref);
		}
		for (Sample sample : p.getSamples()){
			/*SampleBisulfitation sb = new SampleBisulfitation(sample);
			sb.computeSampleBisulfitation(true);*/
			for (Reference reference : p.getReferences()){
				ba.performBowtieAlignment(sample, reference, false, 4, 140, 20, 0, 64, Quals.BEFORE_1_3);
			}
		}
			
		return p;		
	}
	
	@Test
	public void analysisMultithread() throws IOException, InterruptedException{
		Project project = prepareProject();
		try{
			
			MethylationAnalysis ma = new MethylationAnalysis(project);

			List<File> bedFiles = Arrays.asList(new File(Utils.getBedsDirectory()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.endsWith(".bed");
				}
			}));
			
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithFixedErrorRate(
							reference, 
							sample, 
							false, 
							4, 
							true, 
							false,
							false,
							true,
							1,
							0.01, 
							1,
							bedFiles, 
							0.001, 0.001);
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					System.err.println("===========================");

					System.err.println("====METHYLATION-CRICK=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)));
					System.err.println("===========================");
					
					
					System.err.println("=====METHYLCYTOSINES=======");
					System.err.println(Utils.readFile(ma.getMethylcytosinesFile(reference, sample)));
					System.err.println(Utils.readFile(ma.getMethylcytosinesVCFFile(reference, sample)));
					System.err.println("===========================");
					
					System.err.println("==========SUMMARY==========");
					System.err.println(Utils.readFile(ma.getSummaryFile(reference, sample)));
					System.err.println("===========================");
					
				}
			}
		} finally {
			Utils.deleteDirOnJVMExit(project.getProjectDirectory());
		}
	}

}
