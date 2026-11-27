package com.tecnocampus.LS2.protube_back;

import com.tecnocampus.LS2.protube_back.importer.VideoImporter;
import com.tecnocampus.LS2.protube_back.search.SearchIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AppStartupRunnerTest {
    private final VideoImporter importer = mock(VideoImporter.class);
    private final SearchIndexer indexer = mock(SearchIndexer.class);
    private final Environment env = mock(Environment.class);
    private AppStartupRunner runner;

    @BeforeEach
    void setUp() {
        when(env.getProperty("pro_tube.store.dir")).thenReturn("/data/videos");
        when(importer.schemaReady()).thenReturn(true);
        runner = new AppStartupRunner(env, importer, indexer);
    }

    @Test
    void removesSamplesWhenThereAreRealVideos() throws Exception {
        when(importer.hasRealVideos()).thenReturn(true);
        when(env.getProperty("pro_tube.sample_videos", Boolean.class, false)).thenReturn(true);
        runner.run(null);
        verify(importer).importDirectory(Paths.get("/data/videos"));
        verify(importer).removeSamples();
        verify(importer, never()).importSamples();
        verify(indexer).reindex();
    }

    @Test
    void importsSamplesWhenTheCatalogueIsEmpty() throws Exception {
        when(importer.hasRealVideos()).thenReturn(false);
        when(env.getProperty("pro_tube.sample_videos", Boolean.class, false)).thenReturn(true);
        runner.run(null);
        verify(importer).importSamples();
        verify(importer, never()).removeSamples();
        verify(indexer).reindex();
    }

    @Test
    void skipsSamplesWhenTheyAreDisabled() throws Exception {
        when(importer.hasRealVideos()).thenReturn(false);
        when(env.getProperty("pro_tube.sample_videos", Boolean.class, false)).thenReturn(false);
        runner.run(null);
        verify(importer, never()).importSamples();
        verify(importer, never()).removeSamples();
    }

    @Test
    void doesNothingWithoutTheVideosTable() throws Exception {
        when(importer.schemaReady()).thenReturn(false);
        runner.run(null);
        verify(importer, never()).importDirectory(any());
        verify(indexer, never()).reindex();
    }
}
