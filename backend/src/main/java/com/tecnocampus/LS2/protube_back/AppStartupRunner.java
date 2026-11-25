package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.importer.VideoImporter;
import com.tecnocampus.LS2.protube_back.search.SearchIndexer;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AppStartupRunner implements ApplicationRunner {
    private static final Logger LOG =
            LoggerFactory.getLogger(AppStartupRunner.class);

    @Autowired
    VideoService videoService;

    // Example variables from our implementation.
    // Feel free to adapt them to your needs
    private final Environment env;
    private final Path rootPath;
    private final Boolean loadInitialData;
    private final VideoImporter importer;
    private final SearchIndexer searchIndexer;

    public AppStartupRunner(Environment env, VideoImporter importer, SearchIndexer searchIndexer) {
        this.env = env;
        final var rootDir = env.getProperty("pro_tube.store.dir");
        this.rootPath = Paths.get(rootDir);
        loadInitialData = env.getProperty("pro_tube.load_initial_data", Boolean.class);
        this.importer = importer;
        this.searchIndexer = searchIndexer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!importer.schemaReady()) {
            LOG.info("Sin tabla videos (perfil dev): no se importan vídeos");
            return;
        }
        int grabbed = importer.importDirectory(rootPath);
        int samples = env.getProperty("pro_tube.sample_videos", Boolean.class, false) ? importer.importSamples() : 0;
        LOG.info("Vídeos importados: {} del videoGrabber, {} de prueba", grabbed, samples);
        searchIndexer.reindex(); // don't wait for the next scheduled run to make them searchable
    }
}
