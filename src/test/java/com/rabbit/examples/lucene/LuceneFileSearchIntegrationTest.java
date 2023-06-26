package com.rabbit.examples.lucene;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LuceneFileSearchIntegrationTest {

	@Test
	public void givenSearchQueryWhenFetchedFileNamehenCorrect() throws IOException, URISyntaxException {
		String indexPath = "index";
		String dataPath = "data/file1.txt";

		Directory directory = FSDirectory.open(Paths.get(indexPath));
		LuceneFileSearch luceneFileSearch = new LuceneFileSearch(directory, new StandardAnalyzer());

		luceneFileSearch.addFileToIndex(dataPath);

		List<Document> docs = luceneFileSearch.searchFiles("contents", "consectetur");

		Assertions.assertEquals("file1.txt", docs.get(0).get("filename"));
	}

}