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

public class AnalysisTest {

	
	
	private Project prepareProject() throws IOException {
		
		File tempDir = Utils.generateTempDirName("newproject");		
		System.err.println("CREATED PROJECT IN "+tempDir);
		Project p = Project.buildNewProject(
				tempDir,
				new File(Utils.getReferenceDirectory()), 
				new File(Utils.getReadsDirectory()), 
				new File(Utils.getBowtiePath()),
				new File(Utils.getSamtoolsPath()),
				true);
		
		ReferenceBisulfitation rb = new ReferenceBisulfitation(p);
		BowtieAlignment ba = new BowtieAlignment(p);
		
		for (Reference ref : p.getReferences()){				
			rb.computeReferenceBisulfitation(Replacement.CT, ref, true);
			rb.computeReferenceBisulfitation(Replacement.GA, ref, true);
			ba.buildBowtieIndex(ref);
		}
		for (Sample sample : p.getSamples()){
			SampleBisulfitation sb = new SampleBisulfitation(sample);
			sb.computeSampleBisulfitation(true);
			for (Reference reference : p.getReferences()){
				ba.performBowtieAlignment(sample, reference, 4, 140, 20, 0, 64, Quals.BEFORE_1_3);
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
			
			System.out.println("beds2: "+bedFiles);
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithErrorFromControlGenome(
							reference, 
							sample, 
							true, 
							4, 
							true, 
							true,
							false,
							true,
							1,
							0.01, 
							4,
							bedFiles, 
							"control");
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t6\tWATSON\tCG")!=-1);
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t14\tWATSON\tCHG")!=-1);
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
		}finally{
			Utils.deleteDirOnJVMExit(project.getProjectDirectory());
		}
		
	}
	
	@Test
	public void analysisTrimAndBad() throws IOException, InterruptedException{
		Project project = prepareProject();
		try{
			
			MethylationAnalysis ma = new MethylationAnalysis(project);

			List<File> bedFiles = Arrays.asList(new File(Utils.getBedsDirectory()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg0.getName().endsWith(".bed");
				}
			}));
			
			
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithErrorFromControlGenome(
							reference, 
							sample, 
							true, //trim
							4, 
							true, //ambiguous
							true, //bad
							false,
							true,
							1,
							0.01, 
							1, 
							bedFiles, 
							"control");
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					//trim, this line must have depth 4, not 5 due to trimming...
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t59\tWATSON\tCG\t4\t4\t4\t1.0\tCCCC")!=-1);
					System.err.println("===========================");

					System.err.println("====METHYLATION-CRICK=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)));
					//trim test, the following line must be missing
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)).indexOf("chr10\t16\tCRICK\tCHG\t1\t1\t1\t1.0\tG\t0.0\tfalse\tfalse")==-1);
					System.err.println("===========================");
					
					
					System.err.println("=====METHYLCYTOSINES=======");
					System.err.println(Utils.readFile(ma.getMethylcytosinesFile(reference, sample)));
					System.err.println("===========================");
					
					System.err.println("==========SUMMARY==========");
					System.err.println(Utils.readFile(ma.getSummaryFile(reference, sample)));
					System.err.println("===========================");
					
				}
			}
		}finally{
			System.err.println(project.getProjectDirectory());
			Utils.deleteDirOnJVMExit(project.getProjectDirectory());
		}
	}
	
	
	
	@Test
	public void analysisSingleThread() throws IOException, InterruptedException{
		Project project = prepareProject();
		try{
			
			MethylationAnalysis ma = new MethylationAnalysis(project);

			List<File> bedFiles = Arrays.asList(new File(Utils.getBedsDirectory()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg0.getName().endsWith(".bed");
				}
			}));
			
			
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithErrorFromControlGenome(
							reference, 
							sample, 
							false, //notrim
							4, 
							true, 
							true,
							false,
							true,
							1,
							0.01, 
							1, 
							bedFiles, 
							"control");
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t6\tWATSON\tCG")!=-1);
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t14\tWATSON\tCHG")!=-1);
					System.err.println("===========================");

					System.err.println("====METHYLATION-CRICK=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)));
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)).indexOf("chr10\t36\tCRICK\tCHH\t1\t1\t0\t0.0\tA\t1.0\tfalse\tfalse")!=-1);
					System.err.println("===========================");
					
					
					System.err.println("=====METHYLCYTOSINES=======");
					System.err.println(Utils.readFile(ma.getMethylcytosinesFile(reference, sample)));
					System.err.println("===========================");
					
					System.err.println("==========SUMMARY==========");
					System.err.println(Utils.readFile(ma.getSummaryFile(reference, sample)));
					System.err.println("===========================");
					
				}
			}
		}finally{
			//Utils.deleteDirOnJVMExit(project.getProjectDirectory());
		}
		
	}
	
	@Test
	public void testDepth() throws IOException, InterruptedException{
		Project project = prepareProject();
		try{
			
			MethylationAnalysis ma = new MethylationAnalysis(project);

			List<File> bedFiles = Arrays.asList(new File(Utils.getBedsDirectory()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg0.getName().endsWith(".bed");
				}
			}));
			
			int mindepth=2;
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithErrorFromControlGenome(
							reference, 
							sample, 
							true, 
							4, 
							true, 
							true,
							false,
							true,
							mindepth,
							0.01, 
							4, 
							bedFiles, 
							"control");
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					
					System.err.println("===========================");

					System.err.println("====METHYLATION-CRICK=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)));
					System.err.println("===========================");
					
					
					System.err.println("=====METHYLCYTOSINES=======");
					System.err.println(Utils.readFile(ma.getMethylcytosinesFile(reference, sample)));
					String[] lines = Utils.readFile(ma.getMethylcytosinesFile(reference, sample)).split("\n");
					for (String line: lines){
						if (!line.startsWith("#") && line.length()>0)assertTrue(Integer.parseInt(line.split("\t")[5])>=mindepth);
					}
					
					System.err.println("===========================");
					
					System.err.println("==========SUMMARY==========");
					System.err.println(Utils.readFile(ma.getSummaryFile(reference, sample)));
					System.err.println("===========================");
					
				}
			}
		}finally{
			Utils.deleteDirOnJVMExit(project.getProjectDirectory());
		}
		
	}

	@Test
	public void testRemoveClonal() throws IOException, InterruptedException{
		Project project = prepareProject();
		try{
			
			MethylationAnalysis ma = new MethylationAnalysis(project);

			List<File> bedFiles = Arrays.asList(new File(Utils.getBedsDirectory()).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg0.getName().endsWith(".bed");
				}
			}));
			
			int mindepth=2;
			for (Sample sample: project.getSamples()){
				for (Reference reference : project.getReferences()){
					
					ma.analyzeWithErrorFromControlGenome(
							reference, 
							sample, 
							false, 
							4, 
							false, 
							false,
							true, //remove clonal
							true,
							mindepth,
							0.01, 
							4, 
							bedFiles, 
							"control");
					assertTrue(ma.getSummaryFile(reference, sample).exists());
					
					System.err.println("====METHYLATION-WATSON=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)));
					assertTrue(Utils.readFile(ma.getMethylationFile(Strand.WATSON,reference, sample)).indexOf("chr10\t59\tWATSON\tCG\t5\t5\t5\t1.0\tCCCCC")!=-1);
					
					System.err.println("===========================");

					System.err.println("====METHYLATION-CRICK=====");
					System.err.println(Utils.readFile(ma.getMethylationFile(Strand.CRICK,reference, sample)));
					System.err.println("===========================");
					
					
					System.err.println("=====METHYLCYTOSINES=======");
					System.err.println(Utils.readFile(ma.getMethylcytosinesFile(reference, sample)));
					String[] lines = Utils.readFile(ma.getMethylcytosinesFile(reference, sample)).split("\n");
					for (String line: lines){
						if (!line.startsWith("#") && line.length()>0)assertTrue(Integer.parseInt(line.split("\t")[5])>=mindepth);
					}
					
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
