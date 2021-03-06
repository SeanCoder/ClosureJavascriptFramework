package com.github.jlgrock.javascriptframework.closuretesting;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Abstract Closure Testing Mojo.
 */
public abstract class AbstractClosureTestingMojo extends AbstractMojo {
	/**
	 * The logger for this class.
	 */
	private static final Logger LOGGER = Logger
			.getLogger(AbstractClosureTestingMojo.class);

	/**
	 * The test source directory containing test class sources. TODO this should
	 * eventually be ${project.build.testSourceDirectory}, but the value from
	 * the maven model is not pulling in. Need to look into this.
	 * 
	 * @required
	 * @parameter default-value=
	 *            "${basedir}${file.separator}src${file.separator}test${file.separator}javascript"
	 */
	private File testSourceDirectory;

	/**
	 * Command line working directory. TODO this should eventually be
	 * ${project.build.testSourceDirectory}, but the value from the maven model
	 * is not pulling in. Need to look into this.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}javascriptFramework"
	 */
	private File frameworkTargetDirectory;

	/**
	 * The location of the closure library.
	 * 
	 * @parameter default-value=
	 *            "${project.build.directory}${file.separator}javascriptFramework${file.separator}closure-library"
	 */
	private File closureLibraryLocation;

	/**
	 * The file produced after running the dependencies and files through the
	 * compiler. This should match the name of the closure compiler.
	 * 
	 * @parameter default-value="${project.build.finalName}-min.js"
	 * @required
	 */
	private String compiledFilename;

	/**
	 * Set this to "true" to skip running tests, but still compile them. Its use
	 * is NOT RECOMMENDED, but quite convenient on occasion.
	 * 
	 * @parameter default-value="false"
	 */
	private boolean skipTests;

	/**
	 * If set to true, this forces the plug-in to generate and run the test
	 * cases on the compiled version of the code.
	 * 
	 * @parameter default-value="false"
	 */
	private boolean runTestsOnCompiled;

	/**
	 * A list of <exclude> elements specifying the tests (by pattern) that
	 * should be included in testing. When not specified and when the test
	 * parameter is not specified, the default includes will be<br>
	 * &lt;excludes&gt;<br>
	 * &lt;exclude&gt;**\/Test*.java&lt;/exclude&gt;<br>
	 * &lt;exclude&gt;*\/*Test.java&lt;/exclude&gt;<br>
	 * &lt;exclude&gt;**\/*TestCase.java&lt;/exclude&gt;<br>
	 * &lt;/excludes&gt;<br>
	 */
	private List<File> excludes;

	/**
	 * A list of <include> elements specifying the tests (by pattern) that
	 * should be included in testing. When not specified and when the test
	 * parameter is not specified, the default includes will be<br>
	 * &lt;includes&gt;\<br\>
	 * &lt;include&gt;**\/Test*.java&lt;/include&gt;<br>
	 * &lt;include&gt;**\/*Test.java&lt;/include&gt;<br>
	 * &lt;include&gt;**\/*TestCase.java&lt;/include&gt;<br>
	 * &lt;/includes&gt;<br>
	 */
	private List<File> includes;

	/**
	 * The string for the {preamble} of the testing harness.
	 * 
	 * @parameter default-value=""
	 */
	private String preamble = "";

	/**
	 * The string for the {prologue} of the testing harness.
	 * 
	 * @parameter default-value=""
	 */
	private String prologue = "";

	/**
	 * The string for the {epilogue} of the testing harness.
	 * 
	 * @parameter default-value=""
	 */
	private String epilogue = "";

	/**
	 * The maximum number of test case failures before failing the build. -1
	 * indicates unlimited.
	 * 
	 * @parameter default-value="5"
	 */
	private int maximumFailures;

	/**
	 * The maximum number of failures allowed before failing the build. By
	 * limiting this, it will speed up the build if there are many failures.
	 * 
	 * @parameter default-value="10"
	 */
	private long testTimeoutSeconds;

	/**
	 * @return the maximum number of failures allowed before failing the build.
	 *         By limiting this, it will speed up the build if there are many
	 *         failures.
	 */
	public final long getTestTimeoutSeconds() {
		return testTimeoutSeconds;
	}

	/**
	 * The maximum number of threads to spawn for running test files. This
	 * parameter may be any value in the range 1 -
	 * <code>Runtime.getRuntime().availableProcessors() - 1</code>. Any value
	 * outside of this range will result in the default (processor count - 1)
	 * number of threads. Setting this property to 1 will disable
	 * multi-threading and run tests serially.
	 * 
	 * @parameter default-value="-1"
	 */
	private int maxTestThreads;

	/**
	 * Gets the maximum number of configured test threads. If the configured
	 * value is &lt; 1, this method returns one less than the number of
	 * available processors, as returned by
	 * <code>Runtime.getRuntime().availableProcessors()</code>. Note that this
	 * calculation may change if this method is called multiple times as
	 * processors are made (un)available to this VM. This method will never
	 * return a value less than 1.
	 * 
	 * @return the maximum number of test threads
	 */
	public final int getMaxTestThreads() {
		int max;
		int restrictedMax = Math.max(1, Runtime.getRuntime()
				.availableProcessors() - 1);
		if (maxTestThreads < 1) {
			max = restrictedMax;
		} else if (maxTestThreads > restrictedMax) {
			LOGGER.warn(String
					.format("A maximum of %d test threads may be used on this system.  (%d requested)",
							restrictedMax, maxTestThreads));
			max = restrictedMax;
		} else {
			max = maxTestThreads;
		}
		return max;
	}

	/**
	 * @return the testSourceDirectory
	 */
	public final File getTestSourceDirectory() {
		return testSourceDirectory;
	}

	/**
	 * @return the frameworkTargetDirectory
	 */
	public final File getFrameworkTargetDirectory() {
		return frameworkTargetDirectory;
	}

	/**
	 * @return the closureLibrarylocation
	 */
	public final File getClosureLibrarylocation() {
		return closureLibraryLocation;
	}

	/**
	 * @return the skipTests
	 */
	public final boolean isSkipTests() {
		return skipTests;
	}

	/**
	 * @return the excludes
	 */
	public final List<File> getExcludes() {
		return excludes;
	}

	/**
	 * @return the includes
	 */
	public final List<File> getIncludes() {
		return includes;
	}

	/**
	 * @return the runTestsOnCompiled
	 */
	public final boolean isRunTestsOnCompiled() {
		return runTestsOnCompiled;
	}

	/**
	 * @return the compiledFilename
	 */
	public final String getCompiledFilename() {
		return compiledFilename;
	}

	/**
	 * @return the preamble block
	 */
	public final String getPreamble() {
		return preamble;
	}

	/**
	 * @return the prologue block
	 */
	public final String getPrologue() {
		return prologue;
	}

	/**
	 * @return the epilogue block
	 */
	public final String getEpilogue() {
		return epilogue;
	}

	@Override
	public abstract void execute() throws MojoExecutionException,
			MojoFailureException;

	/**
	 * @return the maximum number of failures allowed before failing the build.
	 *         By limiting this, it will speed up the build if there are many
	 *         failures.
	 */
	public final int getMaximumFailures() {
		return maximumFailures;
	}

}
