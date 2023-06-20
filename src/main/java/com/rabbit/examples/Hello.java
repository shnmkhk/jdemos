package com.rabbit.examples;
public class Hello {
	public static void main(String args[]) {
		if (args.length > 0) {
			System.out.println(String.format("Hello, %s", args[0]));
		}
	}
}
