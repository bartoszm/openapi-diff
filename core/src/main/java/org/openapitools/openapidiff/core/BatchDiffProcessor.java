package org.openapitools.openapidiff.core;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.GlobalChange;
import org.openapitools.openapidiff.core.model.GlobalChange.Operation;
import org.openapitools.openapidiff.core.output.MyMardownRenderer;
import org.openapitools.openapidiff.core.output.Render;

public class BatchDiffProcessor {

  private static final OpenAPIParser openApiParser = new OpenAPIParser();

  public static void main(String[] args) throws IOException {
    Path root = Path.of("D:\\projects\\api_root\\");

    Files.list(root)
        .filter(Files::isDirectory)
        .forEach(BatchDiffProcessor::writeDiff);

  }

  private static void writeDiff(Path apiDir) {

    Path original = null;
    try {
      original = findCompare(apiDir.resolve("compare")).orElseThrow();
    } catch (Exception e) {
      System.out.printf("%s does not have original file. skipping\n", apiDir.toAbsolutePath());
      return;
    }
    Predicate<Path> matcher = apiFile(original);

    try {
      var api = Files.list(apiDir)
          .filter(matcher)
          .findFirst();
      if(api.isPresent()) {
        writeDiff(original, api.get());
      } else {
        System.out.printf("no matching api file for %s", original);
      }
    } catch (IOException e2) {
      e2.printStackTrace();
    }
  }

  private static Predicate<Path> apiFile(Path original) {
    var oF = original.getFileName().toString();
    return p -> oF.startsWith(p.getFileName().toString());
  }

  private static Optional<Path> findCompare(Path compareDir) throws IOException {
    if(!Files.isDirectory(compareDir)) {
      return Optional.empty();
    }
    return Files.list(compareDir)
        .filter(f -> f.getFileName().toString().endsWith("v0.0.yaml"))
        .findFirst();
  }

  private static void writeDiff(Path original, Path current) throws IOException {
    var diff = diff(original, current);
    ChangedOpenApi result = diff.compare();

    Render r = new MyMardownRenderer()
        .addGlobal(diff.getObservedChanges());
    var into = current.getParent().resolve("changelog.md");
    System.out.printf("writing summary into %s\n", into.toAbsolutePath());

    try(Writer w = new FileWriter(into.toFile())) {
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
        .filter(new GlobalChange("@type", Operation.remove))
        .filter(new GlobalChange("@baseType", Operation.remove))
        .filter(new GlobalChange("@referredType", Operation.remove));
  }

  private static OpenAPI readLocation(Path location) {
    SwaggerParseResult result = openApiParser.readLocation(location.toAbsolutePath().toString(),
        null, new ParseOptions());
    return result.getOpenAPI();
  }

}
