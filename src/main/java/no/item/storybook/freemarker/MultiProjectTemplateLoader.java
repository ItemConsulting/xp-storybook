package no.item.storybook.freemarker;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MultiProjectTemplateLoader extends MultiTemplateLoader {
  public MultiProjectTemplateLoader(List<String> baseDirPaths) {
    super(createFileTemplateLoaders(baseDirPaths));
  }

  private static TemplateLoader[] createFileTemplateLoaders(List<String> baseDirPaths) {
    return baseDirPaths.stream()
      .map(baseDir -> {
        try {
          return new FileTemplateLoader(new File(baseDir));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .toArray(TemplateLoader[]::new);
  }
}
