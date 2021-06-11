package org.openapitools.openapidiff.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.MyMardownRenderer;
import org.openapitools.openapidiff.core.output.Render;

public class BatchDiffProcessor {

  public static void main(String[] args) throws IOException {
    Path root = Path.of("D:\\projects\\api_root\\");
    Files.list(root)
        .filter(Files::isDirectory)
        .forEach(BatchDiffProcessor::writeDiff);

  }

  private static void writeDiff(Path apiDir) {
    var original = apiDir.resolve("compare/v0.0.yaml");
    Predicate<Path> matcher = p -> p.toString().endsWith(".yaml");

    if(!Files.exists(original)) {
      System.out.printf("%s does not have original file. skipping\n", apiDir.toAbsolutePath());
      return;
    }
    try {
      Files.list(apiDir)
          .filter(matcher)
          .findFirst()
          .ifPresent(f -> {
            try {
              writeDiff(original, f);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    } catch (IOException e2) {
      e2.printStackTrace();
    }
  }

  private static void writeDiff(Path original, Path current) throws IOException {
    ChangedOpenApi result = OpenApiCompare.fromFiles(original.toFile(), current.toFile());

    Render r = new MyMardownRenderer();
    var into = current.getParent().resolve("changelog.md");
    System.out.printf("writing summary into %s\n", into.toAbsolutePath());

    try(Writer w = new FileWriter(into.toFile())) {
      w.write(r.render(result));
    }
  }

}
