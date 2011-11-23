package com.github.jlgrock.javascriptframework.closurecompiler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.github.jlgrock.javascriptframework.mavenutils.io.DirectoryIO;

/**
 * Represents a dependency that is used to build and walk a tree. This is a
 * direct port from the google python script.
 * 
 */
public final class CalcDeps {

	/**
	 * Private Constructor for Utility Class.
	 */
	private CalcDeps() {
	}

	/**
	 * The Logger.
	 */
	private static final Logger LOGGER = Logger.getLogger(CalcDeps.class);

	/**
	 * Build a list of dependencies from a list of files. Takes a list of files,
	 * extracts their provides and requires, and builds out a list of dependency
	 * objects.
	 * 
	 * @param files
	 *            a list of files to be parsed for goog.provides and
	 *            goog.requires.
	 * @return A list of dependency objects, one for each file in the files
	 *          argument.
	 * @throws IOException if there is a problem parsing the files for dependency info
	 */
	private static HashMap<File, DependencyInfo> buildDependenciesFromFiles(
			final File googleBaseJS, final Collection<File> files) throws IOException {
		HashMap<File, DependencyInfo> result = new HashMap<File, DependencyInfo>();
		Set<File> searchedAlready = new HashSet<File>();
		for (File file : files) {
			if (!searchedAlready.contains(file) && !file.equals(googleBaseJS)) {
				DependencyInfo dep = AnnotationFileReader
						.parseForDependencyInfo(file);
				result.put(file, dep);
				searchedAlready.add(file);
			}
		}
		return result;
	}

	/**
	 * Calculates the dependencies for given inputs.
	 * 
	 * This method takes a list of paths (files, directories) and builds a
	 * searchable data structure based on the namespaces that each .js file
	 * provides. It then parses through each input, resolving dependencies
	 * against this data structure. The final output is a list of files,
	 * including the inputs, that represent all of the code that is needed to
	 * compile the given inputs.
	 * 
	 * @param baseJs the base.js file that is in the closure library
	 * @param paths the references (files, directories) that are used to build the
	 *            dependency hash.
	 * @param inputs the inputs (files, directories, namespaces) that have
	 *            dependencies that need to be calculated.
	 * @return A list of all files, including inputs, that are needed to compile
	 *         the given inputs.
	 * @throws IOException if there is a problem parsing the files
	 */
	private static List<DependencyInfo> calculateDependencies(
			final File baseJs, final Collection<File> inputs, final Collection<File> paths)
			throws IOException {
		HashSet<File> temp = new HashSet<File>();
		temp.addAll(inputs);
		HashMap<File, DependencyInfo> inputHash = buildDependenciesFromFiles(baseJs, inputs);
		HashMap<File, DependencyInfo> searchHash = buildDependenciesFromFiles(baseJs, paths);
		LOGGER.info("Dependencies Calculated.");

		List<DependencyInfo> sortedDeps = slowSort(inputHash.values(),
				searchHash.values());
		LOGGER.info("Dependencies Sorted.");

		return sortedDeps;
	}

	/**
	 * Print out a deps.js file from a list of source paths.
	 * 
	 * @param sortedDeps The sorted list of dependencies
	 * @param outputFile The output file.
	 * @throws IOException if there is a problem writing the file
	 * @return True on success, false if it was unable to find the base path to
	 *          generate deps relative to.
	 */
	private static boolean outputDeps(final File googleBaseFile,
			final Collection<DependencyInfo> sortedDeps,
			final File outputFile) throws IOException {
		DirectoryIO.createDir(outputFile.getParentFile());
		FileWriter fw = new FileWriter(outputFile);
		BufferedWriter buff = new BufferedWriter(fw);

		buff.append("\n// This file was autogenerated by CalcDeps.java\n");
		for (DependencyInfo fileDep : sortedDeps) {
			if (fileDep != null) {
				buff.write(fileDep.toString(googleBaseFile));
				buff.write("\n");
				buff.flush();
			}
		}
		LOGGER.info("Deps file written.");

		return true;

	}

	/**
	 * Compare every element to one another. This is significantly slower than a
	 * merge sort, but guarantees that deps end up in the right order
	 * 
	 * @param inputs
	 *            the inputs to scan
	 * @param deps
	 *            the external dependencies
	 * @return the list of dependencyInfo objects
	 */
	private static List<DependencyInfo> slowSort(
			final Collection<DependencyInfo> inputs,
			final Collection<DependencyInfo> deps) {
		HashMap<String, DependencyInfo> searchSet = buildSearchList(deps);
		HashSet<File> seenList = new HashSet<File>();
		ArrayList<DependencyInfo> resultList = new ArrayList<DependencyInfo>();
		for (DependencyInfo input : inputs) {
			if (!seenList.contains(input.getFile())) {
				seenList.add(input.getFile());
				for (String require : input.getRequires()) {
					orderDependenciesForNamespace(require, searchSet, seenList,
							resultList);
				}
				resultList.add(input);
			}
		}
		return resultList;
	}

	/**
	 * Will order the Dependencies for a single namespace.
	 * 
	 * @param requireNamespace whether or not to require the namespace 
	 * @param searchSet the set to search through
	 * @param seenList the list of objects that have been seen already (which may be added to)
	 * @param resultList the resulting list which will be added to
	 */
	private static void orderDependenciesForNamespace(
			final String requireNamespace,
			final HashMap<String, DependencyInfo> searchSet,
			final HashSet<File> seenList, final ArrayList<DependencyInfo> resultList) {
		if (!searchSet.containsKey(requireNamespace)) {
			LOGGER.error("search set doesn't contain key '" + requireNamespace
					+ "'"); //TODO add the file that this comes from at a later point to improve clarity
		}
		DependencyInfo dep = searchSet.get(requireNamespace);
		if (!seenList.contains(dep.getFile())) {
			seenList.add(dep.getFile());
			for (String subRequire : dep.getRequires()) {
				orderDependenciesForNamespace(subRequire, searchSet,
						seenList, resultList);
			}
			resultList.add(dep);
		}
	}

	/**
	 * Build the search list for seraching for dependencies.
	 * 
	 * @param deps the external dependencies
	 * @return the list of external dependencies, hashed by the namespace
	 */
	private static HashMap<String, DependencyInfo> buildSearchList(
			final Collection<DependencyInfo> deps) {
		HashMap<String, DependencyInfo> returnVal = new HashMap<String, DependencyInfo>();
		for (DependencyInfo dep : deps) {
			for (String provide : dep.getProvides()) {
				returnVal.put(provide, dep);
			}
		}
		return returnVal;
	}

	/**
	 * convert the sortedDependency list into a list of files.
	 * 
	 * @param sortedDeps the sorted dependencies
	 * @return the sorted list of files
	 */
	private static List<File> pullFilesFromDeps(final List<DependencyInfo> sortedDeps) {
		ArrayList<File> returnVal = new ArrayList<File>();
		for (DependencyInfo dep : sortedDeps) {
			returnVal.add(dep.getFile());
		}
		return returnVal;
	}

	/**
	 * This will sort the list of dependencies, write a dependency file, and return the list of dependencies.
	 * 
	 * @param googleBaseFile the base.js file that is in the google closure library
	 * @param inputs the set of input files to parse for provides and requires
	 * @param paths to additional resources that will have provides and requires
	 * @param outputFile the output file to write to
	 * @return the list of calculated dependencies, just in case it is needed
	 * @throws IOException if there is a problem reading from any dependencies or writing the depenency file
	 */
	public static List<File> executeCalcDeps(final File googleBaseFile,
			final Collection<File> inputs, final Collection<File> paths, final File outputFile)
			throws IOException {
		LOGGER.debug("Finding Closure dependencies...");
		List<DependencyInfo> sortedDeps = calculateDependencies(googleBaseFile,
				inputs, paths);

		// create deps file
		LOGGER.debug("Outputting Closure dependency file...");
		outputDeps(googleBaseFile, sortedDeps, outputFile);

		LOGGER.debug("Closure dependencies created");
		return pullFilesFromDeps(sortedDeps);
	}

}