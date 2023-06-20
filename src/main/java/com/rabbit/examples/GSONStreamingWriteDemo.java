package com.rabbit.examples;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;

public class GSONStreamingWriteDemo {

	private static final String jsonAsStr = "{\"name\":\"John\",\"age\":31,\"city\":\"New York\"}";

	public static void main(String[] args) throws IOException {
		FileOutputStream fos = new FileOutputStream("output.json", false);
		OutputStreamWriter ow = new OutputStreamWriter(fos);
		final Person person = Person.of("John", 31, "New York");
		ow.write("[\n\t");

		for (int i = 0; i < 10000000; i++) {
			if (i != 0) {
				ow.write(",");
			}
			ow.write(String.format("%s%n", person.toJSON()));
		}
		ow.write("]");
		ow.flush();
	}
}

class Person implements Serializable {
	private String name;
	private int age;
	private String city;

	private Person(final String name, final int age, final String city) {
		this.name = name;
		this.age = age;
		this.city = city;
	}

	public static Person of(final String name, final int age, final String city) {
		return new Person(name, age, city);
	}

	public String toJSON() {
		return String.format("{%n\t\t\"name\":\"%s\", %n\t\t\"age\": \"%d\", %n\t\t\"city\": \"%s\"%n\t}", name, age, city);
	}
}
