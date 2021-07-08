package org.openapitools.openapidiff.core.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openapitools.openapidiff.core.model.GlobalChange.Operation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.GlobalChange;

public class MyMarkdownRenderTest {

  private static final String MINIMAL_PARSABLE_OAS = "openapi: \"3.0.3\"";

  @Test
  public void globalChangeOrdering() {
    MyMardownRenderer render =
        new MyMardownRenderer()
            .addGlobal(
                Set.of(
                    new GlobalChange("board", Operation.add),
                    new GlobalChange("splinter", Operation.remove),
                    new GlobalChange("spike", Operation.remove),
                    new GlobalChange("outside", Operation.change),
                    new GlobalChange("decoration", Operation.add),
                    new GlobalChange("look", Operation.change),
                    new GlobalChange("junk", Operation.remove)));
    ChangedOpenApi diff = OpenApiCompare.fromContents(MINIMAL_PARSABLE_OAS, MINIMAL_PARSABLE_OAS);

    assertThat(render.render(diff))
        .isEqualTo(
            Joiner.on("\n")
                .join(
                    ImmutableList.of(
                        "# Global Changes",
                        "- Added property `board`",
                        "- Added property `decoration`",
                        "- Deleted property `junk`",
                        "- Deleted property `spike`",
                        "- Deleted property `splinter`",
                        "- Changed property `look`",
                        "- Changed property `outside`",
                        "")));
  }
}
