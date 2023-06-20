package com.rabbit.examples;

import org.fusesource.jansi.AnsiConsole;
import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

public class JansiDemo {

	public static void main(String[] args) throws InterruptedException {
		AnsiConsole.systemInstall();
		System.out.println(ansi().eraseScreen().fg(RED).a("Hello").fg(GREEN).a(" World").reset());
		Thread.sleep(500);
		System.out.println( ansi().eraseScreen().render("@|red Revised Hello|@ @|green World|@") );
	}

}
