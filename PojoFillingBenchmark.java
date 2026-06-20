import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A simple Plain Old Java Object (POJO)
class User {
    private int id;
    private String name;
    private String email;

    public User() {} // Default constructor required for instantiation

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}

// Fills a POJO using Java Reflection API
class ReflectionPojoFiller {
    public <T> T fill(Class<T> pojoClass, Map<String, Object> data) throws Exception {
        T instance = pojoClass.getDeclaredConstructor().newInstance(); // Step 1: Instantiate the POJO
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            try {
                // Determine the correct parameter type for the setter (handles primitive int)
                Class<?> paramType = value.getClass();
                if (paramType == Integer.class) {
                    paramType = int.class;
                }
                // Step 2: Use Reflection to find the setter method
                java.lang.reflect.Method setter = pojoClass.getMethod(setterName, paramType);
                // Step 3: Invoke the setter method using reflection
                setter.invoke(instance, value);
            } catch (NoSuchMethodException e) {
                System.err.println("Reflection: No exact setter found for " + fieldName + " with type " + value.getClass().getName() + ". Error: " + e.getMessage());
            }
        }
        return instance;
    }
}

// Fills a POJO using MethodHandles (a higher-performance alternative to raw reflection, closer to bytecode manipulation concept)
class MethodHandlePojoFiller {
    // Cache for MethodHandles to avoid repeated lookup overhead, improving performance after first use
    private static final Map<Class<?>, Map<String, MethodHandle>> CACHED_SETTERS = new ConcurrentHashMap<>();

    public <T> T fill(Class<T> pojoClass, Map<String, Object> data) throws Throwable {
        T instance = pojoClass.getDeclaredConstructor().newInstance(); // Step 1: Instantiate the POJO
        Map<String, MethodHandle> setters = CACHED_SETTERS.computeIfAbsent(pojoClass, k -> new HashMap<>());
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

            MethodHandle setterHandle = setters.get(fieldName);
            if (setterHandle == null) {
                // Determine the correct parameter type for the setter (handles primitive int)
                Class<?> paramType = value.getClass();
                if (paramType == Integer.class) {
                    paramType = int.class;
                }
                MethodType mt = MethodType.methodType(void.class, paramType);
                try {
                    // Step 2: Use MethodHandles to find the setter method handle
                    setterHandle = lookup.findVirtual(pojoClass, setterName, mt);
                    setters.put(fieldName, setterHandle); // Cache the handle for future use
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    System.err.println("MethodHandles: Could not find setter " + setterName + " for field " + fieldName + " with type " + paramType.getName() + ". Error: " + e.getMessage());
                    continue;
                }
            }
            // Step 3: Invoke the method handle directly (much faster than reflection after lookup)
            setterHandle.invoke(instance, value);
        }
        return instance;
    }
}

// Main class for benchmarking the two POJO filling approaches
public class PojoFillingBenchmark {

    private static final int WARMUP_ITERATIONS = 1000; // Number of iterations to warm up the JVM and JIT compiler
    private static final int MEASUREMENT_ITERATIONS = 1_000_000; // Number of objects to fill for performance measurement

    public static void main(String[] args) throws Throwable {
        Map<String, Object> data = new HashMap<>();
        data.put("id", 123);
        data.put("name", "John Doe");
        data.put("email", "john.doe@example.com");

        System.out.println("--- Starting Warmup Phase ---");
        ReflectionPojoFiller reflectionFiller = new ReflectionPojoFiller();
        MethodHandlePojoFiller methodHandleFiller = new MethodHandlePojoFiller();

        // Warmup for Reflection
        long warmupRefStart = System.nanoTime();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            reflectionFiller.fill(User.class, data);
        }
        long warmupRefEnd = System.nanoTime();
        System.out.printf("Reflection Warmup: %d ms%n", (warmupRefEnd - warmupRefStart) / 1_000_000);

        // Warmup for MethodHandles (this phase also builds the MethodHandle cache)
        long warmupMHStart = System.nanoTime();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            methodHandleFiller.fill(User.class, data);
        }
        long warmupMHEnd = System.nanoTime();
        System.out.printf("MethodHandles Warmup: %d ms%n", (warmupMHEnd - warmupMHStart) / 1_000_000);

        System.out.println("\n--- Starting Measurement Phase (" + MEASUREMENT_ITERATIONS + " objects) ---");

        // Measure Reflection performance
        long startTimeReflection = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            reflectionFiller.fill(User.class, data);
        }
        long endTimeReflection = System.nanoTime();
        long durationReflection = (endTimeReflection - startTimeReflection) / 1_000_000; // Convert to milliseconds
        System.out.println("Reflection filling took: " + durationReflection + " ms.");

        // Measure MethodHandles performance
        long startTimeMethodHandles = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            methodHandleFiller.fill(User.class, data);
        }
        long endTimeMethodHandles = System.nanoTime();
        long durationMethodHandles = (endTimeMethodHandles - startTimeMethodHandles) / 1_000_000; // Convert to milliseconds
        System.out.println("MethodHandles filling took: " + durationMethodHandles + " ms.");

        // Verify one instance from each method to ensure correctness
        User userRef = reflectionFiller.fill(User.class, data);
        User userMH = methodHandleFiller.fill(User.class, data);
        System.out.println("\nExample Reflection filled object: " + userRef);
        System.out.println("Example MethodHandles filled object: " + userMH);

        System.out.println("\n--- Performance Summary ---");
        if (durationMethodHandles > 0) {
            System.out.printf("MethodHandles is %.2fx faster than Reflection.%n", (double)durationReflection / durationMethodHandles);
        } else {
            System.out.println("MethodHandles was extremely fast (duration 0 ms), cannot calculate ratio.");
        }
    }
}