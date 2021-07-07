package org.openapitools.openapidiff.cli;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.openapitools.openapidiff.core.FillteringDiff;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.GlobalChange;
import org.openapitools.openapidiff.core.output.MyMardownRenderer;
import org.openapitools.openapidiff.core.output.Render;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final OpenAPIParser openApiParser = new OpenAPIParser();

  private static final String DEFAULT_OAS_EXTENSION = "openapi.yaml";
  private static final String DEFAULT_ORIGINAL_YAML = "v0.0.yaml";
  private static final String DEFAULT_OUTPUT_MD = "changelog.md";

  static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String... args) {
    Options options = new Options();
    options.addOption(Option.builder("h").longOpt("help").desc("print this message").build());
    options.addOption(
        Option.builder("l")
            .longOpt("log")
            .hasArg()
            .argName("level")
            .desc("use given level for log (TRACE, DEBUG, INFO, WARN, ERROR, OFF). Default: ERROR")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("original")
            .hasArg()
            .argName("yaml_file_name")
            .desc(
                "the name of the original OpenAPI spec to be compared. Default: "
                    + DEFAULT_ORIGINAL_YAML)
            .build());
    options.addOption(
        Option.builder()
            .longOpt("output")
            .hasArg()
            .argName("output_file_name")
            .desc("the name of the output file. Default: " + DEFAULT_OUTPUT_MD)
            .build());

    // create the parser
    CommandLineParser parser = new DefaultParser();
    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      if (line.hasOption("h")) { // automatically generate the help statement
        printHelp(options);
        System.exit(0);
      }
      String logLevel = "ERROR";
      if (line.hasOption("log")) {
        logLevel = line.getOptionValue("log");
        if (!logLevel.equalsIgnoreCase("TRACE")
            && !logLevel.equalsIgnoreCase("DEBUG")
            && !logLevel.equalsIgnoreCase("INFO")
            && !logLevel.equalsIgnoreCase("WARN")
            && !logLevel.equalsIgnoreCase("ERROR")
            && !logLevel.equalsIgnoreCase("OFF")) {
          throw new ParseException(
              String.format(
                  "Invalid log level. Excepted: [TRACE, DEBUG, INFO, WARN, ERROR, OFF]. Given: %s",
                  logLevel));
        }
      }
      LogManager.getRootLogger().setLevel(Level.toLevel(logLevel));

      final String originalYaml = line.getOptionValue("original", DEFAULT_ORIGINAL_YAML);
      final String outputFileName = line.getOptionValue("output", DEFAULT_OUTPUT_MD);

      if (logger.isDebugEnabled()) {
        logger.debug(
            "Provided {} arguments: {}",
            line.getArgList().size(),
            String.join(",", line.getArgList()));
      }
      switch (line.getArgList().size()) {
        case 0:
          throw new ParseException("Missing arguments");
        case 1:
          Path root = Path.of(line.getArgList().get(0));
          logger.debug("Processing APIs in bulk in directory: {}", root.toAbsolutePath());
          Files.list(root)
              .filter(Files::isDirectory)
              .forEach(apiDir -> Main.writeDiff(apiDir, originalYaml, outputFileName));
          break;
        default:
          Path apiDir = Path.of(line.getArgList().get(0), line.getArgList().get(1));
          logger.debug("Processing single API in directory: {}", apiDir.toAbsolutePath());
          Main.writeDiff(apiDir, originalYaml, outputFileName);
      }

    } catch (ParseException e) {
      // oops, something went wrong
      System.err.println("Parsing failed. Reason: " + e.getMessage());
      printHelp(options);
      System.exit(2);
    } catch (Exception e) {
      System.err.println(
          "Unexpected exception. Reason: "
              + e.getMessage()
              + "\n"
              + ExceptionUtils.getStackTrace(e));
      System.exit(2);
    }
  }

  public static void printHelp(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("openapi-diff <api_root_dir> [<api_dir>]", options);
  }

  private static void writeDiff(Path apiDir, String originalYaml, String outputFileName) {
    Path original;
    try {
      original = findCompare(apiDir.resolve("compare"), originalYaml).orElseThrow();
    } catch (Exception e) {
      System.out.printf("Skipping: %s - does not have original file\n", apiDir.toAbsolutePath());
      return;
    }
    Predicate<Path> matcher = apiFile(original);

    try {
      var api = Files.list(apiDir).filter(matcher).findFirst();
      if (api.isPresent()) {
        writeDiff(original, api.get(), outputFileName);
      } else {
        System.out.printf("Failed:   no matching api file for %s\n", original);
      }
    } catch (IOException e2) {
      e2.printStackTrace();
    }
  }

  private static Predicate<Path> apiFile(Path original) {
    var oP = original.toFile().getPath();
    return p -> {
      final File f = p.toFile();
      return f.isFile()
          && f.getName().endsWith(DEFAULT_OAS_EXTENSION)
          && oP.startsWith(f.getParent());
    };
  }

  private static Optional<Path> findCompare(Path compareDir, String originalYamlName)
      throws IOException {
    if (!Files.isDirectory(compareDir)) {
      return Optional.empty();
    }
    return Files.list(compareDir)
        .filter(f -> f.getFileName().toString().endsWith(originalYamlName))
        .findFirst();
  }

  private static void writeDiff(Path original, Path current, String outputFileName)
      throws IOException {
    var diff = diff(original, current);
    ChangedOpenApi result = diff.compare();

    Render r = new MyMardownRenderer().addGlobal(diff.getObservedChanges());
    var into = current.getParent().resolve(outputFileName);
    System.out.printf(
        "Success:  %s produced for %s\n", into.toAbsolutePath(), current.getFileName());
    try (Writer w = new FileWriter(into.toFile())) {
      w.write(r.render(result));
    }
  }

  private static FillteringDiff diff(Path old, Path current) {
    var oldSpec = readLocation(old);
    var currentSpec = readLocation(current);
    return getFilteringDiff(oldSpec, currentSpec);
  }

  protected static FillteringDiff getFilteringDiff(OpenAPI oldSpec, OpenAPI currentSpec) {
    return new FillteringDiff(oldSpec, currentSpec)
        .filter(new GlobalChange("@type", GlobalChange.Operation.remove))
        .filter(new GlobalChange("@baseType", GlobalChange.Operation.remove))
        .filter(new GlobalChange("@referredType", GlobalChange.Operation.remove));
  }

  private static OpenAPI readLocation(Path location) {
    SwaggerParseResult result =
        openApiParser.readLocation(location.toAbsolutePath().toString(), null, new ParseOptions());
    return result.getOpenAPI();
  }
}
