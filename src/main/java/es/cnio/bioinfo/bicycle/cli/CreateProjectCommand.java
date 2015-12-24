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

package es.cnio.bioinfo.bicycle.cli;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import es.cnio.bioinfo.bicycle.Project;

public class CreateProjectCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(CreateProjectCommand.class.getName());
	@Override
	public String getName() {
		return "create-project";
	}

	@Override
	public String getDescription() {
		return "Creates a new directory where working data and results will be stored";
	}

	@Override
	public List<Option> createOptions() {
		List<Option> toret = new LinkedList<Option>();
		
		toret.add(new Option("project-directory", "p", "directory where files will be stored", false, true));
		toret.add(new Option("reference-directory", "r", "directory with reference genomes (fasta files)", false, true));
		toret.add(new Option("reads-directory", "f", "directory with reads samples (directories with fastq files). One directory per sample", false, true));
		toret.add(new Option("bowtie-directory", "b", "directory where bowtie aligner is installed", false, true));
		toret.add(new Option("samtools-directory", "s", "directory where samtools are installed", false, true));
		toret.add(new Option("paired-mate1-regexp", "m", "Enable paired-end mode. The value is a regular expression which only can be found inside the mate 1 fastq file names. For example: _1.fastq", true, true));
		
		return toret;
		
	}

	@Override
	public void execute(CLIApplication app, Map<Option, String> parameters) throws IOException {
		logger.info("Running "+this.getName());
		
		File projectDirectory = new File(parameters.get(findOption("p")));
		File referenceDirectory = new File(parameters.get(findOption("r")));
		File readsDirectory = new File(parameters.get(findOption("f")));
		File bowtieDirectory = new File(parameters.get(findOption("b")));
		File samtoolsDirectory = new File(parameters.get(findOption("s")));
		String mate1Regexp = parameters.get(findOption("m"));
		
		Project project = null;
		if (mate1Regexp!=null){
			project = Project.buildNewProject(projectDirectory, referenceDirectory, readsDirectory, bowtieDirectory, samtoolsDirectory, true, mate1Regexp);
				
		}else{
			project = Project.buildNewProject(projectDirectory, referenceDirectory, readsDirectory, bowtieDirectory, samtoolsDirectory);
		}
		ProjectCommand.writeExecutionLog(app, project);
	}

}