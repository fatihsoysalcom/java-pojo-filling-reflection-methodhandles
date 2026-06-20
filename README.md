# Java POJO Filling Reflection MethodHandles

This example demonstrates two approaches for dynamically populating Java Plain Old Java Objects (POJOs) from a map of data: using the Reflection API and using `java.lang.invoke.MethodHandles`. It includes a simple benchmark to compare their performance, illustrating that `MethodHandles` (which is closer to bytecode manipulation) can be significantly faster after an initial setup cost, especially for repeated operations.

## Language

`java`

## How to Run

1. Save the code as `PojoFillingBenchmark.java`.
2. Compile: `javac PojoFillingBenchmark.java`
3. Run: `java PojoFillingBenchmark`

## Original Article

This example accompanies the Turkish article: [Java POJO'ları Doldurma: Reflection mı, ClassFile API mı Daha Hızlı? Kapsamlı Bir Karşılaştırma](https://fatihsoysal.com/blog/java-pojolari-doldurma-reflection-mi-classfile-api-mi-daha-hizli-kapsamli-bir-karsilastirma/).

## License

MIT — see [LICENSE](LICENSE).
