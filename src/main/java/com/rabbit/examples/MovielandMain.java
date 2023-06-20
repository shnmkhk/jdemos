package com.rabbit.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.jline.reader.LineReader;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class MovielandMain {
	public static String read(String fileName) {

		Path path;
		StringBuilder data = new StringBuilder();
		Stream<String> lines = null;
		try {
			if (System.getProperty("banner.properties.file") != null) {
				path = Paths.get(System.getProperty("banner.properties.file"));
			} else {
				path = Paths.get(Thread.currentThread().getContextClassLoader().getResource(fileName).toURI());
			}
			lines = Files.lines(path);
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}

		lines.forEach(line -> data.append(line).append(System.lineSeparator()));
		lines.close();
		return data.toString();
	}

	public static void main(String[] args) throws IOException, URISyntaxException {
		Terminal terminal = TerminalBuilder.terminal();

		LineReader reader = LineReaderBuilder.builder().terminal(terminal)
				.completer(new StringsCompleter("describe", "create")).parser(new DefaultParser()).build();
		System.out.println(read("banner.properties"));

		while (true) {
			String line = reader.readLine("> ");
			if (line == null || line.equalsIgnoreCase("exit")) {
				break;
			}
			line = line.trim();
			reader.getHistory().add(line);

			if (line.equalsIgnoreCase("describe")) {
				Path path = Paths.get(".");
				System.out.println(getDirectoryHierarchy(path));
			} else if (line.equalsIgnoreCase("create")) {
				System.out.println("TBD");
			} else {
				System.out.println("Unknown command: " + line);
			}
		}
	}

	public static String getDirectoryHierarchy(Path path) {
		StringBuilder sb = new StringBuilder();
		try (Stream<Path> paths = Files.walk(path)) {
			paths.sorted().forEach(p -> {
				int depth = path.relativize(p).getNameCount();
				for (int i = 0; i < depth; i++) {
					sb.append(" ");
				}
				if (p.toFile().isDirectory()) {
					sb.append("/");
				}
				sb.append(p.getFileName()).append("\n");
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}