package net.caffeinemc.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.caffeinemc.gradle.GradleMixinConfigPlugin.LOGGER;


public abstract class CreateMixinConfigTask extends DefaultTask {

    @Option(option = "mixinParentPackage", description = "The parent of the mixin package. Mixins will be printed relative to the package.")
    String mixinParentPackage;
    @Option(option = "mixinPackagePrefix", description = "Name of the mixin package relative to the mixinParentPackage.")
    String mixinPackage = "mixin";
    @Option(option = "modShortName", description = "Short name of the mod.")
    String modShortName;
    @Option(option = "outputDirectoryForSummaryDocument", description = "Output directory for the summary markdown with all mixin rules and descriptions.")
    String outputDirectoryForSummaryDocument;

    @InputDirectory
    public abstract DirectoryProperty getInputFiles();

    @InputDirectory
    public abstract DirectoryProperty getIncludeFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void run() {
        var inputSourceSet = this.getInputFiles().get().getAsFile().toPath();
        var outputDirectory = this.getOutputDirectory().get().getAsFile().toPath();

        List<Path> inputFiles;

        try {
            inputFiles = Files.walk(inputSourceSet).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk input directory", e);
        }

        URL url = null;
        try {
            url = inputSourceSet.toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        ClassLoader loader = new URLClassLoader(new URL[]{url}, MixinConfigOption.class.getClassLoader());
        HashSet<String> mixinPackages = new HashSet<>();
        HashSet<String> mixinOptions = new HashSet<>();
        List<MixinRuleRepresentation> sortedMixinConfigOptions = inputFiles.stream().filter(path -> path.toFile().isFile())
                .map((Path inputFile) -> {
                    boolean isPackageInfo = inputFile.endsWith("package-info.class");
                    Path inputPackagePath = inputSourceSet.relativize(inputFile.getParent());
                    String inputPackageName = inputPackagePath.toString().replaceAll(Pattern.quote(inputPackagePath.getFileSystem().getSeparator()), ".");
                    String inputPackageClassName = inputPackageName + ".package-info";
                    if (inputPackageName.startsWith(mixinParentPackage + "." + mixinPackage + ".")) {
                        inputPackageName = inputPackageName.substring(mixinParentPackage.length() + 1);
                    } else {
                        return null;
                    }
                    if (!isPackageInfo) {
                        mixinPackages.add(inputPackageName);
                        return null;
                    }
                    try {
                        Package inputPackage = loader.loadClass(inputPackageClassName).getPackage();
                        MixinConfigOption[] inputPackageAnnotations = inputPackage.getAnnotationsByType(MixinConfigOption.class);
                        if (inputPackageAnnotations.length > 1) {
                            LOGGER.warn(inputPackagePath + " had multiple mixin config option annotations, only using first!");
                        }
                        if (inputPackageAnnotations.length > 0) {
                            MixinConfigOption option = inputPackageAnnotations[0];
                            mixinOptions.add(inputPackageName);
                            return new MixinRuleRepresentation(inputPackageName, option);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .sorted(Comparator.comparing(MixinRuleRepresentation::path))
                .collect(Collectors.toList());

        mixinPackages.removeAll(mixinOptions);
        StringBuilder errorMessage = new StringBuilder();
        for (String packageName : mixinPackages) {
            errorMessage.append("Mixin Package ").append(mixinPackage).append(".").append(packageName).append(" contains files without corresponding MixinConfigOption annotation in a package-info.java file!\n");
        }
        if (!errorMessage.isEmpty()) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }

        try {
            DefaultConfigCreator.writeDefaultConfig(this.modShortName, outputDirectory.resolve(this.modShortName.toLowerCase() + "-mixin-config-default.properties").toFile(), sortedMixinConfigOptions);
            DefaultConfigCreator.writeMixinDependencies(this.modShortName, outputDirectory.resolve(this.modShortName.toLowerCase() + "-mixin-config-dependencies.properties").toFile(), sortedMixinConfigOptions);
            DefaultConfigCreator.writeMixinConfigSummaryMarkdown(this.modShortName, Path.of(this.outputDirectoryForSummaryDocument).resolve(this.modShortName.toLowerCase() + "-mixin-config.md").toFile(), sortedMixinConfigOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    record MixinRuleRepresentation(String path, MixinConfigOption config) {

    }
}