package it.unifi.ing.drivehub.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LayeringTest {
    private static final Path JAVA_ROOT = Path.of("src/main/java/it/unifi/ing/drivehub");

    @Test
    void domainDoesNotDependOnOuterLayers() throws IOException {
        assertNoImports(JAVA_ROOT.resolve("domain"), List.of(
                "it.unifi.ing.drivehub.business",
                "it.unifi.ing.drivehub.dao",
                "it.unifi.ing.drivehub.presentation",
                "javafx."));
    }

    @Test
    void businessDoesNotDependOnAdaptersOrUi() throws IOException {
        assertNoImports(JAVA_ROOT.resolve("business"), List.of(
                "it.unifi.ing.drivehub.dao.postgres",
                "it.unifi.ing.drivehub.presentation",
                "javafx."));
    }

    @Test
    void controllersDoNotAccessPersistenceDirectly() throws IOException {
        assertNoImports(JAVA_ROOT.resolve("presentation/controller"), List.of(
                "it.unifi.ing.drivehub.dao"));
    }

    private static void assertNoImports(Path packageRoot, List<String> forbidden) throws IOException {
        if (!Files.exists(packageRoot)) {
            return;
        }
        try (var files = Files.walk(packageRoot)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                for (String prefix : forbidden) {
                    assertFalse(text.contains("import " + prefix),
                            () -> source + " contains forbidden dependency " + prefix);
                }
            }
        }
    }
}
